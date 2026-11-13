package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleShare;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.gui.PagedMenu;

import java.util.List;

/**
 * Chooses which NPCs receive a module benefit that goes to a number of NPCs. It lists the NPCs
 * that can still be chosen; a choice asks for confirmation, because it is permanent for that NPC.
 */
final class ModuleAssignMenu extends PagedMenu<BusinessNpc> {
    private final ModuleScreens screens;
    private final BusinessMenuContext context;
    private final ModuleShare share;

    ModuleAssignMenu(ModuleScreens screens, BusinessMenuContext context, ModuleShare share) {
        super(screens.menus().theme(), screens.menus().theme().title("business.modules.assign.title",
                Placeholder.component("module", screens.name(share.module()))));
        this.screens = screens;
        this.context = context;
        this.share = share;
    }

    @Override
    protected List<BusinessNpc> items() {
        return screens.npcs().map(side -> side.targets().eligible(context.business(), share)).orElse(List.of());
    }

    @Override
    protected MenuButton button(BusinessNpc npc) {
        return MenuButton.of(theme.item(Material.ARMOR_STAND, "business.modules.assign.npc.name",
                        "business.modules.assign.npc.lore", Placeholder.unparsed("npc", screens.name(npc))),
                click -> confirm(click.player(), npc));
    }

    @Override
    protected @Nullable MenuButton header() {
        return MenuButton.display(theme.item(ModuleScreens.icon(share.module()), screens.name(share.module()),
                screens.lore(share.module(), List.of(Component.empty(), screens.messenger().render(
                        "business.modules.assign.header", Placeholder.unparsed("count", String.valueOf(share.open())))))));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.openList(click.player(), context.business()));
    }

    private void confirm(Player player, BusinessNpc npc) {
        MenuTheme theme = screens.menus().theme();
        Business business = context.business();
        ItemStack summary = theme.item(Material.ARMOR_STAND, "business.modules.assign.confirm.name",
                "business.modules.assign.confirm.lore", Placeholder.unparsed("npc", screens.name(npc)),
                Placeholder.component("module", screens.name(share.module())));
        new ConfirmMenu(theme, theme.title("business.modules.assign.confirm.title"), summary,
                click -> choose(click.player(), npc),
                click -> screens.openAssignOrList(click.player(), business, share.pick())).open(player);
    }

    private void choose(Player player, BusinessNpc npc) {
        Business business = context.business();
        // Asked again: the confirm menu may have been open while the business changed hands.
        if (!screens.menus().context(player, business).actsAsOwner()) {
            screens.messenger().send(player, "business.modules.not-owner");
            return;
        }
        screens.npcs().ifPresent(side -> {
            try {
                side.targets().choose(business, share, npc.npc());
                side.progression().sync(business, npc);
                screens.messenger().send(player, "business.modules.assign.chosen", Placeholder.unparsed("npc", screens.name(npc)),
                        Placeholder.component("module", screens.name(share.module())));
            } catch (BusinessException refusal) {
                screens.messenger().send(player, refusal);
            }
        });
        screens.openAssignOrList(player, business, share.pick());
    }
}
