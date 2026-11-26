package org.skysongdev.skysongEconomy.api.benches;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * A bench was tied to a business, through {@link BusinessBenches} or {@code /se business bench assign}.
 * Fired on the main thread after it was saved. It can't be cancelled.
 */
public final class BenchAssignEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BenchLink link;
    private final Optional<BenchLink> previous;

    /** @param previous its link to another business, which this replaced; empty when it had none */
    public BenchAssignEvent(@NotNull BenchLink link, @NotNull Optional<BenchLink> previous) {
        this.link = Objects.requireNonNull(link, "link");
        this.previous = Objects.requireNonNull(previous, "previous");
    }

    /** @return the new link */
    public @NotNull BenchLink getLink() {
        return link;
    }

    /** @return the link to another business the bench moved from; empty when it belonged to none */
    public @NotNull Optional<BenchLink> getPrevious() {
        return previous;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
