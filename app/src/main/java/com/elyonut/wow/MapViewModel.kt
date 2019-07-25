package com.elyonut.wow

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.pm.PackageManager
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

// Constant values
private const val DEFAULT_COLOR = Color.GRAY
private const val LOW_HEIGHT_COLOR = Color.YELLOW
private const val MIDDLE_HEIGHT_COLOR = Color.MAGENTA
private const val HIGH_HEIGHT_COLOR = Color.RED
private const val RECORD_REQUEST_CODE = 101

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var map: MapboxMap
    private val permissions: IPermissions = PermissionsAdapter(getApplication())
    private lateinit var locationAdapter: LocationAdapter
    var selectedBuildingId = MutableLiveData<String>()
    var isPermissionRequestNeeded = MutableLiveData<Boolean>()
    var isAlertVisible = MutableLiveData<Boolean>()
    var noPermissionsToast = MutableLiveData<Toast>()

    init {
//        val adapter = MapAdapter()

//        val model = BLModel(adapter)
    }

    fun onMapReady(mapboxMap: MapboxMap) {
//        model.onMapReady()

        map = mapboxMap
        map.setStyle(getString(R.string.style_url)) { style ->
            locationSetUp(style)
//            initOfflineMap(style)
            setBuildingFilter(style)
            setSelectedBuildingLayer(style)
        }
    }

    private fun locationSetUp(loadedMapStyle: Style) {
        if (permissions.checkLocationPermissions()) {
            startLocationService(loadedMapStyle)
        } else {
            isPermissionRequestNeeded.value = true
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationService(loadedMapStyle: Style) {
        val myLocationComponentOptions = LocationComponentOptions.builder(getApplication())
            .trackingGesturesManagement(true)
            .accuracyColor(ContextCompat.getColor(getApplication(), R.color.myLocationColor)).build()

        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(getApplication(), loadedMapStyle)
                .locationComponentOptions(myLocationComponentOptions).build()

        map.locationComponent.apply {
            activateLocationComponent(locationComponentActivationOptions)
            isLocationComponentEnabled = true
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }

        locationAdapter = LocationAdapter(getApplication(), map.locationComponent)

        if (!locationAdapter.isGpsEnabled()) {
            isAlertVisible.value = true
        }

        locationAdapter.startLocationService()
    }

    @SuppressLint("ShowToast")
    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        when (requestCode) {
            RECORD_REQUEST_CODE -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    noPermissionsToast.value = Toast.makeText(getApplication(), R.string.permission_not_granted, Toast.LENGTH_LONG)
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
        loadedMapStyle.addSource(GeoJsonSource(getString(R.string.selectedBuildingSourceId)))
        loadedMapStyle.addLayer(
            FillLayer(
                getString(R.string.selectedBuildingLayerId),
                getString(R.string.selectedBuildingSourceId)
            ).withProperties(PropertyFactory.fillExtrusionOpacity(0.7f))
        )
    }

//    fun getBuildingInfo(){
//        return transformToWowBuilding(loadedMapStyle.getSourceAs<GeoJsonSource>(getString(R.string.selectedBuildingSourceId))
//        selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features)))
//    }

    fun onMapClick(mapboxMap: MapboxMap, latLng: LatLng): Boolean {
//        model.onMapClick()
        val loadedMapStyle = mapboxMap.style

        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
            return false
        }

        val point = mapboxMap.projection.toScreenLocation(latLng)
        val features =
            mapboxMap.queryRenderedFeatures(point, getString(R.string.buildings_layer))

        if (features.size > 0) {
            selectedBuildingId.value = features.first().id()
            val selectedBuildingSource =
                loadedMapStyle.getSourceAs<GeoJsonSource>(getString(R.string.selectedBuildingSourceId))
            selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))
        }

        return true
    }

    fun focusOnMyLocation() {
        map.locationComponent.apply {
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }
    }

    private fun getString(stringName: Int): String {
        return getApplication<Application>().getString(stringName)
    }

    fun clean() {
        locationAdapter.cleanLocation()
    }
}

