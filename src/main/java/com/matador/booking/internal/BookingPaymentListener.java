package com.matador.booking.internal;

import com.matador.booking.BookingService;
import com.matador.payment.events.PaymentEvents.PaymentHeld;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Confirms a booking once its deposit hold is placed. */
@Component
class BookingPaymentListener {

    private final BookingService bookingService;

    BookingPaymentListener(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @ApplicationModuleListener
    void on(PaymentHeld event) {
        if (event.bookingId() != null) {
            bookingService.confirmFromPayment(event.bookingId());
        }
    }
}
