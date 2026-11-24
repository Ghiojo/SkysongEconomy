package org.skysongdev.skysongEconomy.api.trade;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One price modifier that changed a trade's price, and how much money it added or took.
 *
 * @param id          the modifier's id, as {@code /se modifier list} shows it
 * @param type        its type key, such as {@code port_tax}
 * @param target      what it was set on, such as {@code global}, {@code ledger:smugglers} or {@code pier:north_dock}
 * @param percent     its signed percentage, such as 10 for +10%
 * @param share       what it changed the price by, in money: {@code base * percent / 100}; negative for a discount
 * @param displayName the name players saw for it
 */
public record AppliedTradeModifier(String id, String type, String target, BigDecimal percent, BigDecimal share,
                                   String displayName) {
    public AppliedTradeModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(percent, "percent");
        Objects.requireNonNull(share, "share");
        Objects.requireNonNull(displayName, "displayName");
    }
}
