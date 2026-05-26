package com.matador.notification.internal;

import com.matador.booking.events.BookingEvents.BookingConfirmed;
import com.matador.customer.CustomerService;
import com.matador.customer.events.CustomerRegistered;
import com.matador.customer.events.CustomerVerified;
import com.matador.notification.NotificationService;
import com.matador.payment.events.PaymentEvents.PaymentFailed;
import java.util.Map;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Translates domain events into queued notifications. */
@Component
class NotificationEventListeners {

    private final NotificationService notifications;
    private final CustomerService customers;

    NotificationEventListeners(NotificationService notifications, CustomerService customers) {
        this.notifications = notifications;
        this.customers = customers;
    }

    @ApplicationModuleListener
    void on(CustomerRegistered event) {
        notifications.queueEmail(
            event.customerId(), "customer-welcome", event.email(), Map.of());
    }

    @ApplicationModuleListener
    void on(CustomerVerified event) {
        notifications.queueEmail(
            event.customerId(),
            "verification-completed",
            customers.customerEmail(event.customerId()),
            Map.of());
    }

    @ApplicationModuleListener
    void on(BookingConfirmed event) {
        notifications.queueEmail(
            event.customerId(),
            "booking-confirmed",
            customers.customerEmail(event.customerId()),
            Map.of("bookingId", event.bookingId().toString()));
    }

    @ApplicationModuleListener
    void on(PaymentFailed event) {
        if (event.customerId() != null) {
            notifications.queueEmail(
                event.customerId(),
                "payment-failed",
                customers.customerEmail(event.customerId()),
                Map.of("reason", event.reason()));
        }
    }
}
