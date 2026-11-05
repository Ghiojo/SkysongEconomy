package org.skysongdev.skysongEconomy.commands.tokens;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.commands.CharacterArguments;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;

import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se tokens balance [player] [profile]: without arguments, the tokens of the character you are
 * playing; staff can name anyone's character.
 */
final class TokenBalance implements SubCommand {
    private final GatheringTokens tokens;
    private final CharacterArguments characters;
    private final Messenger messenger;

    TokenBalance(GatheringTokens tokens, CharacterArguments characters, Messenger messenger) {
        this.tokens = tokens;
        this.characters = characters;
        this.messenger = messenger;
    }

    @Override
    public String name() {
        return "balance";
    }

    @Override
    public String usage() {
        return "[player] [profile]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            characters.requirePlayer(sender).flatMap(characters::ownCharacter).ifPresent(character ->
                    messenger.send(sender, "tokens.balance-own", unparsed("tokens", String.valueOf(tokens.balance(character)))));
            return;
        }
        if (!sender.hasPermission(TokensCommand.ADMIN_PERMISSION)) {
            messenger.send(sender, "command.no-permission");
            return;
        }
        Optional<CharacterRef> character = characters.character(sender, args[0], args.length > 1 ? args[1] : null);
        character.ifPresent(found -> messenger.send(sender, "tokens.balance",
                unparsed("character", characters.describe(found)), unparsed("tokens", String.valueOf(tokens.balance(found)))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TokensCommand.ADMIN_PERMISSION)) {
            return List.of();
        }
        return switch (args.length) {
            case 1 -> characters.onlinePlayers(args[0]);
            case 2 -> characters.profiles(args[0], args[1]);
            default -> List.of();
        };
    }
}
