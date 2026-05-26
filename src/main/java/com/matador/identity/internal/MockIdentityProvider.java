package com.matador.identity.internal;

import java.util.UUID;

/** Local/dev provider used when no Stripe secret key is configured. */
class MockIdentityProvider implements IdentityProvider {

    @Override
    public String name() {
        return "STRIPE_IDENTITY"; // persisted provider label stays stable across envs
    }

    @Override
    public CreatedSession createSession(UUID customerId) {
        String sessionId = "vs_mock_" + UUID.randomUUID().toString().replace("-", "");
        String clientSecret = sessionId + "_secret_" + UUID.randomUUID().toString().replace("-", "");
        return new CreatedSession(sessionId, clientSecret);
    }
}
