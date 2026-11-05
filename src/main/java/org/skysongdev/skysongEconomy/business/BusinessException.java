package org.skysongdev.skysongEconomy.business;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;

/**
 * A business action was refused, for example hiring into a full business. It carries the
 * {@code messages.yml} key that explains why, and the values for that message's placeholders.
 */
public final class BusinessException extends RefusedException {
    private static final long serialVersionUID = 1L;

    /**
     * @param messageKey   the key in {@code messages.yml}
     * @param placeholders placeholder names, without brackets, and their values
     */
    public BusinessException(String messageKey, Map<String, String> placeholders) {
        super(messageKey, placeholders);
    }

    public BusinessException(String messageKey) {
        this(messageKey, Map.of());
    }
}
