package com.elyonut.wow

import com.elyonut.wow.model.MapLayer
import com.elyonut.wow.utilities.Maps

class MapsManager {
    var maps: ArrayList<MapLayer>? = null

    init {
        maps = arrayListOf(
            MapLayer(Maps.MAPBOX_STYLE_URL, "Basic"), MapLayer(
                Maps.MAPBOX_MAP1, "Blue"), MapLayer(Maps.MAPBOX_MAP2, "Red"), MapLayer(Maps.MAPBOX_MAP3, "Green"), MapLayer(
                Maps.MAPBOX_MAP4, "Purple")
        )
    }
}
