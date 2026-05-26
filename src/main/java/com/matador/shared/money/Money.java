package com.matador.shared.money;

import java.util.Currency;
import java.util.Objects;

/**
 * Monetary value as an integer number of minor units (cents) plus a currency.
 * Never use {@code double} or {@code BigDecimal} for money in this codebase.
 */
public record Money(long amountCents, Currency currency) {

    public static final Currency USD = Currency.getInstance("USD");

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money usd(long cents) {
        return new Money(cents, USD);
    }

    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amountCents + other.amountCents, currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amountCents - other.amountCents, currency);
    }

    public Money times(int n) {
        return new Money(amountCents * n, currency);
    }

    public Money negate() {
        return new Money(-amountCents, currency);
    }

    public boolean isPositive() {
        return amountCents > 0;
    }

    public boolean isNegative() {
        return amountCents < 0;
    }

    public boolean isZero() {
        return amountCents == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Currency mismatch: %s vs %s".formatted(currency, other.currency));
        }
    }
}
