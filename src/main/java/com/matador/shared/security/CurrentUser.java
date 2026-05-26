package com.matador.shared.security;

import com.matador.shared.error.UnauthorizedException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static accessor for the authenticated principal in the current request scope.
 * Works for both customer (JWT) and staff (session) authentication.
 */
public final class CurrentUser {

    /** Stable id used as the auditor for background jobs and unauthenticated writes. */
    public static final UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private CurrentUser() {}

    public static Optional<MatadorPrincipal> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
            && auth.isAuthenticated()
            && auth.getPrincipal() instanceof MatadorPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static MatadorPrincipal require() {
        return find().orElseThrow(() -> new UnauthorizedException("Authentication required."));
    }

    public static UUID requireId() {
        return require().id();
    }
}
