package org.skysongdev.skysongEconomy.api.nodes;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.api.items.ItemCount;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Objects;

/**
 * Someone gathered at a node: the harvest is paid for and rolled, and its items are handed over
 * right after. Fired on the main thread. It can't be cancelled; it is for systems that report or
 * count harvests.
 */
public final class NodeHarvestEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NodeInfo node;
    private final CharacterRef character;
    private final int tier;
    private final HarvestCostInfo cost;
    private final List<ItemCount> items;
    private final List<String> modifiers;

    public NodeHarvestEvent(@NotNull Player player, @NotNull NodeInfo node, @NotNull CharacterRef character, int tier,
                            @NotNull HarvestCostInfo cost, @NotNull List<ItemCount> items,
                            @NotNull List<String> modifiers) {
        this.player = Objects.requireNonNull(player, "player");
        this.node = Objects.requireNonNull(node, "node");
        this.character = Objects.requireNonNull(character, "character");
        this.tier = tier;
        this.cost = Objects.requireNonNull(cost, "cost");
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
    }

    /** @return the player who gathered */
    public @NotNull Player getPlayer() {
        return player;
    }

    /** @return the node, as it was when this fired */
    public @NotNull NodeInfo getNode() {
        return node;
    }

    /** @return the character who gathered */
    public @NotNull CharacterRef getCharacter() {
        return character;
    }

    /** @return the tier the player picked */
    public int getTier() {
        return tier;
    }

    /** @return what the harvest cost: downtime, Harvest Tokens and money */
    public @NotNull HarvestCostInfo getCost() {
        return cost;
    }

    /** @return what the player gets, each material once */
    public @NotNull List<ItemCount> getItems() {
        return items;
    }

    /** @return the ids of the calendar modifiers (files in nodes/modifiers) that changed it; empty when none did */
    public @NotNull List<String> getModifiers() {
        return modifiers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
