/**
 * Ledger module: an immutable, insert-only record of financial events — the source of
 * truth for all money state. Subscribes to payment, trip, and incident events.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Ledger")
package com.matador.ledger;
