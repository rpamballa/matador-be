package com.matador.booking.api;

import com.matador.booking.BookingStatus;
import com.matador.pricing.LineItem;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BookingDtos {

    private BookingDtos() {}

    public record QuoteRequest(
        @NotNull UUID vehicleClassId,
        @NotNull Instant pickupAt,
        @NotNull Instant dropoffAt,
        @NotNull Double pickupLat,
        @NotNull Double pickupLng,
        @NotNull Double dropoffLat,
        @NotNull Double dropoffLng,
        String insuranceTier,
        String promoCode) {}

    public record QuoteResponse(
        UUID quoteId,
        boolean dropoffInZone,
        List<LineItem> lineItems,
        long subtotalCents,
        long discountCents,
        long taxCents,
        long totalCents,
        long depositCents,
        Instant expiresAt) {}

    public record CreateBookingRequest(
        @NotNull UUID quoteId,
        @NotNull Double pickupLat,
        @NotNull Double pickupLng,
        String pickupAddress,
        @NotNull Double dropoffLat,
        @NotNull Double dropoffLng,
        String dropoffAddress,
        UUID paymentMethodId) {}

    public record BookingResponse(
        UUID id,
        String bookingNumber,
        BookingStatus status,
        UUID vehicleClassId,
        UUID assignedVehicleId,
        Instant pickupAt,
        Instant dropoffAt,
        long quotedTotalCents,
        long depositAmountCents,
        boolean dropoffInZone,
        String depositHoldClientSecret) {}

    public record CancelRequest(String reason) {}

    public record AssignVehicleRequest(@NotNull UUID vehicleId) {}
}
