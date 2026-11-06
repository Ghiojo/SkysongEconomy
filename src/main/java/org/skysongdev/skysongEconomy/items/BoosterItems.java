package org.skysongdev.skysongEconomy.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.listeners.NodeItemTags;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.boost.Booster;
import org.skysongdev.skysongEconomy.nodes.boost.BoosterEffect;
import org.skysongdev.skysongitems.api.ItemCharges;
import org.skysongdev.skysongitems.api.ItemTags;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

/**
 * Reads booster items from their {@code node_booster_type} and {@code node_booster_effect} tags,
 * and uses them up: one charge per harvest when the item has charges, or else the item itself.
 */
public final class BoosterItems {
    private final ItemTags tags;
    private final ItemCharges charges;
    private final MaterialIdResolver materialIds;
    private final Logger logger;

    /**
     * @param tags    the SkysongItems tag API, from {@code SkysongItemsApi.get().tags()}
     * @param charges the SkysongItems charges API, from {@code SkysongItemsApi.get().charges()}
     */
    public BoosterItems(ItemTags tags, ItemCharges charges, MaterialIdResolver materialIds, Logger logger) {
        this.tags = tags;
        this.charges = charges;
        this.materialIds = materialIds;
        this.logger = logger;
    }

    /** @return whether the item has a {@code node_booster_type} tag, so it is meant as a booster */
    public boolean isBooster(ItemStack item) {
        return type(item).isPresent();
    }

    /**
     * @return the booster the item is; empty when it has no {@code node_booster_type} tag
     * @throws NodeException {@code node.booster.broken} when its effect is missing or can't be
     *                       read, with a console warning naming the item and the problem; or
     *                       {@code node.booster.no-charges} when it has charges and none are left
     */
    public Optional<Booster> read(ItemStack item) {
        Optional<String> type = type(item);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        String effectText = tags.getString(item, NodeItemTags.BOOSTER_EFFECT).orElse("");
        BoosterEffect effect;
        try {
            effect = BoosterEffect.parse(effectText);
        } catch (IllegalArgumentException broken) {
            logger.warning("The booster " + materialIds.fromItem(item).orElse(item.getType().name()) + " has a "
                    + NodeItemTags.BOOSTER_EFFECT + " of '" + effectText + "' that can't be read: " + broken.getMessage());
            throw new NodeException("node.booster.broken");
        }
        if (charges.get(item).isPresent() && !charges.hasLeft(item)) {
            throw new NodeException("node.booster.no-charges");
        }
        return Optional.of(new Booster(type.get(), effect));
    }

    private Optional<String> type(ItemStack item) {
        return tags.getString(item, NodeItemTags.BOOSTER_TYPE).map(String::strip).filter(type -> !type.isEmpty());
    }

    /**
     * Uses the booster for one harvest: takes a charge off when it has charges, or else one item
     * off the stack.
     *
     * @param booster the held booster stack; it is changed in place
     * @return what is left of it; empty when it is used up
     */
    public Optional<ItemStack> useOnce(Player holder, ItemStack booster) {
        OptionalInt left = charges.get(booster);
        if (left.isPresent()) {
            return charges.add(holder, booster, -1) > 0 ? Optional.of(booster) : Optional.empty();
        }
        return booster.getAmount() > 1 ? Optional.of(booster.asQuantity(booster.getAmount() - 1)) : Optional.empty();
    }
}
