package org.skysongdev.skysongEconomy.business.vault;

import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Balances in a map. Set {@link #refuseDeposits} to act like a player with no room for coins. */
final class MapWallet implements Wallet {
    final Map<UUID, BigDecimal> balances = new HashMap<>();
    boolean refuseDeposits;

    @Override
    public BigDecimal balance(UUID player) {
        return balances.getOrDefault(player, BigDecimal.ZERO);
    }

    @Override
    public boolean deposit(UUID player, BigDecimal amount) {
        if (refuseDeposits) {
            return false;
        }
        balances.put(player, balance(player).add(amount));
        return true;
    }

    @Override
    public boolean withdraw(UUID player, BigDecimal amount) {
        if (balance(player).compareTo(amount) < 0) {
            return false;
        }
        balances.put(player, balance(player).subtract(amount));
        return true;
    }

    @Override
    public String format(BigDecimal amount) {
        return amount.toPlainString() + " AU";
    }
}
