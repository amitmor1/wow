package com.elyonut.wow

import android.content.Context
import android.location.LocationManager

class LocationAdapter(var context: Context): ILocationManager {
    private lateinit var locationManager: LocationManager

    override fun startLocationService() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun startLocationUpdates() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}