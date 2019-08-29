package com.elyonut.wow

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Spinner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.viewModel.SharedViewModel
import kotlinx.android.synthetic.main.fragment_filter.view.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FilterFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FilterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FilterFragment : Fragment(), AdapterView.OnItemSelectedListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_filter, container, false)

        mainViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(MainActivityViewModel::class.java)
        sharedViewModel =
            ViewModelProviders.of(this)[SharedViewModel::class.java]

        mainViewModel.chosenLayerId.observe(this, Observer<String> {
            mainViewModel.chosenLayerId.value?.let {
                sharedViewModel.select(it)
            }
        })

        mainViewModel.filterLayerId.observe(this, Observer<String> {
            val propertySpinner = findViewById<Spinner>(R.id.propertiesSpinner)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mainViewModel.initPropertiesAdapter(it))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            propertySpinner.adapter = adapter
        })

        mainViewModel.chosenProperty.observe(this, Observer<String> {
            mainViewModel.initOptionsDropDown(it)
        })

        mainViewModel.isNumberProperty.observe(this, Observer<Boolean> {
            changeViewsVisibility(findViewById<RelativeLayout>(R.id.numberOptions), it)

        })
        mainViewModel.isStringProperty.observe(this, Observer<Boolean> { changeViewsVisibility(findViewById<RelativeLayout>(R.id.stringOptions), it) })

        initOkButton()
        initCancelButton()
        initToolbar()
        initRecyclerView()
        initSpinners()

        return view
    }

    private fun initOkButton(view: View) {
        val okButton: View = view.ok_button
        okButton.setOnClickListener {
            sharedViewModel.filterLayer(mainViewModel.filterLayerId.value)
        }
    }

    private fun initCancelButton(view: View) {
        val cancelButton: View = view.cancel_button
        cancelButton.setOnClickListener {
            sharedViewModel.filterLayer(null)
        }
    }

    private fun initSpinners(view: View) {
        initLayersSpinner(view)
        initPropertiesSpinner(view)
        initStringPropertiesSpinner(view)
        initNumberPropertiesSpinner(view)
    }

    private fun initLayersSpinner(view: View) {
        val layerSpinner = view.layersSpinner
        val layerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mainViewModel.initLayerAdapter()
        )
        layerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        layerSpinner.adapter = layerAdapter
        layerSpinner.onItemSelectedListener = this
    }

    private fun initPropertiesSpinner(view: View) {
        val propertySpinner = view.propertiesSpinner
        propertySpinner.onItemSelectedListener = this
    }

    private fun initNumberPropertiesSpinner(view: View) {
        val numberSpinner = view.numberPropertySpinner
        val numberAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mainViewModel.initNumberPropertyAdapter())
        numberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        numberSpinner.adapter = numberAdapter
        numberSpinner.onItemSelectedListener = this
    }

    private fun initStringPropertiesSpinner(view: View) {
        val stringPropertySpinner = view.stringPropertySpinner
        stringPropertySpinner.onItemSelectedListener = this
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.layersSpinner -> mainViewModel.onLayerItemSelected(position)
            R.id.propertiesSpinner -> mainViewModel.onPropertyItemSelected(position)
            R.id.numberPropertySpinner -> mainViewModel.onNumberItemSelected(position)
        }
    }

    private fun changeViewsVisibility(view: View, isVisible: Boolean) {
        if (isVisible) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }


    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFilterFragmentInteraction()
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
        // TODO: Update argument type and name
        fun onFilterFragmentInteraction()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FilterFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            FilterFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
