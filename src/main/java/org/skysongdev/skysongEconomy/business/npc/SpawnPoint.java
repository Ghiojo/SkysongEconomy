package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.Objects;
import java.util.UUID;

/**
 * Where a new NPC should stand. SkysongNPCs picks the exact free spot nearby, so this only says
 * where to look.
 */
public sealed interface SpawnPoint permits SpawnPoint.InFrontOf, SpawnPoint.NextTo {
    /**
     * A free spot in front of a player. The spot must also be one that player may place an NPC
     * on (the {@code npc-place} region flag).
     *
     * @param player the online player
     */
    record InFrontOf(UUID player) implements SpawnPoint {
        public InFrontOf {
            Objects.requireNonNull(player, "player");
        }
    }

    /**
     * A free spot next to a block, such as the business block.
     *
     * @param block  the block to stand next to
     * @param placer the player creating the NPC, whose region permissions are checked;
     *               {@code null} when nobody's are, as for staff commands from the console
     */
    record NextTo(BlockPosition block, @Nullable UUID placer) implements SpawnPoint {
        public NextTo {
            Objects.requireNonNull(block, "block");
        }
    }
}
