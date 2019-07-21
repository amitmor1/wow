package com.elyonut.wow

import android.content.Context
import android.graphics.Color
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import java.util.*

class MapAdapter : IMap {
    override fun addLayer(layerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeLayer(layerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun colorFilter(layerId: String, colorsList: Dictionary<Int, Color>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initMap(context: Context) {
        Mapbox.getInstance(context, R.string.MAPBOX_ACCESS_TOKEN.toString())
    }

    override fun initOfflineMap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun transformLatLngToWowLatLng(latLng: LatLng): WowLatLng {
        return WowLatLng(latLng.latitude, latLng.longitude)
    }

    fun transformLatLngToMapboxLatLng(latLng: WowLatLng): LatLng {
        return LatLng(latLng.latitude, latLng.longitude)
    }

}