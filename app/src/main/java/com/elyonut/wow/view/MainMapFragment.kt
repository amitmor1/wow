package com.elyonut.wow.view


import android.content.Context
import android.content.Intent
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
//import com.elyonut.wow.Constants
//import com.elyonut.wow.R
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import android.widget.PopupMenu
//import com.elyonut.wow.analysis.ThreatAnalyzer
//import com.elyonut.wow.analysis.TopographyService
//import com.elyonut.wow.model.Threat
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

private const val RECORD_REQUEST_CODE = 101

class MainMapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener, PopupMenu.OnMenuItemClickListener,
    ThreatFragment.OnListFragmentInteractionListener {
    private lateinit var mapView: MapView

    private lateinit var map: MapboxMap
    private lateinit var mapViewModel: MapViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var threatStatus: View
    private var listener: OnFragmentInteractionListener? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(listener as Context, Constants.MAPBOX_ACCESS_TOKEN)
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(MapViewModel::class.java)
        sharedViewModel =
            activity?.run { ViewModelProviders.of(this)[SharedViewModel::class.java] }!!

        initObservers()
        threatStatus = view.findViewById(R.id.status)
        mapView = view.findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initFocusOnMyLocationButton(view)
        initShowRadiusLayerButton(view)
        initMenuButton(view)
        return view
    }

    private fun initObservers() {
        mapViewModel.selectedBuildingId.observe(this, Observer<String> { showDescriptionFragment() })
        mapViewModel.isPermissionRequestNeeded.observe(this, Observer<Boolean> {
            if (it != null && it) {
                requestPermissions1()
            }
        })
        mapViewModel.isAlertVisible.observe(this, Observer<Boolean> { showAlertDialog() })
        mapViewModel.noPermissionsToast.observe(this, Observer<Toast> { showToast() })
        mapViewModel.threatStatus.observe(this, Observer<String> { changeStatus(it) })

        sharedViewModel.selectedLayerId.observe(this, Observer<String> {
            sharedViewModel.selectedLayerId.value?.let { mapViewModel.layerSelected(it) }
        })
    }

    private fun changeStatus(status: String?) {
        (threatStatus as Button).text = status
    }

    private fun showDescriptionFragment() {
        val dataCardFragmentInstance = DataCardFragment.newInstance()

        if (activity!!.supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
            activity!!.supportFragmentManager.beginTransaction().add(
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
        AlertDialog.Builder(listener as Context).setTitle(getString(R.string.turn_on_location_title))
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
        mapboxMap.addOnMapClickListener(this)

        mapViewModel.onMapReady(mapboxMap)
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

                // take to function!
                val dataCardFragmentInstance = DataCardFragment.newInstance()
                dataCardFragmentInstance.arguments = bundle
                if (activity!!.supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
                    activity!!.supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()

            }
        }

        return true
    }

    private fun initMenuButton(view: View) {
        val menuButton: View = view.findViewById(R.id.menu_button)
        menuButton.setOnClickListener {
            PopupMenu(listener as Context, view).apply {
                setOnMenuItemClickListener(this@MainMapFragment)
                inflate(R.menu.menu)
                show()
            }
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
                Toast.makeText(listener as Context, "Select Location", Toast.LENGTH_LONG).show()
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
                if (activity!!.supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
                    activity!!.supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()
            }
        }

        val fragment = activity!!.supportFragmentManager.findFragmentById(R.id.threat_list_fragment_container)
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

    fun onButtonPressed() {
        listener?.onMainFragmentInteraction()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onMainFragmentInteraction()
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
