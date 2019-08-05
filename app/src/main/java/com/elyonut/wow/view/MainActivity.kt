package com.elyonut.wow.view

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.elyonut.wow.*
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback


import android.widget.Button
import android.widget.PopupMenu
import com.elyonut.wow.analysis.ThreatAnalyzer
import com.elyonut.wow.analysis.TopographyService
import com.elyonut.wow.model.Threat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

// Constant values
private const val RECORD_REQUEST_CODE = 101

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener,
    DataCardFragment.OnFragmentInteractionListener,
    PopupMenu.OnMenuItemClickListener,
    ThreatFragment.OnListFragmentInteractionListener {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    //    private var lastUpdatedLocation: Location? = null
    private val logger: ILogger = TimberLogAdapter()
    private lateinit var mapViewModel: MapViewModel
    private lateinit var threatStatus: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, Constants.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        logger.initLogger()
        logger.info("started app")
        mapViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(MapViewModel::class.java)

        initObservers()
        threatStatus = findViewById(R.id.status)
        mapView = findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initFocusOnMyLocationButton()
        initShowRadiusLayerButton()
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
        map = mapboxMap
        mapboxMap.addOnMapClickListener(this)

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

    override fun onMapClick(latLng: LatLng): Boolean {

        // return mapViewModel.onMapClick(map, latLng)

        val loadedMapStyle = map.style

        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
            return false
        }

        loadedMapStyle.removeLayer("threat-source-layer")
        loadedMapStyle.removeSource("threat-source")

        loadedMapStyle.removeLayer("layer-selected-location")
        loadedMapStyle.removeSource("source-marker-click")
        loadedMapStyle.removeImage("marker-icon-id")

        if (mapViewModel.selectLocationManual) {

            // Add the marker image to map
            loadedMapStyle.addImage(
                "marker-icon-id",
                BitmapFactory.decodeResource(
                    App.resourses, R.drawable.mapbox_marker_icon_default
                )
            )

            val geoJsonSource = GeoJsonSource(
                "source-marker-click",
                Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude))
            )

            loadedMapStyle.addSource(geoJsonSource)

            val symbolLayer = SymbolLayer("layer-selected-location", "source-marker-click")
            symbolLayer.withProperties(
                PropertyFactory.iconImage("marker-icon-id")
            )
            loadedMapStyle.addLayer(symbolLayer)

            mapViewModel.updateThreatFeatures(mapView, latLng)
            mapViewModel.threatFeatures.value?.let { visualizeThreats(it) }
            mapViewModel.selectLocationManual = false

        } else {

            val point = map.projection.toScreenLocation(latLng)
            val features = map.queryRenderedFeatures(point, getString(R.string.buildings_layer))

            if (features.size > 0) {
                val selectedBuildingSource =
                    loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.selectedBuildingSourceId)
                selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))

                val threat = mapViewModel.buildingThreatToCurrentLocation(mapView, features[0])

                val bundle = Bundle()
                bundle.putParcelable("threat", threat)

                val dataCardFragmentInstance = DataCardFragment.newInstance()
                dataCardFragmentInstance.arguments = bundle
                if (supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()

            }
        }

        return true
    }

    fun onMenuClick(view: View) {
        PopupMenu(this, view).apply {
            setOnMenuItemClickListener(this@MainActivity)
            inflate(R.menu.menu)
            show()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.threat_list_menu_item -> {

                mapViewModel.updateThreats(mapView)

                mapViewModel.threats.value?.let {
                    val bundle = Bundle()
                    bundle.putParcelableArrayList("threats", it)

                    val transaction = supportFragmentManager.beginTransaction()
                    val fragment = ThreatFragment()
                    fragment.arguments = bundle
                    transaction.replace(R.id.threat_list_fragment_container, fragment)
                    transaction.commit()
                }
                true
            }
            R.id.threats_on_map -> {
                mapViewModel.updateThreatFeatures(mapView)
                mapViewModel.threatFeatures.value?.let { visualizeThreats(it) }
                true
            }
            R.id.threat_select_location -> {
                mapViewModel.selectLocationManual = true
                Toast.makeText(this, "Select Location", Toast.LENGTH_LONG).show()
                true
            }
            else -> false
        }
    }

    override fun onListFragmentInteraction(item: Threat?) {
        if (item != null) {

            val feature = item.feature

            val featureCollection = FeatureCollection.fromFeatures(
                arrayOf(feature)
            )

            val geoJsonSource = GeoJsonSource("threat-source", featureCollection)

            val loadedMapStyle = map.style

            if (loadedMapStyle != null && loadedMapStyle.isFullyLoaded) {
                loadedMapStyle.removeLayer("threat-source-layer")
                loadedMapStyle.removeSource("threat-source")

                // colorize the feature
                loadedMapStyle.addSource(geoJsonSource)
                val fillLayer = FillLayer("threat-source-layer", "threat-source")
                fillLayer.setProperties(
                    PropertyFactory.fillExtrusionColor(Color.RED),
                    PropertyFactory.fillColor(Color.RED)
                )
                loadedMapStyle.addLayer(fillLayer)

      /*          // focus camera to include both threat and current location
                val location = item.location.coordinates[0]
                val currentLocation = LatLng(this.lastUpdatedLocation!!.latitude, this.lastUpdatedLocation!!.longitude)
                val latLngBounds = LatLngBounds.Builder()
                    .include(LatLng(location.latitude, location.longitude))
                    .include(currentLocation)
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 30))*/

                // open card fragment and pass the threat as an argument
                val bundle = Bundle()
                bundle.putParcelable("threat", item)
                val dataCardFragmentInstance = DataCardFragment.newInstance()
                dataCardFragmentInstance.arguments = bundle
                if (supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()
            }
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.threat_list_fragment_container)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment).commit()
        }
    }

    private fun visualizeThreats(features: List<Feature>) {

        val loadedMapStyle = map.style

        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
            return
        }

        loadedMapStyle.removeLayer("threat-source-layer")
        loadedMapStyle.removeSource("threat-source")

        val selectedBuildingSource =
            loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.selectedBuildingSourceId)
        selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

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
