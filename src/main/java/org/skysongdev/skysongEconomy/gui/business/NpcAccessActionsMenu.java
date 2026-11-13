package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;

import java.util.List;
import java.util.Set;

/**
 * NPC permissions, second step: every SkysongNPCs action for one access group. Green actions
 * are allowed; clicking one flips it for every NPC of the business at once.
 */
final class NpcAccessActionsMenu extends PagedMenu<String> {
    private final NpcScreens screens;
    private final BusinessMenuContext context;
    private final String group;

    NpcAccessActionsMenu(NpcScreens screens, BusinessMenuContext context, String group) {
        super(screens.menus().theme(), screens.menus().theme().title("business.npcs.access.actions.title",
                Placeholder.unparsed("group", group)));
        this.screens = screens;
        this.context = context;
        this.group = group;
    }

    @Override
    protected List<String> items() {
        return List.copyOf(screens.access().actions().all());
    }

    @Override
    protected MenuButton button(String action) {
        TagResolver name = Placeholder.unparsed("action", action);
        return allowed().contains(action)
                ? MenuButton.of(theme.item(Material.LIME_DYE, "business.npcs.access.actions.allowed.name",
                        "business.npcs.access.actions.allowed.lore", name), click -> toggle(click.player(), action))
                : MenuButton.of(theme.item(Material.GRAY_DYE, "business.npcs.access.actions.denied.name",
                        "business.npcs.access.actions.denied.lore", name), click -> toggle(click.player(), action));
    }

    /** @return what the group may do; nothing when the group was deleted while the menu was open */
    private Set<String> allowed() {
        try {
            return screens.access().allowed(context.business(), group);
        } catch (BusinessException gone) {
            return Set.of();
        }
    }

    @Override
    protected @Nullable MenuButton header() {
        return MenuButton.display(theme.item(Material.NAME_TAG, "business.npcs.access.actions.header.name",
                "business.npcs.access.actions.header.lore", Placeholder.unparsed("group", group),
                Placeholder.unparsed("business", context.business().displayName())));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.openAccess(click.player(), context.business()));
    }

    private void toggle(Player player, String action) {
        Business business = context.business();
        // Asked again: the menu may have been open while the viewer lost the permission.
        if (!screens.mayEditAccess(screens.menus().context(player, business))) {
            screens.messenger().send(player, "business.npcs.access.not-allowed",
                    Placeholder.unparsed("business", business.displayName()));
            screens.openList(player, business);
            return;
        }
        try {
            screens.access().toggle(business, group, action);
            redraw();
        } catch (BusinessException refusal) {
            screens.messenger().send(player, refusal);
            screens.openAccess(player, business);
        }
    }
}
