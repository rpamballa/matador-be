package com.matador.shared.security.staff;

import com.matador.shared.id.IdGenerator;
import com.matador.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Seeds a default admin account in dev so the admin API is immediately usable. */
@Configuration
@Profile("dev")
public class StaffUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(StaffUserSeeder.class);
    private static final String DEFAULT_EMAIL = "admin@matador.local";
    private static final String DEFAULT_PASSWORD = "matador-dev-admin";

    @Bean
    ApplicationRunner seedDefaultAdmin(
        StaffUserRepository repository, PasswordEncoder passwordEncoder, IdGenerator idGenerator) {
        return args -> {
            if (repository.existsByEmailIgnoreCase(DEFAULT_EMAIL)) {
                return;
            }
            repository.save(
                new StaffUser(
                    idGenerator.newId(),
                    DEFAULT_EMAIL,
                    passwordEncoder.encode(DEFAULT_PASSWORD),
                    "Dev",
                    "Admin",
                    Role.ADMIN));
            log.warn(
                "Seeded dev admin account {} / {} — dev profile only.",
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD);
        };
    }
}
