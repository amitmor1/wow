package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.R
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.model.Threat

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    val selectedLayerId = MutableLiveData<String>()
    val selectedExperimentalOption = MutableLiveData<Int>()
    val selectedThreatItem = MutableLiveData<Threat>()
    val filterLayerId = MutableLiveData<String>()

    fun selectLayer(layerId: String) {
        selectedLayerId.value = layerId
    }

    fun selectExperimentalOption(itemId: Int) {
        selectedExperimentalOption.value = itemId
    }

    fun filterLayer(layerId: String?) {
        filterLayerId.value = layerId
    }
}