package org.skysongdev.skysongEconomy.commands.modifier;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierArguments;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierDraft;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se modifier add|set &lt;type&gt; &lt;target&gt; &lt;percent&gt; [for:&lt;time&gt;] [on:&lt;imports|exports|both&gt;] [name...].
 * {@code add} refuses a second modifier of the same type on a target; {@code set} replaces it.
 */
final class ChangeModifier implements SubCommand {
    /** Whether an existing modifier of the same type is refused or replaced. */
    enum Mode {
        ADD, SET
    }

    private final Mode mode;
    private final PriceModifiers modifiers;
    private final ModifierText text;
    private final Messenger messenger;
    private final Clock clock;

    ChangeModifier(Mode mode, PriceModifiers modifiers, ModifierText text, Messenger messenger, Clock clock) {
        this.mode = mode;
        this.modifiers = modifiers;
        this.text = text;
        this.messenger = messenger;
        this.clock = clock;
    }

    @Override
    public String name() {
        return mode.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String permission() {
        return ModifierCommand.EDIT_PERMISSION;
    }

    @Override
    public String usage() {
        return "<type> <target> <percent> [for:<time>] [on:imports|exports|both] [display name]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < ModifierArguments.REQUIRED) {
            messenger.send(sender, "command.usage", unparsed("usage", "/se modifier " + name() + " " + usage()));
            return;
        }
        try {
            ModifierDraft draft = ModifierArguments.parse(args, sender.getName(), clock.instant());
            if (mode == Mode.ADD) {
                messenger.send(sender, "prices.modifiers.added", text.placeholders(modifiers.add(draft)));
                return;
            }
            PriceModifiers.SetResult result = modifiers.set(draft);
            if (result.replaced().isPresent()) {
                PriceModifier old = result.replaced().get();
                messenger.send(sender, "prices.modifiers.replaced", text.placeholders(result.modifier()),
                        unparsed("old-percent", PriceModifiers.signed(old.percent())));
            } else {
                messenger.send(sender, "prices.modifiers.added", text.placeholders(result.modifier()));
            }
        } catch (PriceException refusal) {
            messenger.send(sender, refusal);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> TargetCompletions.complete(args[1]);
            case 3 -> List.of("+10", "-10");
            case 4, 5 -> List.of("for:7d", "on:imports", "on:exports", "on:both");
            default -> List.of();
        };
    }
}
