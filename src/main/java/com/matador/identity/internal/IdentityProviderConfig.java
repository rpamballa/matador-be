package com.matador.identity.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentityProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(IdentityProviderConfig.class);

    @Bean
    IdentityProvider identityProvider(@Value("${matador.stripe.secret-key:}") String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("No Stripe secret key configured; using mock identity provider.");
            return new MockIdentityProvider();
        }
        return new StripeIdentityProvider(secretKey);
    }
}
