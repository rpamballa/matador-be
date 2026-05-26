package com.matador.payment.internal;

import com.matador.shared.error.ExternalServiceException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import java.util.UUID;

/** Stripe-backed payment gateway. Active when {@code matador.stripe.secret-key} is set. */
class StripePaymentGateway implements PaymentGateway {

    private static final String USD = "usd";

    private final RequestOptions options;

    StripePaymentGateway(String secretKey) {
        this.options = RequestOptions.builder().setApiKey(secretKey).build();
    }

    @Override
    public String ensureCustomer(UUID customerId, String email) {
        try {
            CustomerCreateParams params =
                CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("customer_id", customerId.toString())
                    .build();
            return Customer.create(params, options).getId();
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe customer creation failed", e);
        }
    }

    @Override
    public PaymentGateway.SetupIntent createSetupIntent(String stripeCustomerId) {
        try {
            SetupIntentCreateParams params =
                SetupIntentCreateParams.builder().setCustomer(stripeCustomerId).build();
            com.stripe.model.SetupIntent intent = com.stripe.model.SetupIntent.create(params, options);
            return new PaymentGateway.SetupIntent(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe setup intent failed", e);
        }
    }

    @Override
    public PaymentMethodDetails describePaymentMethod(String stripePaymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(stripePaymentMethodId, options);
            if (pm.getCard() != null) {
                return new PaymentMethodDetails(
                    "CARD",
                    pm.getCard().getBrand(),
                    pm.getCard().getLast4(),
                    pm.getCard().getExpMonth() == null ? null : pm.getCard().getExpMonth().intValue(),
                    pm.getCard().getExpYear() == null ? null : pm.getCard().getExpYear().intValue());
            }
            return new PaymentMethodDetails("CARD", null, null, null, null);
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe payment method lookup failed", e);
        }
    }

    @Override
    public IntentResult createDepositHold(
        String stripeCustomerId, String stripePaymentMethodId, long amountCents) {
        try {
            PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(USD)
                    .setCustomer(stripeCustomerId)
                    .setPaymentMethod(stripePaymentMethodId)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setConfirm(true)
                    .setOffSession(true)
                    .build();
            PaymentIntent intent = PaymentIntent.create(params, options);
            return new IntentResult(intent.getId(), intent.getStatus(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe deposit hold failed", e);
        }
    }

    @Override
    public IntentResult capture(String stripeIntentId, long amountCents) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(stripeIntentId, options);
            PaymentIntentCaptureParams params =
                PaymentIntentCaptureParams.builder().setAmountToCapture(amountCents).build();
            PaymentIntent captured = intent.capture(params, options);
            return new IntentResult(captured.getId(), captured.getStatus(), null);
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe capture failed", e);
        }
    }

    @Override
    public IntentResult cancel(String stripeIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(stripeIntentId, options);
            PaymentIntent canceled = intent.cancel((com.stripe.param.PaymentIntentCancelParams) null, options);
            return new IntentResult(canceled.getId(), canceled.getStatus(), null);
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe cancel failed", e);
        }
    }

    @Override
    public IntentResult chargeOffSession(
        String stripeCustomerId, String stripePaymentMethodId, long amountCents, String description) {
        try {
            PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(USD)
                    .setCustomer(stripeCustomerId)
                    .setPaymentMethod(stripePaymentMethodId)
                    .setConfirm(true)
                    .setOffSession(true)
                    .setDescription(description)
                    .build();
            PaymentIntent intent = PaymentIntent.create(params, options);
            return new IntentResult(intent.getId(), intent.getStatus(), null);
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe off-session charge failed", e);
        }
    }

    @Override
    public void refund(String stripeIntentId, long amountCents, String reason) {
        try {
            RefundCreateParams params =
                RefundCreateParams.builder()
                    .setPaymentIntent(stripeIntentId)
                    .setAmount(amountCents)
                    .build();
            Refund.create(params, options);
        } catch (StripeException e) {
            throw new ExternalServiceException("Stripe refund failed", e);
        }
    }
}
