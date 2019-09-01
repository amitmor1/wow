package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.NumericFilterTypes
import com.elyonut.wow.TempDB
import com.elyonut.wow.model.PropertyModel
import com.elyonut.wow.view.LayerMenuAdapter
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class FilterViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var viewAdapter: LayerMenuAdapter
    private val layerManager = LayerManager(TempDB((application)))
    var chosenLayerId = MutableLiveData<String>()
    var chosenProperty = MutableLiveData<String>()
    lateinit var layerProperties: List<PropertyModel>
    lateinit var propertiesList: List<String>
    private lateinit var layersIdList: List<String>
    private lateinit var numberFilterOptions: List<String>
    var isStringProperty = MutableLiveData<Boolean>()
    var isNumberProperty = MutableLiveData<Boolean>()

    init {
        chosenLayerId.value = layerManager.initLayersIdList()?.first()
    }

    // Do we really need this function? should we put it in the init and just get the property when we need?
    fun getLayersList(): List<String>? {
        layersIdList =
            layerManager.initLayersIdList()!! // TODO Why exception? shouldn't we use null check? if the layersIdList is null, what do we wanna do?
        return layersIdList
    }

    fun initPropertiesList(layerId: String): List<String>? {
        layerProperties = layerManager.getLayerProperties(layerId)
        propertiesList =
            layerProperties.map { it.name } // Should we maybe use a hashMap so we could get the property values when we need it? and not just the names and than search according to the name (?)
        chosenProperty.value = propertiesList.first()
        return propertiesList
    }

    fun initOptionsList(propertyName: String) {
        checkPropertyType(propertyName)
    }

    private fun checkPropertyType(propertyName: String) {
        if (getPropertyType(propertyName).isSubclassOf(java.lang.Number::class)) {
            isNumberProperty.value = true
            isStringProperty.value = false


        } else if (getPropertyType(propertyName).isSubclassOf(java.lang.String::class)) {
            isStringProperty.value = true
            isNumberProperty.value = false
        }
    }

    private fun getPropertyType(propertyName: String): KClass<*> {
        lateinit var type: KClass<*>

        layerProperties.forEach {
            if (it.name == propertyName) {
                type = it.type
                return type
            }
        }

        return type
    }

    fun onLayerItemSelected(position: Int) {
        chosenLayerId.value = layersIdList[position]
    }

    fun onPropertyItemSelected(position: Int) {
        layerProperties =
            layerManager.getLayerProperties(chosenLayerId.value!!) // What is this for?
        chosenProperty.value = propertiesList[position]
    }

    fun initStringPropertyOptions(propertyName: String): List<String>? {
        val chosenLayer = layerManager.getLayer(chosenLayerId.value!!)
        val chosenProperty = propertiesList.find { p -> p == propertyName }
        val allPropertiesOptions =
            chosenLayer?.map { a -> a.properties?.get(chosenProperty).toString() }

        return allPropertiesOptions?.distinct()
    }

    fun initNumberPropertyOptionsList(): List<String> {

        numberFilterOptions =
            NumericFilterTypes.values().map { filterType -> filterType.hName }.toList()
        return numberFilterOptions
    }

    fun onNumberItemSelected(position: Int) {
        when {
            numberFilterOptions[position] == NumericFilterTypes.GREATER.hName -> {
            }
            numberFilterOptions[position] == NumericFilterTypes.LOWER.hName -> {
            }
            numberFilterOptions[position] == NumericFilterTypes.RANGE.hName -> {
            }
            numberFilterOptions[position] == NumericFilterTypes.SPECIFIC.hName -> {
            }
        }
    }
}