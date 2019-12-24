package com.elyonut.wow.view

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.elyonut.wow.OnSwipeTouchListener
import com.elyonut.wow.R
import com.elyonut.wow.model.Threat
import com.elyonut.wow.viewModel.DataCardViewModel
import kotlinx.android.synthetic.main.fragment_data_card.view.*

// const variables
private const val CARD_SIZE_RELATION_TO_SCREEN = 0.33
private const val EXPENDED_CARD_SIZE_RELATION_TO_SCREEN = 0.5

class DataCardFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private lateinit var dataCardViewModel: DataCardViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_card, container, false)
        dataCardViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(DataCardViewModel::class.java)
        view.buildingDataCard.layoutParams = dataCardViewModel.getRelativeLayoutParams(CARD_SIZE_RELATION_TO_SCREEN)
        initObservers(view)
        initReadMoreButton(view)
        initClosingCard(view)

        val threat: Threat = arguments!!.getParcelable("threat")
        initThreatInfo(view, threat)

        return view
    }

    private fun initThreatInfo(view: View, threat: Threat){
        val feature = threat.feature.properties()

        view.knowledgeType.text = feature?.get("knowledgeType")?.asString
        view.eAmount.text = feature?.get("eAmount")?.asString
        view.type.text = feature?.get("type")?.asString
        view.range.text = feature?.get("range")?.asString

        view.buildingStateColor.background.setColorFilter(Threat.color(threat), PorterDuff.Mode.MULTIPLY)
    }

    private fun initObservers(view: View) {
        dataCardViewModel.isReadMoreButtonClicked.observe(viewLifecycleOwner, Observer<Boolean> { extendDataCard(view) })
        dataCardViewModel.shouldCloseCard.observe(viewLifecycleOwner, Observer<Boolean> { closeCard() })
    }

    private fun extendDataCard(view: View) {
//        if (dataCardViewModel.isReadMoreButtonClicked.value!!) {
//            view.buildingDataCard.layoutParams =
//                dataCardViewModel.getRelativeLayoutParams(EXPENDED_CARD_SIZE_RELATION_TO_SCREEN)
//            view.moreContent.visibility = View.VISIBLE
//            view.readMore.text = getString(R.string.read_less_hebrew)
//        } else {
//            view.buildingDataCard.layoutParams = dataCardViewModel.getRelativeLayoutParams(CARD_SIZE_RELATION_TO_SCREEN)
//            view.moreContent.visibility = View.GONE
//            view.readMore.text = getString(R.string.read_more_hebrew)
//        }
    }

    private fun closeCard() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@DataCardFragment)?.commit()
    }

    private fun initReadMoreButton(view: View) {
        view.readMore.setOnClickListener {
          //  dataCardViewModel.readMoreButtonClicked(view.moreContent)
        }
    }

    private fun initClosingCard(view: View) {
        initCloseCardByClickOnMap(view)
        initCloseCardButton(view)
        initFlingCloseListener(view)
    }

    private fun initCloseCardByClickOnMap(view: View) {
        view.setOnClickListener {
            dataCardViewModel.close()
        }
    }

    private fun initCloseCardButton(view: View) {
        view.closeButton?.setOnClickListener {
            dataCardViewModel.close()
        }
    }

    private fun initFlingCloseListener(view: View) {
        view.buildingDataCard.setOnTouchListener(object : OnSwipeTouchListener(this@DataCardFragment.context!!) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                dataCardViewModel.close()
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnMapFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onDataCardFragmentInteraction()

    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DataCardFragment().apply {
            }
    }
}
