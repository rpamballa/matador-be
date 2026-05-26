package com.matador.shared.money;

import java.util.Currency;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * Maps {@link Money} to two columns: {@code amount_cents BIGINT} and {@code currency CHAR(3)}.
 *
 * <p>Apply on an entity field with:
 * <pre>{@code
 * @CompositeType(MoneyType.class)
 * @AttributeOverrides({
 *   @AttributeOverride(name = "amountCents", column = @Column(name = "amount_cents")),
 *   @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3))
 * })
 * private Money amount;
 * }</pre>
 */
public class MoneyType implements CompositeUserType<Money> {

    /** Property holder mirroring {@link Money}'s mapped columns. */
    public record MoneyMapper(long amountCents, String currency) {}

    @Override
    public Object getPropertyValue(Money component, int property) {
        return switch (property) {
            case 0 -> component.amountCents();
            case 1 -> component.currency().getCurrencyCode();
            default -> throw new HibernateException("Unknown property index: " + property);
        };
    }

    @Override
    public Money instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
        long amountCents = values.getValue(0, Long.class);
        String currency = values.getValue(1, String.class);
        return new Money(amountCents, Currency.getInstance(currency));
    }

    @Override
    public Class<?> embeddable() {
        return MoneyMapper.class;
    }

    @Override
    public Class<Money> returnedClass() {
        return Money.class;
    }

    @Override
    public boolean equals(Money x, Money y) {
        return java.util.Objects.equals(x, y);
    }

    @Override
    public int hashCode(Money x) {
        return java.util.Objects.hashCode(x);
    }

    @Override
    public Money deepCopy(Money value) {
        return value; // immutable record
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public java.io.Serializable disassemble(Money value) {
        return value == null
            ? null
            : new long[] {value.amountCents(), value.currency().getNumericCode()};
    }

    @Override
    public Money assemble(java.io.Serializable cached, Object owner) {
        if (cached == null) {
            return null;
        }
        long[] parts = (long[]) cached;
        return new Money(parts[0], currencyFromNumeric((int) parts[1]));
    }

    @Override
    public Money replace(Money detached, Money managed, Object owner) {
        return detached;
    }

    private static Currency currencyFromNumeric(int numericCode) {
        for (Currency c : Currency.getAvailableCurrencies()) {
            if (c.getNumericCode() == numericCode) {
                return c;
            }
        }
        throw new HibernateException("Unknown currency numeric code: " + numericCode);
    }
}
