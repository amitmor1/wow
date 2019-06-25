package com.elyonut.wow

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionColor
import org.json.JSONObject
import timber.log.Timber

class MainActivity : AppCompatActivity(), PermissionsListener, OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var locationManager: LocationManager

    // Constant values
    private var defaultColor = rgb(0,0,0)
    private var lowHeightColor = rgb(242, 241, 45)
    private var middleHeightColor = rgb(218, 156, 32)
    private var highHeightColor = rgb(255,0,0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.MAPBOX_ACCESS_TOKEN))
        setContentView(R.layout.activity_main)
        Timber.i("started app")
        mapView = findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        initLocationButton()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap

        mapboxMap.setStyle(getString(R.string.style_url)) { style ->
            startLocationService(style)
            initOfflineMap(style)
            setBuildingFilter(style)
        }
    }

    private fun setBuildingFilter(style: Style) {
        val buildingLayer =  style.getLayer("building")
        (buildingLayer as FillExtrusionLayer).withProperties(
            fillExtrusionColor(step((get("height")), defaultColor,
                stop(3,lowHeightColor),
                stop(10, middleHeightColor),
                stop(100, highHeightColor)))
        )
    }

    private fun initLocationButton() {
        val currentLocationButton: View = findViewById(R.id.currentLocation)
        currentLocationButton.setOnClickListener {
            map.locationComponent.apply {
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }
        }
    }

    private fun initOfflineMap(loadedMapStyle: Style) {

        val offlineManager = OfflineManager.getInstance(this@MainActivity)
        val definition = getDefinition(loadedMapStyle)
        val metadata = getMetadata()
        val callback = getOfflineRegionCallback()

        if (metadata != null) {
            offlineManager.createOfflineRegion(
                definition,
                metadata,
                callback
            )
        }
    }

    private fun getOfflineRegionCallback(): OfflineManager.CreateOfflineRegionCallback {

        return object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion?) {
                offlineRegion?.setDownloadState(OfflineRegion.STATE_ACTIVE)
                offlineRegion?.setObserver(getObserver())
            }

            override fun onError(error: String?) {
                Timber.e("Error: $error")
            }

        }
    }

    private fun getObserver(): OfflineRegion.OfflineRegionObserver {

        return object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                val percentage = if (status.requiredResourceCount >= 0)
                    100.0 * status.completedResourceCount / status.requiredResourceCount else 0.0

                if (status.isComplete) {
                    Timber.d("Region downloaded successfully.")
                } else if (status.isRequiredResourceCountPrecise) {
                    Timber.d(percentage.toString())
                }
            }

            override fun onError(error: OfflineRegionError) {
                Timber.e("onError reason: %s", error.reason)
                Timber.e("onError message: %s", error.message)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Timber.e("Mapbox tile count limit exceeded: $limit")
            }

        }
    }

    private fun getDefinition(loadedMapStyle: Style): OfflineRegionDefinition {

        // Create a bounding box for the offline region
        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(32.1826, 35.0110)) // Northeast
            .include(LatLng(31.9291, 34.5808)) // Southwest
            .build()

        return OfflineTilePyramidRegionDefinition(
            loadedMapStyle.url,
            latLngBounds,
            10.0,
            20.0,
            resources.displayMetrics.density
        )
    }

    private fun getMetadata(): ByteArray? {
        var metadata: ByteArray? = null
        try {
            val jsonObject = JSONObject()
            jsonObject.put(getString(R.string.json_field_region_name), getString(R.string.region_name))
            val json = jsonObject.toString()
            metadata = json.toByteArray(charset(getString(R.string.charset)))
        } catch (exception: Exception) {
            Timber.e("Failed to encode metadata: %s", exception.message)
        } finally {
            return metadata
        }
    }

    private fun startLocationService(loadedMapStyle: Style) {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        enableLocationComponent(loadedMapStyle)
        enableLocationService()
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        if ((PermissionsManager.areLocationPermissionsGranted(this))
            && (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {

            val myLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this, R.color.myLocationColor)).build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .locationComponentOptions(myLocationComponentOptions).build()

            map.locationComponent.apply {
                activateLocationComponent(locationComponentActivationOptions)
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun enableLocationService() {
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled) {
            AlertDialog.Builder(this).setTitle(getString(R.string.turn_on_location_title))
                .setMessage(getString(R.string.turn_on_location))
                .setPositiveButton(getString(R.string.yes_hebrew)) { dialog, id ->
                    val settingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(settingIntent)
                }.setNegativeButton(getString(R.string.no_thanks_hebrew)) { dialog, id ->
                    dialog.cancel()
                }.show()
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            if (map.style != null) {
                enableLocationComponent(map.style!!)
            }
        } else {
            Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }
}
