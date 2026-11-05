package org.skysongdev.skysongEconomy.integration.gringotts;

import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.api.Eco;
import org.gestern.gringotts.api.PlayerAccount;
import org.gestern.gringotts.api.TransactionResult;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

/** {@link Wallet} backed by Gringotts. Use it on the main thread, as Gringotts moves items. */
public final class GringottsWallet implements Wallet {
    private Eco eco() {
        return Gringotts.instance.getEco();
    }

    private PlayerAccount account(UUID player) {
        return eco().player(player);
    }

    @Override
    public BigDecimal balance(UUID player) {
        return BigDecimal.valueOf(account(player).balance());
    }

    @Override
    public boolean deposit(UUID player, BigDecimal amount) {
        return account(player).add(amount.doubleValue()) == TransactionResult.SUCCESS;
    }

    @Override
    public boolean withdraw(UUID player, BigDecimal amount) {
        PlayerAccount account = account(player);
        return account.has(amount.doubleValue()) && account.remove(amount.doubleValue()) == TransactionResult.SUCCESS;
    }

    @Override
    public String format(BigDecimal amount) {
        return eco().currency().format(amount.doubleValue());
    }
}
