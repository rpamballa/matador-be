package com.matador.shared.security.staff;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffUserRepository repository;

    public StaffUserDetailsService(StaffUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository
            .findByEmailIgnoreCase(username)
            .map(StaffUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("No staff user: " + username));
    }
}
