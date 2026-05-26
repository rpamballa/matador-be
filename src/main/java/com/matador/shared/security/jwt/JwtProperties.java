package com.matador.shared.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "matador.jwt")
public record JwtProperties(
    String privateKeyPem,
    String publicKeyPem,
    int accessTtlMinutes,
    int refreshTtlDays,
    String issuer) {

    public JwtProperties {
        if (accessTtlMinutes <= 0) {
            accessTtlMinutes = 15;
        }
        if (refreshTtlDays <= 0) {
            refreshTtlDays = 30;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "https://api.matador.com";
        }
    }
}
