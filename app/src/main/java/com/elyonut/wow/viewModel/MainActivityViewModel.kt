package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.LayerManager
import com.elyonut.wow.TempDB
import com.elyonut.wow.view.LayerMenuAdapter

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: LayerMenuAdapter
    private val layerManager = LayerManager(TempDB((application)))
    var chosenLayerId = MutableLiveData<String>()

    fun initRecyclerView(recyclerView: RecyclerView) {
        viewManager = LinearLayoutManager(getApplication())
        if (layerManager.layerList != null) {
            viewAdapter = LayerMenuAdapter(layerManager.layerList!!.toTypedArray())
            recyclerView.apply {
                layoutManager = viewManager
                adapter = viewAdapter
            }

            chosenLayerId = Transformations.map(viewAdapter.layerSelected) { l -> l.id } as MutableLiveData<String>
//            viewAdapter.layerSelected.observe()

        }


    }
}

