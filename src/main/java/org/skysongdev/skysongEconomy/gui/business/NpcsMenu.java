package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.NpcSlotGroup;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.SpawnAt;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.gui.PagedMenu;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * A business's NPC slots: every NPC it has, then the free slots, source by source. Clicking an
 * NPC opens {@link NpcDetailsMenu}; whoever may create NPCs clicks a free slot to fill it.
 */
final class NpcsMenu extends PagedMenu<NpcsMenu.Slot> {
    private final NpcScreens screens;
    private final BusinessMenuContext context;

    /**
     * One NPC slot.
     *
     * @param source what gives the slot
     * @param npc    the NPC in it, {@code null} for a free slot
     */
    record Slot(SlotSource source, @Nullable BusinessNpc npc) {
    }

    NpcsMenu(NpcScreens screens, BusinessMenuContext context) {
        super(screens.menus().theme(), screens.menus().theme().title("business.npcs.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.screens = screens;
        this.context = context;
    }

    @Override
    protected List<Slot> items() {
        List<Slot> slots = new ArrayList<>();
        for (NpcSlotGroup group : screens.npcs().slots(context.business())) {
            group.npcs().forEach(npc -> slots.add(new Slot(group.source(), npc)));
            for (int i = 0; i < group.free(); i++) {
                slots.add(new Slot(group.source(), null));
            }
        }
        return slots;
    }

    @Override
    protected MenuButton button(Slot slot) {
        Business business = context.business();
        TagResolver source = Placeholder.component("source", screens.sourceName(slot.source()));
        BusinessNpc npc = slot.npc();
        if (npc != null) {
            return MenuButton.of(theme.item(Material.ARMOR_STAND, "business.npcs.menu.npc.name", "business.npcs.menu.npc.lore",
                            Placeholder.unparsed("npc", screens.name(npc)), source,
                            Placeholder.component("status", screens.status(npc))),
                    click -> new NpcDetailsMenu(screens, screens.menus().context(click.player(), business), npc)
                            .open(click.player()));
        }
        if (!screens.mayCreate(context)) {
            return MenuButton.display(theme.item(Material.GRAY_DYE, "business.npcs.menu.free.name",
                    "business.npcs.menu.free.lore", source));
        }
        return MenuButton.of(theme.item(Material.LIME_DYE, "business.npcs.menu.free-create.name",
                "business.npcs.menu.free-create.lore", source), click -> confirmCreate(click.player(), slot.source()));
    }

    @Override
    protected @Nullable MenuButton header() {
        Business business = context.business();
        return MenuButton.display(theme.item(Material.ARMOR_STAND, "business.npcs.menu.header.name",
                "business.npcs.menu.header.lore",
                Placeholder.unparsed("used", String.valueOf(screens.used(business))),
                Placeholder.unparsed("max", String.valueOf(screens.capacity(business)))));
    }

    /** The NPC permissions button, for whoever may change them. */
    @Override
    protected void drawSide() {
        if (screens.mayEditAccess(context)) {
            set(BorderedLayout.rightColumn().get(0), MenuButton.of(theme.item(Material.IRON_DOOR,
                            "business.npcs.access.button.name", "business.npcs.access.button.lore"),
                    click -> screens.openAccess(click.player(), context.business())));
        }
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.menus().openMain(click.player(), context.business()));
    }

    private void confirmCreate(Player player, SlotSource source) {
        MenuTheme theme = screens.menus().theme();
        Business business = context.business();
        ItemStack summary = theme.item(Material.ARMOR_STAND, "business.npcs.confirm.summary.name",
                "business.npcs.confirm.summary.lore", Placeholder.component("source", screens.sourceName(source)),
                Placeholder.component("place", screens.messenger().render(screens.settings().spawnAt() == SpawnAt.PLAYER
                        ? "business.npcs.confirm.place-player" : "business.npcs.confirm.place-business")));
        new ConfirmMenu(theme, theme.title("business.npcs.confirm.title"), summary,
                click -> create(click.player(), source),
                click -> screens.openList(click.player(), business)).open(player);
    }

    /** Creates the NPC and takes the player straight to its setup. */
    private void create(Player player, SlotSource source) {
        Business business = context.business();
        // Asked again: the confirm menu may have been open while a group lost its permission.
        if (!screens.mayCreate(screens.menus().context(player, business))) {
            screens.messenger().send(player, "business.npcs.not-allowed");
            return;
        }
        try {
            BusinessNpc created = screens.npcs().create(business, source, player.getUniqueId());
            screens.messenger().send(player, "business.npcs.created",
                    Placeholder.unparsed("business", business.displayName()));
            if (!screens.gateway().openChoices(player.getUniqueId(), created.npc())) {
                screens.openList(player, business);
            }
        } catch (BusinessException refusal) {
            screens.messenger().send(player, refusal);
            screens.openList(player, business);
        }
    }
}
