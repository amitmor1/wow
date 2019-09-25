package com.elyonut.wow.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.elyonut.wow.*
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.model.Threat
import com.elyonut.wow.viewModel.MainActivityViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),
    DataCardFragment.OnFragmentInteractionListener,
    NavigationView.OnNavigationItemSelectedListener,
    ThreatFragment.OnListFragmentInteractionListener,
    MainMapFragment.OnMapFragmentInteractionListener,
    FilterFragment.OnFragmentInteractionListener {

    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private val logger: ILogger = TimberLogAdapter()
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("com.elyonut.wow.prefs", Context.MODE_PRIVATE)
        Mapbox.getInstance(applicationContext, Constants.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        logger.initLogger()

        mainViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                .create(MainActivityViewModel::class.java)
        sharedViewModel =
            ViewModelProviders.of(this)[SharedViewModel::class.java]

        setObservers()
        initArea()
        initToolbar()
        initNavigationMenu()
    }

    private fun setObservers() {
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
        mainViewModel.shouldDefineArea.observe(this, Observer {
            if (it) {
                sharedViewModel.shouldDefineArea.value = it
            }
        })

        sharedViewModel.shouldDefineArea.observe(this, Observer {
            if (!it) {
                mainViewModel.shouldDefineArea.value = it
            }
        })
    }

    private fun initArea() {
        val areaOfInterestJson = sharedPreferences.getString(Constants.AREA_OF_INTEREST_KEY, "")

        if (areaOfInterestJson != "") {
            val areaLinesJson = sharedPreferences.getString(Constants.AREA_LINES_KEY, "")
//            val areaCirclesJson = sharedPreferences.getString(Constants.AREA_CIRCLES_KEY, "")

            sharedViewModel.areaOfInterest =
                gson.fromJson<Polygon>(areaOfInterestJson, Polygon::class.java)
            sharedViewModel.areaOfInterestLines =
                gson.fromJson<Array<Point>>(areaLinesJson, Array<Point>::class.java)
                    .toCollection(ArrayList())
//            sharedViewModel.areaOfInterestCircles =
//                gson.fromJson<ArrayList<Feature>>(areaCirclesJson, Array<Feature>::class.java)
//                    .toCollection(ArrayList())

        } else {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.area_not_defined))
                .setPositiveButton(getString(R.string.yes_hebrew)) { _, _ ->
                    mainViewModel.shouldDefineArea.value = true
                }.setNegativeButton(getString(R.string.no_thanks_hebrew)) { dialog, _ ->
                    dialog.cancel()
                }.show()
        }
    }

    private fun filterButtonClicked() {
        val filterFragment = FilterFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.apply {
            add(R.id.fragmentMenuParent, filterFragment).commit()
            addToBackStack(filterFragment.javaClass.simpleName)
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
        val checkBoxView = layoutInflater.inflate(R.layout.widget_check, null) as CheckBox
        navigationView.setNavigationItemSelectedListener(this)

        val layers = mainViewModel.getLayersList()?.toTypedArray()
        if (layers != null) {
            val menu = navigationView.menu
            val layersSubMenu = menu.getItem(0).subMenu
            layers.forEachIndexed { index, layerModel ->
                val menuItem = layersSubMenu.add(R.id.nav_layers, index, index, layerModel.name)
                checkBoxView.tag = layerModel
                menuItem.actionView = checkBoxView
                checkBoxView.setOnCheckedChangeListener { _, _ ->
                    (::onNavigationItemSelected)(menuItem)
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (mainViewModel.onNavigationItemSelected(item)) {
            closeDrawer()
        }

        return true
    }

    private fun closeDrawer() {
        findViewById<DrawerLayout>(R.id.parentLayout).closeDrawer(GravityCompat.START)
    }

    override fun onMapFragmentInteraction() {
    }

    override fun onFilterFragmentInteraction() {
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

    override fun onPause() {
        super.onPause()
        val areaOfInterestJson = gson.toJson(sharedViewModel.areaOfInterest)
        val areaOfInterestLinesJson = gson.toJson(sharedViewModel.areaOfInterestLines)
        val areaOfInterestCirclesJson = gson.toJson(sharedViewModel.areaOfInterestCircles)

        sharedPreferences.edit()
            .putString(Constants.AREA_OF_INTEREST_KEY, areaOfInterestJson)
            .putString(Constants.AREA_LINES_KEY, areaOfInterestLinesJson)
            .putString(Constants.AREA_CIRCLES_KEY, areaOfInterestCirclesJson)
            .apply()
    }
}
