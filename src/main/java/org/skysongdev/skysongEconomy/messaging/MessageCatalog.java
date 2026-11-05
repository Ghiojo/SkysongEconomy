package org.skysongdev.skysongEconomy.messaging;

import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Every player-facing text of the plugin, by key, from {@code messages.yml}.
 * <p>
 * Texts are MiniMessage templates with {@code <name>} placeholders. The server's file
 * overrides the defaults bundled in the jar key by key, so a file from an older version still
 * gets the new messages. A key that is missing from both shows itself, so it is easy to spot.
 */
public final class MessageCatalog {
    /** File name in the data folder and in the jar. */
    public static final String FILE = "messages.yml";
    /** Key of the text put in front of every chat message. */
    public static final String PREFIX = "prefix";

    private final Map<String, Object> texts;

    private MessageCatalog(Map<String, Object> texts) {
        this.texts = Map.copyOf(texts);
    }

    /**
     * @param defaults  the file bundled in the jar
     * @param overrides the server's own file
     * @param warnings  told about keys in the server's file that the plugin doesn't use
     */
    public static MessageCatalog of(ConfigSection defaults, ConfigSection overrides, Consumer<String> warnings) {
        Map<String, Object> texts = new HashMap<>(defaults.leaves());
        overrides.leaves().forEach((key, value) -> {
            if (!texts.containsKey(key)) {
                warnings.accept(FILE + ": unknown message '" + key + "' is ignored");
                return;
            }
            texts.put(key, value);
        });
        return new MessageCatalog(texts);
    }

    /** @return the template for {@code key}; a list is joined with new lines */
    public String text(String key) {
        Object value = texts.get(key);
        if (value == null) {
            return "<red>Missing message: " + key;
        }
        if (value instanceof List<?> list) {
            return String.join("\n", list.stream().map(String::valueOf).toList());
        }
        return value.toString();
    }

    /** @return the template for {@code key} as lines, e.g. for item lore */
    public List<String> lines(String key) {
        Object value = texts.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(text(key));
    }

    public boolean has(String key) {
        return texts.containsKey(key);
    }
}
