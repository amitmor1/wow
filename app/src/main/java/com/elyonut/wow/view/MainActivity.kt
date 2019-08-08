package com.elyonut.wow.view

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.elyonut.wow.Constants
import com.elyonut.wow.ILogger
import com.elyonut.wow.R
import com.elyonut.wow.TimberLogAdapter
import com.google.android.material.navigation.NavigationView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(),
    DataCardFragment.OnFragmentInteractionListener,
    MainMapFragment.OnFragmentInteractionListener {

    private val logger: ILogger = TimberLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, Constants.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        logger.initLogger()
        logger.info("started app")
        val navController = findNavController(R.id.nav_host_fragment)
        val drawerLayout = findViewById<DrawerLayout>(R.id.parentLayout)
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
//        setSupportActionBar(toolbar)
        findViewById<Toolbar>(R.id.toolbar).setupWithNavController(navController, appBarConfiguration)

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
