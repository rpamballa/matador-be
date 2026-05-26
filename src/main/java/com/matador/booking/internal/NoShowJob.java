package com.matador.booking.internal;

import com.matador.booking.BookingPolicyProperties;
import com.matador.booking.BookingService;
import java.time.Clock;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Marks confirmed bookings as NO_SHOW when their pickup window has lapsed. */
@Component
class NoShowJob {

    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final BookingPolicyProperties policy;
    private final Clock clock;

    NoShowJob(
        BookingRepository bookings,
        BookingService bookingService,
        BookingPolicyProperties policy,
        Clock clock) {
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.policy = policy;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT15M")
    @SchedulerLock(name = "booking-no-show", lockAtMostFor = "PT10M")
    void markNoShows() {
        var threshold = clock.instant().minus(Duration.ofMinutes(policy.noShowAfterMinutes()));
        bookings.findConfirmedPastPickup(threshold).forEach(b -> bookingService.markNoShow(b.getId()));
    }
}
