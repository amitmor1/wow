package com.elyonut.wow.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RectF
import android.location.Location
import android.os.AsyncTask
import android.util.ArrayMap
import android.view.Gravity
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.*
import com.elyonut.wow.adapter.LocationAdapter
import com.elyonut.wow.adapter.MapAdapter
import com.elyonut.wow.adapter.PermissionsAdapter
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.analysis.*
import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.model.Threat
import com.elyonut.wow.model.ThreatLevel
import com.elyonut.wow.transformer.MapboxParser
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import java.io.InputStream


private const val RECORD_REQUEST_CODE = 101

class MapViewModel(application: Application) : AndroidViewModel(application) {
    var selectLocationManual: Boolean = false
    var selectLocationManualConstruction: Boolean = false
    var selectLocationManualCoverage: Boolean = false
    var selectLocationManualCoverageAll: Boolean = false
    private lateinit var map: MapboxMap
    private val tempDB = TempDB(application)
    private val permissions: IPermissions =
        PermissionsAdapter(getApplication())
    internal var locationAdapter: ILocationManager? = null
    val layerManager = LayerManager(tempDB)
    private val mapAdapter: MapAdapter =
        MapAdapter(layerManager)
    var selectedBuildingId = MutableLiveData<String>()
    var isPermissionRequestNeeded = MutableLiveData<Boolean>()
    var isAlertVisible = MutableLiveData<Boolean>()
    var noPermissionsToast = MutableLiveData<Toast>()
    var riskStatus = MutableLiveData<RiskStatus>()
    var threats = MutableLiveData<ArrayList<Threat>>()
    var threatFeatures = MutableLiveData<List<Feature>>()
    val isLocationAdapterInitialized = MutableLiveData<Boolean>()
    private val logger: ILogger = TimberLogAdapter()
    var isAreaSelectionMode = false
    var areaOfInterest = MutableLiveData<Polygon>()
    var lineLayerPointList = ArrayList<Point>()
    private var currentLineLayerPointList = ArrayList<Point>()
    private var currentCircleLayerFeatureList = ArrayList<Feature>()
    private lateinit var circleSource: GeoJsonSource
    private lateinit var fillSource: GeoJsonSource
    private lateinit var firstPointOfPolygon: Point
    private lateinit var topographyService: TopographyService
    lateinit var threatAnalyzer: ThreatAnalyzer
    private var calcThreatsTask: CalcThreatStatusAsync? = null
    private var calcThreatCoverageTask: CalcThreatCoverageAsync? = null
    private var allCoverageTask: CalcThreatCoverageAllConstructionAsync? = null
    var threatAlerts = MutableLiveData<ArrayList<String>>()
    var isFocusedOnLocation = MutableLiveData<Boolean>()

    init {
        logger.initLogger()
    }

    @SuppressLint("WrongConstant")
    fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        topographyService = TopographyService(map)
        threatAnalyzer = ThreatAnalyzer(map, topographyService)
        map.setStyle(Constants.MAPBOX_STYLE_URL) { style ->
            addLayersToMapStyle(style)

            addThreatCoverageLayer(style)
            setBuildingFilter(style)
            setActiveThreatsLayer(style)
            setSelectedBuildingLayer(style)
            // addRadiusLayer(style)
            setThreatLayerOpacity(style, Constants.REGULAR_OPACITY)
            circleSource = initCircleSource(style)
            fillSource = initLineSource(style)
            initCircleLayer(style)
            initLineLayer(style)
            locationSetUp(style)

            map.uiSettings.compassGravity = Gravity.RIGHT
        }

