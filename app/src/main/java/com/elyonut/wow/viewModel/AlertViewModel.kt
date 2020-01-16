package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.model.AlertModel

class AlertViewModel(application: Application, var alertsManager: AlertsManager) : AndroidViewModel(application) {
//    var shouldRemoveAlert = MutableLiveData<Boolean>()

    fun zoomToLocationClicked(alert: AlertModel) {
        alertsManager.zoomToLocation(alert)
        alertsManager.shouldRemoveAlert.value = true
    }

    fun acceptAlertClicked(alert: AlertModel) {
        alertsManager.acceptAlert(alert)
        alertsManager.shouldRemoveAlert.value = true
    }

    fun deleteAlertClicked(alert: AlertModel) {
        alertsManager.deleteAlert(alert)
        alertsManager.shouldRemoveAlert.value = true
    }
}