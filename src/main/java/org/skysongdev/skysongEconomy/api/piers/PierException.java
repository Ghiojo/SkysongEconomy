package org.skysongdev.skysongEconomy.api.piers;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;

/**
 * A change to piers was refused, for example creating a pier whose id is taken. It is a
 * {@link RuntimeException}; {@link #messageKey()} names the {@code piers.*} message in
 * {@code messages.yml} and {@link #placeholders()} its values, and {@link #getMessage()} holds both
 * for logs.
 */
public final class PierException extends RefusedException {
    private static final long serialVersionUID = 1L;

    public PierException(String messageKey, Map<String, String> placeholders) {
        super(messageKey, placeholders);
    }

    /** @return a refusal whose message only names the pier, as {@code <pier>} */
    public static PierException about(String messageKey, String pierId) {
        return new PierException(messageKey, Map.of("pier", pierId));
    }
}
