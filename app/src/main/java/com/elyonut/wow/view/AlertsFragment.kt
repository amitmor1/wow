package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.AlertsAdapter
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.AlertsViewModelFactory
import com.elyonut.wow.R
import com.elyonut.wow.viewModel.AlertsViewModel
import com.elyonut.wow.viewModel.SharedViewModel

class AlertsFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var alertsRecyclerView: RecyclerView
    private lateinit var onClickHandler: OnClickInterface
    private var layoutManager: RecyclerView.LayoutManager? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var alertsViewModel: AlertsViewModel
    private lateinit var alertsManager: AlertsManager

    interface OnClickInterface {
        fun setClick(view: View, position: Int)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)

        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        alertsManager = sharedViewModel.alertsManager
        initClickInterface()

        alertsViewModel = ViewModelProviders.of(
            this,
            AlertsViewModelFactory(activity!!.application, alertsManager, onClickHandler)
        ).get(AlertsViewModel::class.java)

        alertsRecyclerView = view.findViewById(R.id.alerts_list)
        alertsRecyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        alertsRecyclerView.layoutManager = layoutManager
        alertsRecyclerView.itemAnimator = DefaultItemAnimator()

        alertsRecyclerView.adapter = alertsViewModel.alertsAdapter

        setObservers()

        return view
    }


    private fun initClickInterface() {
        onClickHandler = object : OnClickInterface {
            override fun setClick(view: View, position: Int) {
                when (view.id) {
                    R.id.deleteAlert -> {
                        alertsViewModel.deleteAlertClicked(position)
                    }
                    R.id.zoomToLocation -> {
                        alertsViewModel.zoomToLocationClicked(alertsManager.alerts.value!![position])
                    }
                    R.id.alertAccepted -> {
                        alertsViewModel.acceptAlertClicked(alertsManager.alerts.value!![position])
                    }
                }
            }
        }
    }

    private fun setObservers() {
        alertsManager.isAlertChanged.observe(this, Observer {
            alertsViewModel.setAlertAccepted()
        })

        alertsManager.isAlertAdded.observe(this, Observer {
            alertsViewModel.addAlert()
        })

        alertsManager.deletedAlertPosition.observe(this, Observer {
            alertsViewModel.deleteAlert(it)
        })
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
        fun newInstance() =
            AlertsFragment().apply {
            }
    }
}
