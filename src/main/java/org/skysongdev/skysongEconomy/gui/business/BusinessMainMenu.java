package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.gui.BorderedMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;

/**
 * What a player sees when they right-click a business block: the business at the top, then one
 * button per feature. The owner can click the top item to rename the business.
 */
final class BusinessMainMenu extends BorderedMenu {
    private final BusinessMenus menus;
    private final BusinessMenuContext context;

    BusinessMainMenu(BusinessMenus menus, BusinessMenuContext context) {
        super(menus.theme(), menus.theme().title("business.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.menus = menus;
        this.context = context;
    }

    @Override
    protected void drawContent() {
        set(BorderedLayout.HEADER, header());
        for (MainMenuEntry entry : menus.entries()) {
            entry.button(context).ifPresent(button -> set(entry.slot(), button));
        }
        set(BorderedLayout.BACK, theme.close());
    }

    private MenuButton header() {
        Business business = context.business();
        TagResolver[] info = menus.headerPlaceholders(business);
        if (!context.canManage()) {
            return MenuButton.display(theme.item(Material.NAME_TAG, "business.menu.header.name", "business.menu.header.lore", info));
        }
        return MenuButton.of(
                theme.item(Material.NAME_TAG, "business.menu.header.name", "business.menu.header.lore-manager", info),
                click -> menus.askNewName(click.player(), business));
    }
}
