package com.elyonut.wow.view

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.elyonut.wow.*
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController

// Constant values
//private const val MY_RISK_RADIUS = 300.0
//private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
private const val RECORD_REQUEST_CODE = 101

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
//    MapboxMap.OnMapClickListener,
    DataCardFragment.OnFragmentInteractionListener,
    MainMapFragment.OnFragmentInteractionListener {

    private var mapView: MapView? = null
    //    private lateinit var map: MapboxMap
    //    private var lastUpdatedLocation: Location? = null
    private val logger: ILogger = TimberLogAdapter()
    private lateinit var mapViewModel: MapViewModel
    private var threatStatus: View? = null

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.navigation_items, menu)
//        return super.onCreateOptionsMenu(menu)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, Constants.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        logger.initLogger()
        logger.info("started app")
//        mapView = findViewById(R.id.mainMapView)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        findViewById<Toolbar>(R.id.appToolbar).setupWithNavController(navController, appBarConfiguration)

        mapViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(MapViewModel::class.java)
//
//        initObservers()
//        threatStatus = findViewById(R.id.status)
//        mapView = findViewById(R.id.mainMapView)
//        mapView.onCreate(savedInstanceState)
//        mapView.getMapAsync(this)
//
//        initFocusOnMyLocationButton()
//        initShowRadiusLayerButton()
    }

    private fun initObservers() {
        mapViewModel.selectedBuildingId.observe(this, Observer<String> { showDescriptionFragment() })
        mapViewModel.isPermissionRequestNeeded.observe(this, Observer<Boolean> {
            if (it != null && it) {
                requestPermissions()
            }
        })
        mapViewModel.isAlertVisible.observe(this, Observer<Boolean> { showAlertDialog() })
        mapViewModel.noPermissionsToast.observe(this, Observer<Toast> { showToast() })
        mapViewModel.threatStatus.observe(this, Observer<String> { changeStatus(it) })
    }

    private fun changeStatus(status: String?) {
        (threatStatus as Button).text = status
    }

    private fun showDescriptionFragment() {
        val dataCardFragmentInstance = DataCardFragment.newInstance()

        if (supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
            supportFragmentManager.beginTransaction().add(R.id.fragmentParent, dataCardFragmentInstance).commit()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            RECORD_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mapViewModel.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(this).setTitle(getString(R.string.turn_on_location_title))
            .setMessage(getString(R.string.turn_on_location))
            .setPositiveButton(getString(R.string.yes_hebrew)) { _, _ ->
                val settingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(settingIntent)
            }.setNegativeButton(getString(R.string.no_thanks_hebrew)) { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    private fun showToast() {
        mapViewModel.noPermissionsToast.value?.show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
//        map = mapboxMap
//        mapboxMap.addOnMapClickListener(this)

        mapViewModel.onMapReady(mapboxMap)
    }

    private fun initFocusOnMyLocationButton() {
        val currentLocationButton: View = findViewById(R.id.currentLocation)
        currentLocationButton.setOnClickListener {
            mapViewModel.focusOnMyLocationClicked()
        }
    }

    private fun initShowRadiusLayerButton() {
        val radiusLayerButton: View = findViewById(R.id.radiusLayer)
        radiusLayerButton.setOnClickListener {
            mapViewModel.showRadiusLayerButtonClicked(Constants.threatRadiusLayerId)
        }
    }

//    override fun onMapClick(latLng: LatLng): Boolean {

//        return mapViewModel.onMapClick(map, latLng)
//    }

//    private fun initOfflineMap(loadedMapStyle: Style) {
//
//        val offlineManager = OfflineManager.getInstance(this@MainActivity)
//        val definition = getDefinition(loadedMapStyle)
//        val metadata = getMetadata()
//        val callback = getOfflineRegionCallback()
//
//        if (metadata != null) {
//            offlineManager.createOfflineRegion(
//                definition,
//                metadata,
//                callback
//            )
//        }
//    }

//    private fun getOfflineRegionCallback(): OfflineManager.CreateOfflineRegionCallback {
//
//        return object : OfflineManager.CreateOfflineRegionCallback {
//            override fun onCreate(offlineRegion: OfflineRegion?) {
//                offlineRegion?.setDownloadState(OfflineRegion.STATE_ACTIVE)
//                offlineRegion?.setObserver(getObserver())
//            }
//
//            override fun onError(error: String?) {
//                error?.let { logger.error(it) }
//            }
//        }
//    }

//    private fun getObserver(): OfflineRegion.OfflineRegionObserver {
//
//        return object : OfflineRegion.OfflineRegionObserver {
//            override fun onStatusChanged(status: OfflineRegionStatus) {
//                val percentage = if (status.requiredResourceCount >= 0)
//                    100.0 * status.completedResourceCount / status.requiredResourceCount else 0.0
//
//                if (status.isComplete) {
//                    logger.debug("Region downloaded successfully.")
//                } else if (status.isRequiredResourceCountPrecise) {
//                    logger.debug(percentage.toString())
//                }
//            }
//
//            override fun onError(error: OfflineRegionError) {
//                logger.error("onError reason: " + error.reason)
//                logger.error("onError message: %s" + error.message)
//            }
//
//            override fun mapboxTileCountLimitExceeded(limit: Long) {
//                logger.error("Mapbox tile count limit exceeded: $limit")
//            }
//
//        }
//    }

//    private fun getDefinition(loadedMapStyle: Style): OfflineRegionDefinition {
//
//        // Create a bounding box for the offline region
//        val latLngBounds = LatLngBounds.Builder()
//            .include(LatLng(32.1826, 35.0110)) // Northeast
//            .include(LatLng(31.9291, 34.5808)) // Southwest
//            .build()
//
//        return OfflineTilePyramidRegionDefinition(
//            loadedMapStyle.url,
//            latLngBounds,
//            10.0,
//            20.0,
//            resources.displayMetrics.density
//        )
//    }

//    private fun getMetadata(): ByteArray? {
//        var metadata: ByteArray? = null
//        try {
//            val jsonObject = JSONObject()
//            jsonObject.put(getString(R.string.json_field_region_name), getString(R.string.region_name))
//            val json = jsonObject.toString()
//            metadata = json.toByteArray(charset(getString(R.string.charset)))
//        } catch (exception: Exception) {
//            logger.error("Failed to encode metadata: " + exception.message)
//        } finally {
//            return metadata
//        }
//    }

    override fun onMainFragmentInteraction() {
    }

    override fun onFragmentInteraction() {
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent leaks
        mapViewModel.clean()
//        map.removeOnMapClickListener(this)
        mapView?.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        if (outState != null) {
            mapView?.onSaveInstanceState(outState)
        }
    }
}
