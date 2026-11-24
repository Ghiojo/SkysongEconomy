package org.skysongdev.skysongEconomy.business;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.business.BusinessChange;

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

    /**
     * What a creation, deletion or change was, told right after {@link #changed} or {@link #deleted}.
     * A tier change that cost workers or skill picks is told once for the tier, then once for each
     * worker and pick it cost.
     */
    default void described(BusinessChange change) {
    }

    /** Every business was (re)loaded from storage. */
    default void loaded(Iterable<Business> businesses) {
    }
}
