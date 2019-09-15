package com.elyonut.wow.analysis

import com.elyonut.wow.App
import com.elyonut.wow.Constants
import com.elyonut.wow.R
import com.elyonut.wow.model.Coordinate
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.math.abs
import kotlin.math.ceil

class TopographyService {

    private var mapboxMap: MapboxMap
    private val LOS_HEIGHT_METERS = 1.5

    constructor(mapboxMap: MapboxMap){
        this.mapboxMap = mapboxMap
    }

    fun isLOS(currentLocation: LatLng, building: Feature): Boolean {

        var locationCoordinates = listOf(Coordinate(currentLocation.latitude, currentLocation.longitude))

        val buildingAtLocation = getBuildingAtLocation(currentLocation)
        if (buildingAtLocation != null) {
            locationCoordinates = getCoordinates(buildingAtLocation.geometry()!!)
            val locationHeight = buildingAtLocation.getNumberProperty("height").toDouble()
            locationCoordinates.forEach() { bc -> bc.heightMeters = locationHeight}
        }

        val buildingCoordinates = getCoordinates(building.geometry()!!)

        val buildingHeight = building.getNumberProperty("height").toDouble()
        buildingCoordinates.forEach() { bc -> bc.heightMeters = buildingHeight}

        return isLOS(locationCoordinates, buildingCoordinates)

    }

    fun isLOS(coords1: List<Coordinate>, coords2: List<Coordinate>): Boolean {
        coords1.forEach() { c1 ->
            coords2.forEach() { c2 ->
                if (isLOS(c1, c2)) {
                    return true
                }
            }
        }
        return false
    }

    fun isLOS(from: Coordinate, to: Coordinate): Boolean {

        var min: Coordinate
        var max: Coordinate
        from.heightMeters = getHeight(from)
        // if located inside building, than reduce the height
        if (from.heightMeters != LOS_HEIGHT_METERS) {
            from.heightMeters -= 0.5
        }
        to.heightMeters = getHeight(to)
        if (from.heightMeters > to.heightMeters) {
            max = from
            min = to
        } else {
            max = to
            min = from
        }

        val routePoints = calcRoutePointsLinear(from, to)


        val currDistance = distanceMeters(from, to)
        val currTan = abs(max.heightMeters - min.heightMeters) / currDistance
        for (i in 0 until routePoints.size) {
            val candidate = routePoints[i]
            val canDistance = distanceMeters(min, candidate)
            candidate.heightMeters = getHeight(candidate)
            val maxHeight = canDistance * currTan
            val canHeight = candidate.heightMeters - min.heightMeters
            //   console.log(canHeight + "," + maxHeight);
            if (canHeight > maxHeight) {
                return false
            }
        }

        return true
    }

    private fun calcRoutePointsLinear(fromPoint: Coordinate, toPoint: Coordinate): ArrayList<Coordinate> {

        val aCoordiates = ArrayList<Coordinate>()

        // The distance between the latitudes
        val latDis = toPoint.latitude - fromPoint.latitude
        // The distance between the longitudes
        val longDis = toPoint.longitude - fromPoint.longitude
        val distanceOfPoints: Double = 1.0
        val samples = ceil(
            TurfMeasurement.distance(
                getPoint(fromPoint),
                getPoint(toPoint),
                TurfConstants.UNIT_METERS
            ) / distanceOfPoints
        ).toInt()
        // get the minimum between the samples and Max samples
        //samples = Math.min(samples,256);
        val deltaLat = latDis / samples
        val deltaLong = longDis / samples
        // start from the first point
        var currentLat = fromPoint.latitude
        var currentLong = fromPoint.longitude
        var i = 0
        while (i < samples) {
            val aPoint = Coordinate(currentLat, currentLong)
            aCoordiates.add(aPoint)
            currentLat += deltaLat
            currentLong += deltaLong
            i++
        }
        // add the last point
        aCoordiates.add(toPoint)

        return aCoordiates
    }

    private fun getHeight(c1: Coordinate): Double {
        if (c1.heightMeters == -10000.0) {
            val buildingAtLocation = getBuildingAtLocation(LatLng(c1.latitude, c1.longitude))
            return if (buildingAtLocation != null) {
                buildingAtLocation.getNumberProperty("height").toDouble()
            } else {
                LOS_HEIGHT_METERS
            }
        }
        return c1.heightMeters;
    }

    private fun getBuildingAtLocation(
        location: LatLng
    ): Feature? {

        val point = mapboxMap.projection.toScreenLocation(location)
        val features = mapboxMap.queryRenderedFeatures(point, Constants.buildingsLayerId)

        if (features.isNullOrEmpty())
            return null

        var sortedByName = features.sortedBy { myObject -> myObject.getNumberProperty("height").toDouble() }
        return sortedByName.last()
    }

    private fun distanceMeters(c1: Coordinate, c2: Coordinate): Double {
        return TurfMeasurement.distance(getPoint(c1), getPoint(c2), TurfConstants.UNIT_METERS)
    }

    fun getCoordinates(featureGeometry: Geometry): List<Coordinate> {
        val geometry: Geometry
        var coordinates: List<Coordinate> = ArrayList()
        when (featureGeometry.type()) {
            "Polygon" -> {
                geometry = featureGeometry as Polygon
                coordinates = geometry.coordinates()[0].map { point ->
                    Coordinate(
                        point.latitude(),
                        point.longitude()
                    )
                } as MutableList<Coordinate>
            }
            "Point" -> {
                geometry = featureGeometry as Point
                coordinates = ArrayList()
                coordinates.add(Coordinate(geometry.coordinates()[0], geometry.coordinates()[1]))
            }
            "MultiPolygon" -> {
                geometry = featureGeometry as MultiPolygon
                // TODO: returning only single geometry
                coordinates = geometry.coordinates()[0][0].map { point ->
                    Coordinate(
                        point.latitude(),
                        point.longitude()
                    )
                } as MutableList<Coordinate>
            }
            "LineString" -> {
                geometry = featureGeometry as LineString
                coordinates = geometry.coordinates().map { point ->
                    Coordinate(
                        point.latitude(),
                        point.longitude()
                    )
                } as MutableList<Coordinate>
            }
            "MultiLineString" -> {
                geometry = featureGeometry as MultiLineString
                TODO("not implemented")
            }
            "MultiPoint" -> {
                geometry = featureGeometry as MultiPoint
                TODO("not implemented")
            }
            else -> {
                geometry = featureGeometry as CoordinateContainer<*>
            }
        }


        return coordinates;
    }

    fun getPoint(c: Coordinate): Point {
        return Point.fromLngLat(c.longitude, c.latitude)

    }
}