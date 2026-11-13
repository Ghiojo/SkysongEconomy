package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.benefit.Changes;
import org.skysongdev.skysongEconomy.business.npc.benefit.ProgressionNotices;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.messaging.Messenger;

/**
 * Tells a business's owner when one of its NPCs received something or has to be assigned again.
 * Only an owner who is online and playing the owning character is told; anyone else sees the
 * NPC's status in the NPCs menu the next time they open it.
 */
public final class NpcProgressionMessages implements ProgressionNotices {
    private final NpcGateway npcs;
    private final CharacterDirectory characters;
    private final Messenger messenger;

    public NpcProgressionMessages(NpcGateway npcs, CharacterDirectory characters, Messenger messenger) {
        this.npcs = npcs;
        this.characters = characters;
        this.messenger = messenger;
    }

    @Override
    public void changed(Business business, BusinessNpc npc, Changes changes) {
        business.owner().ifPresent(owner -> {
            Player player = Bukkit.getPlayer(owner.player());
            if (player == null || characters.active(owner.player()).filter(owner::equals).isEmpty()) {
                return;
            }
            messenger.send(player, key(changes),
                    Placeholder.unparsed("npc", npcs.name(npc.npc()).orElse(npc.npc().toString())),
                    Placeholder.unparsed("business", business.displayName()));
        });
    }

    private static String key(Changes changes) {
        if (changes.choicesReset()) {
            return "business.npcs.progress.reset";
        }
        if (changes.picksReset()) {
            return "business.npcs.progress.picks";
        }
        return changes.leavesChoices() ? "business.npcs.progress.more" : "business.npcs.progress.updated";
    }
}
