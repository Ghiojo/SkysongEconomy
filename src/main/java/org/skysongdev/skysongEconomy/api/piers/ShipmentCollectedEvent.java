package org.skysongdev.skysongEconomy.api.piers;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A shipment was collected or seized at its pier, and its delivery slot is free. Fired on the main
 * thread after {@link ShipmentArriveEvent}, with the final result. It can't be cancelled; it is
 * meant for logs and bookkeeping, such as counting what guards caught.
 */
public final class ShipmentCollectedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Shipment shipment;
    private final Optional<Pier> pier;
    private final CharacterRef collector;
    private final ShipmentOutcome outcome;
    private final List<ShipmentItem> items;
    private final BigDecimal lossChance;
    private final boolean lostByRoll;

    /**
     * @param items      what was handed over; empty when seized
     * @param lossChance the pier's final loss chance for this shipment
     * @param lostByRoll whether the roll lost it, whatever listeners decided
     */
    public ShipmentCollectedEvent(@NotNull Shipment shipment, @NotNull Optional<Pier> pier, @NotNull CharacterRef collector,
                                  @NotNull ShipmentOutcome outcome, @NotNull List<ShipmentItem> items,
                                  @NotNull BigDecimal lossChance, boolean lostByRoll) {
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.pier = Objects.requireNonNull(pier, "pier");
        this.collector = Objects.requireNonNull(collector, "collector");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.items = List.copyOf(items);
        this.lossChance = Objects.requireNonNull(lossChance, "lossChance");
        this.lostByRoll = lostByRoll;
    }

    public @NotNull Shipment getShipment() {
        return shipment;
    }

    /** @return its pier; empty when it has none or the pier was removed */
    public @NotNull Optional<Pier> getPier() {
        return pier;
    }

    /** @return the character who collected it */
    public @NotNull CharacterRef getCollector() {
        return collector;
    }

    public @NotNull ShipmentOutcome getOutcome() {
        return outcome;
    }

    /** @return what was handed over; empty when seized */
    public @NotNull List<ShipmentItem> getItems() {
        return items;
    }

    /** @return the pier's final loss chance for this shipment, 0 to 100 */
    public @NotNull BigDecimal getLossChance() {
        return lossChance;
    }

    /** @return whether the roll lost it, whatever listeners decided */
    public boolean isLostByRoll() {
        return lostByRoll;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
