package com.elyonut.wow.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.*
import com.elyonut.wow.transformer.MapboxTransformer
import com.mapbox.geojson.Feature
import com.elyonut.wow.analysis.ThreatAnalyzer
import com.elyonut.wow.analysis.TopographyService
import com.elyonut.wow.model.Threat
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

// Constant values
private const val DEFAULT_COLOR = Color.GRAY
private const val LOW_HEIGHT_COLOR = Color.YELLOW
private const val MIDDLE_HEIGHT_COLOR = Color.MAGENTA
private const val HIGH_HEIGHT_COLOR = Color.RED
private const val RECORD_REQUEST_CODE = 101

class MapViewModel(application: Application) : AndroidViewModel(application) {

    var selectLocationManual: Boolean = false // Why is it here? never changes
    private lateinit var map: MapboxMap
    private val tempDB = TempDB(application)
    private val permissions: IPermissions = PermissionsAdapter(getApplication())
    private var locationAdapter: ILocationManager? = null
    private val layerManager = LayerManager(tempDB)
    private val analyzer: IAnalyze = AnalyzeManager(layerManager)
    private val mapAdapter: MapAdapter = MapAdapter(layerManager)
    var selectedBuildingId = MutableLiveData<String>()
    var isPermissionRequestNeeded = MutableLiveData<Boolean>()
    var isAlertVisible = MutableLiveData<Boolean>()
    var noPermissionsToast = MutableLiveData<Toast>()
    var threatStatus = MutableLiveData<String>()
    var threats = MutableLiveData<ArrayList<Threat>>()
    var threatFeatures = MutableLiveData<List<Feature>>()

    fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.setStyle(getString(R.string.style_url)) { style ->
            locationSetUp(style)
//            initOfflineMap(style)
            setBuildingFilter(style)
            setSelectedBuildingLayer(style)
            addRadiusLayer(style)
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
            LocationAdapter(getApplication(), map.locationComponent, analyzer, threatStatus)

        if (!locationAdapter!!.isGpsEnabled()) {
            isAlertVisible.value = true
        }

        locationAdapter!!.startLocationService()
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
        val buildingLayer = loadedMapStyle.getLayer(getString(R.string.buildings_layer))
        (buildingLayer as FillExtrusionLayer).withProperties(
            PropertyFactory.fillExtrusionColor(
                Expression.step(
                    (Expression.get("height")), Expression.color(DEFAULT_COLOR),
                    Expression.stop(3, Expression.color(LOW_HEIGHT_COLOR)),
                    Expression.stop(10, Expression.color(MIDDLE_HEIGHT_COLOR)),
                    Expression.stop(100, Expression.color(HIGH_HEIGHT_COLOR))
                )
            ), PropertyFactory.fillExtrusionOpacity(0.5f)
        )
    }

    private fun setSelectedBuildingLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(Constants.selectedBuildingSourceId))
        loadedMapStyle.addLayer(
            FillLayer(
                Constants.selectedBuildingLayerId,
                Constants.selectedBuildingSourceId
            ).withProperties(PropertyFactory.fillExtrusionOpacity(0.7f))
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
            PropertyFactory.fillColor(
                Expression.step(
                    (Expression.get(Constants.threatProperty)), Expression.color(DEFAULT_COLOR),
                    Expression.stop(0, Expression.color(LOW_HEIGHT_COLOR)),
                    Expression.stop(0.4, Expression.color(MIDDLE_HEIGHT_COLOR)),
                    Expression.stop(0.7, Expression.color(HIGH_HEIGHT_COLOR))
                )
            ),
            PropertyFactory.fillOpacity(.4f),
            visibility(NONE)
        )

        loadedStyle.addLayerBelow(fillLayer, getString(R.string.buildings_layer))
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

    fun focusOnMyLocationClicked() {
        map.locationComponent.apply {
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }
    }

    private fun getString(stringName: Int): String {
        return getApplication<Application>().getString(stringName)
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
            (layer as FillLayer).setFilter(
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
            (layer as FillLayer).setFilter((Expression.eq(Expression.get(propertyId), value)))
        }

        fun filterLayerByNumericRange(
            style: Style,
            layerId: String,
            propertyId: String,
            minValue: Number,
            maxValue: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillLayer).setFilter(
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
            (layer as FillLayer).setFilter(
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
            (layer as FillLayer).setFilter(
                Expression.all(
                    Expression.lte(Expression.get(propertyId), maxValue)
                )
            )
        }

        fun removeFilter(style: Style, layerId: String) {
            val layer = style.getLayer(layerId)
            (layer as FillLayer).setFilter(Expression.literal(true))
        }
    }
}