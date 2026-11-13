package org.skysongdev.skysongEconomy.commands.business;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.LostNpc;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcStatus;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.benefit.Changes;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;
import java.util.function.Function;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * What the business NPC commands share: the NPC services and how NPCs are written in chat.
 *
 * @param npcs    the NPCs each business has
 * @param gateway SkysongNPCs, for names
 * @param sheets  what each NPC still has to choose
 * @param skills  skill names
 * @param moduleNames a module's name by its id, as MiniMessage; empty for a module that has no file
 */
record NpcCommandText(BusinessNpcs npcs, NpcGateway gateway, NpcSheetGateway sheets, SkillCatalog skills,
                      Function<String, Optional<String>> moduleNames, Messenger messenger) implements ChangeReport {
    /** @return the NPC's name, or its id when SkysongNPCs no longer knows it */
    String name(BusinessNpc npc) {
        return gateway.name(npc.npc()).orElse(npc.npc().toString());
    }

    /** @return what gives a slot, as players read it: the skill's name for a skill */
    Component sourceName(SlotSource source) {
        if (source.isSkill()) {
            return messenger.render("business.npcs.source.skill", parsed("skill", skills.displayName(source.id())));
        }
        Optional<String> module = source.isModule() ? moduleNames.apply(source.id()) : Optional.empty();
        return module.isPresent()
                ? messenger.render("business.npcs.source.module", parsed("module", module.get()))
                : messenger.render("business.npcs.source.other", unparsed("source", source.key()));
    }

    /** @return what the NPC is waiting for, as text */
    Component status(BusinessNpc npc) {
        return messenger.render(NpcStatus.of(gateway, sheets, npc.npc()).messageKey());
    }

    /** @return the note that says what was given back to choose again, empty when nothing was */
    Component resetNote(Changes changes) {
        if (changes.choicesReset()) {
            return messenger.render("business.npcs.sync.reset");
        }
        return changes.picksReset() ? messenger.render("business.npcs.sync.picks-reset") : Component.empty();
    }

    /** Tells the sender which NPCs the business just lost with their slots, if any. */
    @Override
    public void report(CommandSender sender, Business business) {
        List<LostNpc> lost = npcs.takeLostSlots(business);
        if (lost.isEmpty()) {
            return;
        }
        messenger.send(sender, "business.npcs.lost-slots", unparsed("business", business.displayName()),
                unparsed("npcs", lost.stream().map(LostNpc::name).collect(Collectors.joining(", "))));
    }
}
