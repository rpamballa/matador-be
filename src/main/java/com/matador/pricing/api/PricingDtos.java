package com.matador.pricing.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class PricingDtos {

    private PricingDtos() {}

    public record CreateRateRequest(
        @NotNull UUID vehicleClassId,
        @Min(0) long dailyRateCents,
        @Min(0) long deliveryFeeCents,
        @Min(0) long outOfZoneDropoffFeeCents,
        @NotBlank String insuranceTier,
        @Min(0) long insuranceDailyCents,
        Instant effectiveFrom) {}

    public record RateResponse(UUID id, UUID vehicleClassId, long dailyRateCents) {}

    public record CreatePromoRequest(
        @NotBlank String code,
        @NotBlank String discountType,
        @Min(1) int discountValue,
        Integer maxUses,
        Instant startsAt,
        Instant expiresAt) {}

    public record PromoResponse(UUID id, String code, String discountType, int discountValue) {}
}
