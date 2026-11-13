package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModuleOption;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.gui.PagedMenu;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;

/**
 * The modules one skill's free pick can take. A module the business can pick asks for
 * confirmation, because a pick is permanent; one it can't says why.
 */
final class ModulePoolMenu extends PagedMenu<ModuleOption> {
    private final ModuleScreens screens;
    private final BusinessMenuContext context;
    private final String skill;

    ModulePoolMenu(ModuleScreens screens, BusinessMenuContext context, String skill) {
        super(screens.menus().theme(), screens.menus().theme().title("business.modules.pool.title", screens.skill(skill)));
        this.screens = screens;
        this.context = context;
        this.skill = skill;
    }

    @Override
    protected List<ModuleOption> items() {
        return screens.modules().pool(context.business(), skill);
    }

    @Override
    protected MenuButton button(ModuleOption option) {
        Module module = option.module();
        Messenger messenger = screens.messenger();
        if (!option.available()) {
            Component why = messenger.render(option.blocked().messageKey(),
                    Messenger.unparsed(BusinessModules.blockedPlaceholders(module)));
            return MenuButton.display(theme.item(Material.BARRIER, screens.name(module),
                    screens.lore(module, List.of(Component.empty(), why))));
        }
        return MenuButton.of(theme.item(ModuleScreens.icon(module), screens.name(module),
                        screens.lore(module, List.of(Component.empty(), messenger.render("business.modules.pool.click")))),
                click -> confirm(click.player(), module));
    }

    @Override
    protected @Nullable MenuButton header() {
        return MenuButton.display(theme.item(Material.BOOKSHELF, "business.modules.pool.header.name",
                "business.modules.pool.header.lore", screens.skill(skill),
                Placeholder.unparsed("free", String.valueOf(screens.modules().free(context.business(), skill)))));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.openList(click.player(), context.business()));
    }

    private void confirm(Player player, Module module) {
        MenuTheme theme = screens.menus().theme();
        Business business = context.business();
        ItemStack summary = theme.item(ModuleScreens.icon(module), screens.name(module), screens.lore(module,
                List.of(Component.empty(), screens.messenger().render("business.modules.confirm.permanent"))));
        new ConfirmMenu(theme, theme.title("business.modules.confirm.title"), summary,
                click -> pick(click.player(), module),
                click -> new ModulePoolMenu(screens, screens.menus().context(click.player(), business), skill)
                        .open(click.player())).open(player);
    }

    private void pick(Player player, Module module) {
        Business business = context.business();
        // Asked again: the confirm menu may have been open while the business changed hands.
        if (!screens.menus().context(player, business).actsAsOwner()) {
            screens.messenger().send(player, "business.modules.not-owner");
            return;
        }
        try {
            screens.modules().pick(business, skill, module.id());
            screens.messenger().send(player, "business.modules.picked", Placeholder.component("module", screens.name(module)),
                    Placeholder.unparsed("business", business.displayName()));
        } catch (BusinessException refusal) {
            screens.messenger().send(player, refusal);
        }
        screens.openList(player, business);
    }
}
