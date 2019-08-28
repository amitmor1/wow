package com.elyonut.wow.view

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.elyonut.wow.*
import com.elyonut.wow.model.Threat
import com.elyonut.wow.viewModel.MainActivityViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import com.google.android.material.navigation.NavigationView
import com.mapbox.mapboxsdk.Mapbox

class MainActivity : AppCompatActivity(),
    DataCardFragment.OnFragmentInteractionListener,
    NavigationView.OnNavigationItemSelectedListener,
    ThreatFragment.OnListFragmentInteractionListener,
    MainMapFragment.OnFragmentInteractionListener,
    BlankFragment.OnFragmentInteractionListener {

    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private val logger: ILogger = TimberLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, Constants.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        logger.initLogger()

        mainViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                .create(MainActivityViewModel::class.java)
        sharedViewModel =
            ViewModelProviders.of(this)[SharedViewModel::class.java]

        initObservers()
        initToolbar()
        initNavigationMenu()
    }

    private fun initObservers() {
        mainViewModel.chosenLayerId.observe(this, Observer<String> {
            mainViewModel.chosenLayerId.value?.let {
                sharedViewModel.selectLayer(it)
            }
        })

        mainViewModel.selectedExperimentalOption.observe(
            this,
            Observer { sharedViewModel.selectExperimentalOption(it) }
        )

        mainViewModel.filterSelected.observe(this, Observer {
            if (it) {
                filterButtonClicked()
            }
        })
    }

    private fun filterButtonClicked() {
        val blankFragment = BlankFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.apply {
            add(R.id.fragmentMenuParent, blankFragment).commit()
            addToBackStack(blankFragment.javaClass.simpleName)
        }
    }

    private fun initToolbar() {
        val navController = findNavController(R.id.nav_host_fragment)
        val drawerLayout = findViewById<DrawerLayout>(R.id.parentLayout)
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    private fun initNavigationMenu() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val checkBoxView = layoutInflater.inflate(R.layout.widget_check, null)
        navigationView.setNavigationItemSelectedListener(this)
        mainViewModel.initNavigationMenu(navigationView, checkBoxView, ::onNavigationItemSelected)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (mainViewModel.onNavigationItemSelected(item))
            closeDrawer()

        return true
    }

    private fun closeDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.parentLayout)
        drawer.closeDrawer(GravityCompat.START)
    }

    override fun onMainFragmentInteraction() {
    }

    override fun onBlankFragmentInteraction() {
    }

    override fun onDataCardFragmentInteraction() {
    }

    override fun onListFragmentInteraction(item: Threat?) {
        sharedViewModel.selectedThreatItem.value = item
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
    }
}
