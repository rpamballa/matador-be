package com.matador.pricing;

/** A single line in a price breakdown. {@code amountCents} may be negative (e.g. promo). */
public record LineItem(String type, String description, long amountCents) {

    public static final String RENTAL = "RENTAL";
    public static final String DELIVERY = "DELIVERY";
    public static final String INSURANCE = "INSURANCE";
    public static final String OUT_OF_ZONE_DROPOFF = "OUT_OF_ZONE_DROPOFF";
    public static final String PROMO = "PROMO";
    public static final String TAX = "TAX";
}
