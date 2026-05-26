/**
 * Pricing module: rate cards, promo codes, and quote calculation. Quotes are immutable
 * snapshots with a TTL; bookings reference a valid quote.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Pricing")
package com.matador.pricing;
