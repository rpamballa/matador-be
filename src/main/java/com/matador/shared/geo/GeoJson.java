package com.matador.shared.geo;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Minimal GeoJSON conversion for the geometry types used in the API: Point and Polygon.
 * Coordinates are GeoJSON order {@code [longitude, latitude]}.
 */
public final class GeoJson {

    private static final GeometryFactory FACTORY =
        new GeometryFactory(new PrecisionModel(), GeoSupport.SRID_WGS84);

    private GeoJson() {}

    /** GeoJSON Polygon: {@code {"type":"Polygon","coordinates":[[[lng,lat],...]]}}. */
    public record PolygonGeoJson(String type, List<List<List<Double>>> coordinates) {
        public PolygonGeoJson {
            if (type == null) {
                type = "Polygon";
            }
        }
    }

    /** GeoJSON Point: {@code {"type":"Point","coordinates":[lng,lat]}}. */
    public record PointGeoJson(String type, List<Double> coordinates) {
        public PointGeoJson {
            if (type == null) {
                type = "Point";
            }
        }
    }

    public static PointGeoJson toGeoJson(Point point) {
        return new PointGeoJson("Point", List.of(point.getX(), point.getY()));
    }

    public static PolygonGeoJson toGeoJson(Polygon polygon) {
        List<List<List<Double>>> rings = new ArrayList<>();
        List<List<Double>> exterior = new ArrayList<>();
        for (Coordinate c : polygon.getExteriorRing().getCoordinates()) {
            exterior.add(List.of(c.x, c.y));
        }
        rings.add(exterior);
        return new PolygonGeoJson("Polygon", rings);
    }

    public static Point toPoint(PointGeoJson geoJson) {
        List<Double> c = geoJson.coordinates();
        return GeoSupport.point(c.get(0), c.get(1));
    }

    public static Polygon toPolygon(PolygonGeoJson geoJson) {
        List<List<Double>> exterior = geoJson.coordinates().get(0);
        Coordinate[] coords = new Coordinate[exterior.size()];
        for (int i = 0; i < exterior.size(); i++) {
            List<Double> p = exterior.get(i);
            coords[i] = new Coordinate(p.get(0), p.get(1));
        }
        LinearRing ring = FACTORY.createLinearRing(coords);
        Polygon polygon = FACTORY.createPolygon(ring);
        polygon.setSRID(GeoSupport.SRID_WGS84);
        return polygon;
    }
}
