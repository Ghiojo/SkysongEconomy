package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModulePickGroup;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A business's module picks: every module it picked, then the free picks, skill by skill.
 * Whoever acts as the owner clicks a free pick to choose from that skill's pool in
 * {@link ModulePoolMenu}.
 */
final class ModulesMenu extends PagedMenu<ModulesMenu.Slot> {
    private final ModuleScreens screens;
    private final BusinessMenuContext context;

    /**
     * One module pick.
     *
     * @param skill the skill that gives the pick
     * @param pick  the module picked for it, {@code null} for a free pick
     */
    record Slot(String skill, @Nullable ModulePick pick) {
    }

    ModulesMenu(ModuleScreens screens, BusinessMenuContext context) {
        super(screens.menus().theme(), screens.menus().theme().title("business.modules.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.screens = screens;
        this.context = context;
    }

    @Override
    protected List<Slot> items() {
        List<Slot> slots = new ArrayList<>();
        for (ModulePickGroup group : screens.modules().groups(context.business())) {
            group.picks().forEach(pick -> slots.add(new Slot(group.skill(), pick)));
            for (int i = 0; i < group.free(); i++) {
                slots.add(new Slot(group.skill(), null));
            }
        }
        return slots;
    }

    @Override
    protected MenuButton button(Slot slot) {
        TagResolver skill = screens.skill(slot.skill());
        ModulePick pick = slot.pick();
        if (pick != null) {
            Optional<Module> module = screens.modules().module(pick.moduleId());
            if (module.isEmpty()) {
                return MenuButton.display(theme.item(Material.BARRIER, "business.modules.menu.missing.name",
                        "business.modules.menu.missing.lore", skill, Placeholder.unparsed("module", pick.moduleId())));
            }
            return MenuButton.display(theme.item(ModuleScreens.icon(module.get()), screens.name(module.get()),
                    screens.lore(module.get(), List.of(screens.messenger().render("business.modules.menu.picked-for", skill)))));
        }
        if (!context.actsAsOwner()) {
            return MenuButton.display(theme.item(Material.GRAY_DYE, "business.modules.menu.free.name",
                    "business.modules.menu.free.lore", skill));
        }
        Business business = context.business();
        return MenuButton.of(theme.item(Material.LIME_DYE, "business.modules.menu.free-pick.name",
                        "business.modules.menu.free-pick.lore", skill),
                click -> new ModulePoolMenu(screens, screens.menus().context(click.player(), business), slot.skill())
                        .open(click.player()));
    }

    @Override
    protected @Nullable MenuButton header() {
        Business business = context.business();
        return MenuButton.display(theme.item(Material.BOOKSHELF, "business.modules.menu.header.name",
                "business.modules.menu.header.lore",
                Placeholder.unparsed("picked", String.valueOf(screens.picked(business))),
                Placeholder.unparsed("max", String.valueOf(screens.total(business)))));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.menus().openMain(click.player(), context.business()));
    }
}
