package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.Constants
import com.elyonut.wow.NumericFilterTypes
import com.elyonut.wow.alerts.AlertsManager
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.model.Threat
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
    val alertsManager =  AlertsManager(application)
    val activeAlert = MutableLiveData<AlertModel>()
    var allAlerts = ArrayList<AlertModel>()
    var isAlertChanged = MutableLiveData<Boolean>()

    var coverageRangeMeters: Double = Constants.DEFAULT_COVERAGE_RANGE_METERS
    var coverageResolutionMeters: Double = Constants.DEFAULT_COVERAGE_RESOLUTION_METERS
    var coverageSearchHeightMeters: Double = Constants.DEFAULT_COVERAGE_HEIGHT_METERS
    var coverageSearchHeightMetersChecked: Boolean = false

    fun applySaveCoverageSettingsButtonClicked(coverageRange: Double, resolution: Double, height: Double?, heightChecked: Boolean) {
        this.coverageRangeMeters = coverageRange
        this.coverageResolutionMeters = resolution
        if( height!= null ){
            this.coverageSearchHeightMeters = height
        }
        this.coverageSearchHeightMetersChecked = heightChecked
    }

    fun selectExperimentalOption(itemId: Int) {
        selectedExperimentalOption.value = itemId
    }

    fun updateMessageAccepted(messageID: String) {
        val alert = allAlerts.find { it.id == messageID }
        if (alert != null) {
            alert.isRead = true
        }

        isAlertChanged.value = true
    }
}