package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.NumericFilterTypes
//import com.elyonut.wow.NumericFilterTypes
import com.elyonut.wow.TempDB
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class FilterViewModel(application: Application) : AndroidViewModel(application) {
    // TODO: null handling

    private val layerManager = LayerManager(TempDB((application)))
    private lateinit var propertiesList: List<String>
    private var propertiesHashMap = HashMap<String, KClass<*>>()
    var chosenLayerId = MutableLiveData<String>()
    var chosenProperty = MutableLiveData<String>()
    var layersIdsList: List<String>
    var numberFilterOptions: List<String>
    var isStringProperty = MutableLiveData<Boolean>()
    var isNumberProperty = MutableLiveData<Boolean>()
    val isGreaterChosen = MutableLiveData<Boolean>()
    val isLowerChosen = MutableLiveData<Boolean>()
    val isSpecificChosen = MutableLiveData<Boolean>()
    val shouldApplyFilter = MutableLiveData<Boolean>()
    var isStringType = MutableLiveData<Boolean>()
    lateinit var numericType: NumericFilterTypes

    init {
        layersIdsList = layerManager.initLayersIdList()!!
        numberFilterOptions =
            NumericFilterTypes.values().map { filterType -> filterType.hebrewName }.toList()
    }

    fun applyFilterButtonClicked(shouldApply: Boolean) {
        shouldApplyFilter.value = shouldApply
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
                onNumberItemSelected(0)

                isStringType.value = false

            } else if (propertyType.isSubclassOf(java.lang.String::class)) {
                isStringProperty.value = true
                isNumberProperty.value = false
                onNumberItemSelected(0)
                isStringType.value = true
            }
        }
    }

    private fun getPropertyType(propertyName: String): KClass<*>? {
        return propertiesHashMap[propertyName]
    }

    fun onLayerItemSelected(position: Int) {
        chosenLayerId.value = layersIdsList[position]
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
        ) // TODO: null handling
    }

    fun onNumberItemSelected(position: Int) {
        when (numberFilterOptions[position]) {
            NumericFilterTypes.GREATER.hebrewName -> {
                isGreaterChosen.value = true
                isLowerChosen.value = false
                isSpecificChosen.value = false

                numericType = NumericFilterTypes.GREATER
            }
            NumericFilterTypes.LOWER.hebrewName -> {
                isGreaterChosen.value = false
                isLowerChosen.value = true
                isSpecificChosen.value = false

                numericType = NumericFilterTypes.LOWER
            }
            NumericFilterTypes.RANGE.hebrewName -> {
                isGreaterChosen.value = true
                isLowerChosen.value = true
                isSpecificChosen.value = false

                numericType = NumericFilterTypes.RANGE
            }
            NumericFilterTypes.SPECIFIC.hebrewName -> {
                isGreaterChosen.value = false
                isLowerChosen.value = false
                isSpecificChosen.value = true

                numericType = NumericFilterTypes.SPECIFIC
            }
        }
    }
}