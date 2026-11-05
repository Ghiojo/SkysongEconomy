package org.skysongdev.skysongEconomy.prices;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;

/**
 * A price couldn't be read or worked out, for example a typo in an expression or an item
 * without a base value. It names the {@code prices.*} message that explains why.
 */
public final class PriceException extends RefusedException {
    private static final long serialVersionUID = 1L;

    public PriceException(String messageKey, Map<String, String> placeholders) {
        super(messageKey, placeholders);
    }
}
