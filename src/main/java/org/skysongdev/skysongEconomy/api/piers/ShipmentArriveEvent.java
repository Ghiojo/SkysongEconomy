package org.skysongdev.skysongEconomy.api.piers;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A shipment reached its pier: someone is collecting a delivery that has arrived. Fired on the
 * main thread right after SkysongEconomy rolled the pier's loss chance, before anything is handed over.
 * <p>
 * Cancelled means <b>seized</b>: the shipment is lost, nothing is handed over and nothing refunded.
 * The event starts cancelled when the roll lost it ({@link #isLostByRoll()}); a listener can seize
 * a shipment the roll let through, or save one it lost, with {@link #setCancelled}. It can also
 * change what is handed over with {@link #setItems}. Later priorities see earlier changes.
 */
public final class ShipmentArriveEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Shipment shipment;
    private final Optional<Pier> pier;
    private final CharacterRef collector;
    private final @Nullable Player player;
    private final BigDecimal lossChance;
    private final BigDecimal roll;
    private List<ShipmentItem> items;
    private boolean cancelled;
    private @Nullable String seizeMessage;

    /**
     * @param pier       its pier; empty when it has none or the pier was removed
     * @param collector  the character collecting
     * @param player     the player collecting, when online
     * @param lossChance the pier's final loss chance for this shipment, 0 to 100
     * @param roll       the number rolled, 0 to 100; the shipment is lost by the roll when it is below the chance
     */
    public ShipmentArriveEvent(@NotNull Shipment shipment, @NotNull Optional<Pier> pier, @NotNull CharacterRef collector,
                               @Nullable Player player, @NotNull BigDecimal lossChance, @NotNull BigDecimal roll) {
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.pier = Objects.requireNonNull(pier, "pier");
        this.collector = Objects.requireNonNull(collector, "collector");
        this.player = player;
        this.lossChance = Objects.requireNonNull(lossChance, "lossChance");
        this.roll = Objects.requireNonNull(roll, "roll");
        this.items = shipment.items();
        this.cancelled = isLostByRoll();
    }

    /** @return the shipment as it was ordered: pier, items, orderer, ledger, import, tags */
    public @NotNull Shipment getShipment() {
        return shipment;
    }

    /** @return its pier; empty when it has none or the pier was removed */
    public @NotNull Optional<Pier> getPier() {
        return pier;
    }

    /** @return the character collecting it */
    public @NotNull CharacterRef getCollector() {
        return collector;
    }

    /** @return the player collecting it, when online */
    public @NotNull Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    /** @return the pier's final loss chance for this shipment, in percent, 0 to 100 */
    public @NotNull BigDecimal getLossChance() {
        return lossChance;
    }

    /** @return the number rolled, 0 (included) to 100 (excluded) */
    public @NotNull BigDecimal getRoll() {
        return roll;
    }

    /** @return whether the roll lost the shipment, whatever listeners decided since */
    public boolean isLostByRoll() {
        return roll.compareTo(lossChance) < 0;
    }

    /** @return what will be handed over unless it is seized */
    public @NotNull List<ShipmentItem> getItems() {
        return items;
    }

    /** @param items what to hand over instead; may be empty, so the shipment lands with nothing */
    public void setItems(@NotNull List<ShipmentItem> items) {
        this.items = List.copyOf(items);
    }

    /** @return whether it is seized */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /** @param cancel {@code true} seizes it, {@code false} lets it through */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /** @return what the collector is told when it is seized; empty for messages.yml's {@code piers.seized} */
    public @NotNull Optional<String> getSeizeMessage() {
        return Optional.ofNullable(seizeMessage);
    }

    /** @param message MiniMessage text such as "Seized by the harbour guard"; {@code null} for the default */
    public void setSeizeMessage(@Nullable String message) {
        this.seizeMessage = message;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
