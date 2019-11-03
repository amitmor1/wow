package com.elyonut.wow

import android.location.Location
import androidx.lifecycle.LiveData
import java.util.*
import kotlin.collections.ArrayList

interface ILocationManager {
    fun startLocationService()
    fun isGpsEnabled(): Boolean
    fun cleanLocation()
    fun getCurrentLocation() : LiveData<Location?>
}