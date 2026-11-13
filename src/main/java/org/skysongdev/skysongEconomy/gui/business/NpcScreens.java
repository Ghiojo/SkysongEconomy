package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleNpcRules;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSlotGroup;
import org.skysongdev.skysongEconomy.business.npc.NpcStatus;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.access.BusinessNpcAccess;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitSources;
import org.skysongdev.skysongEconomy.business.npc.benefit.Entitlement;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongEconomy.business.npc.benefit.PickKind;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.Set;

/**
 * What the NPC menus share: the services they read and the texts they all write the same way.
 *
 * @param npcs     the NPCs each business has
 * @param progression what the business owes each NPC
 * @param access   what each access group may do with the NPCs
 * @param gateway  SkysongNPCs, for names, menus and teleporting
 * @param sheets   what each NPC still has to choose
 * @param settings {@code businesses/npcs.yml}
 * @param skills   skill names
 * @param moduleNames a module's name by its id, as MiniMessage; empty for a module that has no file
 */
record NpcScreens(BusinessMenus menus, BusinessNpcs npcs, NpcProgression progression, BusinessNpcAccess access,
                  NpcGateway gateway, NpcSheetGateway sheets,
                  NpcSettings settings, SkillCatalog skills, Function<String, Optional<String>> moduleNames,
                  Messenger messenger) {
    /** @return whether the viewer may create NPCs: who acts as the owner, or a group with {@code manage-npcs} */
    boolean mayCreate(BusinessMenuContext context) {
        return context.actsAsOwner()
                || context.character().filter(character -> npcs.mayCreate(context.business(), character)).isPresent();
    }

    /**
     * @return whether the viewer may change what groups may do with the NPCs: the owner, staff,
     *         or a group with {@code edit-npc-access}
     */
    boolean mayEditAccess(BusinessMenuContext context) {
        return context.canManage()
                || context.character().filter(character -> access.mayEdit(context.business(), character)).isPresent();
    }

    /** @return what a group may do with the NPCs, as one line of text */
    Component allowed(Set<String> actions) {
        return actions.isEmpty()
                ? messenger.render("business.npcs.access.nothing")
                : Component.text(String.join(", ", actions));
    }

    void openAccess(Player player, Business business) {
        new NpcAccessGroupsMenu(this, menus.context(player, business)).open(player);
    }

    /** @return what gives a slot, as players read it: the skill's name for a skill, the module's for a module */
    Component sourceName(SlotSource source) {
        if (source.isSkill()) {
            return messenger.render("business.npcs.source.skill", Placeholder.parsed("skill", skills.displayName(source.id())));
        }
        Optional<String> module = source.isModule() ? moduleNames.apply(source.id()) : Optional.empty();
        return module.isPresent()
                ? messenger.render("business.npcs.source.module", Placeholder.parsed("module", module.get()))
                : messenger.render("business.npcs.source.other", Placeholder.unparsed("source", source.key()));
    }

    /**
     * @return the card SkysongNPCs' own NPC menu shows for a business NPC: its business, the
     *         slot it fills, and the modules that give it something; {@code null} for any other NPC
     */
    @Nullable ItemStack card(UUID npcId) {
        BusinessNpc npc = npcs.find(npcId).orElse(null);
        Business business = npc == null ? null : menus.businesses().find(npc.businessId()).orElse(null);
        if (business == null) {
            return null;
        }
        List<Component> lore = new ArrayList<>();
        lore.add(messenger.render("business.npcs.card.business", Placeholder.unparsed("business", business.displayName())));
        lore.add(messenger.render("business.npcs.card.slot", Placeholder.component("source", sourceName(npc.source()))));
        String own = BenefitSources.of(npc.source());
        progression.owedBySource(business, npc).forEach((source, owed) -> {
            if (!owed.isEmpty() && !source.equals(own)) {
                ModuleNpcRules.moduleOf(source).ifPresent(id -> lore.add(messenger.render("business.npcs.card.module",
                        Placeholder.parsed("module", moduleNames.apply(id).orElse(id)))));
            }
        });
        return menus.theme().item(Material.BELL, messenger.render("business.npcs.card.name"), lore);
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
