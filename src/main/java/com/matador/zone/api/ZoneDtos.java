package com.matador.zone.api;

import com.matador.shared.geo.GeoJson.PointGeoJson;
import com.matador.shared.geo.GeoJson.PolygonGeoJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class ZoneDtos {

    private ZoneDtos() {}

    public record ZoneResponse(
        UUID id,
        String name,
        String slug,
        PolygonGeoJson boundary,
        PointGeoJson center,
        long outOfZoneDropoffFeeCents,
        boolean outOfZoneDropoffAllowed,
        boolean active) {}

    public record PublicZoneResponse(
        UUID id, String name, String slug, PolygonGeoJson boundary, PointGeoJson center) {}

    public record CreateZoneRequest(
        @NotBlank String name,
        @NotBlank String slug,
        @NotNull PolygonGeoJson boundary,
        @NotNull PointGeoJson center,
        long outOfZoneDropoffFeeCents,
        Boolean outOfZoneDropoffAllowed) {}

    public record UpdateZoneRequest(
        Long outOfZoneDropoffFeeCents, Boolean outOfZoneDropoffAllowed, Boolean active) {}

    public record ZoneContainsResponse(boolean inZone, UUID zoneId, String zoneName) {}
}
