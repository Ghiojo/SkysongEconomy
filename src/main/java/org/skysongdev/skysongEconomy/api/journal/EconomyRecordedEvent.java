package org.skysongdev.skysongEconomy.api.journal;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A record was added to the economy journal. Fired on the main thread, once per record, in sequence
 * order, right before the record is written to the database. A plugin that only keeps books or
 * statistics needs only this listener; the specific events add live context, such as the player.
 */
public final class EconomyRecordedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final EconomyRecord record;

    public EconomyRecordedEvent(@NotNull EconomyRecord record) {
        this.record = Objects.requireNonNull(record, "record");
    }

    /** @return the record */
    public @NotNull EconomyRecord getRecord() {
        return record;
    }

    /** @return what the record is about */
    public @NotNull RecordKind getKind() {
        return record.kind();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
