package org.skysongdev.skysongEconomy.money;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Players' money. Money belongs to the player account, not to a character: every character of
 * a player shares one balance.
 * <p>
 * On the server this is Gringotts (Architite coins); tests use a map.
 */
public interface Wallet {
    /** @return what the player has */
    BigDecimal balance(UUID player);

    /**
     * Gives the player money.
     *
     * @param amount more than 0
     * @return whether it was paid; it can fail, for example when there is no room for the coins
     */
    boolean deposit(UUID player, BigDecimal amount);

    /**
     * Takes money from the player.
     *
     * @param amount more than 0
     * @return whether it was taken; nothing is taken when they don't have enough
     */
    boolean withdraw(UUID player, BigDecimal amount);

    /** @return the amount as players should read it, e.g. {@code 12.5 Pouches} */
    String format(BigDecimal amount);
}
