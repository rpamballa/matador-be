package com.matador.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matador.identity.IdentityService;
import com.matador.identity.VerificationSessionStatus;
import com.matador.identity.api.IdentityDtos.MockVerificationEvent;
import com.matador.shared.error.ValidationException;
import com.matador.shared.webhook.WebhookEventStore;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.identity.VerificationSession;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Stripe Identity webhook events. When a webhook secret is configured the Stripe
 * signature is verified; otherwise (dev/test) a simplified JSON payload is accepted so the
 * verification flow can be exercised end-to-end without Stripe. Processing is idempotent.
 */
@RestController
@Tag(name = "Webhooks")
public class StripeIdentityWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeIdentityWebhookController.class);
    private static final String PROVIDER = "stripe-identity";

    private final IdentityService identityService;
    private final WebhookEventStore eventStore;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public StripeIdentityWebhookController(
        IdentityService identityService,
        WebhookEventStore eventStore,
        ObjectMapper objectMapper,
        @Value("${matador.stripe.identity-webhook-secret:}") String webhookSecret) {
        this.identityService = identityService;
        this.eventStore = eventStore;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/api/webhooks/stripe-identity")
    @Operation(summary = "Stripe Identity webhook", description = "Handle identity verification events.")
    public ResponseEntity<Void> handle(
        @RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            handleSignedStripeEvent(payload, signature);
        } else {
            handleMockEvent(payload);
        }
        return ResponseEntity.ok().build();
    }

    private void handleSignedStripeEvent(String payload, String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ValidationException("INVALID_SIGNATURE", "Invalid webhook signature.");
        }
        if (!eventStore.markProcessed(PROVIDER, event.getId(), event.getType(), payload)) {
            return; // already handled
        }
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(object instanceof VerificationSession session)) {
            log.warn("Ignoring Stripe identity event {} without verification session", event.getType());
            return;
        }
        switch (event.getType()) {
            // License-detail extraction from the verification report is a follow-up; the
            // customer is marked verified on success here.
            case "identity.verification_session.verified" ->
                identityService.recordVerified(session.getId(), null, null, null, payload);
            case "identity.verification_session.requires_input" ->
                identityService.recordRequiresInput(session.getId(), payload);
            case "identity.verification_session.canceled" ->
                identityService.recordCanceled(session.getId(), payload);
            default -> log.debug("Unhandled Stripe identity event type {}", event.getType());
        }
    }

    private void handleMockEvent(String payload) {
        MockVerificationEvent event;
        try {
            event = objectMapper.readValue(payload, MockVerificationEvent.class);
        } catch (Exception e) {
            throw new ValidationException("MALFORMED_EVENT", "Could not parse verification event.");
        }
        String eventId = event.providerSessionId() + ":" + event.status();
        if (!eventStore.markProcessed(PROVIDER, eventId, event.status().name(), payload)) {
            return;
        }
        VerificationSessionStatus status = event.status();
        switch (status) {
            case VERIFIED ->
                identityService.recordVerified(
                    event.providerSessionId(),
                    event.licenseNumber(),
                    event.licenseState(),
                    event.licenseExpiresOn(),
                    payload);
            case REQUIRES_INPUT -> identityService.recordRequiresInput(event.providerSessionId(), payload);
            case CANCELED -> identityService.recordCanceled(event.providerSessionId(), payload);
            default ->
                throw new ValidationException(
                    "UNSUPPORTED_STATUS", "Unsupported verification status: " + status);
        }
    }
}
