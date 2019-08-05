package com.elyonut.wow

import android.location.Location

interface ILocationManager {
    fun startLocationService()
    fun isGpsEnabled(): Boolean
    fun cleanLocation()
    fun getCurrentLocation() : Location?
}