package org.skysongdev.skysongEconomy.business.training;

import org.skysongdev.skysongEconomy.business.Business;

import java.util.Optional;

/** Tells other plugins about training sessions. On the server these are Bukkit events; every method has a default that does nothing. */
public interface TrainingEvents {
    /** Nobody is told. */
    TrainingEvents NONE = new TrainingEvents() {
    };

    /** A session was paid for and started. */
    default void started(Business business, TrainingSession session) {
    }

    /**
     * A session finished and its rewards were given.
     *
     * @param business the business; empty when it no longer exists, so nothing was given
     */
    default void finished(Optional<Business> business, TrainingResult result) {
    }
}
