package com.matador.booking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "matador.policy")
public record BookingPolicyProperties(
    int minBookingHours,
    int maxBookingDays,
    int pickupLeadTimeMinutes,
    int noShowAfterMinutes,
    int cancellationFullRefundHours,
    int cancellationPartialHours,
    int cancellationPartialPercentBps) {

    public BookingPolicyProperties {
        if (minBookingHours <= 0) {
            minBookingHours = 4;
        }
        if (maxBookingDays <= 0) {
            maxBookingDays = 30;
        }
        if (pickupLeadTimeMinutes <= 0) {
            pickupLeadTimeMinutes = 120;
        }
        if (noShowAfterMinutes <= 0) {
            noShowAfterMinutes = 120;
        }
        if (cancellationFullRefundHours <= 0) {
            cancellationFullRefundHours = 24;
        }
        if (cancellationPartialHours <= 0) {
            cancellationPartialHours = 2;
        }
        if (cancellationPartialPercentBps <= 0) {
            cancellationPartialPercentBps = 5000;
        }
    }
}
