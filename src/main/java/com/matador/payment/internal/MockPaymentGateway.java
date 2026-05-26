package com.matador.payment.internal;

import java.util.UUID;

/**
 * In-memory payment gateway for dev/test. Deposit holds resolve immediately to
 * {@code requires_capture} (as a saved-card off-session authorization would).
 */
class MockPaymentGateway implements PaymentGateway {

    @Override
    public String ensureCustomer(UUID customerId, String email) {
        return "cus_mock_" + customerId.toString().replace("-", "").substring(0, 16);
    }

    @Override
    public SetupIntent createSetupIntent(String stripeCustomerId) {
        String id = "seti_mock_" + rand();
        return new SetupIntent(id, id + "_secret_" + rand());
    }

    @Override
    public PaymentMethodDetails describePaymentMethod(String stripePaymentMethodId) {
        return new PaymentMethodDetails("CARD", "visa", "4242", 12, 2030);
    }

    @Override
    public IntentResult createDepositHold(
        String stripeCustomerId, String stripePaymentMethodId, long amountCents) {
        String id = "pi_mock_" + rand();
        return new IntentResult(id, "requires_capture", id + "_secret_" + rand());
    }

    @Override
    public IntentResult capture(String stripeIntentId, long amountCents) {
        return new IntentResult(stripeIntentId, "succeeded", null);
    }

    @Override
    public IntentResult cancel(String stripeIntentId) {
        return new IntentResult(stripeIntentId, "canceled", null);
    }

    @Override
    public IntentResult chargeOffSession(
        String stripeCustomerId, String stripePaymentMethodId, long amountCents, String description) {
        String id = "pi_mock_" + rand();
        return new IntentResult(id, "succeeded", null);
    }

    @Override
    public void refund(String stripeIntentId, long amountCents, String reason) {
        // no-op
    }

    private static String rand() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
