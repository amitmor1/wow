package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class SharedViewModel(application: Application): AndroidViewModel(application){
    val selectedLayerId = MutableLiveData<String>()

    fun select(layerId: String) {
        selectedLayerId.value = layerId
    }
}