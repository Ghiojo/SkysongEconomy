package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSlotGroup;
import org.skysongdev.skysongEconomy.business.npc.NpcStatus;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.benefit.Entitlement;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongEconomy.business.npc.benefit.PickKind;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What the NPC menus share: the services they read and the texts they all write the same way.
 *
 * @param npcs     the NPCs each business has
 * @param progression what the business owes each NPC
 * @param gateway  SkysongNPCs, for names, menus and teleporting
 * @param sheets   what each NPC still has to choose
 * @param settings {@code businesses/npcs.yml}
 * @param skills   skill names
 */
record NpcScreens(BusinessMenus menus, BusinessNpcs npcs, NpcProgression progression, NpcGateway gateway,
                  NpcSheetGateway sheets,
                  NpcSettings settings, SkillCatalog skills, Messenger messenger) {
    /** @return whether the viewer may create NPCs: who acts as the owner, or a group with {@code manage-npcs} */
    boolean mayCreate(BusinessMenuContext context) {
        return context.actsAsOwner()
                || context.character().filter(character -> npcs.mayCreate(context.business(), character)).isPresent();
    }

    /** @return what gives a slot, as players read it: the skill's name for a skill */
    Component sourceName(SlotSource source) {
        return source.isSkill()
                ? messenger.render("business.npcs.source.skill", Placeholder.parsed("skill", skills.displayName(source.id())))
                : messenger.render("business.npcs.source.other", Placeholder.unparsed("source", source.key()));
    }

    /** @return the NPC's name, or its id when SkysongNPCs no longer knows it */
    String name(BusinessNpc npc) {
        return gateway.name(npc.npc()).orElse(npc.npc().toString());
    }

    /** @return what the NPC is waiting for, as text */
    Component status(BusinessNpc npc) {
        return messenger.render(NpcStatus.of(gateway, sheets, npc.npc()).messageKey());
    }

    /**
     * @return what the business owes the NPC right now, one line per thing: stat points, picks,
     *         baked stats, fixed proficiencies, abilities and abilities to choose
     */
    List<Component> given(Business business, BusinessNpc npc) {
        Optional<Entitlement> owed = progression.owed(business, npc);
        if (owed.isEmpty()) {
            return List.of(messenger.render("business.npcs.details.given.unknown"));
        }
        Entitlement entitlement = owed.get();
        if (entitlement.isEmpty()) {
            return List.of(messenger.render("business.npcs.details.given.nothing"));
        }
        List<Component> lines = new ArrayList<>();
        if (entitlement.totalPoints() > 0) {
            lines.add(messenger.render("business.npcs.details.given.points", amount(entitlement.totalPoints())));
        }
        for (PickKind kind : entitlement.pickKinds()) {
            TagResolver skill = Placeholder.parsed("skill", skills.displayName(kind.skill()));
            TagResolver tier = Placeholder.unparsed("tier", kind.tier());
            lines.add(kind.promoteTo() == null
                    ? messenger.render("business.npcs.details.given.picks", skill, amount(entitlement.picks().get(kind)), tier)
                    : messenger.render("business.npcs.details.given.picks-promote", skill,
                            amount(entitlement.picks().get(kind)), tier, Placeholder.unparsed("cap", kind.promoteTo()),
                            Placeholder.unparsed("cost", String.valueOf(kind.promotionCost()))));
        }
        entitlement.stats().forEach((stat, bonus) -> lines.add(messenger.render("business.npcs.details.given.stat",
                Placeholder.unparsed("stat", stat), amount(bonus))));
        entitlement.proficiencies().forEach((proficiency, tier) -> lines.add(messenger.render(
                "business.npcs.details.given.proficiency", Placeholder.unparsed("proficiency", proficiency),
                Placeholder.unparsed("tier", tier))));
        entitlement.abilities().forEach(ability -> lines.add(messenger.render("business.npcs.details.given.ability",
                Placeholder.unparsed("ability", ability))));
        entitlement.abilityChoices().forEach((options, count) -> lines.add(messenger.render(
                "business.npcs.details.given.ability-choices", amount(count),
                Placeholder.unparsed("options", String.join(", ", options)))));
        return lines;
    }

    private static TagResolver amount(int amount) {
        return Placeholder.unparsed("amount", String.valueOf(amount));
    }

    /** @return how many NPCs the business has */
    int used(Business business) {
        return npcs.of(business).size();
    }

    /** @return how many NPCs the business can have, counting NPCs kept over a source's limit */
    int capacity(Business business) {
        List<NpcSlotGroup> groups = npcs.slots(business);
        return groups.stream().mapToInt(group -> Math.max(group.capacity(), group.npcs().size())).sum();
    }

    void openList(Player player, Business business) {
        new NpcsMenu(this, menus.context(player, business)).open(player);
    }
}
