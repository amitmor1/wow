package com.elyonut.wow

import android.content.Context
import android.graphics.Color
import com.mapbox.mapboxsdk.geometry.LatLng
import java.util.*

//import com.mapbox.mapboxsdk.maps.Style


interface IMap {
    fun addLayer(layerId: String)

    fun removeLayer(layerId: String)

    fun colorFilter(layerId: String, colorsList: Dictionary<Int, Color>)

    fun initOfflineMap()
}