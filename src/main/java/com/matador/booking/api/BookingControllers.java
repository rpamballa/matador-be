package com.matador.booking.api;

import com.matador.booking.BookingService;
import com.matador.booking.BookingStatus;
import com.matador.booking.api.BookingDtos.AssignVehicleRequest;
import com.matador.booking.api.BookingDtos.BookingResponse;
import com.matador.booking.api.BookingDtos.CancelRequest;
import com.matador.booking.api.BookingDtos.CreateBookingRequest;
import com.matador.booking.api.BookingDtos.QuoteRequest;
import com.matador.booking.api.BookingDtos.QuoteResponse;
import com.matador.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

public final class BookingControllers {

    private BookingControllers() {}

    @RestController
    @Tag(name = "Customer-Bookings")
    public static class CustomerBookingController {

        private final BookingService bookingService;

        public CustomerBookingController(BookingService bookingService) {
            this.bookingService = bookingService;
        }

        @PostMapping("/api/customer/bookings/quote")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Price quote", description = "Get a quote without creating a booking.")
        public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
            return bookingService.quote(CurrentUser.requireId(), request);
        }

        @PostMapping("/api/customer/bookings")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Create booking", description = "Create a booking from a quote; places a deposit hold.")
        public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
            return bookingService.create(CurrentUser.requireId(), request);
        }

        @GetMapping("/api/customer/bookings")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "List bookings", description = "The customer's bookings.")
        public Page<BookingResponse> list(@PageableDefault(size = 20) Pageable pageable) {
            return bookingService.listForCustomer(CurrentUser.requireId(), pageable);
        }

        @GetMapping("/api/customer/bookings/{id}")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Booking detail", description = "A single booking owned by the customer.")
        public BookingResponse get(@PathVariable UUID id) {
            return bookingService.getForCustomer(CurrentUser.requireId(), id);
        }

        @PostMapping("/api/customer/bookings/{id}/cancel")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Cancel booking", description = "Cancel per the cancellation policy.")
        public BookingResponse cancel(@PathVariable UUID id, @RequestBody(required = false) CancelRequest request) {
            String reason = request == null ? null : request.reason();
            return bookingService.cancelByCustomer(CurrentUser.requireId(), id, reason);
        }
    }

    @RestController
    @Tag(name = "Admin-Bookings")
    public static class AdminBookingController {

        private final BookingService bookingService;

        public AdminBookingController(BookingService bookingService) {
            this.bookingService = bookingService;
        }

        @GetMapping("/api/admin/bookings")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "List bookings", description = "Filterable bookings list.")
        public Page<BookingResponse> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID vehicleId,
            @PageableDefault(size = 20) Pageable pageable) {
            return bookingService.search(status, customerId, vehicleId, pageable);
        }

        @GetMapping("/api/admin/bookings/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "Booking detail", description = "A single booking.")
        public BookingResponse get(@PathVariable UUID id) {
            return bookingService.getById(id);
        }

        @PostMapping("/api/admin/bookings/{id}/assign-vehicle")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
        @Operation(summary = "Assign vehicle", description = "Assign a specific vehicle to a booking.")
        public BookingResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignVehicleRequest request) {
            return bookingService.assignVehicle(id, request);
        }

        @PostMapping("/api/admin/bookings/{id}/cancel")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
        @Operation(summary = "Cancel booking", description = "Admin-initiated cancellation.")
        public BookingResponse cancel(@PathVariable UUID id, @RequestBody(required = false) CancelRequest request) {
            return bookingService.cancelByAdmin(id, request == null ? null : request.reason());
        }

        @PostMapping("/api/admin/bookings/{id}/activate")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
        @Operation(summary = "Activate booking", description = "Activate a confirmed booking; starts a trip.")
        public BookingResponse activate(@PathVariable UUID id) {
            return bookingService.activate(id);
        }
    }
}
