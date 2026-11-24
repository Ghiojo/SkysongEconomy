package org.skysongdev.skysongEconomy.api.nodes;

import java.util.Objects;

/**
 * A booster item used up on a harvest.
 *
 * @param type   what it works on: a random table's type, such as {@code mining}, or a farm type, such as {@code crop}
 * @param effect what it changed, written as its {@code node_booster_effect} tag is, such as {@code min+1; max+25%}
 */
public record BoosterInfo(String type, String effect) {
    public BoosterInfo {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(effect, "effect");
    }
}
