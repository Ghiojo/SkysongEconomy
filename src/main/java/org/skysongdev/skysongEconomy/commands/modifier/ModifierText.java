package org.skysongdev.skysongEconomy.commands.modifier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** Sends one modifier as a line of a message. */
final class ModifierText {
    /** Expiry times, always in UTC so every staff member reads the same time. */
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final Messenger messenger;

    ModifierText(Messenger messenger) {
        this.messenger = messenger;
    }

    /**
     * @return the placeholders {@code id}, {@code type}, {@code target}, {@code percent},
     *         {@code applies-to}, {@code name} (MiniMessage, set by staff), {@code by} and
     *         {@code expires} (the time, or the {@code prices.modifiers.never} text)
     */
    TagResolver placeholders(PriceModifier modifier) {
        String expires = modifier.expiry().map(TIME::format).orElseGet(() -> messenger.plainText("prices.modifiers.never"));
        return TagResolver.resolver(
                unparsed("id", modifier.id()),
                unparsed("type", modifier.type()),
                unparsed("target", modifier.target().key()),
                unparsed("percent", PriceModifiers.signed(modifier.percent())),
                unparsed("applies-to", modifier.appliesTo().key()),
                Placeholder.parsed("name", modifier.displayName()),
                unparsed("by", modifier.createdBy()),
                unparsed("expires", expires));
    }
}
