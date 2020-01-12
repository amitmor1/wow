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
//    lateinit var alertsQueue: Queue<AlertModel>

    init {
        alerts.value = ArrayList()
//        alertsQueue = Queue<AlertModel>()
    }

    fun addAlert(alert: AlertModel) {
        alerts.value?.add(
            0,
            AlertModel(alert.alertID, alert.threatId, alert.message, alert.image, alert.time)
        )

        updateAlerts()
        isAlertAdded.value = true
    }

    fun deleteAlert(position: Int) {
        alerts.value?.removeAt(position)
        updateAlerts()
        deletedAlertPosition.value = position
    }

    fun deleteAlert(alert: AlertModel) {
        alerts.value?.remove(alert)
        updateAlerts()
    }

    fun zoomToLocation(alert: AlertModel) {
        sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alert.threatId, alert.alertID)
    }

    fun acceptAlert(alert: AlertModel) {
        sendBroadcastIntent(Constants.ALERT_ACCEPTED_ACTION, alert.threatId, alert.alertID)
    }

    private fun updateAlerts() {
        alerts.value = alerts.value
    }

    private fun sendBroadcastIntent(actionName: String, threatId: String, notificationID: Int) {
        val actionIntent = Intent(actionName).apply {
            putExtra("threatID", threatId)
            putExtra("alertID", notificationID)
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

    fun getNotificationID(threatID: String): Int {
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