package com.matador.shared.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matador.shared.error.UnauthorizedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(null, null, 15, 30, "https://test.matador");
        JwtKeyProvider keys = new JwtKeyProvider(props);
        jwtService = new JwtService(keys, props, Clock.systemUTC());
    }

    @Test
    void issuesAndVerifiesAccessToken() {
        JwtService.TokenPair pair = jwtService.issueTokens(customerId, "jane@example.com");
        assertThat(jwtService.verifyAccessToken(pair.accessToken())).isEqualTo(customerId);
        assertThat(pair.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshAndViceVersa() {
        JwtService.TokenPair pair = jwtService.issueTokens(customerId, "jane@example.com");
        assertThatThrownBy(() -> jwtService.verifyRefreshToken(pair.accessToken()))
            .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> jwtService.verifyAccessToken(pair.refreshToken()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtProperties props = new JwtProperties(null, null, 15, 30, "https://test.matador");
        JwtKeyProvider keys = new JwtKeyProvider(props);
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        JwtService issuer = new JwtService(keys, props, Clock.fixed(base, ZoneOffset.UTC));
        String token = issuer.issueTokens(customerId, "jane@example.com").accessToken();

        JwtService laterVerifier =
            new JwtService(keys, props, Clock.fixed(base.plus(Duration.ofHours(1)), ZoneOffset.UTC));
        assertThatThrownBy(() -> laterVerifier.verifyAccessToken(token))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.issueTokens(customerId, "jane@example.com").accessToken();
        String tampered = token.substring(0, token.length() - 2) + "xy";
        assertThatThrownBy(() -> jwtService.verifyAccessToken(tampered))
            .isInstanceOf(UnauthorizedException.class);
    }
}
