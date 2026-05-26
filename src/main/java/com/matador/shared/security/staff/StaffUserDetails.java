package com.matador.shared.security.staff;

import com.matador.shared.security.MatadorPrincipal;
import com.matador.shared.security.Role;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Adapts a {@link StaffUser} to Spring Security while exposing the staff id as a principal. */
public class StaffUserDetails implements UserDetails, MatadorPrincipal {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public StaffUserDetails(StaffUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.active = user.isActive();
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
