package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NPC permissions, first step: the business's access groups and what each may do with its NPCs.
 * Clicking a group opens {@link NpcAccessActionsMenu} to change it.
 */
final class NpcAccessGroupsMenu extends PagedMenu<Map.Entry<String, Set<String>>> {
    private final NpcScreens screens;
    private final BusinessMenuContext context;

    NpcAccessGroupsMenu(NpcScreens screens, BusinessMenuContext context) {
        super(screens.menus().theme(), screens.menus().theme().title("business.npcs.access.groups.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.screens = screens;
        this.context = context;
    }

    @Override
    protected List<Map.Entry<String, Set<String>>> items() {
        return List.copyOf(screens.access().grants(context.business()).entrySet());
    }

    @Override
    protected MenuButton button(Map.Entry<String, Set<String>> group) {
        return MenuButton.of(theme.item(Material.NAME_TAG, "business.npcs.access.groups.group.name",
                        "business.npcs.access.groups.group.lore", Placeholder.unparsed("group", group.getKey()),
                        Placeholder.component("actions", screens.allowed(group.getValue()))),
                click -> new NpcAccessActionsMenu(screens,
                        screens.menus().context(click.player(), context.business()), group.getKey()).open(click.player()));
    }

    @Override
    protected @Nullable MenuButton header() {
        return MenuButton.display(theme.item(Material.IRON_DOOR, "business.npcs.access.groups.header.name",
                "business.npcs.access.groups.header.lore"));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.openList(click.player(), context.business()));
    }
}
