package com.matador.payment.events;

import java.util.UUID;

/** Domain events published by the payment module; consumed by the ledger module. */
public final class PaymentEvents {

    private PaymentEvents() {}

    public record PaymentHeld(UUID intentId, UUID customerId, UUID bookingId, long amountCents) {}

    public record PaymentCaptured(
        UUID intentId, UUID customerId, UUID bookingId, UUID tripId, String purpose, long amountCents) {}

    public record PaymentReleased(UUID intentId, UUID customerId, UUID bookingId, long amountCents) {}

    public record PaymentFailed(UUID intentId, UUID customerId, String reason) {}

    public record PaymentRefunded(UUID intentId, UUID customerId, long amountCents, String reason) {}

    public record PaymentDisputeCreated(UUID intentId, UUID customerId, long amountCents) {}
}
