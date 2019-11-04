package com.elyonut.wow.analysis

import com.elyonut.wow.App
import com.elyonut.wow.Constants
import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.PolygonModel
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.ceil

class TopographyService {

    private var mapboxMap: MapboxMap
    private val LOS_HEIGHT_METERS = 1.5
    private val vectorIndex: VectorEnvelopeIndex = VectorEnvelopeIndex()

    constructor(mapboxMap: MapboxMap){
        this.mapboxMap = mapboxMap

        val stream: InputStream = App.resourses.assets.open("tlv-buildings.json")
//        val stream: InputStream = App.resourses.assets.open("tlv-buildings_1building.json")
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        val jsonObj = String(buffer, charset("UTF-8"))

        this.vectorIndex.loadBuildingFromGeoJson(jsonObj)
    }

    fun isThreat(currentLocation: Coordinate, feature: Feature): Boolean {
        val threatCoordinates = getGeometryCoordinates(feature.geometry()!!)
        val threatType = feature.getProperty("type")?.asString
        if(threatType != null && threatType.contains("mikush")){
            return isMikushInRange(currentLocation, threatCoordinates)
        }

        val threatRangeMeters = KnowledgeBase.getRangeMeters(threatType)



        var inRange = false
        for (coord in threatCoordinates) {
            val distance = TurfMeasurement.distance(
                Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
                getPoint(coord),
                TurfConstants.UNIT_METERS
            )

            if(distance<= threatRangeMeters) {
                inRange = true
                break
            }
        }

        if(inRange){
            val threatHeight = feature.getNumberProperty("height").toDouble()
            return isLOS(currentLocation, threatCoordinates, threatHeight)
        }


        return false
    }

    fun isLOS(currentLocation: Coordinate, threatCoordinates: List<Coordinate>, threatHeight: Double): Boolean {

        var locationCoordinates = listOf(currentLocation)

        val buildingAtLocation = getBuildingAtLocation(LatLng(currentLocation.latitude, currentLocation.longitude))
        if (buildingAtLocation != null) {
            locationCoordinates = getCoordinatesForAnalysis(buildingAtLocation.geometry()!!)
            val locationHeight = buildingAtLocation.getNumberProperty("height").toDouble()
            locationCoordinates.forEach { bc -> bc.heightMeters = locationHeight}
        }

        val buildingCoordinates = explodeCornerCoordinates(threatCoordinates)
        buildingCoordinates.forEach { bc -> bc.heightMeters = threatHeight}

        return isLOS(locationCoordinates, buildingCoordinates)

    }

    fun isThreat(currentLocation: Coordinate, featureModel: FeatureModel): Boolean {

        val threatType = featureModel.properties?.get("type")?.asString
        if(threatType != null && threatType.contains("mikush")){
            val coordinates = getCoordinates(featureModel.geometry)
            return isMikushInRange(currentLocation, coordinates)
        }

        val threatRangeMeters = KnowledgeBase.getRangeMeters(threatType)

        val threatCoordinates = getCoordinates(featureModel.geometry)

        var inRange = false
        for (coord in threatCoordinates) {
            val distance = TurfMeasurement.distance(
                Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
                getPoint(coord),
                TurfConstants.UNIT_METERS
            )

            if(distance<= threatRangeMeters) {
                inRange = true
                break
            }
        }

        if(inRange) {
            val threatHeight = featureModel.properties?.get("height")!!.asDouble
            return isLOSLocalIndex(currentLocation, threatCoordinates, threatHeight)
        }

        return false
    }

    private fun isMikushInRange(currentLocation: Coordinate, coordinates: List<Coordinate>): Boolean {

        for (coord in coordinates) {
            val distance = TurfMeasurement.distance(
                Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
                getPoint(coord),
                TurfConstants.UNIT_METERS
            )

            if(distance<= KnowledgeBase.MIKUSH_RANGE_METERS)
                return true
        }

        return false
    }

    private fun isLOSLocalIndex(currentLocation: Coordinate, threatCoordinates: List<Coordinate>, threatHeight: Double): Boolean {

        var locationCoordinates = listOf(currentLocation)


        val buildingAtLocation = vectorIndex.getVectorQuad(currentLocation.longitude, currentLocation.latitude)
        if (buildingAtLocation != null) {
            locationCoordinates = getCoordinatesForAnalysis(buildingAtLocation.polygon)
            val locationHeight = buildingAtLocation.getProperties()["height"]!!.toDouble()
            locationCoordinates.forEach { bc -> bc.heightMeters = locationHeight }
        }

        val buildingCoordinates = explodeCornerCoordinates(threatCoordinates)
        buildingCoordinates.forEach { bc -> bc.heightMeters = threatHeight }

        return isLOS(locationCoordinates, buildingCoordinates)

    }

