package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.business.VaultCloseEvent;
import org.skysongdev.skysongEconomy.api.business.VaultCollectEvent;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessDirectory;
import org.skysongdev.skysongEconomy.business.vault.VaultEvents;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Fires {@link VaultCollectEvent} and {@link VaultCloseEvent}. */
public final class BukkitVaultEvents implements VaultEvents {
    private final PluginManager plugins;

    public BukkitVaultEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void collected(Business business, UUID payee, BigDecimal amount) {
        plugins.callEvent(new VaultCollectEvent(BusinessDirectory.info(business), payee, amount));
    }

    @Override
    public void closed(Business business, Optional<UUID> payee, BigDecimal amount, boolean paid) {
        plugins.callEvent(new VaultCloseEvent(BusinessDirectory.info(business), payee.orElse(null), amount, paid));
    }
}
