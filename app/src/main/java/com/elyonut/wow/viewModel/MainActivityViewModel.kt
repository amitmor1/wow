package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.elyonut.wow.R
import com.elyonut.wow.VectorLayersManager
import com.elyonut.wow.adapter.LocationService
import com.elyonut.wow.adapter.PermissionsService
import com.elyonut.wow.analysis.ThreatAnalyzer
import com.elyonut.wow.interfaces.ILocationService
import com.elyonut.wow.interfaces.IPermissions
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.utilities.MapStates
import com.google.android.material.checkbox.MaterialCheckBox
import com.mapbox.geojson.Feature

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val vectorLayersManager = VectorLayersManager.getInstance(application)
    private var _mapStateChanged = MutableLiveData<MapStates>()
    val mapStateChanged: LiveData<MapStates>
        get() = _mapStateChanged
    val chosenLayerId = MutableLiveData<String>()
    val filterSelected = MutableLiveData<Boolean>()
    val coverageSettingsSelected = MutableLiveData<Boolean>()
    val shouldDefineArea = MutableLiveData<Boolean>()
    val shouldOpenAlertsFragment = MutableLiveData<Boolean>()
    val shouldOpenThreatsFragment = MutableLiveData<Boolean>()
    val chosenTypeToFilter = MutableLiveData<Pair<String, Boolean>>()
    val isSelectAllChecked = MutableLiveData<Boolean>()
    private var _isPermissionRequestNeeded = MutableLiveData<Boolean>()
    val isPermissionRequestNeeded: LiveData<Boolean>
        get() = _isPermissionRequestNeeded
    private var _isPermissionDialogShown = MutableLiveData<Boolean>()
    val isPermissionDialogShown: LiveData<Boolean>
        get() = _isPermissionDialogShown
    private var locationService: ILocationService = LocationService.getInstance(getApplication())
    private val permissions: IPermissions =
        PermissionsService.getInstance(application)
    val coordinatesFeaturesInCoverage = MutableLiveData<List<Feature>>()
    var mapLayers: LiveData<List<LayerModel>> =
        Transformations.map(vectorLayersManager.layers, ::layersUpdated)

    init {
        ThreatAnalyzer.getInstance(getApplication())
    }

    private fun layersUpdated(layers: List<LayerModel>) = layers

    fun locationSetUp() {
        if (permissions.isLocationPermitted()) {
            startLocationService()
        } else {
            _isPermissionRequestNeeded.postValue(true)
        }
    }

    private fun startLocationService() {
        if (!locationService.isGpsEnabled()) {
            _isPermissionDialogShown.postValue(true)
        }

        locationService.startLocationService()
    }

    fun onNavigationItemSelected(item: MenuItem): Boolean {
        var shouldCloseDrawer = true

        when {
            item.groupId == R.id.nav_layers -> {
                val layerModel = item.actionView.tag as LayerModel
                chosenLayerId.postValue(layerModel.id)
                shouldCloseDrawer = false
            }
            item.itemId == R.id.select_all -> {
                isSelectAllChecked.postValue((item.actionView as MaterialCheckBox).isChecked)
            }
            item.groupId == R.id.filter_options -> {
                chosenTypeToFilter.postValue(
                    Pair(
                        item.actionView.tag as String,
                        (item.actionView as MaterialCheckBox).isChecked
                    )
                )
                shouldCloseDrawer = false
            }
            item.itemId == R.id.los_buildings_to_location -> {
                _mapStateChanged.value = MapStates.LOS_BUILDINGS_TO_LOCATION
                Toast.makeText(getApplication(), "Select Location", Toast.LENGTH_LONG).show()
            }
            item.itemId == R.id.calculate_coverage -> {
                _mapStateChanged.value = MapStates.CALCULATE_COORDINATES_IN_RANGE
            }
            item.itemId == R.id.define_area -> {
                _mapStateChanged.value = MapStates.DRAWING
                if (shouldDefineArea.value == null || !shouldDefineArea.value!!) {
                    shouldDefineArea.postValue(true)
                }
            }
            item.itemId == R.id.alerts -> {
                shouldOpenAlertsFragment.postValue(true)
            }
            item.itemId == R.id.coverage_settings -> {
                coverageSettingsSelected.postValue(true)
                shouldCloseDrawer = false
            }
            item.itemId == R.id.awareness_status || item.itemId == R.id.threat_list_menu_item -> {
                shouldOpenThreatsFragment.postValue(true)
            }
        }

        return shouldCloseDrawer
    }

    fun getLayerTypeValues(): List<String>? {
        return vectorLayersManager.getValuesOfLayerProperty(Constants.THREAT_LAYER_ID, "type")
    }
}