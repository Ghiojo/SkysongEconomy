package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;

/**
 * A node action was refused, for example creating a node on a business block. It carries the
 * {@code messages.yml} key that explains why, and the values for its placeholders.
 */
public final class NodeException extends RefusedException {
    private static final long serialVersionUID = 1L;

    public NodeException(String messageKey, Map<String, String> placeholders) {
        super(messageKey, placeholders);
    }

    public NodeException(String messageKey) {
        this(messageKey, Map.of());
    }
}
