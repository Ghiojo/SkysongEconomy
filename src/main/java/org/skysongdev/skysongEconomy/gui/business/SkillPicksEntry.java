package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.Optional;

/** The main menu's Skill picks button, which opens {@link SkillPicksMenu}. Shown to everyone. */
public final class SkillPicksEntry implements MainMenuEntry {
    private final BusinessMenus menus;
    private final BusinessService businesses;
    private final SkillCatalog skills;
    private final Messenger messenger;

    public SkillPicksEntry(BusinessMenus menus, BusinessService businesses, SkillCatalog skills, Messenger messenger) {
        this.menus = menus;
        this.businesses = businesses;
        this.skills = skills;
        this.messenger = messenger;
    }

    @Override
    public int slot() {
        return MainMenuSlots.SKILL_PICKS;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        return Optional.of(MenuButton.of(
                menus.theme().item(Material.BOOKSHELF, "business.picks.button.name", "business.picks.button.lore",
                        Placeholder.unparsed("used", String.valueOf(business.picks().size())),
                        Placeholder.unparsed("tier", String.valueOf(business.tier()))),
                click -> new SkillPicksMenu(menus, businesses, skills, messenger, menus.context(click.player(), business))
                        .open(click.player())));
    }
}
