package org.skysongdev.skysongEconomy.commands.upkeep;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.trade.Durations;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepCost;
import org.skysongdev.skysongEconomy.upkeep.UpkeepNpcs;
import org.skysongdev.skysongEconomy.upkeep.UpkeepProfile;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
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
    private final Clock clock;

    /**
     * @param targeted the managed NPC a player looks at, if any
     */
    public UpkeepCommandSupport(UpkeepService upkeep, UpkeepNpcs npcs, Function<Player, Optional<UUID>> targeted,
                                Messenger messenger, Clock clock) {
        this.upkeep = upkeep;
        this.npcs = npcs;
        this.targeted = targeted;
        this.messenger = messenger;
        this.clock = clock;
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

    /** @return the state's word as {@code <state>} */
    TagResolver state(UpkeepState state) {
        return Placeholder.component("state", messenger.render(state == UpkeepState.MAINTAINED
                ? "upkeep.maintained" : "upkeep.unmaintained"));
    }

    /** @return the time left until the next cycle as {@code <next>} */
    TagResolver next(NpcUpkeep upkeep) {
        return Placeholder.unparsed("next", Durations.format(Duration.between(clock.instant(), upkeep.nextDue())));
    }

    /** @return how long ago the state last changed as {@code <since>} */
    TagResolver since(NpcUpkeep upkeep) {
        return Placeholder.unparsed("since", Durations.format(Duration.between(upkeep.changed(), clock.instant())));
    }

    /** @return the cost of one cycle as {@code <cost>}, such as {@code 1x water_flask, 3x any food} */
    TagResolver cost(UpkeepProfile profile) {
        List<Component> entries = new ArrayList<>();
        for (UpkeepCost entry : profile.cost()) {
            TagResolver amount = Placeholder.unparsed("amount", String.valueOf(entry.amount()));
            entries.add(switch (entry.match()) {
                case UpkeepCost.Match.Item item -> messenger.render("upkeep.cost-entry-item", amount,
                        Placeholder.unparsed("item", item.materialId()));
                case UpkeepCost.Match.GoodType type -> messenger.render("upkeep.cost-entry-good-type", amount,
                        Placeholder.unparsed("type", type.goodType()));
            });
        }
        return Placeholder.component("cost", Component.join(JoinConfiguration.commas(true), entries));
    }

    /** @return the profiles' ids, for messages and tab completion */
    List<String> profileIds() {
        return upkeep.settings().profiles().keySet().stream().sorted().toList();
    }
}
