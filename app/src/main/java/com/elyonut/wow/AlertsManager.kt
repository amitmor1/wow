package com.elyonut.wow

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.model.AlertModel
import java.util.*

class AlertsManager(var context: Context) {
    var alerts = MutableLiveData<LinkedList<AlertModel>>()
    var isAlertAccepted = MutableLiveData<Boolean>()
    var isAlertAdded = MutableLiveData<Boolean>()
    var deletedAlertPosition = MutableLiveData<Int>()
//    private var alertIds = HashMap<String, Int>()
//    var alertsQueue = LinkedList<AlertModel>()
    var shouldPopAlert = MutableLiveData<Boolean>()
    var shouldRemoveAlert = MutableLiveData<Boolean>()
    var idCounter = 0

    init {
        alerts.value = LinkedList()
        shouldPopAlert.value = false
        shouldRemoveAlert.value = false
    }

    fun addAlert(alert: AlertModel) {
        alerts.value?.add(
            0,
            AlertModel(idCounter, alert.threatId, alert.message, alert.image, alert.time)
        )

        updateAlertsList()
//        alertsQueue.add(alert)
        isAlertAdded.value = true
//        if (!shouldPopAlert.value!!) {
//            shouldPopAlert.value = true
//        }

        shouldPopAlert.value = true

        idCounter++
    }

    fun deleteAlert(position: Int) {
        alerts.value?.removeAt(position)
        updateAlertsList()
        shouldRemoveAlert.value = true
        shouldPopAlert.value = true
        deletedAlertPosition.value = position
    }

    fun deleteAlert(alert: AlertModel) {
        alerts.value?.remove(alert)
//        if (alertsQueue.isNotEmpty()) {
//            alertsQueue.remove()
//            shouldPopAlert.value = true
//        }
        shouldRemoveAlert.value = true
        shouldPopAlert.value = true
        updateAlertsList()
    }

    fun zoomToLocation(alert: AlertModel) {
        sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alert.threatId, alert.alertID)
//        if (alertsQueue.isNotEmpty()) {
//            alertsQueue.remove()
//            shouldPopAlert.value = true
//        }
//        shouldPopAlert.value = true
        shouldRemoveAlert.value = true
    }

    fun acceptAlert(alert: AlertModel) {
        sendBroadcastIntent(Constants.ALERT_ACCEPTED_ACTION, alert.threatId, alert.alertID)
//        if (alertsQueue.isNotEmpty()) {
//            alertsQueue.remove()
//            shouldPopAlert.value = true
//        }
//        shouldPopAlert.value = true
        shouldRemoveAlert.value = true
    }

    private fun updateAlertsList() {
        alerts.value = alerts.value
    }

    private fun sendBroadcastIntent(actionName: String, threatId: String, alertID: Int) {
        val actionIntent = Intent(actionName).apply {
            putExtra("threatID", threatId)
            putExtra("alertID", alertID)
        }

        this.context.sendBroadcast(actionIntent)
    }

    fun updateMessageAccepted(messageID: String) {
        val alert = alerts.value?.find { it.threatId == messageID }

        if (alert != null) {
            alert.isRead = true
        }

        updateAlertsList()
        isAlertAccepted.value = true

    }

//    fun getAlertID(threatID: String): Int {
//        var notificationID = alertIds[threatID]
//
//        if (notificationID == null) {
//            notificationID = generateNotificationID(threatID)
//        }
//
//        return notificationID
//    }
//
//    private fun generateNotificationID(threatID: String): Int {
//        var newID = Random.nextInt()
//        while (alertIds.containsValue(newID)) {
//            newID = Random.nextInt()
//        }
//
//        alertIds[threatID] = newID
//        return newID
//    }
}