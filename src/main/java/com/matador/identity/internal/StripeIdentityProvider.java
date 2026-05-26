package com.matador.identity.internal;

import com.matador.shared.error.ExternalServiceException;
import com.stripe.exception.StripeException;
import com.stripe.model.identity.VerificationSession;
import com.stripe.net.RequestOptions;
import com.stripe.param.identity.VerificationSessionCreateParams;
import java.util.UUID;

/** Stripe Identity-backed provider. Active when {@code matador.stripe.secret-key} is set. */
class StripeIdentityProvider implements IdentityProvider {

    private final RequestOptions requestOptions;

    StripeIdentityProvider(String secretKey) {
        this.requestOptions = RequestOptions.builder().setApiKey(secretKey).build();
    }

    @Override
    public String name() {
        return "STRIPE_IDENTITY";
    }

    @Override
    public CreatedSession createSession(UUID customerId) {
        try {
            VerificationSessionCreateParams params =
                VerificationSessionCreateParams.builder()
                    .setType(VerificationSessionCreateParams.Type.DOCUMENT)
                    .putMetadata("customer_id", customerId.toString())
                    .build();
            VerificationSession session = VerificationSession.create(params, requestOptions);
            return new CreatedSession(session.getId(), session.getClientSecret());
        } catch (StripeException e) {
            throw new ExternalServiceException("Failed to create Stripe Identity session", e);
        }
    }
}
