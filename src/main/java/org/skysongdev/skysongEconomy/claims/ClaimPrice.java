package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.prices.PriceExpression;

import java.time.Instant;
import java.util.Objects;

/**
 * The asking price of a Claim: what its seller asks, before any modifier.
 *
 * @param claimId    the Claim, in lower case
 * @param expression the price, usually a plain number but any price expression without {@code {base}},
 *                   such as {@code {item:architite_pound} * 600}
 * @param setBy      who set it: a player's name, or {@code CONSOLE}
 * @param setAt      when it was set
 */
public record ClaimPrice(String claimId, PriceExpression expression, String setBy, Instant setAt) {
    public ClaimPrice {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(setBy, "setBy");
        Objects.requireNonNull(setAt, "setAt");
    }
}
