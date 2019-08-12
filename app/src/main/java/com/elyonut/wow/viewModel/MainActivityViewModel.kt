package com.elyonut.wow.viewModel

import android.app.Application
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.AndroidViewModel
import com.elyonut.wow.LayerManager
import com.elyonut.wow.R
import com.elyonut.wow.TempDB
import com.google.android.material.navigation.NavigationView

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val layerManager = LayerManager(TempDB((application)))
    private val idMap = SparseArray<String>()

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

//        layerManager.layerList?.forEach { layer ->
//            menu.add(R.id.layers_group, layer.id.toInt(), 1, layer.name)
//
//        }
    }
}