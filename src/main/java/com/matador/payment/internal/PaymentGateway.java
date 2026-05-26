package com.matador.payment.internal;

import java.util.UUID;

/** Abstraction over the payment processor (Stripe). */
public interface PaymentGateway {

    String ensureCustomer(UUID customerId, String email);

    SetupIntent createSetupIntent(String stripeCustomerId);

    PaymentMethodDetails describePaymentMethod(String stripePaymentMethodId);

    /** Authorize (manual capture) a deposit hold off-session against a saved method. */
    IntentResult createDepositHold(String stripeCustomerId, String stripePaymentMethodId, long amountCents);

    IntentResult capture(String stripeIntentId, long amountCents);

    IntentResult cancel(String stripeIntentId);

    IntentResult chargeOffSession(
        String stripeCustomerId, String stripePaymentMethodId, long amountCents, String description);

    void refund(String stripeIntentId, long amountCents, String reason);

    record SetupIntent(String id, String clientSecret) {}

    record PaymentMethodDetails(String type, String brand, String last4, Integer expMonth, Integer expYear) {}

    record IntentResult(String stripeIntentId, String status, String clientSecret) {}
}
