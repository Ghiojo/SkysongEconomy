package org.skysongdev.skysongEconomy.trade.pier;

import org.skysongdev.skysongEconomy.api.piers.Pier;

import java.util.Optional;

/** Told after a pier was created, changed or removed, and saved. */
@FunctionalInterface
public interface PierChangeListener {
    /**
     * @param before empty when the pier was created
     * @param after  empty when it was removed
     */
    void changed(Optional<Pier> before, Optional<Pier> after);
}
