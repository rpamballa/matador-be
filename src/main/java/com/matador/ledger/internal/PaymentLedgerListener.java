package com.matador.ledger.internal;

import com.matador.ledger.LedgerEntryRequest;
import com.matador.ledger.LedgerEntryType;
import com.matador.ledger.LedgerService;
import com.matador.payment.PaymentService;
import com.matador.payment.events.PaymentEvents.PaymentCaptured;
import com.matador.payment.events.PaymentEvents.PaymentDisputeCreated;
import com.matador.payment.events.PaymentEvents.PaymentHeld;
import com.matador.payment.events.PaymentEvents.PaymentRefunded;
import com.matador.payment.events.PaymentEvents.PaymentReleased;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Writes ledger entries in response to payment events. */
@Component
class PaymentLedgerListener {

    private final LedgerService ledger;

    PaymentLedgerListener(LedgerService ledger) {
        this.ledger = ledger;
    }

    @ApplicationModuleListener
    void on(PaymentHeld event) {
        ledger.record(
            new LedgerEntryRequest(
                null, event.customerId(), event.bookingId(), null, null,
                LedgerEntryType.DEPOSIT_HELD, 0, "Security deposit hold placed", event.intentId(), null));
    }

    @ApplicationModuleListener
    void on(PaymentCaptured event) {
        LedgerEntryType type =
            PaymentService.PURPOSE_DEPOSIT_HOLD.equals(event.purpose())
                ? LedgerEntryType.DEPOSIT_CAPTURED
                : PaymentService.PURPOSE_INCIDENT_CHARGE.equals(event.purpose())
                    ? LedgerEntryType.INCIDENT_CHARGED
                    : LedgerEntryType.RENTAL_CHARGED;
        ledger.record(
            new LedgerEntryRequest(
                null, event.customerId(), event.bookingId(), event.tripId(), null,
                type, event.amountCents(), describe(type), event.intentId(), null));
    }

    @ApplicationModuleListener
    void on(PaymentReleased event) {
        ledger.record(
            new LedgerEntryRequest(
                null, event.customerId(), event.bookingId(), null, null,
                LedgerEntryType.DEPOSIT_RELEASED, 0, "Security deposit released", event.intentId(), null));
    }

    @ApplicationModuleListener
    void on(PaymentRefunded event) {
        ledger.record(
            new LedgerEntryRequest(
                null, event.customerId(), null, null, null,
                LedgerEntryType.REFUND_ISSUED, -Math.abs(event.amountCents()),
                "Refund issued: " + event.reason(), event.intentId(), null));
    }

    @ApplicationModuleListener
    void on(PaymentDisputeCreated event) {
        ledger.record(
            new LedgerEntryRequest(
                null, event.customerId(), null, null, null,
                LedgerEntryType.DISPUTE_CHARGEBACK, event.amountCents(),
                "Dispute chargeback", event.intentId(), null));
    }

    private String describe(LedgerEntryType type) {
        return switch (type) {
            case DEPOSIT_CAPTURED -> "Security deposit captured";
            case INCIDENT_CHARGED -> "Incident charge";
            case RENTAL_CHARGED -> "Rental charge";
            default -> type.name();
        };
    }
}
