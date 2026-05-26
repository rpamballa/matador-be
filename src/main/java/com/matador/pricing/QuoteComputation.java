package com.matador.pricing;

import java.util.List;

/** Result of a price calculation: the breakdown plus rolled-up totals. */
public record QuoteComputation(
    List<LineItem> lineItems,
    int days,
    long subtotalCents,
    long discountCents,
    long taxCents,
    long totalCents,
    long depositCents) {}
