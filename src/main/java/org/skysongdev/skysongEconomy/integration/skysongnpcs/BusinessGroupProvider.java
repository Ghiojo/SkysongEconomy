package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.npc.BusinessGroupDirectory;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongnpcs.api.access.GroupProvider;

import java.util.Collection;
import java.util.List;

/**
 * Tells SkysongNPCs who is in a business access group, so its grants and policies can name
 * {@code economy:<business>/<group>}. The rules are in {@link BusinessGroupDirectory}; this only
 * translates the call.
 */
public final class BusinessGroupProvider implements GroupProvider {
    private final BusinessGroupDirectory groups;
    private final CharacterDirectory characters;

    public BusinessGroupProvider(BusinessGroupDirectory groups, CharacterDirectory characters) {
        this.groups = groups;
        this.characters = characters;
    }

    @Override
    public boolean contains(@NotNull String group, @NotNull Player player, @Nullable String profile) {
        return groups.contains(group, player.getUniqueId(), profile);
    }

    /** Suggests the groups of the businesses the player's current character owns. */
    @Override
    public @NotNull Collection<String> suggestions(@NotNull Player player) {
        return characters.active(player.getUniqueId()).map(groups::suggestions).orElse(List.of());
    }
}
