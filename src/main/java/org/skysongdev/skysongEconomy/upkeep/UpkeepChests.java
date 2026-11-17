package org.skysongdev.skysongEconomy.upkeep;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The upkeep chests players open from an NPC's menu: one shared chest per NPC, so two players
 * looking at the same NPC see the same items. While a chest is open its contents are the NPC's
 * stock: every change is saved, and cycles settled meanwhile show in it at once. Must be used on
 * the server's main thread.
 */
public final class UpkeepChests {
    private final UpkeepService upkeep;
    private final StockedItems stock;
    private final MaterialIdResolver materialIds;
    private final Function<UUID, Component> titles;
    private final Map<UUID, Chest> open = new HashMap<>();

    /**
     * @param titles the chest's title for an NPC
     */
    public UpkeepChests(UpkeepService upkeep, StockedItems stock, MaterialIdResolver materialIds,
                        Function<UUID, Component> titles) {
        this.upkeep = Objects.requireNonNull(upkeep, "upkeep");
        this.stock = Objects.requireNonNull(stock, "stock");
        this.materialIds = Objects.requireNonNull(materialIds, "materialIds");
        this.titles = Objects.requireNonNull(titles, "titles");
    }

    /** An open upkeep chest: the NPC it belongs to, and who may take items out of it. */
    public final class Chest implements InventoryHolder {
        private final UUID npc;
        private final Inventory inventory;

        private Chest(UUID npc, int slots) {
            this.npc = npc;
            this.inventory = Bukkit.createInventory(this, slots, titles.apply(npc));
            fill();
        }

        /** @return the NPC's Citizens UUID */
        public UUID npc() {
            return npc;
        }

        /** @return whether an item may go in: it pays an entry of the NPC's cost */
        public boolean accepts(ItemStack item) {
            if (item == null || item.isEmpty()) {
                return true;
            }
            Optional<UpkeepProfile> profile = upkeep.profile(npc);
            Optional<String> materialId = materialIds.fromItem(item);
            return profile.isPresent() && materialId.isPresent()
                    && upkeep.payment().accepts(profile.get().cost(), materialId.get());
        }

        /** Saves what the chest holds as the NPC's stock. */
        public void save() {
            stock.replace(npc, Arrays.stream(inventory.getContents()).filter(Objects::nonNull).toList());
        }

        private void fill() {
            inventory.clear();
            List<ItemStack> items = stock.items(npc);
            List<ItemStack> overflow = new ArrayList<>();
            for (ItemStack item : items) {
                overflow.addAll(inventory.addItem(item).values());
            }
            if (!overflow.isEmpty()) {
                // Kept in the stock, not lost: the profile's chest got smaller than what it holds.
                List<ItemStack> all = new ArrayList<>(Arrays.stream(inventory.getContents()).filter(Objects::nonNull).toList());
                all.addAll(overflow);
                stock.replace(npc, all);
            }
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    /**
     * Opens the NPC's upkeep chest for a player, sharing it with anyone who already has it open.
     *
     * @return false, opening nothing, when the NPC has no upkeep
     */
    public boolean open(Player player, UUID npc) {
        Optional<UpkeepProfile> profile = upkeep.profile(npc);
        if (profile.isEmpty()) {
            return false;
        }
        Chest chest = open.get(npc);
        if (chest == null || chest.getInventory().getSize() != profile.get().slots()) {
            chest = new Chest(npc, profile.get().slots());
            open.put(npc, chest);
        }
        player.openInventory(chest.getInventory());
        return true;
    }

    /** Saves a chest and forgets it once its last viewer closed it. */
    public void closed(Chest chest, HumanEntity viewer) {
        chest.save();
        List<HumanEntity> left = new ArrayList<>(chest.getInventory().getViewers());
        left.remove(viewer);
        if (left.isEmpty()) {
            open.remove(chest.npc(), chest);
        }
    }

    /** Shows what each open chest's stock holds now, such as after a cycle ate from it. */
    public void refresh() {
        open.values().forEach(Chest::fill);
    }

    /** Saves every open chest, so a cycle settles from what players put in a moment ago. */
    public void saveAll() {
        open.values().forEach(Chest::save);
    }

    /** Closes the NPC's chest for everyone, such as when it loses its upkeep. */
    public void close(UUID npc) {
        Chest chest = open.remove(npc);
        if (chest != null) {
            chest.save();
            List.copyOf(chest.getInventory().getViewers()).forEach(HumanEntity::closeInventory);
        }
    }

    /** Saves and closes every chest, as the plugin stops. */
    public void closeAll() {
        List.copyOf(open.keySet()).forEach(this::close);
    }
}
