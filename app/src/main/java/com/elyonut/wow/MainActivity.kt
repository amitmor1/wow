package com.elyonut.wow

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback

// Constant values
//private const val MY_RISK_RADIUS = 300.0
//private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
private const val RECORD_REQUEST_CODE = 101

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener,
    DataCardFragment.OnFragmentInteractionListener {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    //    private var riskStatus: String = R.string.grey_status.toString()
    //    private var lastUpdatedLocation: Location? = null
    private val logger: ILogger = TimberLogAdapter()
    private lateinit var mapViewModel: MapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.MAPBOX_ACCESS_TOKEN))
        setContentView(R.layout.activity_main)
        logger.initLogger()
        logger.info("started app")
        mapViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(MapViewModel::class.java)

        initObservers()

        mapView = findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initLocationButton()
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
        map = mapboxMap
        mapboxMap.addOnMapClickListener(this)

        mapViewModel.onMapReady(mapboxMap)
    }

    private fun initLocationButton() {
        val currentLocationButton: View = findViewById(R.id.currentLocation)
        currentLocationButton.setOnClickListener {
            mapViewModel.focusOnMyLocation()
        }
    }

    override fun onMapClick(latLng: LatLng): Boolean {

        return mapViewModel.onMapClick(map, latLng)
    }

//    private fun calcRiskStatus(location: Location) {
//        val allFeatures = getFeatures()
//        var currentFeatureLocation: LatLng
//
//        run loop@{
//            riskStatus = R.string.grey_status.toString()
//
//            allFeatures.features()?.forEach { it ->
//                val currentLatitude = it.properties()?.get("latitude")
//                val currentLongitude = it.properties()?.get("longitude")
//
//                if ((currentLatitude != null) || (currentLongitude != null)) {
//                    currentFeatureLocation = LatLng(currentLatitude!!.asDouble, currentLongitude!!.asDouble)
//                    val featureRiskRadius = it.properties()?.get("radius").let { t -> t?.asDouble }
//
//                    val distSq: Double = kotlin.math.sqrt(
//                        ((location.longitude - currentFeatureLocation.longitude)
//                                * (location.longitude - currentFeatureLocation.longitude))
//                                + ((location.latitude - currentFeatureLocation.latitude)
//                                * (location.latitude - currentFeatureLocation.latitude))
//                    )
//
//                    if (distSq + MY_RISK_RADIUS <= featureRiskRadius!!) {
//                        riskStatus = R.string.red_status.toString()
//                        return@loop
//                    } else if ((kotlin.math.abs(MY_RISK_RADIUS - featureRiskRadius) <= distSq && distSq <= (MY_RISK_RADIUS + featureRiskRadius))) {
//                        riskStatus = R.string.orange_status.toString()
//                    }
//                }
//            }
//        }
//
//    }

//    private fun getFeatures(): FeatureCollection {
//        val stream: InputStream = assets.open("features.geojson")
//        val size = stream.available()
//        val buffer = ByteArray(size)
//        stream.read(buffer)
//        stream.close()
//        val jsonObj = String(buffer, charset("UTF-8"))
//        return FeatureCollection.fromJson(jsonObj)
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

    override fun onFragmentInteraction() {
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent leaks
        mapViewModel.clean()
        map.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }
}
