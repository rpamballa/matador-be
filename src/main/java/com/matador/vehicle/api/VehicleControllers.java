package com.matador.vehicle.api;

import com.matador.vehicle.VehicleService;
import com.matador.vehicle.VehicleStatus;
import com.matador.vehicle.api.VehicleDtos.AvailableClassResponse;
import com.matador.vehicle.api.VehicleDtos.CommandResponse;
import com.matador.vehicle.api.VehicleDtos.CreateVehicleClassRequest;
import com.matador.vehicle.api.VehicleDtos.CreateVehicleRequest;
import com.matador.vehicle.api.VehicleDtos.TransitionStatusRequest;
import com.matador.vehicle.api.VehicleDtos.UpdateVehicleClassRequest;
import com.matador.vehicle.api.VehicleDtos.UpdateVehicleRequest;
import com.matador.vehicle.api.VehicleDtos.VehicleClassResponse;
import com.matador.vehicle.api.VehicleDtos.VehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

public final class VehicleControllers {

    private VehicleControllers() {}

    @RestController
    @Tag(name = "Admin-Vehicles")
    public static class AdminVehicleController {

        private final VehicleService vehicleService;

        public AdminVehicleController(VehicleService vehicleService) {
            this.vehicleService = vehicleService;
        }

        @GetMapping("/api/admin/vehicles")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "List vehicles", description = "Paginated, filterable fleet list.")
        public Page<VehicleResponse> list(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID zoneId,
            @PageableDefault(size = 20) Pageable pageable) {
            return vehicleService.search(status, classId, zoneId, pageable);
        }

        @PostMapping("/api/admin/vehicles")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Create vehicle", description = "Add a vehicle to the fleet.")
        public VehicleResponse create(@Valid @RequestBody CreateVehicleRequest request) {
            return vehicleService.create(request);
        }

        @GetMapping("/api/admin/vehicles/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "Vehicle detail", description = "Full vehicle detail.")
        public VehicleResponse get(@PathVariable UUID id) {
            return vehicleService.get(id);
        }

        @PatchMapping("/api/admin/vehicles/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Update vehicle", description = "Update mutable vehicle fields.")
        public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVehicleRequest request) {
            return vehicleService.update(id, request);
        }

        @PostMapping("/api/admin/vehicles/{id}/status")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
        @Operation(summary = "Transition status", description = "Validated vehicle status transition.")
        public VehicleResponse transition(
            @PathVariable UUID id, @Valid @RequestBody TransitionStatusRequest request) {
            return vehicleService.transitionStatus(id, request.status());
        }

        @PostMapping("/api/admin/vehicles/{id}/lock")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
        @Operation(summary = "Lock vehicle", description = "Send a lock command via telematics.")
        public CommandResponse lock(@PathVariable UUID id) {
            return vehicleService.lock(id);
        }

        @PostMapping("/api/admin/vehicles/{id}/unlock")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
        @Operation(summary = "Unlock vehicle", description = "Send an unlock command via telematics.")
        public CommandResponse unlock(@PathVariable UUID id) {
            return vehicleService.unlock(id);
        }
    }

    @RestController
    @Tag(name = "Admin-Vehicles")
    public static class AdminVehicleClassController {

        private final VehicleService vehicleService;

        public AdminVehicleClassController(VehicleService vehicleService) {
            this.vehicleService = vehicleService;
        }

        @GetMapping("/api/admin/vehicle-classes")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "List vehicle classes", description = "All vehicle classes.")
        public List<VehicleClassResponse> list() {
            return vehicleService.listClasses();
        }

        @PostMapping("/api/admin/vehicle-classes")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Create vehicle class", description = "Create a vehicle class.")
        public VehicleClassResponse create(@Valid @RequestBody CreateVehicleClassRequest request) {
            return vehicleService.createClass(request);
        }

        @PatchMapping("/api/admin/vehicle-classes/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Update vehicle class", description = "Update a vehicle class.")
        public VehicleClassResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateVehicleClassRequest request) {
            return vehicleService.updateClass(id, request);
        }
    }

    @RestController
    @Tag(name = "Customer-Vehicles")
    public static class CustomerVehicleController {

        private final VehicleService vehicleService;

        public CustomerVehicleController(VehicleService vehicleService) {
            this.vehicleService = vehicleService;
        }

        @GetMapping("/api/customer/vehicle-classes")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Class catalog", description = "Active vehicle classes for customers.")
        public List<VehicleClassResponse> classes() {
            return vehicleService.listActiveClasses();
        }

        @GetMapping("/api/customer/vehicles/available")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(
            summary = "Available classes",
            description = "Bookable classes with counts and starting price for a window/location.")
        public List<AvailableClassResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant pickupAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dropoffAt,
            @RequestParam double pickupLat,
            @RequestParam double pickupLng) {
            return vehicleService.availableClasses();
        }
    }
}
