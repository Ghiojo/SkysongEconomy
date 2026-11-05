package org.skysongdev.skysongEconomy.business.vault;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The money each business has earned for its owner: a cut of every worker's shift pay and the
 * fees paid for training. It stays in the vault until the owner collects it into their own
 * balance.
 * <p>
 * When a business is deleted, its vault is paid to the owner first. Not thread-safe: use it on
 * the main thread.
 */
public final class BusinessVaults implements BusinessChangeListener {
    private final VaultRepository repository;
    private final Wallet wallet;
    private final Consumer<String> warnings;
    private final Map<String, BigDecimal> balances = new HashMap<>();

    /** @param warnings where to report money that could not be paid out */
    public BusinessVaults(VaultRepository repository, Wallet wallet, Consumer<String> warnings) {
        this.repository = repository;
        this.wallet = wallet;
        this.warnings = warnings;
    }

    /** Loads every saved balance. */
    public void loadAll() {
        balances.clear();
        balances.putAll(repository.loadAll());
    }

    /** @return what the business's vault holds */
    public BigDecimal balance(Business business) {
        return balances.getOrDefault(business.id(), BigDecimal.ZERO);
    }

    /**
     * Puts money in the vault. Callers take the money from somewhere first, such as a trainee's
     * balance or a worker's pay.
     *
     * @param amount 0 or more; 0 changes nothing
     */
    public void deposit(Business business, BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Can't deposit a negative amount: " + amount);
        }
        if (amount.signum() == 0) {
            return;
        }
        setBalance(business, balance(business).add(amount));
    }

    /**
     * Pays everything in the vault to the owner's player.
     *
     * @return the amount paid
     * @throws BusinessException {@code business.vault.empty} when there is nothing to collect, or
     *                           {@code business.vault.collect-failed} when the money could not
     *                           be paid (the vault keeps it)
     */
    public BigDecimal collect(Business business) {
        BigDecimal amount = balance(business);
        if (amount.signum() == 0) {
            throw new BusinessException("business.vault.empty");
        }
        if (!wallet.deposit(business.owner().player(), amount)) {
            throw new BusinessException("business.vault.collect-failed", Map.of("amount", wallet.format(amount)));
        }
        setBalance(business, BigDecimal.ZERO);
        return amount;
    }

    /** @return the amount as players should read it */
    public String format(BigDecimal amount) {
        return wallet.format(amount);
    }

    /** Pays a deleted business's vault to its owner; the database removes the row itself. */
    @Override
    public void deleted(Business business) {
        BigDecimal amount = balances.remove(business.id());
        if (amount == null || amount.signum() == 0) {
            return;
        }
        if (!wallet.deposit(business.owner().player(), amount)) {
            warnings.accept("Could not pay the vault of deleted business " + business.id() + " (" + amount.toPlainString()
                    + ") to its owner " + business.owner() + "; pay them by hand");
        }
    }

    private void setBalance(Business business, BigDecimal balance) {
        balances.put(business.id(), balance);
        repository.save(business.id(), balance);
    }
}
