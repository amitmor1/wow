package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.R
import com.elyonut.wow.TempDB
import com.elyonut.wow.model.LayerModel

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val layerManager = LayerManager(TempDB((application)))
    val chosenLayerId = MutableLiveData<String>()
    val selectedExperimentalOption = MutableLiveData<Int>()
    val filterSelected = MutableLiveData<Boolean>()
    val shouldDefineArea = MutableLiveData<Boolean>()
    val shouldOpenAlertsFragment = MutableLiveData<Boolean>()

    fun onNavigationItemSelected(item: MenuItem): Boolean {
        var shouldCloseDrawer = true
        when {
            item.groupId == R.id.nav_layers -> {
                val layerModel = item.actionView.tag as LayerModel
                chosenLayerId.value = layerModel.id
                shouldCloseDrawer = false
            }
            item.itemId == R.id.filterButton -> {
                filterSelected.value = true
                shouldCloseDrawer = false
            }
            item.groupId == R.id.nav_experiments ->
                this.selectedExperimentalOption.value = item.itemId
            item.itemId == R.id.define_area -> {
                if (shouldDefineArea.value == null || !shouldDefineArea.value!!) {
                    shouldDefineArea.value = true
                }
            }
            item.itemId == R.id.alerts -> {
                shouldOpenAlertsFragment.value = true
            }
        }

        return shouldCloseDrawer
    }

    fun getLayersList(): List<LayerModel>? {
        return layerManager.layersList
    }
}

