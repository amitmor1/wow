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

    var selectLocationManual: Boolean = false // Why is it here? never changes
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
    lateinit var riskStatus: LiveData<RiskStatus>
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

    @SuppressLint("WrongConstant")  // TODO why wrongconstant?!
    fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.setStyle(Constants.MAPBOX_STYLE_URL) { style ->
            locationSetUp(style)
//            initOfflineMap(style)
//            setBuildingFilter(style)
            setSelectedBuildingLayer(style)
            addRadiusLayer(style)
            setThreatLayerOpacity(style, Constants.regularOpacity)
            circleSource = initCircleSource(style)
            fillSource = initFillSource(style)
            initCircleLayer(style)
            initFillLayer(style)
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
        riskStatus = locationAdapter!!.getRiskStatus()!!
        isLocationAdapterInitialized.value = true
        riskStatus.observeForever {
            if (riskStatus.value == RiskStatus.HIGH || riskStatus.value == RiskStatus.MEDIUM) {
                setThreatLayerOpacity(loadedMapStyle, Constants.HighOpacity)
            } else {
                setThreatLayerOpacity(loadedMapStyle, Constants.regularOpacity)
            }
        }
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
        val buildingLayer = loadedMapStyle.getLayer(Constants.buildingsLayerId)
        (buildingLayer as FillExtrusionLayer).withProperties(
            PropertyFactory.fillExtrusionColor(
                Expression.step(
                    (Expression.get("height")), Expression.color(RiskStatus.NONE.color),
                    Expression.stop(3, Expression.color(RiskStatus.LOW.color)),
                    Expression.stop(10, Expression.color(RiskStatus.MEDIUM.color)),
                    Expression.stop(100, Expression.color(RiskStatus.HIGH.color))
                )
            ), fillExtrusionOpacity(0.5f)
        )
    }

    private fun setThreatLayerOpacity(loadedMapStyle: Style, opacity: Float) {
        val threatLayer = loadedMapStyle.getLayer(Constants.constructionLayerId)
        (threatLayer as FillExtrusionLayer).withProperties(
            fillExtrusionOpacity(
                opacity
            )
        )
    }

    private fun setSelectedBuildingLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(Constants.selectedBuildingSourceId))
        loadedMapStyle.addLayer(
            FillLayer(
                Constants.selectedBuildingLayerId,
                Constants.selectedBuildingSourceId
            ).withProperties(fillExtrusionOpacity(0.7f))
        )
    }

    private fun addRadiusLayer(loadedStyle: Style) {
        createRadiusSource(loadedStyle)
        createRadiusLayer(loadedStyle)
    }

    private fun createRadiusSource(loadedStyle: Style) {
        val circleGeoJsonSource =
            GeoJsonSource(Constants.threatRadiusSourceId, getThreatRadiuses())
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
            Constants.threatRadiusLayerId,
            Constants.threatRadiusSourceId
        )
        fillLayer.setProperties(
            fillColor(
                Expression.step(
                    Expression.get(Constants.threatProperty),
                    Expression.color(RiskStatus.NONE.color),
                    Expression.stop(0, Expression.color(RiskStatus.LOW.color)),
                    Expression.stop(0.4, Expression.color(RiskStatus.MEDIUM.color)),
                    Expression.stop(0.7, Expression.color(RiskStatus.HIGH.color))
                )
            ),
            fillOpacity(.4f),
            visibility(NONE)
        )

        loadedStyle.addLayerBelow(fillLayer, Constants.buildingsLayerId)
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
//                loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.selectedBuildingSourceId)
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

    private fun initFillSource(loadedMapStyle: Style): GeoJsonSource {
        val fillFeatureCollection = makePolygonFeatureCollection(lineLayerPointList)
        val fillGeoJsonSource = GeoJsonSource(Constants.FILL_SOURCE_ID, fillFeatureCollection)
        loadedMapStyle.addSource(fillGeoJsonSource)

        return fillGeoJsonSource
    }

    private fun initFillLayer(loadedMapStyle: Style) {
        val fillLayer = FillLayer(
            Constants.FILL_LAYER_ID,
            Constants.FILL_SOURCE_ID
        )
        fillLayer.setProperties(
            fillOpacity(0.5f),
            fillColor(Color.parseColor("#ff0000")),
            fillOutlineColor(Color.RED)
        )
        loadedMapStyle.addLayerBelow(fillLayer, Constants.CIRCLE_LAYER_ID)
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