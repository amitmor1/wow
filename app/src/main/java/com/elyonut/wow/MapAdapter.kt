package com.elyonut.wow

import android.graphics.Color
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.GeometryModel
import com.elyonut.wow.model.WowLatLng
import com.google.gson.JsonObject
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeta
import com.mapbox.turf.TurfTransformation
import java.util.*
import kotlin.collections.ArrayList


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

    fun transfromLatLngToWowLatLng(latLng: LatLng): WowLatLng {
        return WowLatLng(latLng.latitude, latLng.longitude)
    }

    fun transfromLatLngToMapboxLatLng(latLng: WowLatLng): LatLng {
        return LatLng(latLng.latitude, latLng.longitude)
    }

    fun transfromFeatureModelToMapboxFeature(featureModel: FeatureModel): Feature {
        return Feature.fromGeometry(
            transformGeometryToMapboxGeometry(featureModel.geometry),
            featureModel.properties,
            featureModel.id
        )
    }

    fun transfromMapboxFeatureToFeatureModel(feature: Feature): FeatureModel {
        return FeatureModel(
            feature.id(),
            feature.properties(),
            transformMapboxGeometryToGeometryModel(feature.geometry() as Polygon),
            feature.type()
        )
    }

    private fun transformGeometryToMapboxGeometry(geometryModel: GeometryModel): Geometry {
        val points = ArrayList<Point>()
        geometryModel.coordinates.forEach { it -> it.forEach { points.add(Point.fromLngLat(it[0], it[1])) } }
        val pointsList = arrayListOf(points.toList()).toList()
        return Polygon.fromLngLats(pointsList)
    }

    private fun transformMapboxGeometryToGeometryModel(polygon: Polygon): GeometryModel {
        val points = arrayListOf<Double>()

        polygon.coordinates().forEach { it ->
            it.forEach {
                points.add(it.latitude())
                points.add(it.longitude())
            }
        }
        return GeometryModel(listOf(listOf(points)), polygon.type())
    }

    override fun createThreatRadiusSource(): ArrayList<FeatureModel> {
        val circleLayerFeatureList = ArrayList<FeatureModel>()
        val allFeatures = (tempDB.getFeatures())
        allFeatures.forEach {
            val polygonArea = createPolygonArea(it)
            val properties = createThreatProperties(it)
            if (polygonArea != null) {
                val feature = transfromMapboxFeatureToFeatureModel(
                    Feature.fromGeometry(
                        Polygon.fromOuterInner(
                            LineString.fromLngLats(TurfMeta.coordAll(polygonArea, false))
                        ), properties
                    )
                )
                it.geometry = feature.geometry
                circleLayerFeatureList.add(it)
            }
        }

        return circleLayerFeatureList
    }

    private fun createPolygonArea(feature: FeatureModel): Polygon? {
        var polygon: Polygon? = null
        val currentLatitude = feature.properties?.get("latitude")
        val currentLongitude = feature.properties?.get("longitude")

        if ((currentLatitude != null) && (currentLongitude != null)) {
            val featureRiskRadius = feature.properties?.get("radius").let { t -> t?.asDouble }
            val currPoint = Point.fromLngLat(currentLongitude.asDouble, currentLatitude.asDouble)
            polygon = TurfTransformation.circle(currPoint, featureRiskRadius!!, circleSteps, circleUnit)
        }
        return polygon
    }

    private fun createThreatProperties(feature: FeatureModel): JsonObject {
        val featureThreatLevel = feature.properties?.get("risk")
        val properties = JsonObject()
        if (featureThreatLevel != null) {
            properties.add(Constants.threatProperty, featureThreatLevel)
        }
        return properties
    }
}