/**
 * Payment module: wraps Stripe (payment methods, intents, deposit holds, captures,
 * refunds) and publishes financial events the ledger module consumes.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Payment")
package com.matador.payment;
