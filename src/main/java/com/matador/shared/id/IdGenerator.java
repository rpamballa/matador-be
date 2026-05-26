package com.matador.shared.id;

import java.util.UUID;

/**
 * Source of time-ordered UUIDv7 primary keys. Inject this everywhere a new
 * aggregate id is needed; never call a UUID generator directly from domain code.
 */
public interface IdGenerator {

    UUID newId();
}
