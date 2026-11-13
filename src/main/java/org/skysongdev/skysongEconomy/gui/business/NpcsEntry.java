package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.Optional;

/**
 * The main menu's NPCs button, which opens {@link NpcsMenu}. Shown when the business has an NPC
 * slot or an NPC; what each viewer can do there is decided slot by slot.
 */
public final class NpcsEntry implements MainMenuEntry {
    private final NpcScreens screens;

    public NpcsEntry(BusinessMenus menus, BusinessNpcs npcs, NpcGateway gateway, NpcSheetGateway sheets,
                     NpcSettings settings, SkillCatalog skills, Messenger messenger) {
        this.screens = new NpcScreens(menus, npcs, gateway, sheets, settings, skills, messenger);
    }

    @Override
    public int slot() {
        return MainMenuSlots.NPCS;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (!screens.npcs().hasSlots(business)) {
            return Optional.empty();
        }
        return Optional.of(MenuButton.of(
                screens.menus().theme().item(Material.ARMOR_STAND, "business.npcs.button.name", "business.npcs.button.lore",
                        Placeholder.unparsed("used", String.valueOf(screens.used(business))),
                        Placeholder.unparsed("max", String.valueOf(screens.capacity(business)))),
                click -> screens.openList(click.player(), business)));
    }
}
