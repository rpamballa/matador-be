package com.matador.shared.webhook;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}
