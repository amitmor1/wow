package com.elyonut.wow

import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LayerModel

class LayerManager(tempDB: TempDB) {
    var layerList: List<LayerModel>? = null

    init {
        layerList = tempDB.getFeatures()
    }

    fun getLayer(id: String): List<FeatureModel>? {
       layerList?.forEach {
           when(it.id) {
               id-> return it.features
           }
       }
        return null
    }
}