package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceExpression;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Every Claim's asking price, kept in memory and saved through a {@link ClaimPriceRepository}.
 * It only stores and works out prices; {@link ClaimSales} decides when they may change, so use
 * that to change them. Main thread only.
 */
public final class ClaimPrices {
    private final ClaimPriceRepository repository;
    private final PriceDatabase database;
    private final PriceSettings settings;
    private final Clock clock;
    private final Map<String, ClaimPrice> prices = new LinkedHashMap<>();

    /**
     * @param database where {@code {item:...}} in a price gets its value
     * @param settings how many decimals prices keep
     */
    public ClaimPrices(ClaimPriceRepository repository, PriceDatabase database, PriceSettings settings, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.database = Objects.requireNonNull(database, "database");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Loads every saved price, replacing what is in memory.
     *
     * @param warnings told about rows whose expression can't be read; those are skipped and stay saved
     * @return how many were loaded
     */
    public int loadAll(Consumer<String> warnings) {
        prices.clear();
        for (ClaimPriceRepository.Row row : repository.loadAll()) {
            try {
                PriceExpression expression = PriceExpression.parse(row.expression());
                String id = normalize(row.claimId());
                prices.put(id, new ClaimPrice(id, expression, row.setBy(), Instant.ofEpochMilli(row.setAtMillis())));
            } catch (PriceException e) {
                warnings.accept("The price of the Claim " + row.claimId() + " can't be read and is skipped: "
                        + row.expression());
            }
        }
        return prices.size();
    }

    /** @return the Claim's asking price as it was set; empty when it has none */
    public Optional<ClaimPrice> find(String claimId) {
        return Optional.ofNullable(prices.get(normalize(claimId)));
    }

    /** @return every price, by Claim id, in the order they were loaded or set */
    public Map<String, ClaimPrice> all() {
        return Collections.unmodifiableMap(prices);
    }

    /**
     * @return what the Claim's asking price is worth now, truncated; empty when it has none
     * @throws PriceException when it can't be worked out, e.g. an item it names lost its base value
     */
    public Optional<BigDecimal> value(String claimId) {
        return find(claimId).map(price -> worth(price.expression()));
    }

    /**
     * Sets a Claim's asking price.
     *
     * @param text a price expression, such as {@code 2000} or {@code {item:architite_pound} * 600}
     * @param by   who set it
     * @return what it is worth now
     * @throws PriceException when the expression is invalid, uses {@code {base}}, or names an item without a base value
     * @throws ClaimException {@code claims.price-not-positive} when it isn't worth more than 0
     */
    BigDecimal set(String claimId, String text, String by) {
        PriceExpression expression = PriceExpression.parse(text);
        BigDecimal value = check(expression);
        String id = normalize(claimId);
        ClaimPrice price = new ClaimPrice(id, expression, by, clock.instant());
        prices.put(id, price);
        repository.save(price);
        return value;
    }

    /** @return whether the Claim had a price, which is now gone */
    boolean remove(String claimId) {
        String id = normalize(claimId);
        if (prices.remove(id) == null) {
            return false;
        }
        repository.delete(id);
        return true;
    }

    /**
     * Works out what a price would be worth, with the checks {@link #set} makes, without saving it,
     * such as to show an owner what they'd ask before they confirm.
     *
     * @throws PriceException when the expression can't be used
     * @throws ClaimException {@code claims.price-not-positive}
     */
    public BigDecimal check(String text) {
        return check(PriceExpression.parse(text));
    }

    private BigDecimal check(PriceExpression expression) {
        if (expression.usesBase()) {
            throw new PriceException("prices.no-base", Map.of());
        }
        BigDecimal value = worth(expression);
        if (value.signum() <= 0) {
            throw new ClaimException("claims.price-not-positive",
                    Map.of("expression", expression.text(), "value", value.toPlainString()));
        }
        return value;
    }

    private BigDecimal worth(PriceExpression expression) {
        return settings.truncate(expression.evaluate(database.context(null)));
    }

    private static String normalize(String claimId) {
        return claimId.toLowerCase(Locale.ROOT);
    }
}
