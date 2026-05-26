/**
 * Identity module: ID and driver's-license verification via Stripe Identity.
 *
 * <p>Decouples the customer module from the verification provider. Publishes
 * {@link com.matador.identity.events.VerificationCompleted} which the customer module
 * consumes to advance {@code verification_status}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Identity")
package com.matador.identity;
