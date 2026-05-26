package com.matador.pricing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure price calculation — no persistence, no Spring. All money is integer cents.
 *
 * <p>Rules (BACKEND.md §6.6): days = ceil(duration / 24h); tax applies to the
 * post-discount subtotal; deposit = max(min, percent of subtotal) rounded up to $50.
 */
public final class PricingCalculator {

    private static final long DEPOSIT_ROUNDING_CENTS = 5000; // nearest $50

    private PricingCalculator() {}

    public record Inputs(
        Instant pickupAt,
        Instant dropoffAt,
        long dailyRateCents,
        long deliveryFeeCents,
        long insuranceDailyCents,
        String vehicleClassName,
        boolean dropoffInZone,
        long outOfZoneFeeCents,
        long discountCents,
        String promoLabel,
        int taxRateBps,
        long depositMinCents,
        int depositPercentBps) {}

    public static int billableDays(Instant pickupAt, Instant dropoffAt) {
        long seconds = Duration.between(pickupAt, dropoffAt).getSeconds();
        if (seconds <= 0) {
            return 1;
        }
        long perDay = Duration.ofDays(1).getSeconds();
        return (int) ((seconds + perDay - 1) / perDay);
    }

    public static QuoteComputation calculate(Inputs in) {
        int days = billableDays(in.pickupAt(), in.dropoffAt());
        List<LineItem> items = new ArrayList<>();

        long rental = in.dailyRateCents() * days;
        items.add(
            new LineItem(
                LineItem.RENTAL, "%d days × %s".formatted(days, in.vehicleClassName()), rental));

        if (in.deliveryFeeCents() > 0) {
            items.add(new LineItem(LineItem.DELIVERY, "Delivery fee", in.deliveryFeeCents()));
        }

        long insurance = in.insuranceDailyCents() * days;
        if (insurance > 0) {
            items.add(
                new LineItem(
                    LineItem.INSURANCE, "Standard protection × %d days".formatted(days), insurance));
        }

        long outOfZone = in.dropoffInZone() ? 0 : in.outOfZoneFeeCents();
        if (outOfZone > 0) {
            items.add(new LineItem(LineItem.OUT_OF_ZONE_DROPOFF, "Out-of-zone dropoff", outOfZone));
        }

        long grossSubtotal = rental + in.deliveryFeeCents() + insurance + outOfZone;

        long discount = Math.min(in.discountCents(), grossSubtotal);
        if (discount > 0) {
            items.add(new LineItem(LineItem.PROMO, "Promo " + in.promoLabel(), -discount));
        }

        long taxable = grossSubtotal - discount;
        long tax = Math.round(taxable * (in.taxRateBps() / 10000.0));
        items.add(new LineItem(LineItem.TAX, "Sales tax", tax));

        long total = taxable + tax;

        long depositByPercent = Math.round(grossSubtotal * (in.depositPercentBps() / 10000.0));
        long deposit = roundUpTo(Math.max(in.depositMinCents(), depositByPercent), DEPOSIT_ROUNDING_CENTS);

        return new QuoteComputation(List.copyOf(items), days, grossSubtotal, discount, tax, total, deposit);
    }

    private static long roundUpTo(long value, long step) {
        if (step <= 0) {
            return value;
        }
        return ((value + step - 1) / step) * step;
    }
}
