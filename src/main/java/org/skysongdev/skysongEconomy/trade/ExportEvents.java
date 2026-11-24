package org.skysongdev.skysongEconomy.trade;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.trade.ExportSale;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.Optional;

/**
 * Tells other plugins about export sales and lets them stop one. The export rules call it; the
 * server's version fires the Bukkit events in {@code api/trade}, and tests use a fake. Every method
 * has a default that changes nothing.
 */
public interface ExportEvents {
    /** What a listener decided about a sale. */
    record Verdict(boolean cancelled, Optional<String> message) {
        /** Go ahead. */
        public static final Verdict ALLOW = new Verdict(false, Optional.empty());

        /** @param message MiniMessage text for the seller, or {@code null} for the default */
        public static Verdict cancel(@Nullable String message) {
            return new Verdict(true, Optional.ofNullable(message));
        }
    }

    /** Fires nothing and changes nothing. */
    ExportEvents NONE = new ExportEvents() {
    };

    /** A batch is about to be sold, before anything is paid or taken. */
    default Verdict selling(Business business, ExportSale sale) {
        return Verdict.ALLOW;
    }

    /** A batch was sold: the seller was paid and the items taken. */
    default void sold(Business business, ExportSale sale) {
    }
}
