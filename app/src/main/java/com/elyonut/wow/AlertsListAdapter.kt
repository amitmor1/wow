package com.elyonut.wow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import com.elyonut.wow.model.AlertModel
import com.squareup.picasso.Picasso
import kotlin.collections.ArrayList

class AlertsListAdapter(
    var context: Context,
    allAlerts: ArrayList<AlertModel>
) : BaseAdapter() {
    private var inflater: LayoutInflater? = null
    private var alertsList = ArrayList<AlertModel>()

    init {
        alertsList = allAlerts
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
    }

    inner class Holder {
        internal var alertMessage: TextView? = null
        internal var alertImage: ImageView? = null
        internal var currentTime: TextView? = null
        internal var cardView: CardView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val holder = Holder()
        val view = inflater?.inflate(R.layout.alert_item, null)

        holder.alertMessage = view?.findViewById(R.id.alert_message)
        holder.alertImage = view?.findViewById(R.id.alert_image)
        holder.currentTime = view?.findViewById(R.id.current_time)
        holder.cardView = view?.findViewById(R.id.card_view)

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

        return view
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return alertsList.count()
    }
}