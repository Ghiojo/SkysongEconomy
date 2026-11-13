package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModulePickGroup;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleShare;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleTargets;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** What the module menus share: the services they read and how a module is drawn. */
final class ModuleScreens {
    private final BusinessMenus menus;
    private final BusinessModules modules;
    private final SkillCatalog skills;
    private final Messenger messenger;
    /** The modules' NPC side; {@code null} until the business NPCs run, and without SkysongNPCs. */
    private @Nullable NpcSide npcs;

    /**
     * What choosing NPCs for a module needs.
     *
     * @param targets     which NPCs were chosen for which benefit
     * @param gateway     SkysongNPCs, for names
     * @param progression gives a chosen NPC its benefit at once
     */
    record NpcSide(ModuleTargets targets, NpcGateway gateway, NpcProgression progression) {
    }

    /**
     * @param modules the modules each business picked
     * @param skills  skill names
     */
    ModuleScreens(BusinessMenus menus, BusinessModules modules, SkillCatalog skills, Messenger messenger) {
        this.menus = menus;
        this.modules = modules;
        this.skills = skills;
        this.messenger = messenger;
    }

    void enableNpcs(NpcSide side) {
        this.npcs = side;
    }

    BusinessMenus menus() {
        return menus;
    }

    BusinessModules modules() {
        return modules;
    }

    Messenger messenger() {
        return messenger;
    }

    /** @return the modules' NPC side, empty when business NPCs are off */
    Optional<NpcSide> npcs() {
        return Optional.ofNullable(npcs);
    }

    /** @return the first benefit of the pick that still has NPCs to choose, empty when there is none */
    Optional<ModuleShare> openShare(Business business, ModulePick pick) {
        return npcs().flatMap(side -> side.targets().shares(business, pick).stream()
                .filter(share -> share.open() > 0).findFirst());
    }

    /** @return how many NPCs are still to choose for the pick */
    int open(Business business, ModulePick pick) {
        return npcs().map(side -> side.targets().shares(business, pick).stream().mapToInt(ModuleShare::open).sum()).orElse(0);
    }

    /** @return the NPC's name, or its id when SkysongNPCs no longer knows it */
    String name(BusinessNpc npc) {
        return npcs().flatMap(side -> side.gateway().name(npc.npc())).orElse(npc.npc().toString());
    }

    /** @return the {@code skill} placeholder: the skill's name, with its formatting */
    TagResolver skill(String skillId) {
        return Placeholder.parsed("skill", skills.displayName(skillId));
    }

    /** @return the module's name, rendered */
    Component name(Module module) {
        return messenger.renderText(module.name());
    }

    /** @return the module's description, then the given lines */
    List<Component> lore(Module module, List<Component> after) {
        List<Component> lore = new ArrayList<>(module.description().stream().map(messenger::renderText).toList());
        lore.addAll(after);
        return lore;
    }

    /** @return the item named in a module file, or paper when it isn't an item */
    static Material icon(Module module) {
        Material material = Material.matchMaterial(module.icon());
        return material != null && material.isItem() ? material : Material.PAPER;
    }

    /** @return how many modules the business has */
    int picked(Business business) {
        return modules.of(business).size();
    }

    /** @return how many module picks the business has, counting modules kept over a skill's total */
    int total(Business business) {
        return modules.groups(business).stream().mapToInt(group -> Math.max(group.total(), group.picks().size())).sum();
    }

    /** @return how many module picks are still free */
    int free(Business business) {
        return modules.groups(business).stream().mapToInt(ModulePickGroup::free).sum();
    }

    void openList(Player player, Business business) {
        new ModulesMenu(this, menus.context(player, business)).open(player);
    }

    /**
     * Opens the menu to choose NPCs for the pick's next open benefit, when there is one and the
     * business has an NPC that can be chosen; otherwise the list of modules.
     */
    void openAssignOrList(Player player, Business business, ModulePick pick) {
        Optional<ModuleShare> share = openShare(business, pick)
                .filter(open -> !npcs().orElseThrow().targets().eligible(business, open).isEmpty());
        if (share.isPresent()) {
            new ModuleAssignMenu(this, menus.context(player, business), share.get()).open(player);
        } else {
            openList(player, business);
        }
    }
}
