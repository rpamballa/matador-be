package com.matador.shared.security;

import java.util.UUID;

/**
 * Common contract for the authenticated subject, regardless of whether the request
 * was authenticated by JWT (customer) or session cookie (staff).
 */
public interface MatadorPrincipal {

    UUID id();

    Role role();
}
