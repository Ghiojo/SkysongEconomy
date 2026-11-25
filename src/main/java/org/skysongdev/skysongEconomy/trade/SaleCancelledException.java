package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;
import java.util.Optional;

/**
 * Another plugin cancelled an export sale ({@code ExportSellEvent}). Nothing was paid or taken.
 * Its {@link #reason()} is MiniMessage that plugin wrote; without one, show
 * {@code business.exports.sale-cancelled}.
 */
public final class SaleCancelledException extends RefusedException {
    private static final long serialVersionUID = 1L;

    private final transient Optional<String> reason;

    public SaleCancelledException(Optional<String> reason) {
        super("business.exports.sale-cancelled", Map.of());
        this.reason = reason;
    }

    /** @return the cancelling plugin's MiniMessage text for the player, if it gave one */
    public Optional<String> reason() {
        return reason;
    }
}
