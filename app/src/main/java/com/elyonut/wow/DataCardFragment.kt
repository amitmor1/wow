package com.elyonut.wow

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.fragment_data_card.view.*

class DataCardFragment : Fragment() {

    // const variables
    private val cardSizeRelativeToScreen = 0.33
    private val openedCardSizeRelativeToScreen = 0.5

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
        view.BuildingDataCard.layoutParams = getRelativeLayoutParams(cardSizeRelativeToScreen)
        initReadMoreButton(view)
        initCloseCardByClick(view)
        initCloseCardButton(view)
        initFlingCloseListener(view)
        return view
    }

    private fun initReadMoreButton(view: View) {
        view.ReadMore.setOnClickListener {
            if (view.MoreContent.visibility == View.GONE) {
                view.BuildingDataCard.layoutParams = getRelativeLayoutParams(openedCardSizeRelativeToScreen)
                view.MoreContent.visibility = View.VISIBLE
                view.ReadMore.text = getString(R.string.readLessHebrew)
            } else {
                view.BuildingDataCard.layoutParams = getRelativeLayoutParams(cardSizeRelativeToScreen)
                view.MoreContent.visibility = View.GONE
                view.ReadMore.text = getString(R.string.readMoreHebrew)
            }
        }
    }

    private fun initCloseCardByClick(view: View) {
        view.setOnClickListener {
            closeCard()
        }
    }

    private fun initCloseCardButton(view: View) {
        view.CloseButton?.setOnClickListener {
            closeCard()
        }
    }

    private fun initFlingCloseListener(view: View) {
        view.BuildingDataCard.setOnTouchListener(object : OnSwipeTouchListener(this@DataCardFragment.context!!) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                closeCard()
            }
        })
    }

    private fun closeCard() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@DataCardFragment)?.commit()
    }

    private fun getRelativeLayoutParams(sizeRelativelyToScreen: Double): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            (getDeviceHeight() * sizeRelativelyToScreen).toInt()
        )
    }

    private fun getDeviceHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
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

    companion object {
        @JvmStatic
        fun newInstance() =
            DataCardFragment().apply {
            }
    }
}
