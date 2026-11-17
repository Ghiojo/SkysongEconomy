package org.skysongdev.skysongEconomy.commands.upkeep;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.trade.Durations;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepProfile;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;
import org.skysongdev.skysongEconomy.upkeep.UpkeepStock;

import java.util.Optional;
import java.util.UUID;

/** /se upkeep info: the upkeep of the NPC the sender looks at. */
final class UpkeepInfo extends UpkeepSubCommand {
    /** The most cycles {@code info} counts the stock for. */
    static final int MAX_CYCLES_SHOWN = 99;

    private final UpkeepStock stock;

    UpkeepInfo(UpkeepCommandSupport support, UpkeepStock stock) {
        super(support, "info", "", 0);
        this.stock = stock;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Optional<UUID> target = support.requireTarget(sender);
        if (target.isEmpty()) {
            return;
        }
        UUID npc = target.get();
        TagResolver name = support.npcName(npc);
        Optional<NpcUpkeep> found = support.upkeep.upkeep(npc);
        if (found.isEmpty()) {
            boolean byHand = support.upkeep.saved(npc).map(NpcUpkeep::setByHand).orElse(false);
            support.messenger.send(sender, byHand ? "upkeep.none-by-hand" : "upkeep.none", name);
            return;
        }
        NpcUpkeep upkeep = found.get();
        TagResolver profileId = Placeholder.unparsed("profile", upkeep.profileId().orElseThrow());
        support.messenger.send(sender, "upkeep.info-header", name, support.state(upkeep.state()));
        Optional<UpkeepProfile> profile = support.upkeep.profile(npc);
        if (profile.isEmpty()) {
            support.messenger.send(sender, "upkeep.profile-missing", profileId);
            return;
        }
        support.messenger.send(sender, "upkeep.info-profile", profileId,
                Placeholder.component("source", support.messenger.render(upkeep.setByHand()
                        ? "upkeep.source-hand" : "upkeep.source-template")),
                Placeholder.unparsed("every", Durations.format(profile.get().every())));
        support.messenger.send(sender, "upkeep.info-cost", support.cost(profile.get()));
        if (upkeep.state() == UpkeepState.UNMAINTAINED) {
            support.messenger.send(sender, "upkeep.info-unmaintained", support.since(upkeep));
            return;
        }
        int cycles = support.upkeep.payment().cyclesCovered(profile.get().cost(), stock.view(npc), MAX_CYCLES_SHOWN);
        support.messenger.send(sender, "upkeep.info-next", support.next(upkeep), support.cycles(cycles, MAX_CYCLES_SHOWN));
    }
}
