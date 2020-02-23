package com.elyonut.wow

import com.elyonut.wow.model.MapLayer
import com.elyonut.wow.utilities.Maps

class MapsManager {
    var maps: ArrayList<MapLayer>? = null

    init {
        maps = arrayListOf(
            MapLayer(Maps.MAPBOX_STYLE_URL, "Basic"), MapLayer(
                Maps.MAPBOX_MAP1, "Blue"), MapLayer(Maps.MAPBOX_MAP2, "Red"), MapLayer(Maps.MAPBOX_MAP3, "Green"), MapLayer(
                "https://api.tomtom.com/map/1/style/20.0.0-8/basic_main.json?key=lAaAl01ZjuZaiSc4Y4h6yMIY1mPmobUv", "TOMTOM")
        )
    }
}
