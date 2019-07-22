package com.elyonut.wow

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.Style
import java.lang.ref.WeakReference

// Const values
private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

class LocationAdapter(
    var context: Context,
    private var locationComponent: LocationComponent,
    private var permissions: IPermissions
) : ILocationManager {
    //    private val permissions: IPermissions = PermissionsAdapter(context)
    private lateinit var locationManager: LocationManager
    private var locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(context)
    private var callback = LocationUpdatesCallback(locationComponent)


    override fun startLocationService() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        enableLocationService() // TODO FIX!!!!!!!
        initLocationEngine(context)
//        return enableLocationComponent()
    }

    fun enableLocationService(): Intent? {
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        return if (!gpsEnabled) {
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        } else {
            null
        }
    }

    override fun startLocationUpdates() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun enableLocationComponent(): Boolean {
        if (permissions.getLocationPermissions()) {

//            val myLocationComponentOptions = LocationComponentOptions.builder(context)
//                .trackingGesturesManagement(true)
//                .accuracyColor(ContextCompat.getColor(context, R.color.myLocationColor)).build()
//
//            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(context, loadedMapStyle)
//                .locationComponentOptions(myLocationComponentOptions).build()
//
//            locationComponent.apply {
//                activateLocationComponent(locationComponentActivationOptions)
//                isLocationComponentEnabled = true
//                cameraMode = CameraMode.TRACKING
//                renderMode = RenderMode.COMPASS
//            }

            initLocationEngine(context)
            return true
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine(context: Context) {
        val request: LocationEngineRequest = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()


        locationEngine.requestLocationUpdates(request, callback, context.mainLooper)
        locationEngine.getLastLocation(callback)
    }

    fun cleanLocation() {
        locationEngine.removeLocationUpdates(callback)
    }

    private class LocationUpdatesCallback(locationComponent: LocationComponent) :
        LocationEngineCallback<LocationEngineResult> {
        private var locationComponentWeakReference: WeakReference<LocationComponent> = WeakReference(locationComponent)

        override fun onSuccess(result: LocationEngineResult?) {

            val location: Location = result?.lastLocation ?: return
            locationComponentWeakReference.get()?.forceLocationUpdate(location)

//                if (activity.lastUpdatedLocation == null || ((activity.lastUpdatedLocation)?.longitude != location.longitude || (activity.lastUpdatedLocation)?.latitude != location.latitude)) {
//                    activity.calcRiskStatus(location)
//                }

//                    activity.lastUpdatedLocation = location
        }

        override fun onFailure(exception: java.lang.Exception) {
            val locationComponent = locationComponentWeakReference.get()
            if (locationComponent != null) {
                //log
            }
        }
    }
}