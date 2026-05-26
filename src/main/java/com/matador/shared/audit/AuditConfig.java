package com.matador.shared.audit;

import com.matador.shared.security.CurrentUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> Optional.of(CurrentUser.find().map(u -> u.id()).orElse(CurrentUser.SYSTEM_ID));
    }
}
