package com.elyonut.wow

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.model.AlertModel
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random

class AlertsManager(var context: Context) {
    var alerts = MutableLiveData<ArrayList<AlertModel>>()
    var isAlertChanged = MutableLiveData<Boolean>()
    var isAlertAdded = MutableLiveData<Boolean>()
    var deletedAlertPosition = MutableLiveData<Int>()
    private var alertIds = HashMap<String, Int>()
    var alertsQueue = LinkedList<AlertModel>()
    var shouldPopAlert = MutableLiveData<Boolean>()

    init {
        alerts.value = ArrayList()
        shouldPopAlert.value = false
    }

    fun addAlert(alert: AlertModel) {
        alerts.value?.add(
            0,
            AlertModel(alert.alertID, alert.threatId, alert.message, alert.image, alert.time)
        )

        updateAlerts()
        alertsQueue.add(alert)
        isAlertAdded.value = true

        if (!shouldPopAlert.value!!) {
            shouldPopAlert.value = true
        }
    }

    fun deleteAlert(position: Int) {
        alerts.value?.removeAt(position)
        updateAlerts()
        shouldPopAlert.value = true
        deletedAlertPosition.value = position
    }

    fun deleteAlert(alert: AlertModel) {
        alerts.value?.remove(alert)
        if (alertsQueue.isNotEmpty()) {
            alertsQueue.remove()
            shouldPopAlert.value = true
        }
        updateAlerts()
    }

    fun zoomToLocation(alert: AlertModel) {
        sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alert.threatId, alert.alertID)
        if (alertsQueue.isNotEmpty()) {
            alertsQueue.remove()
            shouldPopAlert.value = true
        }
    }

    fun acceptAlert(alert: AlertModel) {
        sendBroadcastIntent(Constants.ALERT_ACCEPTED_ACTION, alert.threatId, alert.alertID)
        if (alertsQueue.isNotEmpty()) {
            alertsQueue.remove()
            shouldPopAlert.value = true
        }
    }

    private fun updateAlerts() {
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

        updateAlerts()
        isAlertChanged.value = true

    }

    fun getAlertID(threatID: String): Int {
        var notificationID = alertIds[threatID]

        if (notificationID == null) {
            notificationID = generateNotificationID(threatID)
        }

        return notificationID
    }

    private fun generateNotificationID(threatID: String): Int {
        var newID = Random.nextInt()
        while (alertIds.containsValue(newID)) {
            newID = Random.nextInt()
        }

        alertIds[threatID] = newID
        return newID
    }
}