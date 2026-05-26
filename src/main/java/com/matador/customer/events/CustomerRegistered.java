package com.matador.customer.events;

import java.time.Instant;
import java.util.UUID;

public record CustomerRegistered(UUID customerId, String email, Instant registeredAt) {}
