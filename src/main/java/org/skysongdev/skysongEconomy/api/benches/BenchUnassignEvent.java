package org.skysongdev.skysongEconomy.api.benches;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * A bench no longer belongs to its business. Fired on the main thread after it was saved. It can't
 * be cancelled. A bench moving to another business fires a {@link BenchAssignEvent} instead.
 */
public final class BenchUnassignEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BenchLink link;
    private final BenchUnassignCause cause;
    private final Optional<String> by;

    /** @param by who untied it, only when {@code cause} is {@link BenchUnassignCause#UNASSIGNED} */
    public BenchUnassignEvent(@NotNull BenchLink link, @NotNull BenchUnassignCause cause, @NotNull Optional<String> by) {
        this.link = Objects.requireNonNull(link, "link");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.by = Objects.requireNonNull(by, "by");
    }

    /** @return the link as it was before it was removed */
    public @NotNull BenchLink getLink() {
        return link;
    }

    /** @return why it was removed */
    public @NotNull BenchUnassignCause getCause() {
        return cause;
    }

    /** @return who untied it: a player's or plugin's name; empty unless it was {@link BenchUnassignCause#UNASSIGNED} */
    public @NotNull Optional<String> getBy() {
        return by;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
