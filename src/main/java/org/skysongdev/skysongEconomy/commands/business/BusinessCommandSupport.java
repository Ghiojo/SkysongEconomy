package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.commands.CharacterArguments;
import org.skysongdev.skysongEconomy.commands.TargetBlocks;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * What the /se business commands share: finding players' characters (through
 * {@link CharacterArguments}), the block a player is looking at, and who may manage a business.
 * Each lookup that fails tells the sender why.
 */
final class BusinessCommandSupport {
    /** Staff permission: every business command, on every business. */
    static final String ADMIN_PERMISSION = "skysongeconomy.business.admin";

    /**
     * Typed instead of a player name to make the server the owner. {@code #} can't appear in a
     * Minecraft name, so no player is ever mistaken for it.
     */
    static final String SERVER_OWNER = "#server";

    final BusinessService businesses;
    final CharacterDirectory characters;
    final Messenger messenger;
    private final CharacterArguments characterArgs;

    BusinessCommandSupport(BusinessService businesses, CharacterDirectory characters, Messenger messenger) {
        this.businesses = businesses;
        this.characters = characters;
        this.messenger = messenger;
        this.characterArgs = new CharacterArguments(characters, messenger);
    }

    Optional<Player> requirePlayer(CommandSender sender) {
        return characterArgs.requirePlayer(sender);
    }

    /** @see TargetBlocks#target */
    Optional<BlockPosition> targetBlock(Player player) {
        return TargetBlocks.target(player, messenger);
    }

    /** @see CharacterArguments#character */
    Optional<CharacterRef> character(CommandSender sender, String playerName, @Nullable String profile) {
        return characterArgs.character(sender, playerName, profile);
    }

    /** @see CharacterArguments#ownCharacter */
    Optional<CharacterRef> ownCharacter(Player player) {
        return characterArgs.ownCharacter(player);
    }

    /**
     * Staff may manage every business, players only the ones their active character owns.
     *
     * @throws BusinessException {@code business.not-owner} when the sender may not
     */
    void requireManager(CommandSender sender, Business business) {
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        boolean owner = sender instanceof Player player
                && characters.active(player.getUniqueId()).filter(business::isOwner).isPresent();
        if (!owner) {
            throw new BusinessException("business.not-owner", Map.of("business", business.displayName()));
        }
    }

    /** @return a character's name for messages: the profile, and the player when they differ */
    String describe(CharacterRef character) {
        return characterArgs.describe(character);
    }

    /** @return the owner's name for messages, or the configured name of the server */
    String describeOwner(Business business) {
        return business.owner().map(this::describe).orElseGet(() -> messenger.plainText("business.server-owner"));
    }

    /** @return whether the typed owner is {@link #SERVER_OWNER} */
    static boolean isServerOwner(String typed) {
        return typed.equalsIgnoreCase(SERVER_OWNER);
    }

    /** @return the online players matching what was typed, plus {@link #SERVER_OWNER} when it matches */
    List<String> owners(String typed) {
        List<String> owners = new ArrayList<>(onlinePlayers(typed));
        if (SERVER_OWNER.startsWith(typed.toLowerCase(Locale.ROOT))) {
            owners.add(SERVER_OWNER);
        }
        return owners;
    }

    List<String> businessIds(String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return businesses.all().stream().map(Business::id).filter(id -> id.startsWith(prefix)).toList();
    }

    List<String> onlinePlayers(String typed) {
        return characterArgs.onlinePlayers(typed);
    }

    List<String> profiles(String playerName, String typed) {
        return characterArgs.profiles(playerName, typed);
    }
}
