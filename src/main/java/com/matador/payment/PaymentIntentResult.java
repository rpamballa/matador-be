package com.matador.payment;

import java.util.UUID;

/** Result of a payment operation returned to callers (e.g. booking). */
public record PaymentIntentResult(
    UUID intentId, String stripeIntentId, String status, String clientSecret, long amountCents) {}
