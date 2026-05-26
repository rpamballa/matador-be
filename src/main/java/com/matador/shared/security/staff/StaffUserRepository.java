package com.matador.shared.security.staff;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepository extends JpaRepository<StaffUser, UUID> {

    Optional<StaffUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
