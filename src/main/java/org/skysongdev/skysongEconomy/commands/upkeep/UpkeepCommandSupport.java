package org.skysongdev.skysongEconomy.commands.upkeep;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepNpcs;
import org.skysongdev.skysongEconomy.upkeep.UpkeepProfile;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;
import org.skysongdev.skysongEconomy.upkeep.UpkeepText;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** What the /se upkeep commands share: the upkeep service, the NPC a player looks at, and text for messages. */
public final class UpkeepCommandSupport {
    final UpkeepService upkeep;
    final UpkeepNpcs npcs;
    final Messenger messenger;
    private final Function<Player, Optional<UUID>> targeted;
    private final UpkeepText text;

    /**
     * @param targeted the managed NPC a player looks at, if any
     */
    public UpkeepCommandSupport(UpkeepService upkeep, UpkeepNpcs npcs, Function<Player, Optional<UUID>> targeted,
                                Messenger messenger, UpkeepText text) {
        this.upkeep = upkeep;
        this.npcs = npcs;
        this.targeted = targeted;
        this.messenger = messenger;
        this.text = text;
    }

    /** @return the managed NPC the sender looks at; tells them why when there is none */
    Optional<UUID> requireTarget(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messenger.send(sender, "command.players-only");
            return Optional.empty();
        }
        Optional<UUID> npc = targeted.apply(player);
        if (npc.isEmpty()) {
            messenger.send(sender, "upkeep.look-at-npc");
            return Optional.empty();
        }
        if (!npcs.exists(npc.get())) {
            messenger.send(sender, "upkeep.not-managed");
            return Optional.empty();
        }
        return npc;
    }

    /** @return the NPC's name as {@code <npc>}, its UUID when it has none */
    TagResolver npcName(UUID npc) {
        return Placeholder.unparsed("npc", npcs.name(npc).orElse(npc.toString()));
    }

    /** @see UpkeepText#state */
    TagResolver state(UpkeepState state) {
        return text.state(state);
    }

    /** @see UpkeepText#next */
    TagResolver next(NpcUpkeep upkeep) {
        return text.next(upkeep);
    }

    /** @see UpkeepText#since */
    TagResolver since(NpcUpkeep upkeep) {
        return text.since(upkeep);
    }

    /** @see UpkeepText#cycles */
    TagResolver cycles(int covered, int max) {
        return text.cycles(covered, max);
    }

    /** @see UpkeepText#cost */
    TagResolver cost(UpkeepProfile profile) {
        return text.cost(profile);
    }

    /** @return the profiles' ids, for messages and tab completion */
    List<String> profileIds() {
        return upkeep.settings().profiles().keySet().stream().sorted().toList();
    }
}
