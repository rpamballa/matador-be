package com.matador.trip.api;

import com.matador.trip.TripService;
import com.matador.trip.TripStatus;
import com.matador.trip.api.TripDtos.TripResponse;
import com.matador.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

public final class TripControllers {

    private TripControllers() {}

    @RestController
    @Tag(name = "Customer-Trips")
    public static class CustomerTripController {

        private final TripService tripService;

        public CustomerTripController(TripService tripService) {
            this.tripService = tripService;
        }

        @GetMapping("/api/customer/me/trips/current")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Current trip", description = "The customer's active trip, if any.")
        public TripResponse current() {
            return tripService.currentForCustomer(CurrentUser.requireId());
        }

        @GetMapping("/api/customer/me/trips")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Trip history", description = "Paginated trip history.")
        public Page<TripResponse> history(@PageableDefault(size = 20) Pageable pageable) {
            return tripService.historyForCustomer(CurrentUser.requireId(), pageable);
        }

        @GetMapping("/api/customer/me/trips/{id}")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Trip detail", description = "A single trip owned by the customer.")
        public TripResponse get(@PathVariable UUID id) {
            return tripService.getForCustomer(CurrentUser.requireId(), id);
        }

        @PostMapping("/api/customer/me/trips/{id}/end")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "End trip", description = "End the active trip (awaiting dropoff inspection).")
        public TripResponse end(@PathVariable UUID id) {
            return tripService.endByCustomer(CurrentUser.requireId(), id);
        }
    }

    @RestController
    @Tag(name = "Admin-Trips")
    public static class AdminTripController {

        private final TripService tripService;

        public AdminTripController(TripService tripService) {
            this.tripService = tripService;
        }

        @GetMapping("/api/admin/trips")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "List trips", description = "Filterable trips list.")
        public Page<TripResponse> list(
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
            return tripService.search(status, customerId, pageable);
        }

        @GetMapping("/api/admin/trips/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "Trip detail", description = "A single trip.")
        public TripResponse get(@PathVariable UUID id) {
            return tripService.getById(id);
        }

        @PostMapping("/api/admin/trips/{id}/close")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
        @Operation(summary = "Close trip", description = "Close after inspection; computes final charges.")
        public TripResponse close(@PathVariable UUID id) {
            return tripService.close(id);
        }
    }
}
