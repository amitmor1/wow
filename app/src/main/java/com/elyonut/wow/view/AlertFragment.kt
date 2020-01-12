package com.elyonut.wow.view

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.AlertsManager

import com.elyonut.wow.R
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.viewModel.SharedViewModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.alert_item.view.*


class AlertFragment(var alert: AlertModel) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var alertsManager: AlertsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alert, container, false)

        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        alertsManager = sharedViewModel.alertsManager

        initView(view)

        return view
    }

    private fun initView(view: View) {
        Picasso.with(context).load(alert.image).into(view.alert_image)
        view.alert_message.text = alert.message
        view.current_time.text = alert.time

        if (!alert.isRead) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.card_view?.setCardBackgroundColor(context!!.getColor(R.color.unreadMessage))
            }
        } else {
            view.card_view?.setCardBackgroundColor(Color.WHITE)
        }

        initViewButtons(view)
    }

    private fun initViewButtons(view: View) {
        view.zoomToLocation.setOnClickListener {
            alertsManager.zoomToLocation(alert)
        }

        view.alertAccepted.setOnClickListener {
            alertsManager.acceptAlert(alert)
        }

        view.deleteAlert.setOnClickListener {
            alertsManager.deleteAlert(alert)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AlertFragment.
         */
        @JvmStatic
        fun newInstance(alert: AlertModel) =
            AlertFragment(alert).apply {
            }
    }
}
