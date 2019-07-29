package com.elyonut.wow

import android.graphics.Color
import com.google.gson.JsonObject
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeta
import com.mapbox.turf.TurfTransformation
import java.util.*


private const val circleUnit = TurfConstants.UNIT_KILOMETERS
private const val circleSteps = 180
class MapAdapter(var tempDB: TempDB) : IMap {

    override fun addLayer(layerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeLayer(layerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun colorFilter(layerId: String, colorsList: Dictionary<Int, Color>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initOfflineMap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun transformLatLngToWowLatLng(latLng: LatLng): WowLatLng {
        return WowLatLng(latLng.latitude, latLng.longitude)
    }

    fun transformLatLngToMapboxLatLng(latLng: WowLatLng): LatLng {
        return LatLng(latLng.latitude, latLng.longitude)
    }

    fun createThreatRadiusSource(): FeatureCollection {
        val circleLayerFeatureList = ArrayList<Feature>()
        val allFeatures = FeatureCollection.fromJson(tempDB.getFeatures())
        allFeatures.features()?.forEach { it ->
                var polygonArea = createPolygonArea(it)
                val properties = createThreatProperties(it)
                if (polygonArea != null) {
                    circleLayerFeatureList.add(
                        Feature.fromGeometry(
                            Polygon.fromOuterInner(
                                LineString.fromLngLats(TurfMeta.coordAll(polygonArea, false))), properties))
                    }
            }

        return (FeatureCollection.fromFeatures(circleLayerFeatureList))
    }

    private fun createPolygonArea(feature: Feature): Polygon? {
        var polygon: Polygon? = null
        val currentLatitude = feature.properties()?.get("latitude")
        val currentLongitude = feature.properties()?.get("longitude")

        if ((currentLatitude != null) && (currentLongitude != null)) {
            val featureRiskRadius = feature.properties()?.get("radius").let { t -> t?.asDouble }
            val currPoint = Point.fromLngLat(currentLongitude.asDouble, currentLatitude.asDouble)
            polygon =  TurfTransformation.circle(currPoint, featureRiskRadius!!, circleSteps, circleUnit)
        }
        return polygon
    }

    private fun createThreatProperties(feature: Feature): JsonObject {
        val featureThreatLevel = feature.properties()?.get("risk")
        val properties = JsonObject()
        if (featureThreatLevel != null) {
            properties.add(Constants.threatProperty, featureThreatLevel)
        }
        return properties
    }
}