package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.AlertsAdapter

import com.elyonut.wow.R
import com.elyonut.wow.model.AlertModel
import kotlin.collections.ArrayList

class AlertsFragment(var allAlerts: ArrayList<AlertModel>) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var alertsRecyclerView: RecyclerView
    private var alertsAdapter:AlertsAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)
        alertsRecyclerView = view.findViewById(R.id.alerts_list)
        alertsRecyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        alertsRecyclerView.layoutManager = layoutManager
        alertsRecyclerView.itemAnimator = DefaultItemAnimator()
        alertsAdapter = AlertsAdapter(context!!, allAlerts)
        alertsRecyclerView.adapter = alertsAdapter

        return view
    }

    fun addAlert(alert: AlertModel) {
        allAlerts.add(0, AlertModel(alert.notificationID ,alert.threatId, alert.message, alert.image, alert.time))
        alertsAdapter?.notifyItemInserted(0)
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
