package org.skysongdev.skysongEconomy.character;

import java.util.UUID;

/**
 * Spends a player's downtime, the resource that limits how often they can work or train.
 * <p>
 * The pool it is taken from (global or the active character's) follows the player's own
 * {@code /downtime preference}.
 */
public interface DowntimeWallet {
    /**
     * Takes {@code amount} downtime from the player's preferred pool, but only if the pool
     * holds at least that much. Nothing is taken otherwise.
     *
     * @param amount the downtime to spend, 0 or more
     * @return whether the downtime was spent
     * @throws IllegalArgumentException when the amount is negative
     */
    boolean trySpend(UUID player, int amount);

    /** @return the downtime in the player's preferred pool, 0 when they have none */
    int available(UUID player);
}