        setCameraMoveListener()
    }

    private fun locationSetUp(loadedMapStyle: Style) {
        if (permissions.isLocationPermitted()) {
            startLocationService(loadedMapStyle)
        } else {
            isPermissionRequestNeeded.value = true
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationService(loadedMapStyle: Style) {
        val myLocationComponentOptions = LocationComponentOptions.builder(getApplication())
            .trackingGesturesManagement(true)
            .accuracyColor(ContextCompat.getColor(getApplication(), R.color.myLocationColor))
            .build()

        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(getApplication(), loadedMapStyle)
                .locationComponentOptions(myLocationComponentOptions).build()

        map.locationComponent.apply {
            activateLocationComponent(locationComponentActivationOptions)
            isLocationComponentEnabled = true
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }

        locationAdapter =
            LocationAdapter(
                getApplication(),
                map.locationComponent
            )


        if (!locationAdapter!!.isGpsEnabled()) {
            isAlertVisible.value = true
        }

        locationAdapter!!.startLocationService()
        isLocationAdapterInitialized.value = true
    }

    fun changeLocation(location: Location) {
        if (calcThreatsTask != null && calcThreatsTask!!.status != AsyncTask.Status.FINISHED) {
            return //Returning as the current task execution is not finished yet.
        }
        calcThreatsTask = CalcThreatStatusAsync(this, false)
        val latLng = LatLng(location.latitude, location.longitude)
        calcThreatsTask!!.execute(latLng)

    }

    fun checkRiskStatus() {
        val currentThreatsIds = getCurrentThreatIds()
        if (riskStatus.value == RiskStatus.HIGH) {
            threatAlerts.value = currentThreatsIds[ThreatLevel.High]
        }
    }

    private fun getCurrentThreatIds(): ArrayMap<ThreatLevel, ArrayList<String>> {
        val threatIds = ArrayMap<ThreatLevel, ArrayList<String>>()

        threatIds[ThreatLevel.Low] = ArrayList()
        threatIds[ThreatLevel.Medium] = ArrayList()
        threatIds[ThreatLevel.High] = ArrayList()

        threats.value?.forEach {
            threatIds[it.level]?.add(it.feature.id()!!)
        }

        return threatIds
    }

    @SuppressLint("ShowToast")
    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        when (requestCode) {
            RECORD_REQUEST_CODE -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    noPermissionsToast.value =
                        Toast.makeText(
                            getApplication(),
                            R.string.permission_not_granted,
                            Toast.LENGTH_LONG
                        )
                } else {
                    startLocationService((map.style!!))
                }
            }
        }
    }

    private fun setBuildingFilter(loadedMapStyle: Style) {
        val buildingLayer = loadedMapStyle.getLayer(Constants.BUILDINGS_LAYER_ID)
        (buildingLayer as FillExtrusionLayer).withProperties(
            fillExtrusionColor(
                step(
                    (get("height")), color(
                        Color.parseColor("#dbd3c1")
                    ),
                    stop(30, color(Color.parseColor("#ada799"))),
                    stop(60, color(Color.parseColor("#918c80"))),
                    stop(100, color(Color.parseColor("#615d55")))
                )
            ), fillExtrusionOpacity(0.5f)
        )
    }

    private fun setThreatLayerOpacity(loadedMapStyle: Style, opacity: Float) {
        val threatLayer = loadedMapStyle.getLayer(Constants.THREAT_LAYER_ID)
        (threatLayer as FillExtrusionLayer).withProperties(
            fillExtrusionOpacity(
                opacity
            )
        )
    }

    private fun setLayer(
        loadedMapStyle: Style,
        sourceId: String,
        layerId: String,
        opacity: Float = Constants.REGULAR_OPACITY
    ) {
        loadedMapStyle.addSource(GeoJsonSource(sourceId))
        loadedMapStyle.addLayer(
            FillLayer(
                layerId,
                sourceId
            ).withProperties(fillExtrusionOpacity(opacity))
        )
    }

    private fun setActiveThreatsLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(Constants.ACTIVE_THREATS_SOURCE_ID))
        loadedMapStyle.addLayer(
            FillExtrusionLayer(
                Constants.ACTIVE_THREATS_LAYER_ID,
                Constants.ACTIVE_THREATS_SOURCE_ID
            ).withProperties(
                fillExtrusionOpacity(Constants.HIGH_OPACITY),
                fillExtrusionHeight(get("height"))
            )
        )
    }

    private fun setSelectedBuildingLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(Constants.SELECTED_BUILDING_SOURCE_ID))
        loadedMapStyle.addLayer(
            FillExtrusionLayer(
                Constants.SELECTED_BUILDING_LAYER_ID,
                Constants.SELECTED_BUILDING_SOURCE_ID
            ).withProperties(
                fillExtrusionOpacity(Constants.HIGH_OPACITY),
                fillExtrusionColor(Color.parseColor("#870485")),
                fillExtrusionHeight(get("height"))
            )
        )
    }

    private fun addThreatCoverageLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(
            GeoJsonSource(
                Constants.THREAT_COVERAGE_SOURCE_ID,
                getCoveragePointsJson()
            )
        )
        val circleLayer = CircleLayer(
            Constants.THREAT_COVERAGE_LAYER_ID,
            Constants.THREAT_COVERAGE_SOURCE_ID
        )
        circleLayer.setProperties(
            circleRadius(4f),
            circleColor(Color.RED),
            circleBlur(1f),
            visibility(NONE)
        )

        loadedMapStyle.addLayer(circleLayer)
    }

    fun toggleThreatCoverage() {
        changeLayerVisibility(Constants.THREAT_COVERAGE_LAYER_ID)
    }

    private fun getCoveragePointsJson(): String {
        val stream: InputStream =
            App.resourses.assets.open("arlozerov_coverage.geojson") //TODO what is this file, and why not in constatns
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        val jsonObj = String(buffer, charset("UTF-8"))
        return jsonObj
    }

    private fun addRadiusLayer(loadedStyle: Style) {
        createRadiusSource(loadedStyle)
        createRadiusLayer(loadedStyle)
    }

    private fun createRadiusSource(loadedStyle: Style) {
        val circleGeoJsonSource =
            GeoJsonSource(Constants.THREAT_RADIUS_SOURCE_ID, getThreatRadiuses())
        loadedStyle.addSource(circleGeoJsonSource)
    }

    private fun getThreatRadiuses(): FeatureCollection {
        val threatRadiuses = mutableListOf<Feature>()

        mapAdapter.createThreatRadiusSource().forEach {
            threatRadiuses.add(MapboxParser.parseToMapboxFeature(it))
        }

        return FeatureCollection.fromFeatures(threatRadiuses)
    }

    private fun createRadiusLayer(loadedStyle: Style) {
        val fillLayer = FillLayer(
            Constants.THREAT_RADIUS_LAYER_ID,
            Constants.THREAT_RADIUS_SOURCE_ID
        )
        fillLayer.setProperties(
            fillColor(
                step(
                    get(Constants.THREAT_PROPERTY),
                    color(RiskStatus.NONE.color),
                    stop(0, color(RiskStatus.LOW.color)),
                    stop(0.4, color(RiskStatus.MEDIUM.color)),
                    stop(0.7, color(RiskStatus.HIGH.color))
                )
            ),
            fillOpacity(.4f),
            visibility(NONE)
        )

        loadedStyle.addLayer(fillLayer)
    }

    fun showRadiusLayerButtonClicked(layerId: String) {
        changeLayerVisibility(layerId)
    }

    fun layerSelected(layerId: String) {
        changeLayerVisibility(layerId)
    }

    private fun changeLayerVisibility(layerId: String) {
        val layer = map.style?.getLayer(layerId)
        if (layer != null) {
            if (layer.visibility.getValue() == VISIBLE) {
                layer.setProperties(visibility(NONE))
            } else {
                layer.setProperties(visibility(VISIBLE))
            }
        }
    }

    fun setLayerVisibility(layerId: String, visibility: PropertyValue<String>) {
        val layer = map.style?.getLayer(layerId)
        layer?.setProperties(visibility)
    }

    fun applyFilter(
        loadedStyle: Style,
        layerId: String,
        propertyId: String,
        isStringType: Boolean,
        numericType: NumericFilterTypes,
        stringValue: String,
        specificValue: Number,
        minValue: Number,
        maxValue: Number
    ) {
        if (isStringType) {
            FilterHandler.filterLayerByStringProperty(loadedStyle, layerId, propertyId, stringValue)
        } else {
            when (numericType) {
                NumericFilterTypes.RANGE -> {
                    FilterHandler.filterLayerByNumericRange(
                        loadedStyle,
                        layerId,
                        propertyId,
                        minValue,
                        maxValue
                    )
                }
                NumericFilterTypes.LOWER -> {
                    FilterHandler.filterLayerByMaxValue(loadedStyle, layerId, propertyId, maxValue)
                }
                NumericFilterTypes.GREATER -> {
                    FilterHandler.filterLayerByMinValue(loadedStyle, layerId, propertyId, minValue)
                }
                NumericFilterTypes.SPECIFIC -> {
                    FilterHandler.filterLayerBySpecificNumericProperty(
                        loadedStyle,
                        layerId,
                        propertyId,
                        specificValue
                    )
                }
            }
        }
    }

    fun removeFilter(style: Style, layerId: String) {
        FilterHandler.removeFilter(style, layerId)
    }


