package com.elyonut.wow

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AlertsListAdapter(var context: Context, alerts: ArrayList<String>, images: ArrayList<Int>): BaseAdapter() {
    private var inflater: LayoutInflater? = null
    private var alertsMessages = ArrayList<String>()
    private var alertsImages = ArrayList<Int>()

    init {
        alertsMessages = alerts
        alertsImages = images
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
    }

    inner class Holder {
        internal var alertMessage: TextView? = null
        internal var alertImage: ImageView? = null
        internal var currentTime: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var holder = Holder()
        var view = inflater?.inflate(R.layout.alert_item, null)

        holder.alertMessage = view?.findViewById(R.id.alert_message)
        holder.alertImage = view?.findViewById(R.id.alert_image)
        holder.currentTime = view?.findViewById(R.id.current_time)

        holder.alertMessage?.text = alertsMessages[position]
        Picasso.with(context).load(alertsImages[position]).into(holder.alertImage)

        val date = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        val currentDateTime = date.format(Date())
        holder.currentTime?.text = currentDateTime

        return view
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return alertsMessages.count()
    }
}