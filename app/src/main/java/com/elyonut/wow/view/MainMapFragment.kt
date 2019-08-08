package com.elyonut.wow.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
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
import com.elyonut.wow.Constants
import com.elyonut.wow.R
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback

private const val RECORD_REQUEST_CODE = 101

class MainMapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener {
    private lateinit var map: MapboxMap
    private lateinit var mapViewModel: MapViewModel
    private lateinit var threatStatus: View
    private lateinit var mapView: MapView

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

        initObservers()
        threatStatus = view.findViewById(R.id.status)
        mapView = view.findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initFocusOnMyLocationButton(view)
        initShowRadiusLayerButton(view)
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

        return mapViewModel.onMapClick(map, latLng)
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
