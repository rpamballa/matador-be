package com.matador.pricing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "matador.pricing")
public record PricingProperties(
    int quoteTtlMinutes, int taxRateBps, long depositMinCents, int depositPercentBps) {

    public PricingProperties {
        if (quoteTtlMinutes <= 0) {
            quoteTtlMinutes = 15;
        }
    }
}
