package com.elyonut.wow

import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class LayersManager {

    var layers = ArrayList<WowLayer>()

    fun createLayer(layerId: String, sourceId: String) {
        layers.add(WowLayer(layerId, sourceId))
    }

    fun showLayer(id: String) {

    }

    fun removeLayer(id: String) {

    }

    fun showAllLayers() {

    }

    fun removeAllLayers() {

    }
}
