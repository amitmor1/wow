package com.elyonut.wow.viewModel

import android.app.Application
import android.util.SparseArray
import android.view.Menu
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.LayerManager
import com.elyonut.wow.R
import com.elyonut.wow.TempDB
import com.elyonut.wow.view.LayerMenuAdapter
import com.google.android.material.navigation.NavigationView

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: RecyclerView.Adapter<LayerMenuAdapter.LayerViewHolder>
    private val layerManager = LayerManager(TempDB((application)))
    private val idMap = SparseArray<String>()

    fun initRecyclerView(recyclerView: RecyclerView) {
        viewManager = LinearLayoutManager(getApplication())
        if (layerManager.layerList != null) {
            viewAdapter = LayerMenuAdapter(layerManager.layerList!!.toTypedArray())
            recyclerView.apply {
                layoutManager = viewManager
                adapter = viewAdapter
            }
        }
    }


    fun buildMenu(navigationView: NavigationView) {
        val menu = navigationView.menu
        val subMenu = menu.addSubMenu(R.id.layers_group, Menu.NONE, 1, R.string.layers_item)
        var index = 1

        idMap.clear()
        subMenu.clear()
        navigationView.invalidate()

        layerManager.layerList?.forEach { layer ->
            idMap.append(index, layer.id)
            val item = subMenu.add(R.id.layers_group, index, index, layer.name)
            index++
            item.isCheckable = true

        }
        navigationView.invalidate()
    }
}

