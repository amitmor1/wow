package com.elyonut.wow

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.support.v4.content.ContextCompat
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

class LocationAdapter(var context: Context, var locationComponent: LocationComponent): ILocationManager {
    private val permissions: IPermissions = PermissionsAdapter(context)
    private lateinit var locationManager: LocationManager
    private lateinit var locationEngine: LocationEngine

    override fun startLocationService() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        enableLocationComponent(loadedMapStyle)
//        enableLocationService()
    }

    override fun startLocationUpdates() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (permissions.getLocationPermissions()) {

            val myLocationComponentOptions = LocationComponentOptions.builder(context)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(context, R.color.myLocationColor)).build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(context, loadedMapStyle)
                .locationComponentOptions(myLocationComponentOptions).build()

            locationComponent.apply {
                activateLocationComponent(locationComponentActivationOptions)
//                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }

//            initLocationEngine()

        }
    }

    private fun initLocationEngine() {
//        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        val request: LocationEngineRequest = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()

//        locationEngine.requestLocationUpdates(request, callback, context.mainLooper)
//        locationEngine.getLastLocation(callback)
    }

    private class MainActivityLocationCallback(activity: MainActivity) : LocationEngineCallback<LocationEngineResult> {
        private var activityWeakReference: WeakReference<MainActivity> = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            val activity: MainActivity? = activityWeakReference.get()

            if (activity != null) {
                val location: Location = result?.lastLocation ?: return

//                if (activity.lastUpdatedLocation == null || ((activity.lastUpdatedLocation)?.longitude != location.longitude || (activity.lastUpdatedLocation)?.latitude != location.latitude)) {
//                    activity.calcRiskStatus(location)
//                }

                // Pass the new location to the Maps SDK's LocationComponent
                if (result.lastLocation != null) {
//                    activity.map.locationComponent.forceLocationUpdate(result.lastLocation)
//                    activity.lastUpdatedLocation = location
                }
            }
        }

        override fun onFailure(exception: java.lang.Exception) {
            val activity = activityWeakReference.get()
            if (activity != null) {
                Toast.makeText(
                    activity, exception.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}