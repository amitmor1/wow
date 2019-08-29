package com.elyonut.wow

import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LayerModel

class LayerManager(tempDB: TempDB) {
    var layersList: List<LayerModel>? = null

    init {
        layersList = tempDB.getFeatures()
    }

    fun getLayer(id: String): List<FeatureModel>? {
        layersList?.forEach {
            when(it.id) {
                id-> return it.features
            }
        }
        return null
    }
}