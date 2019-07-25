package com.elyonut.wow

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.location.LocationComponent
import java.lang.ref.WeakReference

// Const values
private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

class LocationAdapter(
    private var context: Context,
    locationComponent: LocationComponent
) : ILocationManager {
    private var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(context)
    private var callback = LocationUpdatesCallback(locationComponent)


    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun startLocationService() {
        initLocationEngine(context)
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
        }

        override fun onFailure(exception: java.lang.Exception) {
            val locationComponent = locationComponentWeakReference.get()
            if (locationComponent != null) {
                //log
            }
        }
    }
}