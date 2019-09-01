package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.R
import com.elyonut.wow.viewModel.FilterViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import kotlinx.android.synthetic.main.fragment_filter.view.*

class FilterFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var fragmentContext: OnFragmentInteractionListener? = null
    private lateinit var filterViewModel: FilterViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)

        filterViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(FilterViewModel::class.java)
        sharedViewModel =
            ViewModelProviders.of(this)[SharedViewModel::class.java]

        setObservers(view)
        initOkButton(view)
        initCancelButton(view)
        initSpinners(view)

        return view
    }

    private fun setObservers(view: View) {
        filterViewModel.chosenLayerId.observe(this, Observer<String> {
            val propertySpinner = view.propertiesSpinner
            val propertiesList = filterViewModel.initPropertiesList(it)
            if (propertiesList != null) {
                val adapter = ArrayAdapter(
                    activity!!.application,
                    android.R.layout.simple_spinner_item,
                    propertiesList.toMutableList()
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                propertySpinner.adapter = adapter
            } // TODO what else?? if null?
        })

        filterViewModel.chosenProperty.observe(this, Observer<String> {
            filterViewModel.initOptionsList(it)
        })

        filterViewModel.isNumberProperty.observe(this, Observer<Boolean> {
            changeViewsVisibility(view.numberOptions, it)

        })

        filterViewModel.isStringProperty.observe(this, Observer<Boolean> {
            initStringPropertiesSpinner(view)
            changeViewsVisibility(view.stringOptions, it)
        })
    }

    private fun initOkButton(view: View) {
        val okButton: View = view.ok_button
        okButton.setOnClickListener {
            sharedViewModel.filterLayer(filterViewModel.chosenLayerId.value)
            sharedViewModel.chosenProperty(filterViewModel.chosenProperty.value)
        }
    }

    private fun initCancelButton(view: View) {
        val cancelButton: View = view.cancel_button
        cancelButton.setOnClickListener {
            sharedViewModel.filterLayer("")
            sharedViewModel.chosenProperty("")
        }
    }

    private fun initSpinners(view: View) {
        initLayersSpinner(view)
        initPropertiesSpinner(view)
        initNumberPropertiesSpinner(view)
//        initStringPropertiesSpnner(view)???
    }

    private fun initLayersSpinner(view: View) {
        val layerSpinner = view.layersSpinner
        val layersList = filterViewModel.getLayersList()
        if (layersList != null) {
            val layerAdapter = ArrayAdapter(
                activity!!.application,
                android.R.layout.simple_spinner_item,
                layersList.toMutableList()
            )

            layerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            layerSpinner.adapter = layerAdapter
            layerSpinner.onItemSelectedListener = this
        }  // TODO what else?? if null?
    }

    private fun initPropertiesSpinner(view: View) {
        val propertySpinner = view.propertiesSpinner
        propertySpinner.onItemSelectedListener = this
    }

    private fun initNumberPropertiesSpinner(view: View) {
        val numberSpinner = view.numberPropertySpinner
        val numberAdapter = ArrayAdapter(
            activity!!.application,
            android.R.layout.simple_spinner_item,
            filterViewModel.initNumberPropertyOptionsList()
        )
        numberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        numberSpinner.adapter = numberAdapter
        numberSpinner.onItemSelectedListener = this
    }

    private fun initStringPropertiesSpinner(view: View) {
        val stringPropertySpinner = view.stringPropertySpinner
        val allPropertiesValues =
            filterViewModel.initStringPropertyOptions(filterViewModel.chosenProperty.value!!)
        if (allPropertiesValues != null) {
            val stringAdapter = ArrayAdapter(
                activity!!.application,
                android.R.layout.simple_spinner_item,
                allPropertiesValues.toMutableList()
            )

            stringAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            stringPropertySpinner.adapter = stringAdapter
            stringPropertySpinner.onItemSelectedListener = this
        } // TODO what else?? if null?
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.layersSpinner -> filterViewModel.onLayerItemSelected(position)
            R.id.propertiesSpinner -> filterViewModel.onPropertyItemSelected(position)
            R.id.numberPropertySpinner -> filterViewModel.onNumberItemSelected(position)
//            R.id.stringPropertySpinner -> filterViewModel.onNumberItemSelected(position)
        }
    }

    private fun changeViewsVisibility(view: View, isVisible: Boolean) {
        if (isVisible) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    private fun rangeFilter() {

    }

    private fun lowerRange() {

    }

    private fun upperRange() {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            fragmentContext = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        fragmentContext = null
    }

    interface OnFragmentInteractionListener {
        fun onFilterFragmentInteraction()
    }

    companion object {
        @JvmStatic
        fun newInstance() = FilterFragment()
    }
}
