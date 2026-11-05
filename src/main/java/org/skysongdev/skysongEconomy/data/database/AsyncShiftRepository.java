package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.shift.Shift;
import org.skysongdev.skysongEconomy.business.shift.ShiftRepository;

import java.util.List;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}; share the businesses' writer
 * so a business row is always saved before its shifts. Loading stays on the calling thread.
 */
public final class AsyncShiftRepository implements ShiftRepository {
    private final ShiftRepository delegate;
    private final BackgroundWriter writer;

    public AsyncShiftRepository(ShiftRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<Shift> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(Shift shift) {
        writer.run("save shift " + shift.id(), () -> delegate.save(shift));
    }

    @Override
    public void delete(UUID shiftId) {
        writer.run("delete shift " + shiftId, () -> delegate.delete(shiftId));
    }
}
