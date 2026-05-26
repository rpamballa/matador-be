package com.matador.zone;

import java.util.UUID;

/** Lightweight zone projection shared with other modules (e.g. booking, pricing). */
public record ZoneInfo(
    UUID id,
    String name,
    String slug,
    long outOfZoneDropoffFeeCents,
    boolean outOfZoneDropoffAllowed) {}
