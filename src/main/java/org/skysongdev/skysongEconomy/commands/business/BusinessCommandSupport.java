package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.block.Block;
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
import org.skysongdev.skysongEconomy.messaging.Messenger;

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

    private static final int TARGET_RANGE = 5;

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

    /** @return the block the player is looking at, within reach */
    Optional<BlockPosition> targetBlock(Player player) {
        Block block = player.getTargetBlockExact(TARGET_RANGE);
        if (block == null || block.getType().isAir()) {
            messenger.send(player, "command.look-at-block");
            return Optional.empty();
        }
        return Optional.of(positionOf(block));
    }

    static BlockPosition positionOf(Block block) {
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
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
