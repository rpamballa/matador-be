package com.matador.booking.internal;

import com.matador.booking.BookingService;
import com.matador.trip.events.TripEvents.TripClosed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Completes a booking when its trip is closed. */
@Component
class BookingTripListener {

    private final BookingService bookingService;

    BookingTripListener(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @ApplicationModuleListener
    void on(TripClosed event) {
        bookingService.completeFromTrip(event.bookingId(), event.tripId());
    }
}
