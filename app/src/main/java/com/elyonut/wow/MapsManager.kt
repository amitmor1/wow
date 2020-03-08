package com.elyonut.wow

import android.content.Context
import com.elyonut.wow.model.MapLayer
import com.elyonut.wow.utilities.Maps

class MapsManager(context: Context) {
    var maps: ArrayList<MapLayer>? = null

    init {
        maps = arrayListOf(
            MapLayer(
                Maps.MAPBOX_STYLE_URL,
                "Basic",
                context.resources.getIdentifier("basic_map", "drawable", context.packageName)
            ),
            MapLayer(
                Maps.MAPBOX_MAP1,
                "Blue",
                context.resources.getIdentifier("blue_map", "drawable", context.packageName)
            ),
            MapLayer(
                Maps.MAPBOX_MAP2,
                "Red",
                context.resources.getIdentifier("red_map", "drawable", context.packageName)
            ),
            MapLayer(
                Maps.MAPBOX_MAP3,
                "Green",
                context.resources.getIdentifier("green_map", "drawable", context.packageName)
            ),
            MapLayer(
                "https://api.tomtom.com/map/1/style/20.0.0-8/basic_main.json?key=lAaAl01ZjuZaiSc4Y4h6yMIY1mPmobUv",
                "TOMTOM"
            )
        )
    }
}
