package com.elyonut.wow

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.viewModel.AlertsViewModel

class AlertsViewModelFactory(val application: Application, var allAlerts: MutableLiveData<ArrayList<AlertModel>>, var alertsAdapter: AlertsAdapter?): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return  AlertsViewModel(application, allAlerts, alertsAdapter) as T
    }
}