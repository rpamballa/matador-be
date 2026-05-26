package com.matador.customer.events;

import java.time.Instant;
import java.util.UUID;

public record CustomerSuspended(UUID customerId, String reason, Instant suspendedAt) {}
