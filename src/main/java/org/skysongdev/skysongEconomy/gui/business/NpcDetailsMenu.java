package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.gui.BorderedMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;

import java.util.function.Consumer;

/**
 * What a viewer can do with one of a business's NPCs. Everyone can open the NPC's own
 * SkysongNPCs menu, which shows them only what their access allows. Whoever acts as the owner
 * can also open its setup and choices, and bring it to them.
 */
final class NpcDetailsMenu extends BorderedMenu {
    private static final int OPEN = BorderedLayout.slot(2, 2);
    private static final int CHOICES = BorderedLayout.slot(2, 4);
    private static final int TELEPORT = BorderedLayout.slot(2, 6);

    private final NpcScreens screens;
    private final BusinessMenuContext context;
    private final BusinessNpc npc;

    NpcDetailsMenu(NpcScreens screens, BusinessMenuContext context, BusinessNpc npc) {
        super(screens.menus().theme(), screens.menus().theme().title("business.npcs.details.title",
                Placeholder.unparsed("npc", screens.name(npc))));
        this.screens = screens;
        this.context = context;
        this.npc = npc;
    }

    @Override
    protected void drawContent() {
        Business business = context.business();
        set(OPEN, MenuButton.of(theme.item(Material.BOOK, "business.npcs.details.open.name",
                "business.npcs.details.open.lore"), click -> act(click.player(), this::openMenu)));
        if (context.actsAsOwner()) {
            set(CHOICES, MenuButton.of(theme.item(Material.NETHER_STAR, "business.npcs.details.choices.name",
                            "business.npcs.details.choices.lore", Placeholder.component("status", screens.status(npc))),
                    click -> act(click.player(), this::openChoices)));
            set(TELEPORT, MenuButton.of(theme.item(Material.ENDER_PEARL, "business.npcs.details.teleport.name",
                    "business.npcs.details.teleport.lore"), click -> act(click.player(), this::teleport)));
        }
        set(BorderedLayout.BACK, theme.back(click -> screens.openList(click.player(), business)));
    }

    /** Runs an action on the NPC if the business still has it; otherwise goes back to the list. */
    private void act(Player player, Consumer<Player> action) {
        boolean stillHere = screens.npcs().find(npc.npc())
                .filter(found -> found.businessId().equals(context.business().id())).isPresent();
        if (!stillHere) {
            gone(player);
            return;
        }
        action.accept(player);
    }

    private void openMenu(Player player) {
        if (!screens.gateway().openMenu(player.getUniqueId(), npc.npc())) {
            gone(player);
        }
    }

    private void openChoices(Player player) {
        if (!screens.gateway().openChoices(player.getUniqueId(), npc.npc())) {
            gone(player);
        }
    }

    private void teleport(Player player) {
        String name = screens.name(npc);
        player.closeInventory();
        screens.messenger().send(player, screens.gateway().teleportTo(npc.npc(), player.getUniqueId())
                ? "business.npcs.teleported" : "business.npcs.not-teleported", Placeholder.unparsed("npc", name));
    }

    private void gone(Player player) {
        screens.messenger().send(player, "business.npcs.gone");
        screens.openList(player, context.business());
    }
}
