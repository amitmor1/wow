package com.elyonut.wow.view

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.Constants
import com.elyonut.wow.ILogger
import com.elyonut.wow.R
import com.elyonut.wow.TimberLogAdapter
import com.elyonut.wow.viewModel.MainActivityViewModel
import com.google.android.material.navigation.NavigationView
import com.mapbox.mapboxsdk.Mapbox

class MainActivity : AppCompatActivity(),
    DataCardFragment.OnFragmentInteractionListener,
    MainMapFragment.OnFragmentInteractionListener {

    private lateinit var viewModel: MainActivityViewModel

    private val logger: ILogger = TimberLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, Constants.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        logger.initLogger()
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        viewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(MainActivityViewModel::class.java)

        initToolbar()
        initRecyclerView()
//        initMenu(navigationView)
    }

    private fun initRecyclerView() {
        viewModel.initRecyclerView(findViewById<RecyclerView>(R.id.layersRecyclerView))
    }

    private fun initMenu(navigationView: NavigationView) {
//        val menu = navigationView.menu
//        val subMenu = menu.addSubMenu(R.id.layers_group, Menu.NONE, 1, R.string.layers_item)
//        subMenu.clear()
//        navigationView.invalidate()
        viewModel.buildMenu(navigationView)
    }

    private fun initToolbar() {
        val navController = findNavController(R.id.nav_host_fragment)
        val drawerLayout = findViewById<DrawerLayout>(R.id.parentLayout)
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onMainFragmentInteraction() {
    }

    override fun onFragmentInteraction() {
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
    }
}
