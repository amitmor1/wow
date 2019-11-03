package com.elyonut.wow.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.*
import com.elyonut.wow.adapter.LocationAdapter
import com.elyonut.wow.adapter.MapAdapter
import com.elyonut.wow.adapter.PermissionsAdapter
import com.elyonut.wow.analysis.ThreatAnalyzer
import com.elyonut.wow.analysis.TopographyService
import com.elyonut.wow.model.Threat
import com.elyonut.wow.transformer.MapboxTransformer
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection

private const val RECORD_REQUEST_CODE = 101

class MapViewModel(application: Application) : AndroidViewModel(application) {

    var selectLocationManual: Boolean = false // Why is it here? never changes in the view model
    private lateinit var map: MapboxMap
    private val tempDB = TempDB(application)
    private val permissions: IPermissions =
        PermissionsAdapter(getApplication())
    private var locationAdapter: ILocationManager? = null
    private val layerManager = LayerManager(tempDB)
    private val analyzer: IAnalyze = AnalyzeManager(layerManager)
    private val mapAdapter: MapAdapter =
        MapAdapter(layerManager)
    var selectedBuildingId = MutableLiveData<String>()
    var isPermissionRequestNeeded = MutableLiveData<Boolean>()
    var isAlertVisible = MutableLiveData<Boolean>()
    var noPermissionsToast = MutableLiveData<Toast>()
    lateinit var riskStatusDetails: LiveData<Pair<RiskStatus, HashMap<RiskStatus, ArrayList<String>>>>
    var threats = MutableLiveData<ArrayList<Threat>>()
    var threatFeatures = MutableLiveData<List<Feature>>()
    val isLocationAdapterInitialized = MutableLiveData<Boolean>()
    var isAreaSelectionMode = false
    var areaOfInterest = MutableLiveData<Polygon>()
    var lineLayerPointList = ArrayList<Point>()
    private var currentLineLayerPointList = ArrayList<Point>()
    private var currentCircleLayerFeatureList = ArrayList<Feature>()
    private lateinit var circleSource: GeoJsonSource
    private lateinit var fillSource: GeoJsonSource
    private lateinit var firstPointOfPolygon: Point
    var isInsideThreatArea = MutableLiveData<Boolean>()
    var threatIdsByStatus = HashMap<RiskStatus, ArrayList<String>>()

    @SuppressLint("WrongConstant")  // TODO why wrongconstant?!
    fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.setStyle(Constants.MAPBOX_STYLE_URL) { style ->
            locationSetUp(style)
//            initOfflineMap(style)
//            setBuildingFilter(style)
            isInsideThreatArea.value = false
            addLayers(style)
            addRadiusLayer(style)
            circleSource = initCircleSource(style)
            fillSource = initLineSource(style)
            initCircleLayer(style)
            initLineLayer(style)
        }
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
                map.locationComponent,
                analyzer
            )


        if (!locationAdapter!!.isGpsEnabled()) {
            isAlertVisible.value = true
        }

        locationAdapter!!.startLocationService()
        initRiskStatus(loadedMapStyle)

    }

    private fun initRiskStatus(loadedMapStyle: Style) {
        riskStatusDetails = locationAdapter!!.getRiskStatusDetails()
        isLocationAdapterInitialized.value = true
        riskStatusDetails.observeForever {
            if (riskStatusDetails.value?.first == RiskStatus.HIGH || riskStatusDetails.value?.first == RiskStatus.MEDIUM) {
                val features = ArrayList<Feature>()
                riskStatusDetails.value?.second?.get(RiskStatus.HIGH)?.forEach { id ->
                    val c = loadedMapStyle.getSourceAs<GeoJsonSource>("construction")
                    val v = c?.querySourceFeatures(Expression.all())
                    val currentThreat = threatFeatures.value?.find { threatFeature ->
                        //threatFeatures ???
                        threatFeature.id() == id
                    }

                    if (currentThreat != null) {
                        features.add(currentThreat)
                    }
                }
                val currentThreateningBuildingSource =
                    loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.SELECTED_BUILDING_SOURCE_ID)
                currentThreateningBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features as MutableList<Feature>))
//                setLayerOpacity(
//                    loadedMapStyle,
//                    Constants.HIGH_OPACITY,
//                    Constants.CURRENT_THREATENING_BUILDINGS_LAYER_ID
//                )
            } else {
//                setLayerOpacity(
//                    loadedMapStyle,
//                    Constants.REGULAR_OPACITY,
//                    Constants.CURRENT_THREATENING_BUILDINGS_LAYER_ID
//                )
            }

            if (riskStatusDetails.value?.first == RiskStatus.HIGH) {
                if (threatIdsByStatus.isEmpty() || (threatIdsByStatus != riskStatusDetails.value?.second!!)) {
                    isInsideThreatArea.value = true
                    threatIdsByStatus = riskStatusDetails.value?.second!!
                }
            }
        }
    }

    //    private fun setBuildingFilter(loadedMapStyle: Style) {
