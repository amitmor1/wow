package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.AlertsAdapter
import com.elyonut.wow.R
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.viewModel.AlertsViewModel
import kotlinx.android.synthetic.main.alert_item.view.*
import kotlin.collections.ArrayList
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.AlertsViewModelFactory

class AlertsFragment(var allAlerts: MutableLiveData<ArrayList<AlertModel>>) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var alertsRecyclerView: RecyclerView
    private var alertsAdapter: AlertsAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
//    private lateinit var alertsViewModel: AlertsViewModel

    interface OnClickInterface {
        fun setClick(view: View, position: Int)
    }

    private lateinit var onClickInterface: OnClickInterface

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
        initClickInterface()
        alertsAdapter = AlertsAdapter(context!!, allAlerts.value!!, onClickInterface)
        alertsRecyclerView.adapter = alertsAdapter

        return view
    }

    private fun initClickInterface() {
        onClickInterface = object : OnClickInterface {
            override fun setClick(view: View, position: Int) {
                when (view.id) {
                    R.id.deleteAlert -> {
                        deleteAlert(position)
                    }
                }
            }
        }
    }

    fun addAlert(alert: AlertModel) {
        allAlerts.value?.add(
            0,
            AlertModel(alert.notificationID, alert.threatId, alert.message, alert.image, alert.time)
        )
        alertsAdapter?.notifyItemInserted(0)
        updateAlerts()
    }

    fun setAlertAccepted() {
        alertsAdapter?.notifyDataSetChanged()
        updateAlerts()
    }


    fun deleteAlert(position: Int) {
        allAlerts.value?.removeAt(position)
        alertsAdapter?.notifyItemRemoved(position)
        alertsAdapter?.notifyItemRangeChanged(position, allAlerts.value!!.count())
        updateAlerts()
    }

    private fun updateAlerts() {
        allAlerts.value = allAlerts.value
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
        fun newInstance(allAlerts: MutableLiveData<ArrayList<AlertModel>>) =
            AlertsFragment(allAlerts).apply {
            }
    }
}
