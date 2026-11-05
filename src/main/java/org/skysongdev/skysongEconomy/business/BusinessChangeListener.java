package org.skysongdev.skysongEconomy.business;

import org.jetbrains.annotations.Nullable;

/**
 * Told about every change {@link BusinessService} makes, after it is made. Lets features such as
 * the floating name above the block follow businesses without the service knowing about them.
 */
public interface BusinessChangeListener {
    /**
     * A business was created or changed.
     *
     * @param previousPosition where its block was before the change, {@code null} if it had none
     *                         or was just created
     */
    default void changed(Business business, @Nullable BlockPosition previousPosition) {
    }

    /** A business was deleted. */
    default void deleted(Business business) {
    }

    /** Every business was (re)loaded from storage. */
    default void loaded(Iterable<Business> businesses) {
    }
}
