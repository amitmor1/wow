package com.elyonut.wow

import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LatLngModel
import com.elyonut.wow.model.LayerModel
import com.google.gson.JsonPrimitive
import kotlin.reflect.KClass

class LayerManager(tempDB: TempDB) {
    var layersList: List<LayerModel>? = null

    init {
        layersList = tempDB.getFeatures()
    }

    fun getLayer(id: String): List<FeatureModel>? {
        return layersList?.find { layer -> id == layer.id }?.features
    }

    fun initLayersIdList(): List<String>? {
        return layersList?.map { it.id }
    }

    fun getLayerProperties(id: String): HashMap<String, KClass<*>> {
        val currentLayer = getLayer(id)
        val propertiesHashMap = HashMap<String, KClass<*>>()

        // Temp- until we have a real DB and real data
        currentLayer?.first()?.properties?.entrySet()?.forEach {
            if ((it.value as JsonPrimitive).isNumber) {
                propertiesHashMap[it.key] = Number::class
            } else if ((it.value as JsonPrimitive).isString) {
                propertiesHashMap[it.key] = String::class
            }
        }

        return propertiesHashMap
    }

    fun getValuesOfLayerProperty(layerId: String, propertyName: String): List<String>? {
           return getLayer(layerId)?.map { a -> a.properties?.get(propertyName).toString() }?.distinct()
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

    fun getFeatureLocation(featureID: String): LatLngModel {
        var feature: FeatureModel? = null
        layersList?.forEach { it ->
            feature = it.features.find { it.id == featureID }
        }

        return LatLngModel(feature?.properties?.get("latitude")!!.asDouble, feature?.properties?.get("longitude")!!.asDouble)
    }

    fun getFeatureName(featureID: String): String {
        var feature: FeatureModel? = null
        layersList?.forEach { it ->
            feature = it.features.find { it.id == featureID }

            if(feature != null){
                return feature?.properties?.get("namestr")!!.asString
            }

        }

        return feature?.properties?.get("namestr")!!.asString
    }
}
