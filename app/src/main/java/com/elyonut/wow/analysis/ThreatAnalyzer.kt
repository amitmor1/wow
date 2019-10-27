package com.elyonut.wow.analysis

import android.graphics.RectF
import com.elyonut.wow.App
import com.elyonut.wow.Constants
import com.elyonut.wow.model.*
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.turf.TurfMeasurement
import java.io.InputStream


class ThreatAnalyzer(private var mapView: MapView, private var mapboxMap: MapboxMap) {
    private var topographyService: TopographyService = TopographyService(mapboxMap)

    fun getThreats(currentLocation: LatLng): ArrayList<Threat> {

        val features = getFeaturesFromMapbox(mapView, mapboxMap)
        val filterWithLOS = filterWithLOS(features, currentLocation)

        val res = filterWithLOS.mapIndexed { index, it ->
            featureToThreat(it, currentLocation, true, index)
        }.sortedBy { it.distanceMeters }

        return ArrayList(res)
    }

    fun getThreatFeatures(currentLocation: LatLng): List<Feature> {
        val features = getFeaturesFromMapbox(mapView, mapboxMap)
        return filterWithLOS(features, currentLocation)
    }



    private fun filterWithLOS(
        buildingFeatureCollection: List<Feature>,
        currentLocation: LatLng
    ): List<Feature> {
        return buildingFeatureCollection.filter { topographyService.isLOS(currentLocation, it) }
    }

    public fun featureToThreat(feature: Feature, currentLocation: LatLng, isLos: Boolean, index: Int = 1): Threat {
        val threat = Threat()
        // threat.radius = feature.getNumberProperty("radius").toDouble()
        val height = feature.getNumberProperty("height").toDouble()
        threat.level = getThreatLevel(height)

        val geometryCoordinates = topographyService.getCoordinates(feature.geometry()!!)
        val featureLatitude = geometryCoordinates[0].latitude
        val featureLongitude = geometryCoordinates[0].longitude
        val featureLocation = LatLng(featureLatitude, featureLongitude)

        threat.location = GeoLocation(LocationType.Polygon, geometryCoordinates as ArrayList<Coordinate>)
        threat.distanceMeters = currentLocation.distanceTo(featureLocation)
        threat.azimuth = bearingToAzimuth(TurfMeasurement.bearing(
            Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
            Point.fromLngLat(featureLongitude, featureLatitude)))
        threat.creator = "ישראל ישראלי"
        threat.description = "תיאור"
        threat.name = "איום " + index.toString() // feature.id()
        threat.feature = feature
        threat.isLos = isLos

        return threat
    }

    private fun bearingToAzimuth(bearing: Double): Double {
        var angle = bearing % 360;
        if (angle < 0) { angle += 360; }
        return angle
    }

    private fun getThreatLevel(height: Double): ThreatLevel = when {
        height < 3 -> ThreatLevel.None
        height < 10 -> ThreatLevel.Low
        height < 100 -> ThreatLevel.Medium
        else -> ThreatLevel.High
    }

    private fun getFeaturesFromMapbox(
        mapView: MapView,
        mapboxMap: MapboxMap
    ): List<Feature> {
        val rectF = RectF(
            mapView.left.toFloat(),
            mapView.top.toFloat(),
            mapView.right.toFloat(),
            mapView.bottom.toFloat()
        )

        val features = mapboxMap.queryRenderedFeatures(rectF, Constants.BUILDINGS_LAYER_ID) //???
        //val uniqueFeatures = features.distinctBy { it.id() }
        return features

    }

    private fun getFeatures(): FeatureCollection {
        val stream: InputStream = App.resourses.assets.open("features.geojson")
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        val jsonObj = String(buffer, charset("UTF-8"))
        return FeatureCollection.fromJson(jsonObj)
    }
}