//    fun onMapClick(mapboxMap: MapboxMap, latLng: LatLng): Boolean {
////        model.onMapClick()
//        val loadedMapStyle = mapboxMap.style
//
//        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
//            return false
//        }
//
//        val point = mapboxMap.projection.toScreenLocation(latLng)
//        val features =
//            mapboxMap.queryRenderedFeatures(point, getString(R.string.buildings_layer))
//
//        if (features.size > 0) {
//            selectedBuildingId.value = features.first().id()
//            val selectedBuildingSource =
//                loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.SELECTED_BUILDING_SOURCE_ID)
//            selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))
//        }
//
//        return true
//    }

    fun drawPolygonMode(latLng: LatLng) {
        val mapTargetPoint = Point.fromLngLat(latLng.longitude, latLng.latitude)

        if (currentCircleLayerFeatureList.isEmpty()) {
            firstPointOfPolygon = mapTargetPoint
        }

        currentCircleLayerFeatureList.add(Feature.fromGeometry(mapTargetPoint))
        circleSource.setGeoJson(FeatureCollection.fromFeatures(currentCircleLayerFeatureList))

        when {
            currentCircleLayerFeatureList.size < 3 -> currentLineLayerPointList.add(mapTargetPoint)
            currentCircleLayerFeatureList.size == 3 -> {
                currentLineLayerPointList.add(mapTargetPoint)
                currentLineLayerPointList.add(firstPointOfPolygon)
            }
            else -> {
                currentLineLayerPointList.removeAt(currentCircleLayerFeatureList.size - 1)
                currentLineLayerPointList.add(mapTargetPoint)
                currentLineLayerPointList.add(firstPointOfPolygon)
            }
        }

        fillSource.setGeoJson(makePolygonFeatureCollection(currentLineLayerPointList))
    }

    fun removeAreaFromMap() {
        currentCircleLayerFeatureList = ArrayList()
        currentLineLayerPointList = ArrayList()
        circleSource.setGeoJson(FeatureCollection.fromFeatures(currentCircleLayerFeatureList))
        fillSource.setGeoJson(makeLineFeatureCollection(currentLineLayerPointList))
    }

    fun undo() {
        if (currentCircleLayerFeatureList.isNotEmpty()) {
            when {
                currentCircleLayerFeatureList.size < 3 -> {
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                }
                currentCircleLayerFeatureList.size == 3 -> {
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                }
                else -> {
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                    currentLineLayerPointList.add(currentLineLayerPointList[0])
                }
            }

            currentCircleLayerFeatureList.removeAt(currentCircleLayerFeatureList.size - 1)
            circleSource.setGeoJson(FeatureCollection.fromFeatures(currentCircleLayerFeatureList))
            fillSource.setGeoJson(makeLineFeatureCollection(currentLineLayerPointList))
        }
    }

    fun saveAreaOfInterest() {
        circleSource.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))
        lineLayerPointList = currentLineLayerPointList

        if (lineLayerPointList.isEmpty()) {
            areaOfInterest.value = null
        } else {
            areaOfInterest.value = Polygon.fromLngLats(listOf(lineLayerPointList))
        }
    }

    fun cancelAreaSelection() {
        currentCircleLayerFeatureList = ArrayList()
        currentLineLayerPointList = ArrayList()
        circleSource.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))
        fillSource.setGeoJson(makePolygonFeatureCollection(lineLayerPointList))
    }

    private fun makeLineFeatureCollection(pointArrayList: ArrayList<Point>): FeatureCollection {
        return FeatureCollection.fromFeatures(
            arrayOf(
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        pointArrayList
                    )
                )
            )
        )
    }

    private fun makePolygonFeatureCollection(pointArrayList: ArrayList<Point>): FeatureCollection {
        return FeatureCollection.fromFeatures(
            arrayOf(
                Feature.fromGeometry(
                    Polygon.fromLngLats(
                        listOf(pointArrayList)
                    )
                )
            )
        )
    }

    private fun initCircleSource(loadedMapStyle: Style): GeoJsonSource {
        val circleFeatureCollection = FeatureCollection.fromFeatures(ArrayList())
        val circleGeoJsonSource = GeoJsonSource(Constants.CIRCLE_SOURCE_ID, circleFeatureCollection)
        loadedMapStyle.addSource(circleGeoJsonSource)

        return circleGeoJsonSource
    }

    private fun initCircleLayer(loadedMapStyle: Style) {
        val circleLayer = CircleLayer(
            Constants.CIRCLE_LAYER_ID,
            Constants.CIRCLE_SOURCE_ID
        )
        circleLayer.setProperties(
            circleRadius(7f),
            circleColor(Color.WHITE)
        )

        loadedMapStyle.addLayer(circleLayer)
    }

    private fun initLineSource(loadedMapStyle: Style): GeoJsonSource {
        val lineFeatureCollection = makePolygonFeatureCollection(lineLayerPointList)
        val lineGeoJsonSource = GeoJsonSource(Constants.LINE_SOURCE_ID, lineFeatureCollection)
        loadedMapStyle.addSource(lineGeoJsonSource)

        return lineGeoJsonSource
    }

    private fun initLineLayer(loadedMapStyle: Style) {
        val lineLayer = LineLayer(
            Constants.LINE_LAYER_ID,
            Constants.LINE_SOURCE_ID
        )

        lineLayer.setProperties(
            lineColor(Color.parseColor("#494949")),
            lineWidth(2.5f)
        )

        loadedMapStyle.addLayerBelow(lineLayer, Constants.CIRCLE_LAYER_ID)
    }

    fun focusOnMyLocationClicked() {
        map.locationComponent.apply {
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }
    }

    fun clean() {
        locationAdapter?.cleanLocation()
    }

    fun updateThreatFeaturesBuildings(mapView: MapView, latLng: LatLng) {

        val boundingBox = RectF(
            mapView.left.toFloat(),
            mapView.top.toFloat(),
            mapView.right.toFloat(),
            mapView.bottom.toFloat()
        )

        threatFeatures.value = threatAnalyzer.getThreatFeaturesBuildings(latLng, boundingBox)
    }

    fun updateThreatFeaturesConstruction(latLng: LatLng) {

        if (calcThreatsTask != null && calcThreatsTask!!.status != AsyncTask.Status.FINISHED) {
            return //Returning as the current task execution is not finished yet.
        }
        calcThreatsTask = CalcThreatStatusAsync(this, true)

        calcThreatsTask!!.execute(latLng).get()
    }

    fun calculateCoverageFromPoint(
        latLng: LatLng,
        coverageRangeMeters: Double,
        coverageResolutionMeters: Double,
        coverageSearchHeightMeters: Double,
        progressBar: ProgressBar
    ) {

        if (calcThreatCoverageTask != null && calcThreatCoverageTask!!.status != AsyncTask.Status.FINISHED) {
            return //Returning as the current task execution is not finished yet.
        }


        calcThreatCoverageTask = CalcThreatCoverageAsync(this, progressBar)
        calcThreatCoverageTask!!.execute(
            ThreatCoverageData(
                latLng,
                coverageRangeMeters,
                coverageResolutionMeters,
                coverageSearchHeightMeters
            )
        )
    }

    fun calculateCoverageForAll(
        latLng: LatLng,
        coverageRangeMeters: Double,
        coverageResolutionMeters: Double,
        coverageSearchHeightMeters: Double,
        progressBar: ProgressBar
    ) {

        if (allCoverageTask != null && allCoverageTask!!.status != AsyncTask.Status.FINISHED) {
            return //Returning as the current task execution is not finished yet.
        }

        allCoverageTask = CalcThreatCoverageAllConstructionAsync(this, progressBar)
        allCoverageTask!!.execute(
            ThreatCoverageData(
                latLng,
                coverageRangeMeters,
                coverageResolutionMeters,
                coverageSearchHeightMeters
            )
        )
    }

    fun buildingThreatToCurrentLocation(building: Feature): Threat {
        val location = locationAdapter?.getCurrentLocation()!!.value
        val currentLocation = LatLng(location!!.latitude, location.longitude)

        val threatCoordinates = topographyService.getGeometryCoordinates(building.geometry()!!)
        val threatHeight = building.getNumberProperty("height").toDouble()
        val isLOS = topographyService.isLOS(
            Coordinate(
                currentLocation.latitude,
                currentLocation.longitude
            ), threatCoordinates, threatHeight
        )

        return threatAnalyzer.featureToThreat(building, currentLocation, isLOS)
    }

    fun setZoomLocation(threatID: String) {
        val location = layerManager.getFeatureLocation(threatID)
        val position = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(17.0)
            .build()

        map.locationComponent.cameraMode = CameraMode.NONE
        map.easeCamera(
            CameraUpdateFactory
                .newCameraPosition(position)
        )
    }

    fun getFeatureName(threatID: String): String {
        return layerManager.getFeatureName(threatID)
    }

    private fun setCameraMoveListener() {
        map.addOnCameraMoveListener {
            val cameraLocation =
                LatLng(map.cameraPosition.target.latitude, map.cameraPosition.target.longitude)
            val currentLocation = LatLng(
                locationAdapter?.getCurrentLocation()!!.value!!.latitude,
                locationAdapter?.getCurrentLocation()!!.value!!.longitude
            )
            isFocusedOnLocation.value =
                cameraLocation.distanceTo(currentLocation) <= Constants.MAX_DISTANCE_TO_CURRENT_LOCATION
        }
    }

    private fun addLayersToMapStyle(style: Style) {
        layerManager.layers?.forEach { layerModel ->
            val features = layerModel.features.map { featureModel ->
                MapboxParser.parseToMapboxFeature(featureModel)
            }

            val layerGeoJsonSource =
                GeoJsonSource(layerModel.id, FeatureCollection.fromFeatures(features))
            style.addSource(layerGeoJsonSource)

            val layer: Layer = if (layerModel.id == Constants.THREAT_LAYER_ID) {
                FillExtrusionLayer(layerModel.id, layerModel.id).withProperties(
                    fillExtrusionColor(Color.RED), fillExtrusionOpacity(0.5f),
                    fillExtrusionHeight(get("height"))
                )
            } else {
                FillLayer(layerModel.id, layerModel.id)
            }

            style.addLayerAt(layer, style.layers.size - 1)
        }
    }
}

class FilterHandler {
    companion object {
        fun filterLayerByStringProperty(
            style: Style,
            layerId: String,
            propertyId: String,
            type: String
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(eq(get(propertyId), type))
            )
        }

        fun filterLayerBySpecificNumericProperty(
            style: Style,
            layerId: String,
            propertyId: String,
            value: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                (eq(
                    get(propertyId),
                    value
                ))
            )
        }

        fun filterLayerByNumericRange(
            style: Style,
            layerId: String,
            propertyId: String,
            minValue: Number,
            maxValue: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(
                    gte(
                        get(propertyId),
                        minValue
                    ), lte(get(propertyId), maxValue)
                )
            )
        }

        fun filterLayerByMinValue(
            style: Style,
            layerId: String,
            propertyId: String,
            minValue: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(
                    gte(
                        get(propertyId),
                        minValue
                    )
                )
            )
        }

        fun filterLayerByMaxValue(
            style: Style,
            layerId: String,
            propertyId: String,
            maxValue: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(
                    lte(get(propertyId), maxValue)
                )
            )
        }

        fun removeFilter(style: Style, layerId: String) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(literal(true))
        }
    }
}
