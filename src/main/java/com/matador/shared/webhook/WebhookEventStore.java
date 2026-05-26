package com.matador.shared.webhook;

import com.matador.shared.id.IdGenerator;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Provides idempotent recording of inbound webhook events across providers. */
@Service
public class WebhookEventStore {

    private final WebhookEventRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public WebhookEventStore(
        WebhookEventRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * Records an event as processed. Returns {@code true} if this is the first time the
     * event is seen (caller should process it), {@code false} if already recorded.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(
        String provider, String providerEventId, String eventType, String payload) {
        if (repository.existsByProviderAndProviderEventId(provider, providerEventId)) {
            return false;
        }
        try {
            repository.saveAndFlush(
                new WebhookEvent(
                    idGenerator.newId(),
                    provider,
                    providerEventId,
                    eventType,
                    payload,
                    clock.instant()));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            // Lost a race with a concurrent delivery of the same event.
            return false;
        }
    }
}
