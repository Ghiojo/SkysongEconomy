package org.skysongdev.skysongEconomy.nodes.harvest;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Objects;

/**
 * One finished harvest: paid for and rolled, ready to hand over.
 *
 * @param node      where it happened
 * @param character who gathered
 * @param tier      the tier they picked
 * @param paid      what was charged
 * @param items     what they get, each material once; may be empty after an unlucky roll
 */
public record Harvest(Node node, CharacterRef character, int tier, HarvestCost paid, List<ItemAmount> items) {
    public Harvest {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(paid, "paid");
        items = List.copyOf(items);
    }
}
