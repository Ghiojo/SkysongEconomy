package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepChests;
import org.skysongdev.skysongEconomy.upkeep.UpkeepProfile;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;
import org.skysongdev.skysongEconomy.upkeep.UpkeepStock;
import org.skysongdev.skysongEconomy.upkeep.UpkeepText;
import org.skysongdev.skysongnpcs.api.SkysongNpcsApi;
import org.skysongdev.skysongnpcs.api.access.NpcAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Upkeep button, {@code skysongeconomy:upkeep}, in the menu SkysongNPCs opens on an NPC. It
 * shows only on NPCs with an upkeep, to anyone who can open their menu: green while maintained,
 * red once unmaintained, with the cost, the next cycle and what the stock covers. A click opens
 * the NPC's upkeep chest.
 */
public final class UpkeepMenuButton {
    /** The button's name; its id in SkysongNPCs' menus.yml is {@code skysongeconomy:upkeep}. */
    public static final String NAME = "upkeep";
    /** Next to the Pickup button in SkysongNPCs' default three-row menu. */
    static final int SLOT = 23;
    /** The most cycles the button counts the stock for. */
    static final int MAX_CYCLES_SHOWN = 99;

    private final UpkeepService upkeep;
    private final UpkeepStock stock;
    private final UpkeepChests chests;
    private final UpkeepText text;
    private final Messenger messenger;

    public UpkeepMenuButton(UpkeepService upkeep, UpkeepStock stock, UpkeepChests chests, UpkeepText text,
                            Messenger messenger) {
        this.upkeep = upkeep;
        this.stock = stock;
        this.chests = chests;
        this.text = text;
        this.messenger = messenger;
    }

    /** Adds the button to every NPC's menu; SkysongNPCs removes it when this plugin disables. */
    public void register(Plugin plugin, SkysongNpcsApi npcs) {
        npcs.menu().registerButton(plugin, NAME, SLOT, context -> icon(context.npc().id()),
                context -> upkeep.upkeep(context.npc().id()).isPresent(),
                click -> {
                    click.viewer().closeInventory();
                    chests.open(click.viewer(), click.npc().id());
                });
    }

    /** @return whether a player may take items out of an NPC's upkeep chest: its {@code inventory-edit} action */
    public static boolean mayTakeOut(SkysongNpcsApi npcs, Player player, UUID npc) {
        return npcs.access().may(player, npc, NpcAction.INVENTORY_EDIT);
    }

    ItemStack icon(UUID npc) {
        Optional<NpcUpkeep> found = upkeep.upkeep(npc);
        Optional<UpkeepProfile> profile = upkeep.profile(npc);
        boolean maintained = found.map(u -> u.state() == UpkeepState.MAINTAINED).orElse(true);
        ItemStack icon = new ItemStack(maintained ? Material.BARREL : Material.REDSTONE_BLOCK);
        List<Component> lore = new ArrayList<>();
        if (found.isPresent() && profile.isPresent()) {
            NpcUpkeep current = found.get();
            TagResolver state = text.state(current.state());
            lore.add(line("upkeep.button.state", state));
            lore.add(line("upkeep.button.cost", text.cost(profile.get())));
            if (maintained) {
                int cycles = upkeep.payment().cyclesCovered(profile.get().cost(), stock.view(npc), MAX_CYCLES_SHOWN);
                lore.add(line("upkeep.button.next", text.next(current), text.cycles(cycles, MAX_CYCLES_SHOWN)));
            } else {
                lore.add(line("upkeep.button.unmaintained", text.since(current)));
            }
        }
        lore.add(line("upkeep.button.hint"));
        icon.editMeta(meta -> {
            meta.displayName(line("upkeep.button.name"));
            meta.lore(lore);
        });
        return icon;
    }

    private Component line(String key, TagResolver... placeholders) {
        return messenger.render(key, placeholders).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
