package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.benefit.Changes;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;

import java.util.List;
import java.util.Map;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business npcsync &lt;id&gt;: staff bring every NPC of a business in line with what it is
 * owed right now, and see what changed. It happens by itself whenever a business changes; this
 * is for after something was changed by hand in SkysongNPCs.
 */
final class SyncBusinessNpcs extends BusinessSubCommand {
    private final NpcCommandText text;
    private final NpcProgression progression;

    SyncBusinessNpcs(BusinessCommandSupport support, NpcCommandText text, NpcProgression progression) {
        super(support, "npcsync", "<id>", 1, BusinessCommandSupport.ADMIN_PERMISSION);
        this.text = text;
        this.progression = progression;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        Map<BusinessNpc, Changes> changed = progression.sync(business);
        if (changed.values().stream().noneMatch(Changes::any)) {
            support.messenger.send(sender, "business.npcs.sync.none", unparsed("business", business.displayName()));
            return;
        }
        support.messenger.send(sender, "business.npcs.sync.header", unparsed("business", business.displayName()));
        changed.forEach((npc, changes) -> {
            if (changes.any()) {
                support.messenger.send(sender, "business.npcs.sync.line", unparsed("npc", text.name(npc)),
                        unparsed("points", String.valueOf(changes.pointsAdded())),
                        unparsed("picks", String.valueOf(changes.picksAdded())),
                        unparsed("choices", String.valueOf(changes.abilityChoicesAdded())),
                        unparsed("granted", String.valueOf(changes.abilitiesGranted().size())),
                        unparsed("revoked", String.valueOf(changes.abilitiesRevoked().size())),
                        unparsed("sheet", String.valueOf(changes.statsChanged() + changes.proficienciesChanged())),
                        component("reset", text.resetNote(changes)));
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.businessIds(args[0]) : List.of();
    }
}
