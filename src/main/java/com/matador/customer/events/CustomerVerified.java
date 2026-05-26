package com.matador.customer.events;

import java.time.Instant;
import java.util.UUID;

public record CustomerVerified(UUID customerId, Instant verifiedAt) {}