    private fun isLOS(coords1: List<Coordinate>, coords2: List<Coordinate>): Boolean {
        for(c1 in coords1){
            val coordinatesDistances = ArrayList<CoordinateDistance>()
            for(c2 in coords2){
                coordinatesDistances.add(CoordinateDistance(c1, c2))
            }
            coordinatesDistances.sort()

            for(cd in coordinatesDistances){
                if (isLOS(cd.c1, cd.c2)) {
                    return true
                }
            }
        }
        return false
    }

    private fun isLOS(from: Coordinate, to: Coordinate): Boolean {

        val min: Coordinate
        val max: Coordinate
        from.heightMeters = getHeight(from)
        // if located inside building, than reduce the height
        if (from.heightMeters > 0 && from.heightMeters != LOS_HEIGHT_METERS) {
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

        val routePoints = calcRoutePointsLinear(from, to, false,  1.0)


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

    private fun calcRoutePointsLinear(fromPoint: Coordinate, toPoint: Coordinate, skipFirst: Boolean, distanceOfPoints: Double): ArrayList<Coordinate> {
        val aCoordiates = ArrayList<Coordinate>()

        // The distance between the latitudes
        val latDis = toPoint.latitude - fromPoint.latitude
        // The distance between the longitudes
        val longDis = toPoint.longitude - fromPoint.longitude
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
            if(!skipFirst || i > 0) {
                val aPoint = Coordinate(currentLat, currentLong)
                aCoordiates.add(aPoint)
            }
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

            val height = this.vectorIndex.getHeight(Point.fromLngLat(c1.longitude, c1.latitude))
            return height
        }
        return c1.heightMeters;
    }

    private fun getHeightMapBox(c1: Coordinate): Double {
        if (c1.heightMeters == -10000.0) {
            val buildingAtLocation = getBuildingAtLocation(LatLng(c1.latitude, c1.longitude))
            return buildingAtLocation?.getNumberProperty("height")?.toDouble() ?: LOS_HEIGHT_METERS
        }
        return c1.heightMeters;
    }

    private fun getBuildingAtLocation(
        location: LatLng
    ): Feature? {

        val point = mapboxMap.projection.toScreenLocation(location)
        val features = mapboxMap.queryRenderedFeatures(point, Constants.BUILDINGS_LAYER_ID) //????

        if (features.isNullOrEmpty())
            return null

        val sortedByName = features.sortedBy { myObject -> myObject.getNumberProperty("height").toDouble() }
        return sortedByName.last()
    }

    private fun distanceMeters(c1: Coordinate, c2: Coordinate): Double {
        return TurfMeasurement.distance(getPoint(c1), getPoint(c2), TurfConstants.UNIT_METERS)
    }

    private fun getCoordinatesForAnalysis(featureGeometry: Geometry): List<Coordinate> {
        val corners = getGeometryCoordinates(featureGeometry)
        return explodeCornerCoordinates(corners)
    }


    private fun explodeCornerCoordinates(originalCoordinates: List<Coordinate>): List<Coordinate> {
        val coordinates: ArrayList<Coordinate> = ArrayList()
        for ((index, value) in originalCoordinates.withIndex()) {
            coordinates.add(value)
            if(index < originalCoordinates.size -1){
                val explodedCoordinates = calcRoutePointsLinear(value, originalCoordinates[index + 1], true, 4.0)
                coordinates.addAll(explodedCoordinates)
            }
        }

        return coordinates
    }

    private fun getCoordinates(polygonModel: PolygonModel): List<Coordinate> {
        val coordinates = polygonModel.coordinates[0].map{
                coords -> Coordinate(coords[1], coords[0])
        }
        return coordinates
    }



    fun getGeometryCoordinates(featureGeometry: Geometry): List<Coordinate> {
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
//            "MultiLineString" -> {
//                geometry = featureGeometry as MultiLineString
//                TODO("not implemented")
//            }
//            "MultiPoint" -> {
//                geometry = featureGeometry as MultiPoint
//                TODO("not implemented")
//            }
            else -> {
//                geometry = featureGeometry as CoordinateContainer<*>
            }
        }


        return coordinates;
    }

    private fun getPoint(c: Coordinate): Point {
        return Point.fromLngLat(c.longitude, c.latitude)

    }
}