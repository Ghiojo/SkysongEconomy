package org.skysongdev.skysongEconomy.commands.modifier;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se modifier remove &lt;id&gt;: deletes a modifier. */
final class RemoveModifier implements SubCommand {
    private final PriceModifiers modifiers;
    private final ModifierText text;
    private final Messenger messenger;

    RemoveModifier(PriceModifiers modifiers, ModifierText text, Messenger messenger) {
        this.modifiers = modifiers;
        this.text = text;
        this.messenger = messenger;
    }

    @Override
    public String name() {
        return "remove";
    }

    @Override
    public String permission() {
        return ModifierCommand.EDIT_PERMISSION;
    }

    @Override
    public String usage() {
        return "<id>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            messenger.send(sender, "command.usage", unparsed("usage", "/se modifier remove " + usage()));
            return;
        }
        try {
            PriceModifier removed = modifiers.remove(args[0]);
            messenger.send(sender, "prices.modifiers.removed", text.placeholders(removed));
        } catch (PriceException refusal) {
            messenger.send(sender, refusal);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        return modifiers.all().stream().map(PriceModifier::id).filter(id -> id.startsWith(typed)).toList();
    }
}
