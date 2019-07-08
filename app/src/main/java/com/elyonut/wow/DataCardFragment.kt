package com.elyonut.wow

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_data_card.view.*

class DataCardFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_data_card, container, false)
        initReadMoreButton(view)
        initCloseCardByClick(view)
        initCloseCardButton(view)
        initFlingCloseListener(view)
        return view
    }

    fun onButtonPressed() {
        listener?.onFragmentInteraction()
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

        fun onFragmentInteraction()
    }

    private fun initReadMoreButton(view: View) {
        view.readMore.setOnClickListener {
            if (view.moreContent.visibility == View.GONE) {
                view.moreContent.visibility = View.VISIBLE
                view.readMore.text = getString(R.string.readLessHebrew)
            } else {
                view.moreContent.visibility = View.GONE
                view.readMore.text = getString(R.string.readMoreHebrew)
            }
        }
    }

    private fun initCloseCardByClick(view: View) {
        view.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
        }
    }

    private fun initCloseCardButton(view: View) {
        view.closeButton?.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
        }
    }

    private fun initFlingCloseListener(view: View) {
        view.BuildingDataCard.setOnTouchListener(object : OnSwipeTouchListener(this@DataCardFragment.context!!) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                activity?.supportFragmentManager?.beginTransaction()?.remove(this@DataCardFragment)?.commit()
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DataCardFragment().apply {
            }
    }
}
