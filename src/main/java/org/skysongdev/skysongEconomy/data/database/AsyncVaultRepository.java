package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.vault.VaultRepository;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}; share the businesses' writer
 * so a business row is always saved before its vault. Loading stays on the calling thread.
 */
public final class AsyncVaultRepository implements VaultRepository {
    private final VaultRepository delegate;
    private final BackgroundWriter writer;

    public AsyncVaultRepository(VaultRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public Map<String, BigDecimal> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(String businessId, BigDecimal balance) {
        writer.run("save the vault of business " + businessId, () -> delegate.save(businessId, balance));
    }
}
