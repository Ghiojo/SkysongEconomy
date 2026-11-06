package org.skysongdev.skysongEconomy.blocks;

import java.util.Map;
import java.util.Objects;

/**
 * A block that a feature holds, such as a business block or a node block, and what happens when
 * someone tries to break it.
 *
 * @param name             what holds it, shown when another feature can't use the block
 * @param breakPermission  lets staff break it anyway
 * @param protectedMessage {@code messages.yml} key told to a player who may not break it
 * @param unplacedMessage  {@code messages.yml} key told to staff who broke it
 * @param placeholders     the values for both messages' placeholders, shown as typed
 * @param formatted        more placeholders whose values are MiniMessage staff wrote, such as a
 *                         node table's name, so their colours show
 * @param unplace          takes the feature off the block when staff break it, keeping its data
 */
public record GuardedBlock(String name, String breakPermission, String protectedMessage, String unplacedMessage,
                           Map<String, String> placeholders, Map<String, String> formatted, Runnable unplace) {
    public GuardedBlock {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(breakPermission, "breakPermission");
        Objects.requireNonNull(protectedMessage, "protectedMessage");
        Objects.requireNonNull(unplacedMessage, "unplacedMessage");
        placeholders = Map.copyOf(placeholders);
        formatted = Map.copyOf(formatted);
        Objects.requireNonNull(unplace, "unplace");
    }
}
