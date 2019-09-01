package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.TempDB
import com.elyonut.wow.model.PropertyModel
import com.elyonut.wow.view.LayerMenuAdapter
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class FilterViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var viewAdapter: LayerMenuAdapter
    private val layerManager = LayerManager(TempDB((application)))
    var filterLayerId = MutableLiveData<String>()
    var chosenProperty = MutableLiveData<String>()
    lateinit var layerProperties: List<PropertyModel>
    lateinit var propertiesList: List<String>
    private lateinit var layerList: List<String>
    private lateinit var numberFilterOptions: List<String>
    var isStringProperty = MutableLiveData<Boolean>()
    var isNumberProperty = MutableLiveData<Boolean>()

    init {
        filterLayerId.value = layerManager.getLayers()?.first()
    }

    fun initLayerList(): List<String>? {
        layerList = layerManager.getLayers()!!
        return layerList
    }

    fun initPropertiesList(layerId: String): List<String>? {
        layerProperties = layerManager.getLayerProperties(layerId)
        propertiesList = layerProperties.map { it.name }
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

        filterLayerId.value = layerList.get(position)
    }

    fun onPropertyItemSelected(position: Int) {
        layerProperties = layerManager.getLayerProperties(filterLayerId.value!!)
        chosenProperty.value = propertiesList[position]
    }

    fun initStringPropertyOptions() {

    }

    fun initNumberPropertyOptionsList(): List<String> {
        numberFilterOptions = listOf("טווח", "קטן מ", "גדול מ", "בחר ערך מסוים")
        return numberFilterOptions
    }

    fun onNumberItemSelected(position: Int) {

    }
}