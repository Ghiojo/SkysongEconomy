package org.skysongdev.skysongEconomy.commands.tokens;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.commands.CharacterArguments;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se tokens add|remove|set &lt;player&gt; &lt;amount&gt; [profile]: staff change a character's tokens. */
final class ChangeTokens implements SubCommand {
    /** What the command does to the balance. */
    enum Change {
        ADD, REMOVE, SET;

        String commandName() {
            return name().toLowerCase(Locale.ROOT);
        }

        long apply(GatheringTokens tokens, CharacterRef character, long amount) {
            return switch (this) {
                case ADD -> tokens.add(character, amount);
                case REMOVE -> tokens.remove(character, amount);
                case SET -> tokens.set(character, amount);
            };
        }
    }

    private final Change change;
    private final GatheringTokens tokens;
    private final CharacterArguments characters;
    private final Messenger messenger;

    ChangeTokens(Change change, GatheringTokens tokens, CharacterArguments characters, Messenger messenger) {
        this.change = change;
        this.tokens = tokens;
        this.characters = characters;
        this.messenger = messenger;
    }

    @Override
    public String name() {
        return change.commandName();
    }

    @Override
    public String permission() {
        return TokensCommand.ADMIN_PERMISSION;
    }

    @Override
    public String usage() {
        return "<player> <amount> [profile]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messenger.send(sender, "command.usage", unparsed("usage", "/se tokens " + name() + " " + usage()));
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            messenger.send(sender, "tokens.invalid-amount", unparsed("amount", args[1]));
            return;
        }
        characters.character(sender, args[0], args.length > 2 ? args[2] : null).ifPresent(character -> {
            try {
                long balance = change.apply(tokens, character, amount);
                messenger.send(sender, "tokens.changed", unparsed("character", characters.describe(character)),
                        unparsed("tokens", String.valueOf(balance)));
            } catch (BusinessException refusal) {
                messenger.send(sender, refusal);
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> characters.onlinePlayers(args[0]);
            case 3 -> characters.profiles(args[0], args[2]);
            default -> List.of();
        };
    }
}
