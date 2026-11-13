package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Optional;
import java.util.UUID;

/**
 * Creates and removes SkysongNPCs and says who owns them. Main thread only.
 * <p>
 * An NPC is owned by a character or by the server, never by a business: a business's NPCs belong
 * to its owner's character, or to the server when the server owns the business.
 */
public interface NpcGateway {
    /** @return whether SkysongNPCs still has that NPC */
    boolean exists(UUID npc);

    /**
     * Creates an NPC from a SkysongNPCs template on a free spot. The new NPC starts in
     * SkysongNPCs' first setup, so whatever it should choose there must be given to it before a
     * player opens that setup.
     *
     * @param template the SkysongNPCs template's name
     * @param owner    the owning character, {@code null} for the server
     * @param where    where to look for a free spot
     * @param radius   how many blocks around that point to look, 0 or more
     * @return the new NPC, or why there is none; never throws for a bad template or a full area
     */
    NpcCreation create(String template, @Nullable CharacterRef owner, SpawnPoint where, int radius);

    /**
     * Removes an NPC for good, with its sheet and everything it was given.
     *
     * @return false when there was no such NPC
     */
    boolean remove(UUID npc);

    /**
     * Gives an NPC to another owner.
     *
     * @param owner the new owning character, {@code null} for the server
     * @return false when there is no such NPC
     */
    boolean setOwner(UUID npc, @Nullable CharacterRef owner);

    /**
     * @param owner a character, or {@code null} for the server
     * @return whether SkysongNPCs has that NPC and records it as owned by that owner
     */
    boolean isOwnedBy(UUID npc, @Nullable CharacterRef owner);

    /** @return whether the NPC is still waiting for its first setup */
    boolean inSetup(UUID npc);

    /** @return the NPC's name as plain text, without colours or other formatting; empty when there is no such NPC */
    Optional<String> name(UUID npc);

    /**
     * Opens SkysongNPCs' own menu of the NPC for a player. SkysongNPCs shows them only what
     * their access allows.
     *
     * @return false when the player is offline or there is no such NPC
     */
    boolean openMenu(UUID player, UUID npc);

    /**
     * Opens the NPC's first setup for a player while it is in setup, otherwise its menu of
     * pending choices. This checks no access: only offer it to someone who may choose.
     *
     * @return false when the player is offline or there is no such NPC
     */
    boolean openChoices(UUID player, UUID npc);

    /**
     * Brings an NPC that is out in the world to a player.
     *
     * @return false when the player is offline, or the NPC is stored or gone
     */
    boolean teleportTo(UUID npc, UUID player);
}
