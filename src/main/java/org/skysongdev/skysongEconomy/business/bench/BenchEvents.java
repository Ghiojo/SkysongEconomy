package org.skysongdev.skysongEconomy.business.bench;

import java.util.Optional;

/** Told about every change to bench links, after it was made. */
public interface BenchEvents {
    /**
     * A bench was tied to a business.
     *
     * @param previous its link to another business, which this replaced; empty when it had none
     */
    default void assigned(BusinessBench link, Optional<BusinessBench> previous) {
    }

    /**
     * A bench no longer belongs to its business.
     *
     * @param by who untied it, only for {@link UnassignCause#UNASSIGNED}
     */
    default void unassigned(BusinessBench link, UnassignCause cause, Optional<String> by) {
    }
}
