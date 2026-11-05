package org.skysongdev.skysongEconomy.messaging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.BusinessException;

import java.util.Map;

/**
 * Sends prefixed MiniMessage messages to players and the console.
 * <p>
 * New code sends texts from {@code messages.yml} by key with {@link #send}. The older
 * {@code info}, {@code success} and {@code error} methods take the text itself.
 * <p>
 * Values that come from users (material IDs, good types, ...) must be passed as
 * {@link net.kyori.adventure.text.minimessage.tag.resolver.Placeholder#unparsed unparsed placeholders}
 * so they can never inject MiniMessage tags.
 */
public final class Messenger {
    private static final String PREFIX = "<dark_gray>[<gold>SkysongEconomy<dark_gray>] <reset>";

    private final MiniMessage miniMessage;
    private final MessageCatalog catalog;

    /** @param catalog the texts of {@code messages.yml}, used by {@link #send} and {@link #render} */
    public Messenger(MiniMessage miniMessage, MessageCatalog catalog) {
        this.miniMessage = miniMessage;
        this.catalog = catalog;
    }

    /**
     * Sends the {@code messages.yml} text {@code key}, after the configured prefix.
     *
     * @param placeholders values for the text's {@code <name>} placeholders; use unparsed ones for user input
     */
    public void send(CommandSender recipient, String key, TagResolver... placeholders) {
        recipient.sendMessage(miniMessage.deserialize(catalog.text(MessageCatalog.PREFIX) + catalog.text(key), placeholders));
    }

    /** Sends the message of a refused business action, with its placeholders as unparsed text. */
    public void send(CommandSender recipient, BusinessException refusal) {
        send(recipient, refusal.messageKey(), unparsed(refusal.placeholders()));
    }

    /** @return the {@code messages.yml} text {@code key} as a component, without the prefix */
    public Component render(String key, TagResolver... placeholders) {
        return miniMessage.deserialize(catalog.text(key), placeholders);
    }

    /** @return one unparsed placeholder per entry, so user-typed values can't inject tags */
    public static TagResolver[] unparsed(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
    }

    public void info(CommandSender recipient, String message, TagResolver... placeholders) {
        sendText(recipient, "<gray>" + message, placeholders);
    }

    public void success(CommandSender recipient, String message, TagResolver... placeholders) {
        sendText(recipient, "<green>" + message, placeholders);
    }

    public void error(CommandSender recipient, String message, TagResolver... placeholders) {
        sendText(recipient, "<red>" + message, placeholders);
    }

    private void sendText(CommandSender recipient, String message, TagResolver... placeholders) {
        recipient.sendMessage(miniMessage.deserialize(PREFIX + message, placeholders));
    }
}
