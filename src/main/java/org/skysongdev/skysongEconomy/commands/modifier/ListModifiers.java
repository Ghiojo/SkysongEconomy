package org.skysongdev.skysongEconomy.commands.modifier;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se modifier list [target]: every modifier that still applies, or only those on one target. */
final class ListModifiers implements SubCommand {
    private final PriceModifiers modifiers;
    private final ModifierText text;
    private final Messenger messenger;

    ListModifiers(PriceModifiers modifiers, ModifierText text, Messenger messenger) {
        this.modifiers = modifiers;
        this.text = text;
        this.messenger = messenger;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String usage() {
        return "[target]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<PriceModifier> shown;
        try {
            shown = args.length == 0 ? modifiers.all() : modifiers.on(ModifierTarget.parse(args[0]));
        } catch (PriceException refusal) {
            messenger.send(sender, refusal);
            return;
        }
        if (shown.isEmpty()) {
            messenger.send(sender, "prices.modifiers.list-empty");
            return;
        }
        messenger.send(sender, "prices.modifiers.list-header", unparsed("count", String.valueOf(shown.size())));
        shown.forEach(modifier -> messenger.send(sender, "prices.modifiers.list-entry", text.placeholders(modifier)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? TargetCompletions.complete(args[0]) : List.of();
    }
}
