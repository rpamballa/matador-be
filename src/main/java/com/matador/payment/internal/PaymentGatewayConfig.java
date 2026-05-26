package com.matador.payment.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PaymentGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayConfig.class);

    @Bean
    PaymentGateway paymentGateway(@Value("${matador.stripe.secret-key:}") String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("No Stripe secret key configured; using mock payment gateway.");
            return new MockPaymentGateway();
        }
        return new StripePaymentGateway(secretKey);
    }
}
