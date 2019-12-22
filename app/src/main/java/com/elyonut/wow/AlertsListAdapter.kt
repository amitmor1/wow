package com.elyonut.wow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.model.AlertModel
import com.squareup.picasso.Picasso
import kotlin.collections.ArrayList

class AlertsListAdapter(
    var context: Context,
    allAlerts: ArrayList<AlertModel>
) : RecyclerView.Adapter<AlertsListAdapter.AlertsViewHolder>() {

    class AlertsViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val alertMessage: TextView? = view.findViewById(R.id.alert_message)
        val alertImage: ImageView? = view.findViewById(R.id.alert_image)
        val currentTime: TextView? = view.findViewById(R.id.current_time)
        val cardView: CardView? = view.findViewById(R.id.card_view)
        val zoomLocationButton: TextView? = view.findViewById(R.id.zoomToLocation)
        val alertAcceptedButton: TextView? = view.findViewById(R.id.alertAccepted)
    }

    private var alertsList = ArrayList<AlertModel>()

    init {
        alertsList = allAlerts
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertsViewHolder {
        return AlertsViewHolder(LayoutInflater.from(context).inflate(R.layout.alert_item, parent, false))
    }

    override fun getItemCount(): Int {
        return alertsList.count()
    }

    override fun onBindViewHolder(holder: AlertsViewHolder, position: Int) {
        holder.alertMessage?.text = alertsList[position].message
        Picasso.with(context).load(alertsList[position].image).into(holder.alertImage)
        holder.currentTime?.text = alertsList[position].time

        if (!alertsList[position].isRead) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.cardView?.setCardBackgroundColor(context.getColor(R.color.unreadMessage))
            }
        } else {
            holder.cardView?.setCardBackgroundColor(Color.WHITE)
        }

        holder.zoomLocationButton?.setOnClickListener {
            sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alertsList[position].threatId, alertsList[position].notificationID)
            (context as FragmentActivity).supportFragmentManager.popBackStack()
        }

        holder.alertAcceptedButton?.setOnClickListener {
            sendBroadcastIntent(Constants.ALERT_ACCEPTED_ACTION, alertsList[position].threatId, alertsList[position].notificationID)
        }
    }

    private fun sendBroadcastIntent(actionName: String, threatId: String, notificationID: Int) {
        val actionIntent = Intent(actionName).apply {
            putExtra("threatID", threatId)
            putExtra("notificationID", notificationID)
        }

        context.sendBroadcast(actionIntent)
    }
}
