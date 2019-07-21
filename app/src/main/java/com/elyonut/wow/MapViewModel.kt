package com.elyonut.wow

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.res.Resources
import android.support.v4.app.FragmentManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class MapViewModel(var application: Application) : ViewModel(), OnMapReadyCallback {
    private lateinit var map: MapboxMap

    private var mainActivity: IActivity = MainActivity()

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap

        mapboxMap.setStyle(application.getString(R.string.style_url)) { style ->

        }
    }


    fun onMapClick(mapboxMap: MapboxMap, latLng: LatLng, fragmentManager: FragmentManager): Boolean {
        val loadedMapStyle = mapboxMap.style

        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
            return false
        }

        val point = mapboxMap.projection.toScreenLocation(latLng)
        val features = mapboxMap.queryRenderedFeatures(point, application.getString(R.string.buildings_layer))

        if (features.size > 0) {
            val selectedBuildingSource =
                loadedMapStyle.getSourceAs<GeoJsonSource>(application.getString(R.string.selectedBuildingSourceId))
            selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))

            val dataCardFragmentInstance = DataCardFragment.newInstance()
            if (fragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
                fragmentManager.beginTransaction().add(R.id.fragmentParent, dataCardFragmentInstance).commit()

        }

        return true
    }

}

