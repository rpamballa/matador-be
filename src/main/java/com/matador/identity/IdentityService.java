package com.matador.identity;

import com.matador.identity.api.IdentityDtos.VerificationResponse;
import com.matador.identity.events.VerificationCompleted;
import com.matador.identity.events.VerificationStarted;
import com.matador.identity.internal.IdentityProvider;
import com.matador.identity.internal.IdentityProvider.CreatedSession;
import com.matador.identity.internal.VerificationSession;
import com.matador.identity.internal.VerificationSessionRepository;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.id.IdGenerator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the identity module. */
@Service
public class IdentityService {

    private final VerificationSessionRepository sessions;
    private final IdentityProvider provider;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public IdentityService(
        VerificationSessionRepository sessions,
        IdentityProvider provider,
        IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events) {
        this.sessions = sessions;
        this.provider = provider;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public VerificationResponse startSession(UUID customerId) {
        // Reuse an existing non-terminal session rather than creating duplicates.
        VerificationSession existing =
            sessions.findFirstByCustomerIdOrderByCreatedAtDesc(customerId).orElse(null);
        if (existing != null && !existing.getStatus().isTerminal()) {
            return toResponse(existing, existing.getClientSecret());
        }

        CreatedSession created = provider.createSession(customerId);
        VerificationSession session =
            new VerificationSession(
                idGenerator.newId(),
                customerId,
                provider.name(),
                created.providerSessionId(),
                created.clientSecret(),
                clock.instant());
        sessions.save(session);
        events.publishEvent(
            new VerificationStarted(customerId, session.getId(), clock.instant()));
        return toResponse(session, created.clientSecret());
    }

    @Transactional(readOnly = true)
    public VerificationResponse currentStatus(UUID customerId) {
        VerificationSession session =
            sessions
                .findFirstByCustomerIdOrderByCreatedAtDesc(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("VerificationSession", customerId));
        return toResponse(session, null);
    }

    @Transactional
    public void recordVerified(
        String providerSessionId,
        String licenseNumber,
        String licenseState,
        LocalDate licenseExpiresOn,
        String rawPayload) {
        VerificationSession session = requireByProviderId(providerSessionId);
        var completedAt = clock.instant();
        session.applyResult(VerificationSessionStatus.VERIFIED, rawPayload, completedAt);
        events.publishEvent(
            new VerificationCompleted(
                session.getCustomerId(),
                session.getId(),
                true,
                licenseNumber,
                licenseState,
                licenseExpiresOn,
                completedAt));
    }

    @Transactional
    public void recordRequiresInput(String providerSessionId, String rawPayload) {
        VerificationSession session = requireByProviderId(providerSessionId);
        session.applyResult(VerificationSessionStatus.REQUIRES_INPUT, rawPayload, clock.instant());
    }

    @Transactional
    public void recordCanceled(String providerSessionId, String rawPayload) {
        VerificationSession session = requireByProviderId(providerSessionId);
        var completedAt = clock.instant();
        session.applyResult(VerificationSessionStatus.CANCELED, rawPayload, completedAt);
        events.publishEvent(
            new VerificationCompleted(
                session.getCustomerId(), session.getId(), false, null, null, null, completedAt));
    }

    private VerificationSession requireByProviderId(String providerSessionId) {
        return sessions
            .findByProviderSessionId(providerSessionId)
            .orElseThrow(
                () -> ResourceNotFoundException.of("VerificationSession", providerSessionId));
    }

    private VerificationResponse toResponse(VerificationSession session, String clientSecret) {
        return new VerificationResponse(
            session.getId(), clientSecret, session.getStatus(), session.getCompletedAt());
    }
}
