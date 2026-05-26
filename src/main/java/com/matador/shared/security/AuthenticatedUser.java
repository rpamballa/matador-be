package com.matador.shared.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The principal stored in the Spring Security context for both staff (session)
 * and customer (JWT) authentication. Carries the database id of the authenticated
 * subject so that auditing and authorization can use it directly.
 */
public record AuthenticatedUser(UUID id, String username, Role role) implements MatadorPrincipal {

    public Collection<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }
}
