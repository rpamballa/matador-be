package com.matador.payment.api;

import com.matador.payment.internal.StripeWebhookHandler;
import com.matador.shared.error.ValidationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stripe payments webhook. Verifies the signature when a webhook secret is configured.
 * In dev/test (no secret), the mock gateway emits events directly so this endpoint is a no-op.
 */
@RestController
@Tag(name = "Webhooks")
public class StripeWebhookController {

    private final StripeWebhookHandler handler;
    private final String webhookSecret;

    public StripeWebhookController(
        StripeWebhookHandler handler,
        @Value("${matador.stripe.webhook-secret:}") String webhookSecret) {
        this.handler = handler;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/api/webhooks/stripe")
    @Operation(summary = "Stripe webhook", description = "Handle Stripe payment events.")
    public ResponseEntity<Void> handle(
        @RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return ResponseEntity.ok().build();
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ValidationException("INVALID_SIGNATURE", "Invalid webhook signature.");
        }
        handler.handle(event, payload);
        return ResponseEntity.ok().build();
    }
}
