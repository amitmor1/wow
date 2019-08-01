package com.elyonut.wow.transformer

import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.GeometryModel
import com.elyonut.wow.model.LatLngModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.geometry.LatLng

class MapboxTransformer {
    companion object {
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
            val points = ArrayList<List<List<Double>>>()

            polygon.coordinates().forEach { it ->
                val coordinatesList = ArrayList<List<Double>>()
                it.forEach {
                    coordinatesList.add(it.coordinates())
                }

                points.add(coordinatesList)
            }

            return GeometryModel(points, polygon.type())
        }
    }
}