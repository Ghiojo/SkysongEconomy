package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.NpcSlotGroup;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business npcs &lt;id&gt;: the business's NPC slots and NPCs, for its owner and staff. NPCs
 * are numbered from 1, oldest first; {@code dismissnpc} takes that number.
 */
final class ListBusinessNpcs extends BusinessSubCommand {
    private final NpcCommandText text;

    ListBusinessNpcs(BusinessCommandSupport support, NpcCommandText text) {
        super(support, "npcs", "<id>", 1, null);
        this.text = text;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        List<NpcSlotGroup> groups = text.npcs().slots(business);
        if (groups.isEmpty()) {
            support.messenger.send(sender, "business.npcs.list-none", unparsed("business", business.displayName()));
            return;
        }
        support.messenger.send(sender, "business.npcs.list-header", unparsed("business", business.displayName()));
        List<BusinessNpc> numbered = text.npcs().of(business);
        for (NpcSlotGroup group : groups) {
            support.messenger.send(sender, "business.npcs.list-group", component("source", text.sourceName(group.source())),
                    unparsed("used", String.valueOf(group.npcs().size())),
                    unparsed("max", String.valueOf(Math.max(group.capacity(), group.npcs().size()))));
            for (BusinessNpc npc : group.npcs()) {
                support.messenger.send(sender, "business.npcs.list-npc",
                        unparsed("number", String.valueOf(numbered.indexOf(npc) + 1)),
                        unparsed("npc", text.name(npc)), component("status", text.status(npc)));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.businessIds(args[0]) : List.of();
    }
}
