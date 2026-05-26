package com.matador.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void addsAndSubtractsWithinSameCurrency() {
        assertThat(Money.usd(1500).plus(Money.usd(500))).isEqualTo(Money.usd(2000));
        assertThat(Money.usd(1500).minus(Money.usd(500))).isEqualTo(Money.usd(1000));
        assertThat(Money.usd(300).times(3)).isEqualTo(Money.usd(900));
    }

    @Test
    void rejectsMixedCurrencyArithmetic() {
        Money eur = new Money(100, Currency.getInstance("EUR"));
        assertThatThrownBy(() -> Money.usd(100).plus(eur))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportsSign() {
        assertThat(Money.usd(1).isPositive()).isTrue();
        assertThat(Money.usd(-1).isNegative()).isTrue();
        assertThat(Money.usd(0).isZero()).isTrue();
        assertThat(Money.usd(5).negate()).isEqualTo(Money.usd(-5));
    }
}
