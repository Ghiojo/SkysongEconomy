package org.skysongdev.skysongEconomy.events;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.piers.Piers;
import org.skysongdev.skysongEconomy.api.piers.Shipment;
import org.skysongdev.skysongEconomy.api.piers.ShipmentArriveEvent;
import org.skysongdev.skysongEconomy.api.piers.ShipmentCollectedEvent;
import org.skysongdev.skysongEconomy.api.piers.ShipmentOrderEvent;
import org.skysongdev.skysongEconomy.api.piers.ShipmentOutcome;
import org.skysongdev.skysongEconomy.trade.delivery.ShipmentEvents;

import java.util.List;

/** Fires the shipment events in {@code api/piers}. */
public final class BukkitShipmentEvents implements ShipmentEvents {
    private final PluginManager plugins;
    private final Piers piers;

    /** @param piers to hand listeners the pier itself */
    public BukkitShipmentEvents(PluginManager plugins, Piers piers) {
        this.plugins = plugins;
        this.piers = piers;
    }

    @Override
    public Verdict ordering(Shipment shipment) {
        ShipmentOrderEvent event = new ShipmentOrderEvent(shipment, shipment.pierId().flatMap(piers::find),
                Bukkit.getPlayer(shipment.orderedBy().player()));
        plugins.callEvent(event);
        return event.isCancelled() ? Verdict.cancel(event.getCancelMessage().orElse(null)) : Verdict.ALLOW;
    }

    @Override
    public Outcome arriving(Arrival arrival) {
        ShipmentArriveEvent event = new ShipmentArriveEvent(arrival.shipment(), arrival.pier(), arrival.collector(),
                Bukkit.getPlayer(arrival.collector().player()), arrival.lossChance(), arrival.roll());
        plugins.callEvent(event);
        return new Outcome(event.isCancelled(), event.getItems(), event.getSeizeMessage());
    }

    @Override
    public void collected(Arrival arrival, Outcome outcome) {
        plugins.callEvent(new ShipmentCollectedEvent(arrival.shipment(), arrival.pier(), arrival.collector(),
                outcome.seized() ? ShipmentOutcome.SEIZED : ShipmentOutcome.DELIVERED,
                outcome.seized() ? List.of() : outcome.items(), arrival.lossChance(), arrival.lostByRoll()));
    }
}
