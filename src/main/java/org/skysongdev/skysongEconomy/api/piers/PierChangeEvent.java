package org.skysongdev.skysongEconomy.api.piers;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * A pier was created, changed or removed: through {@link Piers}, a {@code /se pier} command, or
 * {@code /se pier reload}. Fired on the main thread after the change was saved. It can't be cancelled.
 */
public final class PierChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Optional<Pier> before;
    private final Optional<Pier> after;

    /**
     * @param before the pier before the change, empty when it was created
     * @param after  the pier after the change, empty when it was removed
     */
    public PierChangeEvent(@NotNull Optional<Pier> before, @NotNull Optional<Pier> after) {
        this.before = Objects.requireNonNull(before, "before");
        this.after = Objects.requireNonNull(after, "after");
        if (before.isEmpty() && after.isEmpty()) {
            throw new IllegalArgumentException("A change needs a pier before or after it");
        }
    }

    /** @return the pier before the change; empty when it was just created */
    public @NotNull Optional<Pier> getBefore() {
        return before;
    }

    /** @return the pier now; empty when it was removed */
    public @NotNull Optional<Pier> getAfter() {
        return after;
    }

    /** @return the id of the pier that changed */
    public @NotNull String getPierId() {
        return after.or(() -> before).orElseThrow().id();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
