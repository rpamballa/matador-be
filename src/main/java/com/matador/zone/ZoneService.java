package com.matador.zone;

import com.matador.shared.error.ConflictException;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.geo.GeoJson;
import com.matador.shared.id.IdGenerator;
import com.matador.zone.api.ZoneDtos.CreateZoneRequest;
import com.matador.zone.api.ZoneDtos.PublicZoneResponse;
import com.matador.zone.api.ZoneDtos.UpdateZoneRequest;
import com.matador.zone.api.ZoneDtos.ZoneResponse;
import com.matador.zone.internal.ZoneRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the zone module. */
@Service
public class ZoneService {

    private final ZoneRepository zones;
    private final IdGenerator idGenerator;

    public ZoneService(ZoneRepository zones, IdGenerator idGenerator) {
        this.zones = zones;
        this.idGenerator = idGenerator;
    }

    // ---- Cross-module query API ----

    @Transactional(readOnly = true)
    public Optional<ZoneInfo> findContaining(double lat, double lng) {
        return zones.findContaining(lat, lng).map(this::toInfo);
    }

    @Transactional(readOnly = true)
    public boolean isInZone(UUID zoneId, double lat, double lng) {
        return Boolean.TRUE.equals(zones.contains(zoneId, lat, lng));
    }

    @Transactional(readOnly = true)
    public ZoneInfo requireZone(UUID zoneId) {
        return zones
            .findById(zoneId)
            .map(this::toInfo)
            .orElseThrow(() -> ResourceNotFoundException.of("Zone", zoneId));
    }

    // ---- Admin API ----

    @Transactional
    public ZoneResponse create(CreateZoneRequest request) {
        if (zones.findBySlug(request.slug()).isPresent()) {
            throw new ConflictException("ZONE_SLUG_TAKEN", "Zone slug already exists.");
        }
        Zone zone =
            new Zone(
                idGenerator.newId(),
                request.name(),
                request.slug(),
                GeoJson.toPolygon(request.boundary()),
                GeoJson.toPoint(request.center()),
                request.outOfZoneDropoffFeeCents(),
                request.outOfZoneDropoffAllowed() == null || request.outOfZoneDropoffAllowed());
        return toResponse(zones.save(zone));
    }

    @Transactional
    public ZoneResponse update(UUID id, UpdateZoneRequest request) {
        Zone zone = zones.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Zone", id));
        zone.update(request.outOfZoneDropoffFeeCents(), request.outOfZoneDropoffAllowed(), request.active());
        return toResponse(zone);
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> listAll() {
        return zones.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicZoneResponse> listPublic() {
        return zones.findByActiveTrue().stream().map(this::toPublic).toList();
    }

    private ZoneInfo toInfo(Zone z) {
        return new ZoneInfo(
            z.getId(), z.getName(), z.getSlug(), z.getOutOfZoneDropoffFeeCents(), z.isOutOfZoneDropoffAllowed());
    }

    private ZoneResponse toResponse(Zone z) {
        return new ZoneResponse(
            z.getId(),
            z.getName(),
            z.getSlug(),
            GeoJson.toGeoJson(z.getBoundary()),
            GeoJson.toGeoJson(z.getCenter()),
            z.getOutOfZoneDropoffFeeCents(),
            z.isOutOfZoneDropoffAllowed(),
            z.isActive());
    }

    private PublicZoneResponse toPublic(Zone z) {
        return new PublicZoneResponse(
            z.getId(),
            z.getName(),
            z.getSlug(),
            GeoJson.toGeoJson(z.getBoundary()),
            GeoJson.toGeoJson(z.getCenter()));
    }
}
