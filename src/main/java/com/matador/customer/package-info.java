/**
 * Customer module: manages customer profiles, credentials, addresses, and lifecycle.
 *
 * <p>Owns {@code verification_status}; consumes {@code VerificationCompleted} events from
 * the identity module to advance it. Exposes {@link com.matador.customer.CustomerService}
 * as its public API.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Customer")
package com.matador.customer;
