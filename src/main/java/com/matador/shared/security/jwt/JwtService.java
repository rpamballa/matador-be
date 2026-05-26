package com.matador.shared.security.jwt;

import com.matador.shared.error.UnauthorizedException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies customer JWTs signed with RS256.
 *
 * <p>Access tokens are short-lived bearer tokens; refresh tokens are long-lived and
 * carry {@code type=refresh} so they cannot be used to authenticate API calls directly.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtKeyProvider keys;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtKeyProvider keys, JwtProperties properties, Clock clock) {
        this.keys = keys;
        this.properties = properties;
        this.clock = clock;
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}

    public TokenPair issueTokens(UUID customerId, String email) {
        Instant now = clock.instant();
        long accessTtlSeconds = properties.accessTtlMinutes() * 60L;
        String access =
            sign(customerId, email, TYPE_ACCESS, now, now.plus(properties.accessTtlMinutes(), ChronoUnit.MINUTES));
        String refresh =
            sign(customerId, email, TYPE_REFRESH, now, now.plus(properties.refreshTtlDays(), ChronoUnit.DAYS));
        return new TokenPair(access, refresh, accessTtlSeconds);
    }

    /** Verifies an access token and returns the customer id. */
    public UUID verifyAccessToken(String token) {
        JWTClaimsSet claims = verify(token);
        requireType(claims, TYPE_ACCESS);
        return UUID.fromString(claims.getSubject());
    }

    /** Verifies a refresh token and returns the customer id. */
    public UUID verifyRefreshToken(String token) {
        JWTClaimsSet claims = verify(token);
        requireType(claims, TYPE_REFRESH);
        return UUID.fromString(claims.getSubject());
    }

    private String sign(UUID subject, String email, String type, Instant issuedAt, Instant expiresAt) {
        try {
            JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                    .subject(subject.toString())
                    .issuer(properties.issuer())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim(CLAIM_TYPE, type)
                    .claim(CLAIM_EMAIL, email)
                    .build();
            SignedJWT jwt =
                new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.keyId()).build(), claims);
            jwt.sign(new RSASSASigner(keys.privateKey()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private JWTClaimsSet verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new RSASSAVerifier(keys.publicKey()))) {
                throw new UnauthorizedException("Invalid token signature.");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiry = claims.getExpirationTime();
            if (expiry == null || expiry.toInstant().isBefore(clock.instant())) {
                throw new UnauthorizedException("Token expired.");
            }
            return claims;
        } catch (java.text.ParseException | JOSEException e) {
            throw new UnauthorizedException("Malformed token.");
        }
    }

    private void requireType(JWTClaimsSet claims, String expected) {
        try {
            if (!expected.equals(claims.getStringClaim(CLAIM_TYPE))) {
                throw new UnauthorizedException("Unexpected token type.");
            }
        } catch (java.text.ParseException e) {
            throw new UnauthorizedException("Malformed token.");
        }
    }
}
