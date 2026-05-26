package com.matador.shared.security.jwt;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Supplies the RSA keypair used to sign and verify customer JWTs (RS256).
 *
 * <p>In production the PEM-encoded keys are provided via env vars. When absent (local
 * dev/test), an ephemeral keypair is generated at startup so the app remains runnable;
 * a warning is logged because tokens will not survive a restart.
 */
@Component
public class JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);
    private static final String KEY_ID = "matador-jwt";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public JwtKeyProvider(JwtProperties properties) {
        if (hasText(properties.privateKeyPem()) && hasText(properties.publicKeyPem())) {
            this.privateKey = parsePrivateKey(properties.privateKeyPem());
            this.publicKey = parsePublicKey(properties.publicKeyPem());
        } else {
            log.warn(
                "No JWT keypair configured (matador.jwt.private-key-pem / public-key-pem); "
                    + "generating an ephemeral RSA keypair. Do NOT use this in production.");
            KeyPair pair = generateKeyPair();
            this.publicKey = (RSAPublicKey) pair.getPublic();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
        }
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public String keyId() {
        return KEY_ID;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA keypair", e);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            byte[] der = Base64.getDecoder().decode(stripPem(pem));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT private key PEM", e);
        }
    }

    private static RSAPublicKey parsePublicKey(String pem) {
        try {
            byte[] der = Base64.getDecoder().decode(stripPem(pem));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT public key PEM", e);
        }
    }

    private static String stripPem(String pem) {
        return pem.replaceAll("-----BEGIN (.*)-----", "")
            .replaceAll("-----END (.*)-----", "")
            .replaceAll("\\s", "");
    }
}
