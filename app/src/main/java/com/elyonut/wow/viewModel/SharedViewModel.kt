package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.NumericFilterTypes
import com.elyonut.wow.model.Threat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    val selectedLayerId = MutableLiveData<String>()
    val selectedExperimentalOption = MutableLiveData<Int>()
    val selectedThreatItem = MutableLiveData<Threat>()
    var chosenLayerId = ""
    var chosenPropertyId = ""
    var chosenPropertyValue = ""
    var minValue: Number = 0
    var maxValue: Number = 0
    var specificValue: Number = 0
    val shouldApplyFilter = MutableLiveData<Boolean>()
    var isStringType: Boolean = false
    lateinit var numericType: NumericFilterTypes
    var shouldDefineArea = MutableLiveData<Boolean>()
    var areaOfInterest: Polygon? = null
    var areaOfInterestLines = ArrayList<Point>()
    var areaOfInterestCircles = ArrayList<Feature>()

    fun selectLayer(layerId: String) {
        selectedLayerId.value = layerId
    }

    fun selectExperimentalOption(itemId: Int) {
        selectedExperimentalOption.value = itemId
    }

    fun applyFilter(shouldApply: Boolean) {
        shouldApplyFilter.value = shouldApply
    }
}