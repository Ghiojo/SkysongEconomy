package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Every open receiving pier, to choose where an import is unloaded. Clicking a pier picks it; the
 * chosen one is marked. Closed piers aren't listed.
 */
final class PierMenu extends PagedMenu<Pier> {
    private final ImportScreens screens;
    private final Optional<String> chosen;
    private final BiConsumer<Player, Pier> onPick;
    private final Consumer<Player> onBack;

    /**
     * @param chosen the pier picked so far, if any
     * @param onPick runs with the pier clicked
     * @param onBack runs when the player goes back without picking
     */
    PierMenu(ImportScreens screens, Optional<String> chosen, BiConsumer<Player, Pier> onPick, Consumer<Player> onBack) {
        super(screens.menus.theme(), screens.menus.theme().title("business.imports.pier.menu.title"));
        this.screens = screens;
        this.chosen = chosen;
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected List<Pier> items() {
        return screens.piers.open();
    }

    @Override
    protected MenuButton button(Pier pier) {
        List<Component> lore = new ArrayList<>(pier.details().description().stream()
                .map(screens.messenger::renderText).toList());
        lore.add(Component.empty());
        boolean selected = chosen.filter(pier.id()::equals).isPresent();
        lore.add(screens.messenger.render(selected ? "business.imports.pier.menu.selected" : "business.imports.pier.menu.click"));
        return MenuButton.of(theme.item(selected ? Material.LIME_DYE : LedgerScreens.material(pier.details().icon()),
                screens.messenger.renderText(pier.details().displayName()), lore), click -> onPick.accept(click.player(), pier));
    }

    @Override
    protected @Nullable MenuButton header() {
        return MenuButton.display(theme.item(Material.OAK_BOAT, "business.imports.pier.menu.header.name",
                "business.imports.pier.menu.header.lore"));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> onBack.accept(click.player()));
    }
}
