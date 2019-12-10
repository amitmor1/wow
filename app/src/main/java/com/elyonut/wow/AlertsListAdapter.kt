package com.elyonut.wow

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.model.AlertModel
import com.squareup.picasso.Picasso
import kotlin.collections.ArrayList

class AlertsListAdapter(
    var context: Context,
    allAlerts: ArrayList<AlertModel>
) : RecyclerView.Adapter<AlertsListAdapter.AlertsViewHolder>() {

    class AlertsViewHolder (view: View) : RecyclerView.ViewHolder(view) {


        var alertMessage: TextView? = view?.findViewById(R.id.alert_message)
        var alertImage: ImageView? = view?.findViewById(R.id.alert_image)
        var currentTime: TextView? = view?.findViewById(R.id.current_time)
        var cardView: CardView? = view?.findViewById(R.id.card_view)
        var zoomLocationButton: ImageView? = view?.findViewById(R.id.zoomToLocation)
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
            val actionIntent = Intent(Constants.ZOOM_LOCATION_ACTION).apply {
                putExtra("threatID", alertsList[position].id)
//                putExtra("notificationID", notificationID)
            }

            PendingIntent.getBroadcast(context, alertsList[position].id as Int, actionIntent, 0).send()


        }
    }
}
