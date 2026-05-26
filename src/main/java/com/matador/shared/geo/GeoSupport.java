package com.matador.shared.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** Helpers for building WGS-84 (SRID 4326) geometries used with PostGIS geography columns. */
public final class GeoSupport {

    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory FACTORY =
        new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private GeoSupport() {}

    /** Build a point. Note PostGIS geography orders coordinates as (longitude, latitude). */
    public static Point point(double lng, double lat) {
        Point p = FACTORY.createPoint(new Coordinate(lng, lat));
        p.setSRID(SRID_WGS84);
        return p;
    }

    public static double lat(Point p) {
        return p.getY();
    }

    public static double lng(Point p) {
        return p.getX();
    }
}
