package com.elyonut.wow

import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.model.PropertyModel
import com.google.gson.JsonPrimitive

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

    fun getLayers(): List<String>? {
        val layers = ArrayList<String>()
        layersList?.forEach {
            layers.add(it.id)
        }

        return layers
    }

    fun getLayerProperties(id: String): List<PropertyModel> {
        val propertiesList = ArrayList<PropertyModel>()
        val currentLayer = getLayer(id)
        var type = null

        currentLayer?.first()?.properties?.entrySet()?.forEach {
            if ((it.value as JsonPrimitive).isNumber) {
                propertiesList.add(PropertyModel(it.key, Number::class))
            } else if ((it.value as JsonPrimitive).isString) {
                propertiesList.add(PropertyModel(it.key, String::class))
            }

//            propertiesList.add(PropertyModel(it.key, it.value.javaClass.typeName.javaClass))
        }

        return propertiesList
    }

    fun getPropertyMinValue(layerId: String, property: String): Int {
        val currentLayer = getLayer(layerId)
        var minValue = 1000000000

        currentLayer?.forEach {
            if (it.properties?.get(property)!!.asInt < minValue) {
                minValue = it.properties?.get(property)!!.asInt
            }
        }

        return minValue
    }

    fun getPropertyMaxValue(layerId: String, property: String): Int {
        val currentLayer = getLayer(layerId)
        var maxValue = -1000000000

        currentLayer?.forEach {
            if (it.properties?.get(property)!!.asInt > maxValue) {
                maxValue = it.properties?.get(property)!!.asInt
            }
        }

        return maxValue
    }
}
