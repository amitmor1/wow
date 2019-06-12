package com.elyonut.wow

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.CompassEngine
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style

class MainActivity : AppCompatActivity(), PermissionsListener, OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.MAPBOX_ACCESS_TOKEN))
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            startLocationService(style)
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
            AlertDialog.Builder(this).setTitle("Location service settings")
                .setMessage("Location services are off, would you like to turn it on?")
                .setPositiveButton("Yes", DialogInterface.OnClickListener() { dialog, id ->
                    val settingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(settingIntent)
                }).setNegativeButton("No, thanks", DialogInterface.OnClickListener() { dialog, id ->
                    dialog.cancel()
                }).show()
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
