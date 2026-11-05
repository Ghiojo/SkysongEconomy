package org.skysongdev.skysongEconomy.messaging;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/**
 * Sends prefixed MiniMessage messages to players and the console.
 * <p>
 * Values that come from users (material IDs, good types, ...) must be passed as
 * {@link net.kyori.adventure.text.minimessage.tag.resolver.Placeholder#unparsed unparsed placeholders}
 * so they can never inject MiniMessage tags.
 */
public final class Messenger {
    private static final String PREFIX = "<dark_gray>[<gold>SkysongEconomy<dark_gray>] <reset>";

    private final MiniMessage miniMessage;

    public Messenger(MiniMessage miniMessage) {
        this.miniMessage = miniMessage;
    }

    public void info(CommandSender recipient, String message, TagResolver... placeholders) {
        send(recipient, "<gray>" + message, placeholders);
    }

    public void success(CommandSender recipient, String message, TagResolver... placeholders) {
        send(recipient, "<green>" + message, placeholders);
    }

    public void error(CommandSender recipient, String message, TagResolver... placeholders) {
        send(recipient, "<red>" + message, placeholders);
    }

    private void send(CommandSender recipient, String message, TagResolver... placeholders) {
        recipient.sendMessage(miniMessage.deserialize(PREFIX + message, placeholders));
    }
}