//        val buildingLayer = loadedMapStyle.getLayer(Constants.BUILDINGS_LAYER_ID)
//        (buildingLayer as FillExtrusionLayer).withProperties(
//            fillExtrusionColor(
//                Expression.step(
//                    (Expression.get("height")), Expression.color(RiskStatus.NONE.color),
//                    Expression.stop(3, Expression.color(RiskStatus.LOW.color)),
//                    Expression.stop(10, Expression.color(RiskStatus.MEDIUM.color)),
//                    Expression.stop(100, Expression.color(RiskStatus.HIGH.color))
//                )
//            ), fillExtrusionOpacity(0.5f)
//        )

    private fun addLayers(loadedMapStyle: Style) {
        setLayer(
            loadedMapStyle,
            Constants.SELECTED_BUILDING_SOURCE_ID,
            Constants.SELECTED_BUILDING_LAYER_ID,
            0.7f
        )
        setLayer(
            loadedMapStyle,
            Constants.CURRENT_THREATENING_BUILDINGS_SOURCE_ID,
            Constants.CURRENT_THREATENING_BUILDINGS_LAYER_ID,
            Constants.HIGH_OPACITY
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
            ).withProperties(fillExtrusionOpacity(opacity), fillColor(Color.RED))
        )
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

//    }

    private fun setLayerOpacity(loadedMapStyle: Style, opacity: Float, layerId: String) {
        val layer = loadedMapStyle.getLayer(layerId)
        (layer as FillExtrusionLayer?)?.withProperties(
            fillExtrusionOpacity(
                opacity
            )
        )
    }

//    private fun setSelectedBuildingLayer(loadedMapStyle: Style) {
//        loadedMapStyle.addSource(GeoJsonSource(Constants.SELECTED_BUILDING_SOURCE_ID))
//        loadedMapStyle.addLayer(
//            FillLayer(
//                Constants.SELECTED_BUILDING_LAYER_ID,
//                Constants.SELECTED_BUILDING_SOURCE_ID
//            ).withProperties(fillExtrusionOpacity(0.7f))
//        )
//    }

//    private fun setCurrentThreateningBuildingLayer(loadedMapStyle: Style) {
//        loadedMapStyle.addSource(GeoJsonSource(Constants.CURRENT_THREATENING_BUILDINGS_SOURCE_ID))
//        loadedMapStyle.addLayer(
//            FillLayer(
//                Constants.CURRENT_THREATENING_BUILDINGS_LAYER_ID,
//                Constants.CURRENT_THREATENING_BUILDINGS_SOURCE_ID
//            ).withProperties(fillExtrusionOpacity(Constants.HIGH_OPACITY))
//        )
//    }

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
            threatRadiuses.add(MapboxTransformer.transfromFeatureModelToMapboxFeature(it))
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
                Expression.step(
                    Expression.get(Constants.THREAT_PROPERTY),
                    Expression.color(RiskStatus.NONE.color),
                    Expression.stop(0, Expression.color(RiskStatus.LOW.color)),
                    Expression.stop(0.4, Expression.color(RiskStatus.MEDIUM.color)),
                    Expression.stop(0.7, Expression.color(RiskStatus.HIGH.color))
                )
            ),
            fillOpacity(.4f),
            visibility(NONE)
        )

//        loadedStyle.addLayerBelow(fillLayer, Constants.BUILDINGS_LAYER_ID)???????????????????????
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

    fun updateThreats(mapView: MapView) {
        val ta = ThreatAnalyzer(mapView, map)
        val location = locationAdapter?.getCurrentLocation()
        val currentLocation = LatLng(location!!.latitude, location.longitude)
        threats.value = ta.getThreats(currentLocation)
    }

    fun updateThreatFeatures(mapView: MapView) {
        val ta = ThreatAnalyzer(mapView, map)
        val location = locationAdapter?.getCurrentLocation()
        val currentLocation = LatLng(location!!.latitude, location.longitude)
        threatFeatures.value = ta.getThreatFeatures(currentLocation)
    }

    fun updateThreatFeatures(mapView: MapView, latLng: LatLng) {
        val ta = ThreatAnalyzer(mapView, map)
        threatFeatures.value = ta.getThreatFeatures(latLng)
    }

    fun buildingThreatToCurrentLocation(mapView: MapView, building: Feature): Threat {
        val location = locationAdapter?.getCurrentLocation()
        val currentLocation = LatLng(location!!.latitude, location.longitude)
        val topographyService = TopographyService(map)
        val isLOS = topographyService.isLOS(currentLocation, building)

        val ta = ThreatAnalyzer(mapView, map)

        return ta.featureToThreat(building, currentLocation, isLOS)
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
                Expression.all(Expression.eq(Expression.get(propertyId), type))
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
                (Expression.eq(
                    Expression.get(propertyId),
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
                Expression.all(
                    Expression.gte(
                        Expression.get(propertyId),
                        minValue
                    ), Expression.lte(Expression.get(propertyId), maxValue)
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
                Expression.all(
                    Expression.gte(
                        Expression.get(propertyId),
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
                Expression.all(
                    Expression.lte(Expression.get(propertyId), maxValue)
                )
            )
        }

        fun removeFilter(style: Style, layerId: String) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(Expression.literal(true))
        }
    }
}