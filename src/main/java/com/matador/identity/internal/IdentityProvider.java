package com.matador.identity.internal;

import java.util.UUID;

/** Abstraction over the third-party identity verification provider (Stripe Identity). */
public interface IdentityProvider {

    String name();

    CreatedSession createSession(UUID customerId);

    record CreatedSession(String providerSessionId, String clientSecret) {}
}
