package org.skysongdev.skysongEconomy.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;

/**
 * Someone gathered at a node: the harvest is paid for and rolled, and its items are handed over
 * right after. It can't be cancelled; it is for systems that report or count harvests, such as a
 * Discord bridge.
 */
public final class NodeHarvestEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Harvest harvest;

    public NodeHarvestEvent(Player player, Harvest harvest) {
        this.player = player;
        this.harvest = harvest;
    }

    public Player getPlayer() {
        return player;
    }

    public Node getNode() {
        return harvest.node();
    }

    /** @return the character who gathered */
    public CharacterRef getCharacter() {
        return harvest.character();
    }

    /** @return the tier the player picked */
    public int getTier() {
        return harvest.tier();
    }

    /** @return what the harvest cost */
    public HarvestCost getPaid() {
        return harvest.paid();
    }

    /** @return what the player gets, each material once */
    public List<ItemAmount> getItems() {
        return harvest.items();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
