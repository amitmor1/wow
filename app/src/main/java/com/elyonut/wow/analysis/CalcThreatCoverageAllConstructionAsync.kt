package com.elyonut.wow.analysis

import android.os.AsyncTask
import android.widget.ProgressBar
import com.elyonut.wow.Constants
import com.elyonut.wow.ILogger
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class CalcThreatCoverageAllConstructionAsync(
    private val mapViewModel: MapViewModel,
    private val progressBar: ProgressBar

    ) : AsyncTask<ThreatCoverageData, Int, Unit>() {

    private val logger: ILogger = TimberLogAdapter()

    override fun doInBackground(vararg coverageData: ThreatCoverageData?) {
        val input = coverageData[0]
        if (input != null) {
            logger.info("calculating coverage for all enemies!")

            val allFeatures = mapViewModel.layerManager.getLayer(Constants.THREAT_LAYER_ID)


            mapViewModel.threatAnalyzer.calculateCoverageAlpha(allFeatures!!, input.rangeMeters, input.pointResolutionMeters, input.heightMeters)
        }
    }

    override fun onPostExecute(result: Unit?) {
        progressBar.visibility = android.view.View.GONE
        logger.info("coverage calculated!")
    }

}
