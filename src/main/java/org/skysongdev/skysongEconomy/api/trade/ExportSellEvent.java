package org.skysongdev.skysongEconomy.api.trade;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;

import java.util.Objects;
import java.util.Optional;

/**
 * A batch of an export is about to be sold: every check passed, nothing has been paid and no item
 * has been taken yet. Fired on the main thread. Cancelling it stops the sale; the seller is told
 * {@link #getCancelMessage()}, or messages.yml's {@code business.exports.sale-cancelled}.
 */
public final class ExportSellEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final ExportSale sale;
    private final Player player;
    private boolean cancelled;
    private @Nullable String cancelMessage;

    public ExportSellEvent(@NotNull BusinessInfo business, @NotNull ExportSale sale, @NotNull Player player) {
        this.business = Objects.requireNonNull(business, "business");
        this.sale = Objects.requireNonNull(sale, "sale");
        this.player = Objects.requireNonNull(player, "player");
    }

    /** @return the business it is sold at */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return what would be sold: ledger, export, seller, items, price, tags */
    public @NotNull ExportSale getSale() {
        return sale;
    }

    /** @return the player selling */
    public @NotNull Player getPlayer() {
        return player;
    }

    /** @return what the seller is told when it is cancelled; empty for the default */
    public @NotNull Optional<String> getCancelMessage() {
        return Optional.ofNullable(cancelMessage);
    }

    /** @param message MiniMessage text for the seller when it is cancelled; {@code null} for the default */
    public void setCancelMessage(@Nullable String message) {
        this.cancelMessage = message;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
