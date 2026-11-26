package org.skysongdev.skysongEconomy.api.claims;

import net.kyori.adventure.text.Component;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Objects;
import java.util.Optional;

/**
 * A character is trying to buy a Claim SkysongEconomy has on sale. Fired on the main thread when
 * the confirmation is about to open and again on Confirm, before SkysongClaims' own checks and
 * before any money moves. Cancel it to stop the purchase; {@link #setCancelled(Component)} also
 * tells the buyer why, for example "You need to be a Citizen to own a Cottage".
 */
public final class ClaimPurchaseAttemptEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterRef buyer;
    private final String claimId;
    private final String claimTypeId;
    private boolean cancelled;
    private Component reason;

    public ClaimPurchaseAttemptEvent(@NotNull CharacterRef buyer, @NotNull String claimId, @NotNull String claimTypeId) {
        this.buyer = Objects.requireNonNull(buyer, "buyer");
        this.claimId = Objects.requireNonNull(claimId, "claimId");
        this.claimTypeId = Objects.requireNonNull(claimTypeId, "claimTypeId");
    }

    /** @return the character that would own it; {@code getBuyer().player()} is the account that would pay */
    public @NotNull CharacterRef getBuyer() {
        return buyer;
    }

    public @NotNull String getClaimId() {
        return claimId;
    }

    /** @return the Claim's SkysongClaims type */
    public @NotNull String getClaimTypeId() {
        return claimTypeId;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
        if (!cancel) {
            reason = null;
        }
    }

    /** Stops the purchase and tells the buyer why, after SkysongEconomy's prefix. */
    public void setCancelled(@NotNull Component reason) {
        this.cancelled = true;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /** @return what the buyer is told when cancelled; empty means SkysongEconomy's general refusal */
    public @NotNull Optional<Component> getReason() {
        return Optional.ofNullable(reason);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
