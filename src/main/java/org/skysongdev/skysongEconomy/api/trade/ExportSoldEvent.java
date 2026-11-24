package org.skysongdev.skysongEconomy.api.trade;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;

import java.util.Objects;
import java.util.Optional;

/**
 * A batch of an export was sold: the seller was paid and the items were taken. Fired on the main
 * thread. It can't be cancelled; it is meant for logs, books and statistics.
 */
public final class ExportSoldEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final ExportSale sale;
    private final @Nullable Player player;

    public ExportSoldEvent(@NotNull BusinessInfo business, @NotNull ExportSale sale, @Nullable Player player) {
        this.business = Objects.requireNonNull(business, "business");
        this.sale = Objects.requireNonNull(sale, "sale");
        this.player = player;
    }

    /** @return the business it was sold at */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return what was sold: ledger, export, seller, items, price, tags */
    public @NotNull ExportSale getSale() {
        return sale;
    }

    /** @return the player who sold, while online */
    public @NotNull Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
