package com.matador.telematics.internal;

import com.matador.shared.crypto.FieldCipher;
import com.matador.shared.id.IdGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Stores telematics OAuth tokens encrypted at rest. */
@Component
public class TelematicsTokenStore {

    private final TelematicsTokenRepository tokens;
    private final FieldCipher cipher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public TelematicsTokenStore(
        TelematicsTokenRepository tokens, FieldCipher cipher, IdGenerator idGenerator, Clock clock) {
        this.tokens = tokens;
        this.cipher = cipher;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean hasToken(UUID vehicleId, String provider) {
        return tokens.existsByVehicleIdAndProvider(vehicleId, provider);
    }

    @Transactional
    public void store(
        UUID vehicleId,
        String provider,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        String scopes) {
        tokens.save(
            new TelematicsToken(
                idGenerator.newId(),
                vehicleId,
                provider,
                cipher.encrypt(accessToken),
                cipher.encrypt(refreshToken),
                accessTokenExpiresAt,
                scopes,
                clock.instant()));
    }
}
