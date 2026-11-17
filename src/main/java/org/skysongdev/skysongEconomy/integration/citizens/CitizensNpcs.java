package org.skysongdev.skysongEconomy.integration.citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Finds NPCs in the world through Citizens, which SkysongNPCs is built on: the NPC a player looks
 * at, where an NPC stands, and its body in the world. Only used when Citizens is installed, which it always is with
 * SkysongNPCs. Must be used on the server's main thread.
 */
public final class CitizensNpcs {
    /** Citizens' plugin name, as plugin.yml's {@code softdepend} lists it. */
    public static final String CITIZENS = "Citizens";

    /** How far away, in blocks, a player can target an NPC. */
    static final int TARGET_RANGE = 8;

    /**
     * @return the Citizens UUID of the NPC the player is looking at, within a few blocks; empty
     *         when they look at no NPC
     */
    public Optional<UUID> targeted(Player player) {
        Entity looked = player.getTargetEntity(TARGET_RANGE);
        if (looked == null) {
            return Optional.empty();
        }
        NPC npc = registry().getNPC(looked);
        return npc == null ? Optional.empty() : Optional.of(npc.getUniqueId());
    }

    /** @return where the NPC stands; empty when Citizens has no such NPC or it is not in the world */
    public Optional<Location> location(UUID npc) {
        NPC found = registry().getByUniqueId(npc);
        if (found == null || !found.isSpawned()) {
            return Optional.empty();
        }
        return Optional.ofNullable(found.getStoredLocation()).map(Location::clone);
    }

    /** @return the NPC's body in the world; empty when Citizens has no such NPC or it is not spawned */
    public Optional<Entity> entity(UUID npc) {
        NPC found = registry().getByUniqueId(npc);
        if (found == null || !found.isSpawned()) {
            return Optional.empty();
        }
        return Optional.ofNullable(found.getEntity());
    }

    private static NPCRegistry registry() {
        return CitizensAPI.getNPCRegistry();
    }
}
