package com.elyonut.wow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationReceiver: BroadcastReceiver() {
    lateinit var featureID: String
    var shouldZoomToLocation = MutableLiveData<Boolean>()

    init {
        shouldZoomToLocation.value = false
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "ZOOM_LOCATION" -> zoomLoaction(intent.getStringExtra("threatID"))
        }
    }

    fun getShouldZoomToLocation(): LiveData<Boolean> {
        return shouldZoomToLocation
    }

    private fun zoomLoaction(threadID: String) {
        featureID = threadID
        shouldZoomToLocation.value = true
    }
}

