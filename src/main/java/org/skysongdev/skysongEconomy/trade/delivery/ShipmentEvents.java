package org.skysongdev.skysongEconomy.trade.delivery;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.Shipment;
import org.skysongdev.skysongEconomy.api.piers.ShipmentItem;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tells other plugins about shipments and lets them step in. The delivery rules call it; the
 * server's version fires the Bukkit events in {@code api/piers}, and tests use a fake. Every
 * method has a default that changes nothing, so a version only overrides what it needs.
 */
public interface ShipmentEvents {
    /** What a listener decided about an order. */
    record Verdict(boolean cancelled, Optional<String> message) {
        /** Go ahead. */
        public static final Verdict ALLOW = new Verdict(false, Optional.empty());

        /** @param message MiniMessage text for the player, or {@code null} for the default */
        public static Verdict cancel(@Nullable String message) {
            return new Verdict(true, Optional.ofNullable(message));
        }
    }

    /**
     * A delivery that arrived is being collected; its pier's loss chance was just rolled.
     *
     * @param pier       its pier; empty when it has none or the pier was removed
     * @param collector  the character collecting
     * @param lossChance the pier's final loss chance for this shipment, 0 to 100
     * @param roll       the number rolled, 0 (included) to 100 (excluded); lost when below the chance
     * @param items      what it brings
     */
    record Arrival(Shipment shipment, Optional<Pier> pier, CharacterRef collector, BigDecimal lossChance,
                   BigDecimal roll, List<ShipmentItem> items) {
        public Arrival {
            Objects.requireNonNull(shipment, "shipment");
            Objects.requireNonNull(pier, "pier");
            Objects.requireNonNull(collector, "collector");
            Objects.requireNonNull(lossChance, "lossChance");
            Objects.requireNonNull(roll, "roll");
            items = List.copyOf(items);
        }

        /** @return whether the roll lost the shipment */
        public boolean lostByRoll() {
            return roll.compareTo(lossChance) < 0;
        }

        /** @return the outcome when nobody steps in: lost by the roll, or every item handed over */
        public Outcome asRolled() {
            return new Outcome(lostByRoll(), items, Optional.empty());
        }
    }

    /**
     * What happens to a collected shipment.
     *
     * @param seized       whether it is lost: nothing is handed over
     * @param items        what is handed over when it isn't seized
     * @param seizeMessage MiniMessage text for the collector when seized; empty for the default
     */
    record Outcome(boolean seized, List<ShipmentItem> items, Optional<String> seizeMessage) {
        public Outcome {
            items = List.copyOf(items);
            Objects.requireNonNull(seizeMessage, "seizeMessage");
        }
    }

    /** Fires nothing and changes nothing. */
    ShipmentEvents NONE = new ShipmentEvents() {
    };

    /** An import is about to be ordered, before anything is paid. */
    default Verdict ordering(Shipment shipment) {
        return Verdict.ALLOW;
    }

    /** @return what listeners made of an arriving shipment; by default, what the roll decided */
    default Outcome arriving(Arrival arrival) {
        return arrival.asRolled();
    }

    /** A shipment was collected or seized, and its slot is free. */
    default void collected(Arrival arrival, Outcome outcome) {
    }
}
