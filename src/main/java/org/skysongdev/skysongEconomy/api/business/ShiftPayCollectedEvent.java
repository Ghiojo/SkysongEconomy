package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A worker was paid for finished shifts: money to their player, Gathering Tokens to the character.
 * Fired on the main thread. It fires when they collect their pay, and for each shift of a deleted
 * business, which pays everything it promised. The money is new to the economy.
 */
public final class ShiftPayCollectedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final CharacterRef worker;
    private final List<ShiftInfo> shifts;
    private final BigDecimal money;
    private final long tokens;
    private final boolean businessDeleted;

    public ShiftPayCollectedEvent(@NotNull BusinessInfo business, @NotNull CharacterRef worker,
                                  @NotNull List<ShiftInfo> shifts, @NotNull BigDecimal money, long tokens,
                                  boolean businessDeleted) {
        this.business = Objects.requireNonNull(business, "business");
        this.worker = Objects.requireNonNull(worker, "worker");
        this.shifts = List.copyOf(Objects.requireNonNull(shifts, "shifts"));
        this.money = Objects.requireNonNull(money, "money");
        this.tokens = tokens;
        this.businessDeleted = businessDeleted;
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the character paid */
    public @NotNull CharacterRef getWorker() {
        return worker;
    }

    /** @return the shifts paid for */
    public @NotNull List<ShiftInfo> getShifts() {
        return shifts;
    }

    /** @return the money paid, 0 or more */
    public @NotNull BigDecimal getMoney() {
        return money;
    }

    /** @return the Gathering Tokens given */
    public long getTokens() {
        return tokens;
    }

    /** @return whether the business was deleted, so it paid without the worker collecting */
    public boolean isBusinessDeleted() {
        return businessDeleted;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
