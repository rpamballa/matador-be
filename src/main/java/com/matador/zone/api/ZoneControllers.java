package com.matador.zone.api;

import com.matador.zone.ZoneInfo;
import com.matador.zone.ZoneService;
import com.matador.zone.api.ZoneDtos.CreateZoneRequest;
import com.matador.zone.api.ZoneDtos.PublicZoneResponse;
import com.matador.zone.api.ZoneDtos.UpdateZoneRequest;
import com.matador.zone.api.ZoneDtos.ZoneContainsResponse;
import com.matador.zone.api.ZoneDtos.ZoneResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin, customer, and public zone endpoints. */
public final class ZoneControllers {

    private ZoneControllers() {}

    @RestController
    @Tag(name = "Admin-Settings")
    public static class AdminZoneController {

        private final ZoneService zoneService;

        public AdminZoneController(ZoneService zoneService) {
            this.zoneService = zoneService;
        }

        @GetMapping("/api/admin/zones")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "List zones", description = "All zones with GeoJSON boundaries.")
        public List<ZoneResponse> list() {
            return zoneService.listAll();
        }

        @PostMapping("/api/admin/zones")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Create zone", description = "Create a zone from a GeoJSON polygon.")
        public ZoneResponse create(@Valid @RequestBody CreateZoneRequest request) {
            return zoneService.create(request);
        }

        @PatchMapping("/api/admin/zones/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Update zone", description = "Update fees, dropoff policy, or active flag.")
        public ZoneResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateZoneRequest request) {
            return zoneService.update(id, request);
        }
    }

    @RestController
    @Tag(name = "Customer-Vehicles")
    public static class PublicZoneController {

        private final ZoneService zoneService;

        public PublicZoneController(ZoneService zoneService) {
            this.zoneService = zoneService;
        }

        @GetMapping("/api/customer/zones")
        @Operation(summary = "Public zones", description = "Active zones for map display.")
        public List<PublicZoneResponse> publicZones() {
            return zoneService.listPublic();
        }

        @GetMapping("/api/zones/contains")
        @Operation(summary = "Zone containment", description = "Which zone (if any) contains a point.")
        public ZoneContainsResponse contains(
            @RequestParam double lat, @RequestParam double lng) {
            return zoneService
                .findContaining(lat, lng)
                .map(z -> new ZoneContainsResponse(true, z.id(), z.name()))
                .orElseGet(() -> new ZoneContainsResponse(false, null, null));
        }
    }
}
