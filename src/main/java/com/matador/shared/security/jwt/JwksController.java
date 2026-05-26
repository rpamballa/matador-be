package com.matador.shared.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publishes the RSA public key as a JWK Set so token signatures can be verified externally. */
@RestController
@Tag(name = "Customer-Auth")
public class JwksController {

    private final JwtKeyProvider keys;

    public JwksController(JwtKeyProvider keys) {
        this.keys = keys;
    }

    @GetMapping("/jwks.json")
    @Operation(summary = "JWKS", description = "RSA public key set for verifying customer JWTs.")
    public Map<String, Object> jwks() {
        RSAKey jwk =
            new RSAKey.Builder(keys.publicKey())
                .keyID(keys.keyId())
                .keyUse(KeyUse.SIGNATURE)
                .build();
        return new JWKSet(jwk).toJSONObject();
    }
}
