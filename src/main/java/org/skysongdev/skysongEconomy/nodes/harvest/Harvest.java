package org.skysongdev.skysongEconomy.nodes.harvest;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.boost.Booster;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One finished harvest: paid for and rolled, ready to hand over.
 *
 * @param node      where it happened
 * @param character who gathered
 * @param tier      the tier they picked
 * @param paid      what was charged
 * @param items     what they get, each material once; may be empty after an unlucky roll
 * @param modifiers the ids of the calendar modifiers that changed it; empty when none did
 * @param booster   the booster used up on it; empty when none was
 * @param resource  the farm resource harvested, for a farm node; empty for a random node
 */
public record Harvest(Node node, CharacterRef character, int tier, HarvestCost paid, List<ItemAmount> items,
                      List<String> modifiers, Optional<Booster> booster, Optional<String> resource) {
    public Harvest {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(paid, "paid");
        items = List.copyOf(items);
        modifiers = List.copyOf(modifiers);
        Objects.requireNonNull(booster, "booster");
        Objects.requireNonNull(resource, "resource");
    }

    /** A harvest without a booster, at a random node. */
    public Harvest(Node node, CharacterRef character, int tier, HarvestCost paid, List<ItemAmount> items,
                   List<String> modifiers) {
        this(node, character, tier, paid, items, modifiers, Optional.empty(), Optional.empty());
    }
}
