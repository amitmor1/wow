package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.LayerManager
import com.elyonut.wow.R
import com.elyonut.wow.TempDB
import com.elyonut.wow.view.LayerMenuAdapter
import com.google.android.material.navigation.NavigationView

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val layerManager = LayerManager(TempDB((application)))
    var chosenLayerId = MutableLiveData<String>()

    fun initNavigationMenu(
        navigationView: NavigationView,
        actionView: View,
        selectedListener: (MenuItem) -> Boolean
    ) {
        val menu = navigationView.menu
        if (layerManager.layerList != null) {
            val layers = layerManager.layerList!!.toTypedArray()
            val layersSubMenu = menu.getItem(0).subMenu
            layers.forEachIndexed { index, layerModel ->
                val menuItem = layersSubMenu.add(R.id.nav_layers, index, index, layerModel.name)
                menuItem.actionView = actionView
                actionView.tag = layerModel
                val checkBoxView = actionView as CheckBox
                menuItem.isCheckable = true
                checkBoxView.setOnCheckedChangeListener { _, isChecked ->
                    menuItem.isChecked = isChecked
                    navigationView.setCheckedItem(menuItem.itemId)
                    selectedListener(menuItem)
                }
            }

        }

    }
}

