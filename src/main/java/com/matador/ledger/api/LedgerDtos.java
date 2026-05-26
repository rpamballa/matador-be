package com.matador.ledger.api;

import com.matador.ledger.LedgerEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class LedgerDtos {

    private LedgerDtos() {}

    public record LedgerEntryView(
        UUID id,
        Instant occurredAt,
        LedgerEntryType entryType,
        long amountCents,
        String currency,
        String description) {}

    public record AdjustmentRequest(
        @NotNull UUID customerId,
        UUID bookingId,
        UUID tripId,
        long amountCents,
        @NotBlank String reason) {}
}
