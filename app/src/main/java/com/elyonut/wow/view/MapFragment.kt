package com.elyonut.wow.view


import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import android.widget.PopupMenu
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.*
import com.elyonut.wow.model.Threat
import com.elyonut.wow.viewModel.SharedViewModel
import kotlinx.android.synthetic.main.fragment_map.view.*

private const val RECORD_REQUEST_CODE = 101

class MainMapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener,
    PopupMenu.OnMenuItemClickListener {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var mapViewModel: MapViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var threatStatusView: View
    private lateinit var threatStatusColorView: View
    private var listenerMap: OnMapFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(listenerMap as Context, Constants.MAPBOX_ACCESS_TOKEN)
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(MapViewModel::class.java)
        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        threatStatusView = view.findViewById(R.id.status)
        threatStatusColorView = view.findViewById(R.id.statusColor)
        mapView = view.findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        initArea()
        setObservers(view)
        initFocusOnMyLocationButton(view)
        initShowRadiusLayerButton(view)

        return view
    }

    private fun initArea() {
        if (sharedViewModel.areaOfInterest != null) {
            mapViewModel.areaOfInterest.value = sharedViewModel.areaOfInterest

            var polygonPoints = ArrayList<Point>()
            sharedViewModel.areaOfInterest!!.coordinates().forEach { it ->
                it.forEach {
                    polygonPoints.add(it)
                }
            }

            mapViewModel.lineLayerPointList = polygonPoints
        }
    }

    private fun setObservers(view: View) {
        mapViewModel.isAlertVisible.observe(this, Observer<Boolean> { showAlertDialog() })
        mapViewModel.noPermissionsToast.observe(this, Observer<Toast> { showToast() })
        mapViewModel.areaOfInterest.observe(this, Observer {
            sharedViewModel.areaOfInterest = it
        })
        mapViewModel.isPermissionRequestNeeded.observe(this, Observer<Boolean> {
            if (it != null && it) {
                requestPermissions1()
            }
        })
        mapViewModel.selectedBuildingId.observe(
            this,
            Observer<String> { showDescriptionFragment() }
        )
        mapViewModel.isLocationAdapterInitialized.observe(
            this,
            Observer<Boolean> { observeRiskStatus(it) }
        )

        mapViewModel.isInsideThreatArea.observe(this, Observer<Boolean> {
            sharedViewModel.alertsManager.sendNotification(
                getString(R.string.inside_threat_notification_title),
                getString(R.string.inside_threat_notification_content) + mapViewModel.threatID,
                R.drawable.ic_warning_black,
                Constants.INSIDE_THREAT_ALERT_ID
            )

        })

        sharedViewModel.selectedLayerId.observe(this, Observer<String> {
            it?.let { mapViewModel.layerSelected(it) }
        })

        sharedViewModel.selectedExperimentalOption.observe(
            this,
            Observer<Int> { applyExperimentalOption(it) }
        )
        sharedViewModel.selectedThreatItem.observe(
            this,
            Observer<Threat> { onListFragmentInteraction(it) }
        )
        sharedViewModel.shouldApplyFilter.observe(this,
            Observer<Boolean> { filter(it) }
        )
        sharedViewModel.shouldDefineArea.observe(this, Observer {
            if (it) {
                enableAreaSelection(view, it)
            }
        })
    }

    private fun observeRiskStatus(isLocationAdapterInitialized: Boolean) {
        if (isLocationAdapterInitialized)
            mapViewModel.riskStatus.observe(
                this,
                Observer<Pair<RiskStatus, String?>> { changeStatus(it.first) })
    }

    private fun filter(shouldApplyFilter: Boolean) {
        if (!shouldApplyFilter) {
            mapViewModel.removeFilter(map.style!!, sharedViewModel.chosenLayerId)
        } else {
            mapViewModel.applyFilter(
                map.style!!,
                sharedViewModel.chosenLayerId,
                sharedViewModel.chosenPropertyId,
                sharedViewModel.isStringType,
                sharedViewModel.numericType,
                sharedViewModel.chosenPropertyValue,
                sharedViewModel.specificValue,
                sharedViewModel.minValue,
                sharedViewModel.maxValue
            )
        }
    }

    private fun changeStatus(status: RiskStatus) {
        (threatStatusView as Button).text = status.text
        (threatStatusColorView as Button).backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled)
            ), intArrayOf(status.color)
        )
    }

    private fun showDescriptionFragment() {
        val dataCardFragmentInstance = DataCardFragment.newInstance()

        activity!!.supportFragmentManager.beginTransaction().replace(
            R.id.fragmentParent,
            dataCardFragmentInstance
        ).commit()
    }

    private fun requestPermissions1() {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            RECORD_REQUEST_CODE
        )
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(listenerMap as Context)
            .setTitle(getString(R.string.turn_on_location_title))
            .setMessage(getString(R.string.turn_on_location))
            .setPositiveButton(getString(R.string.yes_hebrew)) { _, _ ->
                val settingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(settingIntent)
            }.setNegativeButton(getString(R.string.no_thanks_hebrew)) { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mapViewModel.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun showToast() {
        mapViewModel.noPermissionsToast.value?.show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.addOnMapClickListener(this)
        mapViewModel.onMapReady(map)
    }

    private fun initFocusOnMyLocationButton(view: View) {
        val currentLocationButton: View = view.findViewById(R.id.currentLocation)
        currentLocationButton.setOnClickListener {
            mapViewModel.focusOnMyLocationClicked()
        }
    }

    private fun initShowRadiusLayerButton(view: View) {
        val radiusLayerButton: View = view.findViewById(R.id.radiusLayer)
        radiusLayerButton.setOnClickListener {
            mapViewModel.showRadiusLayerButtonClicked(Constants.threatRadiusLayerId)
        }
    }

    override fun onMapClick(latLng: LatLng): Boolean { // TODO UniqAi need to fix

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

        if (mapViewModel.isAreaSelectionMode) {
            mapViewModel.drawPolygonMode(latLng)
        } else {
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
                val features = map.queryRenderedFeatures(point, Constants.buildingsLayerId)

                if (features.size > 0) {
                    val selectedBuildingSource =
                        loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.selectedBuildingSourceId)
                    selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))

                    val threat = mapViewModel.buildingThreatToCurrentLocation(mapView, features[0])

                    val bundle = Bundle()
                    bundle.putParcelable("threat", threat)

                    // take to function!
                    val dataCardFragmentInstance = DataCardFragment.newInstance()
                    dataCardFragmentInstance.arguments = bundle
                    activity!!.supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()
                    activity!!.supportFragmentManager.fragments
                }
            }
        }

        return true
    }

    private fun applyExperimentalOption(id: Int) {
        when (id) {
            R.id.threat_list_menu_item -> {

                mapViewModel.updateThreats(mapView)

                mapViewModel.threats.value?.let {
                    val bundle = Bundle()
                    bundle.putParcelableArrayList("threats", it)

                    val transaction = activity!!.supportFragmentManager.beginTransaction()
                    val fragment = ThreatFragment()
                    fragment.arguments = bundle
                    transaction.replace(R.id.threat_list_fragment_container, fragment)
                    transaction.commit()
                }
            }
            R.id.threats_on_map -> {
                mapViewModel.updateThreatFeatures(mapView)
                mapViewModel.threatFeatures.value?.let { visualizeThreats(it) }
            }
            R.id.threat_select_location -> {
                mapViewModel.selectLocationManual = true
                Toast.makeText(listenerMap as Context, "Select Location", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableAreaSelection(view: View, shouldEnable: Boolean) {
        val mainMapLayoutView = view.mainMapLayout
        val currentLocationButton = view.currentLocation
        val radiusLayerButton = view.radiusLayer

        if (shouldEnable) {
            layoutInflater.inflate(R.layout.area_selection, mainMapLayoutView)
            val areaModeView = view.findViewById<View>(R.id.area_mode)
            initUndoButton(areaModeView)
            initCancelAreaButton(areaModeView)
            initApplyAreaButton(areaModeView)
            mapViewModel.removeAreaFromMap()
        } else {
            mainMapLayoutView.removeView(view.findViewById(R.id.area_mode))
            sharedViewModel.shouldDefineArea.value = false
        }

        radiusLayerButton.isEnabled = !shouldEnable
        currentLocationButton.isEnabled = !shouldEnable
        mapViewModel.isAreaSelectionMode = shouldEnable
    }

    private fun initUndoButton(view: View) {
        view.findViewById<View>(R.id.undo).setOnClickListener {
            mapViewModel.undo()
        }
    }

    private fun initApplyAreaButton(view: View) { // MVVM ? applyClicked function?
        view.findViewById<View>(R.id.apply_area).setOnClickListener {
            mapViewModel.saveAreaOfInterest()
            enableAreaSelection(view.parent as View, false)
        }
    }

    private fun initCancelAreaButton(view: View) {
        view.findViewById<View>(R.id.cancel_area).setOnClickListener {
            mapViewModel.cancelAreaSelection()
            enableAreaSelection(view.parent as View, false)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.threat_list_menu_item -> {
                mapViewModel.updateThreats(mapView)

                mapViewModel.threats.value?.let {
                    val bundle = Bundle()
                    bundle.putParcelableArrayList("threats", it)

                    val transaction = activity!!.supportFragmentManager.beginTransaction()
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
                Toast.makeText(listenerMap as Context, "Select Location", Toast.LENGTH_LONG).show()
                true
            }
            else -> false
        }
    }

    private fun onListFragmentInteraction(item: Threat?) {
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
                if (activity!!.supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
                    activity!!.supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()
            }
        }

        val fragment =
            activity!!.supportFragmentManager.findFragmentById(R.id.threat_list_fragment_container)
        if (fragment != null) {
            activity!!.supportFragmentManager.beginTransaction()
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMapFragmentInteractionListener) {
            listenerMap = context
        } else {
            throw RuntimeException("$context must implement OnMapFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listenerMap = null
    }

    interface OnMapFragmentInteractionListener {
        fun onMapFragmentInteraction()
    }

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
        if (mapViewModel.riskStatus.hasObservers()) {
            mapViewModel.riskStatus.removeObservers(this)
        }
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
}
