package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleTargets;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.Optional;

/**
 * The main menu's Modules button, which opens {@link ModulesMenu}. Shown when the business has a
 * module pick or a module; only whoever acts as the owner can pick there.
 */
public final class ModulesEntry implements MainMenuEntry {
    private final ModuleScreens screens;

    public ModulesEntry(BusinessMenus menus, BusinessModules modules, SkillCatalog skills, Messenger messenger) {
        this.screens = new ModuleScreens(menus, modules, skills, messenger);
    }

    /**
     * Switches on choosing the NPCs for module benefits. Called once the business NPCs run.
     *
     * @param targets     which NPCs were chosen for which benefit
     * @param npcs        SkysongNPCs, for names
     * @param progression gives a chosen NPC its benefit at once
     */
    public void enableNpcs(ModuleTargets targets, NpcGateway npcs, NpcProgression progression) {
        screens.enableNpcs(new ModuleScreens.NpcSide(targets, npcs, progression));
    }

    @Override
    public int slot() {
        return MainMenuSlots.MODULES;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (!screens.modules().hasPicks(business)) {
            return Optional.empty();
        }
        return Optional.of(MenuButton.of(
                screens.menus().theme().item(Material.BOOKSHELF, "business.modules.button.name", "business.modules.button.lore",
                        Placeholder.unparsed("picked", String.valueOf(screens.picked(business))),
                        Placeholder.unparsed("max", String.valueOf(screens.total(business))),
                        Placeholder.unparsed("free", String.valueOf(screens.free(business)))),
                click -> screens.openList(click.player(), business)));
    }
}
