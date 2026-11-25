package org.skysongdev.skysongEconomy.business.vault;

import org.skysongdev.skysongEconomy.business.Business;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Tells other plugins when money leaves a vault. On the server these are Bukkit events; every method has a default that does nothing. */
public interface VaultEvents {
    /** Nobody is told. */
    VaultEvents NONE = new VaultEvents() {
    };

    /** The vault was collected: {@code amount} went to {@code payee}. */
    default void collected(Business business, UUID payee, BigDecimal amount) {
    }

    /**
     * A deleted business's vault, holding {@code amount}, was emptied.
     *
     * @param payee the owner's player; empty for a server-owned business, whose vault left the economy
     * @param paid  whether the owner was paid
     */
    default void closed(Business business, Optional<UUID> payee, BigDecimal amount, boolean paid) {
    }
}
