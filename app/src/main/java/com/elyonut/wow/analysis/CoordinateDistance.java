package com.elyonut.wow.analysis;

import com.elyonut.wow.model.Coordinate;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

public class CoordinateDistance implements Comparable<CoordinateDistance> {
    private Coordinate c1;
    private Coordinate c2;
    private Double distanceMeters;

    public CoordinateDistance(Coordinate c1, Coordinate c2) {
        this.c1 = c1;
        this.c2 = c2;
        distanceMeters = TurfMeasurement.distance(Point.fromLngLat(c1.getLongitude(), c1.getLatitude()),
                                                    Point.fromLngLat(c2.getLongitude(), c2.getLatitude())
                                                    , TurfConstants.UNIT_METERS);
    }

    public Coordinate getC1() {
        return c1;
    }

    public Coordinate getC2() {
        return c2;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    @Override
    public int compareTo(CoordinateDistance coordinateDistance) {
        return this.distanceMeters.compareTo(coordinateDistance.distanceMeters);
    }
}
