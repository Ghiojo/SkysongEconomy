package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.access.BusinessNpcAccess;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
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

    /**
     * The ways into SkysongNPCs the menus use.
     *
     * @param npcs   names, menus and teleporting
     * @param sheets what each NPC still has to choose
     */
    public record Gateways(NpcGateway npcs, NpcSheetGateway sheets) {
    }

    /**
     * What the menus show and change.
     *
     * @param npcs        the NPCs each business has
     * @param progression what the business owes each NPC, shown in the NPC's details
     * @param access      what each access group may do with the NPCs
     */
    public record Services(BusinessNpcs npcs, NpcProgression progression, BusinessNpcAccess access) {
    }

    public NpcsEntry(BusinessMenus menus, Services services, Gateways gateways,
                     NpcSettings settings, SkillCatalog skills, Messenger messenger) {
        this.screens = new NpcScreens(menus, services.npcs(), services.progression(), services.access(),
                gateways.npcs(), gateways.sheets(), settings, skills, messenger);
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
