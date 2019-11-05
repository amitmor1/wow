package com.elyonut.wow.view

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.elyonut.wow.AlertsListAdapter

import com.elyonut.wow.R
import com.elyonut.wow.model.AlertModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AlertsFragment(var allAlerts: ArrayList<AlertModel>) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var alertsList: ListView
    private var alertsAdapter:AlertsListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)
        alertsAdapter = AlertsListAdapter(context!!, allAlerts)
        alertsList = view.findViewById(R.id.alerts_list)
        alertsList.adapter = alertsAdapter

        return view
    }

    fun addAlert(alert: AlertModel) {
        allAlerts.add(AlertModel(alert.id, alert.message, alert.image, alert.time))
        alertsAdapter?.notifyDataSetChanged()
    }

    fun setAlertAccepted() {
        alertsAdapter?.notifyDataSetChanged()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onAlertsFragmentInteraction()
    }

    companion object {
        @JvmStatic
        fun newInstance(allAlerts: ArrayList<AlertModel>) =
            AlertsFragment(allAlerts).apply {

            }
    }
}
