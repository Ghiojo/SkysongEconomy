package org.skysongdev.skysongEconomy.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns {@code <player> [profile]} command arguments into characters, completes them, and
 * names characters in messages. Each lookup that fails tells the sender why.
 */
public final class CharacterArguments {
    private final CharacterDirectory characters;
    private final Messenger messenger;

    public CharacterArguments(CharacterDirectory characters, Messenger messenger) {
        this.characters = characters;
        this.messenger = messenger;
    }

    /** @return the sender as a player; tells them when they aren't one */
    public Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        messenger.send(sender, "command.players-only");
        return Optional.empty();
    }

    /**
     * Finds a player's character: the named profile, or the one they are playing now.
     *
     * @param profile a profile name, or {@code null} for their active character
     */
    public Optional<CharacterRef> character(CommandSender sender, String playerName, @Nullable String profile) {
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
    public Optional<CharacterRef> ownCharacter(Player player) {
        Optional<CharacterRef> active = characters.active(player.getUniqueId());
        if (active.isEmpty()) {
            messenger.send(player, "command.you-have-no-character");
        }
        return active;
    }

    /** @return the character the sender plays now, when they are a player */
    public Optional<CharacterRef> activeCharacterOf(CommandSender sender) {
        return sender instanceof Player player ? characters.active(player.getUniqueId()) : Optional.empty();
    }

    /** @return a character's name for messages: the profile, and the player when they differ */
    public String describe(CharacterRef character) {
        String playerName = Bukkit.getOfflinePlayer(character.player()).getName();
        if (playerName == null || playerName.equalsIgnoreCase(character.profile())) {
            return character.profile();
        }
        return character.profile() + " (" + playerName + ")";
    }

    public List<String> onlinePlayers(String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    public List<String> profiles(String playerName, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return findPlayer(playerName)
                .map(player -> characters.all(player.getUniqueId()).stream().map(CharacterRef::profile)
                        .filter(profile -> profile.toLowerCase(Locale.ROOT).startsWith(prefix)).toList())
                .orElse(List.of());
    }

    /** @return the online player with that name, or a known offline one */
    public static Optional<OfflinePlayer> findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online);
        }
        return Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(name));
    }
}
