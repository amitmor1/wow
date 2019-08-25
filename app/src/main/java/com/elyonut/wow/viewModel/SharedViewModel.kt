package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import android.widget.CheckBox
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.R
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.model.Threat

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    val selectedLayerId = MutableLiveData<String>()
    val selectedExperimentalOption = MutableLiveData<Int>()
    val selectedThreatItem = MutableLiveData<Threat>()

    fun select(layerId: String) {
        selectedLayerId.value = layerId
    }

    fun onNavigationItemSelected(item: MenuItem) {
        if (item.groupId == R.id.nav_layers) {
            val checkbox = item.actionView as CheckBox
            checkbox.isChecked = true
            val layerModel = item.actionView.tag as LayerModel
            this.select(layerModel.id)
        } else if (item.groupId == R.id.nav_experiments) {
            this.selectedExperimentalOption.value = item.itemId
        }
    }
}