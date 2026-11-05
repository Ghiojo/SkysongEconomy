package org.skysongdev.skysongEconomy.commands.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * What the /se business commands share: finding players' characters, the block a player is
 * looking at, and who may manage a business. Each lookup that fails tells the sender why.
 */
final class BusinessCommandSupport {
    /** Staff permission: every business command, on every business. */
    static final String ADMIN_PERMISSION = "skysongeconomy.business.admin";

    private static final int TARGET_RANGE = 5;

    final BusinessService businesses;
    final CharacterDirectory characters;
    final Messenger messenger;

    BusinessCommandSupport(BusinessService businesses, CharacterDirectory characters, Messenger messenger) {
        this.businesses = businesses;
        this.characters = characters;
        this.messenger = messenger;
    }

    Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        messenger.send(sender, "command.players-only");
        return Optional.empty();
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

    /**
     * Finds a player's character: the named profile, or the one they are playing now.
     *
     * @param profile a profile name, or {@code null} for their active character
     */
    Optional<CharacterRef> character(CommandSender sender, String playerName, @Nullable String profile) {
        Optional<OfflinePlayer> player = findPlayer(playerName);
        if (player.isEmpty()) {
            messenger.send(sender, "command.unknown-player", Placeholder.unparsed("player", playerName));
            return Optional.empty();
        }
        if (profile == null) {
            Optional<CharacterRef> active = characters.active(player.get().getUniqueId());
            if (active.isEmpty()) {
                messenger.send(sender, "command.no-active-character", Placeholder.unparsed("player", playerName));
            }
            return active;
        }
        Optional<CharacterRef> named = characters.all(player.get().getUniqueId()).stream()
                .filter(character -> character.profile().equalsIgnoreCase(profile))
                .findFirst();
        if (named.isEmpty()) {
            messenger.send(sender, "command.unknown-character",
                    Placeholder.unparsed("player", playerName), Placeholder.unparsed("profile", profile));
        }
        return named;
    }

    /** @return the character the player is playing now */
    Optional<CharacterRef> ownCharacter(Player player) {
        Optional<CharacterRef> active = characters.active(player.getUniqueId());
        if (active.isEmpty()) {
            messenger.send(player, "command.you-have-no-character");
        }
        return active;
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
        String playerName = Bukkit.getOfflinePlayer(character.player()).getName();
        if (playerName == null || playerName.equalsIgnoreCase(character.profile())) {
            return character.profile();
        }
        return character.profile() + " (" + playerName + ")";
    }

    List<String> businessIds(String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return businesses.all().stream().map(Business::id).filter(id -> id.startsWith(prefix)).toList();
    }

    List<String> onlinePlayers(String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    List<String> profiles(String playerName, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return findPlayer(playerName)
                .map(player -> characters.all(player.getUniqueId()).stream().map(CharacterRef::profile)
                        .filter(profile -> profile.toLowerCase(Locale.ROOT).startsWith(prefix)).toList())
                .orElse(List.of());
    }

    private static Optional<OfflinePlayer> findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online);
        }
        return Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(name));
    }
}
