package com.matador.identity.events;

import java.time.Instant;
import java.util.UUID;

public record VerificationStarted(UUID customerId, UUID sessionId, Instant startedAt) {}
