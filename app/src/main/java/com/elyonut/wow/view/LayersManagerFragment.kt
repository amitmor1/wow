package com.elyonut.wow.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.elyonut.wow.R
import com.elyonut.wow.viewModel.LayersManagerViewModel

class LayersManagerFragment : Fragment() {

    private lateinit var viewModel: LayersManagerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layers_manager_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProviders.of(this).get(LayersManagerViewModel::class.java)
//         TODO: Use the ViewModel
    }

}
