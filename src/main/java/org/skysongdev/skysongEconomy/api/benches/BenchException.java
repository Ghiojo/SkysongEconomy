package org.skysongdev.skysongEconomy.api.benches;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;

/**
 * A change to a business's crafting benches was refused, for example tying a block that isn't a
 * SkysongCrafting workstation. It is a {@link RuntimeException}; {@link #messageKey()} names the
 * {@code business.bench.*} message in {@code messages.yml} and {@link #placeholders()} its values.
 */
public final class BenchException extends RefusedException {
    private static final long serialVersionUID = 1L;

    public BenchException(String messageKey, Map<String, String> placeholders) {
        super(messageKey, placeholders);
    }
}
