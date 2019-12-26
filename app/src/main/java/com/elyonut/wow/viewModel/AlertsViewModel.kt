package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.AlertsAdapter
import com.elyonut.wow.model.AlertModel

class AlertsViewModel(application: Application, var allAlerts: MutableLiveData<ArrayList<AlertModel>>, var alertsAdapter: AlertsAdapter?): AndroidViewModel(application) {

    fun addAlert(alert: AlertModel) {
        allAlerts.value?.add(0, AlertModel(alert.notificationID ,alert.threatId, alert.message, alert.image, alert.time))
        alertsAdapter?.notifyItemInserted(0)
        updateAlerts()
    }

    fun setAlertAccepted() {
        alertsAdapter?.notifyDataSetChanged()
        updateAlerts()
    }

    private fun updateAlerts() {
        allAlerts.value = allAlerts.value
    }

    fun deleteAlert(position: Int) {
        allAlerts.value?.removeAt(position)
        alertsAdapter?.notifyItemRemoved(position)
        alertsAdapter?.notifyItemRangeChanged(position, allAlerts.value!!.count())
        updateAlerts()
    }
}