package org.skysongdev.skysongEconomy.messaging;

import java.util.Map;

/**
 * An action was refused because of what the player asked for, for example hiring into a full
 * business or a price with a typo. It carries the {@code messages.yml} key that explains why
 * and the values for that message's placeholders; {@link Messenger#send(org.bukkit.command.CommandSender,
 * RefusedException)} shows it.
 */
public class RefusedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final transient Map<String, String> placeholders;

    /**
     * @param messageKey   the key in {@code messages.yml}
     * @param placeholders placeholder names, without brackets, and their values
     */
    public RefusedException(String messageKey, Map<String, String> placeholders) {
        super(messageKey + " " + placeholders);
        this.messageKey = messageKey;
        this.placeholders = Map.copyOf(placeholders);
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, String> placeholders() {
        return placeholders;
    }
}
