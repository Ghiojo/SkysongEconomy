package org.skysongdev.skysongEconomy.business;

import java.util.Map;

/**
 * A business action was refused, for example hiring into a full business. It carries the
 * {@code messages.yml} key that explains why, and the values for that message's placeholders.
 */
public final class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final transient Map<String, String> placeholders;

    /**
     * @param messageKey   the key in {@code messages.yml}
     * @param placeholders placeholder names, without brackets, and their values
     */
    public BusinessException(String messageKey, Map<String, String> placeholders) {
        super(messageKey + " " + placeholders);
        this.messageKey = messageKey;
        this.placeholders = Map.copyOf(placeholders);
    }

    public BusinessException(String messageKey) {
        this(messageKey, Map.of());
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, String> placeholders() {
        return placeholders;
    }
}
