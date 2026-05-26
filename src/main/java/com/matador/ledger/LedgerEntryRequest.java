package com.matador.ledger;

import java.time.Instant;
import java.util.UUID;

/** Request to record a ledger entry. */
public record LedgerEntryRequest(
    Instant occurredAt,
    UUID customerId,
    UUID bookingId,
    UUID tripId,
    UUID incidentId,
    LedgerEntryType entryType,
    long amountCents,
    String description,
    UUID paymentIntentId,
    String metadata) {}
