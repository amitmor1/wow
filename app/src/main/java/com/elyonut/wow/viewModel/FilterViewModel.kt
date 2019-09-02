package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.NumericFilterTypes
import com.elyonut.wow.TempDB
import com.elyonut.wow.view.LayerMenuAdapter
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class FilterViewModel(application: Application) : AndroidViewModel(application) {
    // TODO: null handling

    private lateinit var viewAdapter: LayerMenuAdapter
    private val layerManager = LayerManager(TempDB((application)))
    var chosenLayerId = MutableLiveData<String>()
    var chosenProperty = MutableLiveData<String>()
    //    lateinit var layerProperties: List<PropertyModel>
    lateinit var propertiesList: List<String>
    private lateinit var layersIdList: List<String>
    private lateinit var numberFilterOptions: List<String>
    var isStringProperty = MutableLiveData<Boolean>()
    var isNumberProperty = MutableLiveData<Boolean>()
    val isGreaterChosen = MutableLiveData<Boolean>()
    val isLowerChosen = MutableLiveData<Boolean>()
    val isSpecificChosen = MutableLiveData<Boolean>()
    private var propertiesHashMap = HashMap<String, KClass<*>>()

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
        propertiesHashMap = layerManager.getLayerProperties(layerId)
        propertiesList = propertiesHashMap.keys.toList()
        chosenProperty.value = propertiesList.first()
        return propertiesList
    }

    fun initOptionsList(propertyName: String) {
        checkPropertyType(propertyName)
    }

    private fun checkPropertyType(propertyName: String) {
        val propertyType = getPropertyType(propertyName)
        if (propertyType != null) {
            if (propertyType.isSubclassOf(java.lang.Number::class)) {
                isNumberProperty.value = true
                isStringProperty.value = false

            } else if (propertyType.isSubclassOf(java.lang.String::class)) {
                isStringProperty.value = true
                isNumberProperty.value = false
                isGreaterChosen.value = false
                isLowerChosen.value = false
                isSpecificChosen.value = false
            }
        }
    }

    private fun getPropertyType(propertyName: String): KClass<*>? {
        return propertiesHashMap[propertyName]
    }

    fun onLayerItemSelected(position: Int) {
        chosenLayerId.value = layersIdList[position]
    }

    fun onPropertyItemSelected(position: Int) {
        propertiesHashMap =
            layerManager.getLayerProperties(chosenLayerId.value!!)
        chosenProperty.value = propertiesList[position]
    }

    fun initStringPropertyOptions(propertyName: String): List<String>? {
        return layerManager.getValuesOfLayerProperty(
            chosenLayerId.value!!,
            propertyName
        )     // TODO: null handling
    }

    fun initNumberPropertyOptionsList(): List<String> {
        numberFilterOptions =
            NumericFilterTypes.values().map { filterType -> filterType.hebrewName }.toList()
        return numberFilterOptions
    }

    fun onNumberItemSelected(position: Int) {
        when {
            numberFilterOptions[position] == NumericFilterTypes.GREATER.hebrewName -> {
                isGreaterChosen.value = true
                isLowerChosen.value = false
                isSpecificChosen.value = false
            }
            numberFilterOptions[position] == NumericFilterTypes.LOWER.hebrewName -> {
                isGreaterChosen.value = false
                isLowerChosen.value = true
                isSpecificChosen.value = false
            }
            numberFilterOptions[position] == NumericFilterTypes.RANGE.hebrewName -> {
                isGreaterChosen.value = true
                isLowerChosen.value = true
                isSpecificChosen.value = false
            }
            numberFilterOptions[position] == NumericFilterTypes.SPECIFIC.hebrewName -> {
                isGreaterChosen.value = false
                isLowerChosen.value = false
                isSpecificChosen.value = true
            }
        }
    }
}