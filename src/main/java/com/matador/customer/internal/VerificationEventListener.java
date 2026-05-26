package com.matador.customer.internal;

import com.matador.customer.CustomerService;
import com.matador.identity.events.VerificationCompleted;
import com.matador.identity.events.VerificationStarted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Advances a customer's verification status when the identity module reports progress. */
@Component
class VerificationEventListener {

    private final CustomerService customerService;

    VerificationEventListener(CustomerService customerService) {
        this.customerService = customerService;
    }

    @ApplicationModuleListener
    void on(VerificationStarted event) {
        customerService.markVerificationStarted(event.customerId());
    }

    @ApplicationModuleListener
    void on(VerificationCompleted event) {
        customerService.applyVerificationResult(
            event.customerId(),
            event.success(),
            event.licenseNumber(),
            event.licenseState(),
            event.licenseExpiresOn(),
            event.completedAt());
    }
}
