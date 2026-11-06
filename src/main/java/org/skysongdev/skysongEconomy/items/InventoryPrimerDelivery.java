package org.skysongdev.skysongEconomy.items;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.nodes.farm.PrimerDelivery;

import java.util.UUID;

/**
 * Gives primers back into an online player's inventory, dropping what doesn't fit at their feet,
 * and tells them. An offline player gets nothing yet, so the primers wait for them.
 */
public final class InventoryPrimerDelivery implements PrimerDelivery {
    private final Server server;
    private final ItemHandOver handOver;
    private final Messenger messenger;

    public InventoryPrimerDelivery(Server server, ItemHandOver handOver, Messenger messenger) {
        this.server = server;
        this.handOver = handOver;
        this.messenger = messenger;
    }

    @Override
    public boolean tryGive(UUID playerId, StoredItems items) {
        Player player = server.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return false;
        }
        boolean dropped = handOver.giveStacks(player, ItemStackBytes.restore(items));
        messenger.send(player, dropped ? "node.farm.returned-dropped" : "node.farm.returned");
        return true;
    }
}
