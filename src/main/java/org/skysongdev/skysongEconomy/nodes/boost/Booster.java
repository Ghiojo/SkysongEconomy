package org.skysongdev.skysongEconomy.nodes.boost;

import org.skysongdev.skysongEconomy.loot.YieldAdjuster;
import org.skysongdev.skysongEconomy.nodes.NodeException;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A booster item, as its two tags describe it.
 *
 * @param type   what it works on: a random table's {@code type}, such as {@code mining}, or a
 *               farm type's id, such as {@code crop}; from the {@code node_booster_type} tag
 * @param effect what it changes, from the {@code node_booster_effect} tag
 */
public record Booster(String type, BoosterEffect effect) {
    public Booster {
        type = Objects.requireNonNull(type, "type").strip().toLowerCase(Locale.ROOT);
        Objects.requireNonNull(effect, "effect");
    }

    /** @return whether it works on a node whose booster type is {@code nodeType} */
    public boolean worksOn(String nodeType) {
        return type.equals(nodeType.strip().toLowerCase(Locale.ROOT));
    }

    /**
     * Checks that a harvest's booster, if any, works on the node.
     *
     * @param nodeType the node's booster type
     * @throws NodeException {@code node.booster.wrong-type} when it doesn't
     */
    public static void requireWorksOn(Optional<Booster> booster, String nodeType) {
        if (booster.isPresent() && !booster.get().worksOn(nodeType)) {
            throw new NodeException("node.booster.wrong-type", Map.of("type", booster.get().type(), "needed", nodeType));
        }
    }

    /** @return what a harvest's booster changes; nothing without one */
    public static YieldAdjuster effectOf(Optional<Booster> booster) {
        return booster.<YieldAdjuster>map(Booster::effect).orElse(YieldAdjuster.NONE);
    }
}
