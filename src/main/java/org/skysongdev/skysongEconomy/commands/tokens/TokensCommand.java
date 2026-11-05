package org.skysongdev.skysongEconomy.commands.tokens;

import org.skysongdev.skysongEconomy.commands.CharacterArguments;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;

/**
 * /se tokens: Gathering Token balances. Everyone may see their own; seeing others' and changing
 * balances needs {@link #ADMIN_PERMISSION}.
 */
public final class TokensCommand extends CommandGroup {
    /** Staff permission: see anyone's tokens and change them. */
    public static final String ADMIN_PERMISSION = "skysongeconomy.tokens.admin";

    public TokensCommand(GatheringTokens tokens, CharacterArguments characters, Messenger messenger) {
        super("tokens", "skysongeconomy.tokens", messenger);
        register(new TokenBalance(tokens, characters, messenger));
        for (ChangeTokens.Change change : ChangeTokens.Change.values()) {
            register(new ChangeTokens(change, tokens, characters, messenger));
        }
    }
}
