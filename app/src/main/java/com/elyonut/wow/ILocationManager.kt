package com.elyonut.wow

import android.location.Location
import androidx.lifecycle.LiveData

interface ILocationManager {
    fun startLocationService()
    fun isGpsEnabled(): Boolean
    fun cleanLocation()
    fun getCurrentLocation() : LiveData<Location?>
}