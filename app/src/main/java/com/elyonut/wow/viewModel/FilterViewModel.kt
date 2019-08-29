package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.elyonut.wow.LayerManager
import com.elyonut.wow.TempDB
import com.elyonut.wow.model.PropertyModel
import com.elyonut.wow.view.LayerMenuAdapter
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class FilterViewModel(application: Application): AndroidViewModel(application) {
    private lateinit var viewAdapter: LayerMenuAdapter
    private val layerManager = LayerManager(TempDB((application)))
    var filterLayerId = MutableLiveData<String>()
    var chosenProperty = MutableLiveData<String>()
    lateinit var layerProperties: List<PropertyModel>
    lateinit var propertyAdapter: List<String>
    private lateinit var layerAdapter: List<String>
    private lateinit var numberPropertyDropDownAdapter: List<String>
    var isStringProperty = MutableLiveData<Boolean>()
    var isNumberProperty = MutableLiveData<Boolean>()

    init {
        filterLayerId.value = layerManager.getLayers()?.first()
    }

    fun initLayerAdapter(): List<String>? {
        layerAdapter = layerManager.getLayers()!!
        return layerAdapter
    }

    fun initPropertiesAdapter(layerId: String): List<String>? {
        layerProperties = layerManager.getLayerProperties(layerId)
        propertyAdapter = layerProperties.map { it.name }
        return propertyAdapter
    }

    fun initOptionsDropDown(propertyName: String) {
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

        filterLayerId.value = layerAdapter?.get(position)
    }

    fun onPropertyItemSelected(position: Int) {
        layerProperties = layerManager.getLayerProperties(filterLayerId.value!!)
        chosenProperty.value = propertyAdapter[position]
    }

    fun initStringPropertyDropDown() {

    }

    fun initNumberPropertyAdapter(): List<String>{
        numberPropertyDropDownAdapter = listOf("טווח", "קטן מ","גדול מ","בחר ערך מסוים")
        return numberPropertyDropDownAdapter
    }

    fun onNumberItemSelected(position: Int) {

    }
}