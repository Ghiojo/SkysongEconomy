package org.skysongdev.skysongEconomy.upkeep;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.items.ItemStackBytes;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.items.StoredItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The real item stacks stocked in each NPC for its upkeep, kept in memory and written through to
 * the {@link UpkeepStockRepository}. Stacks keep every name, lore line and tag they had. Must be
 * used on the server's main thread, since it reads items' {@code material_id} through SkysongItems.
 */
public final class StockedItems implements UpkeepStock {
    private final UpkeepStockRepository repository;
    private final MaterialIdResolver materialIds;
    private final Map<UUID, List<ItemStack>> byNpc = new HashMap<>();

    public StockedItems(UpkeepStockRepository repository, MaterialIdResolver materialIds) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.materialIds = Objects.requireNonNull(materialIds, "materialIds");
    }

    /**
     * Reads every NPC's stocked items.
     *
     * @param warnings told about NPCs whose saved items can't be read, which start empty
     * @return how many NPCs have items stocked
     */
    public int loadAll(Consumer<String> warnings) {
        byNpc.clear();
        repository.loadAll().forEach((npc, items) -> {
            try {
                byNpc.put(npc, new ArrayList<>(ItemStackBytes.restore(items)));
            } catch (RuntimeException e) {
                warnings.accept("The upkeep stock of the NPC " + npc + " can't be read and was left out: " + e.getMessage());
            }
        });
        return byNpc.size();
    }

    @Override
    public List<StockStack> view(UUID npc) {
        return byNpc.getOrDefault(npc, List.of()).stream()
                .map(stack -> new StockStack(materialIds.fromItem(stack).orElse(null), stack.getAmount()))
                .toList();
    }

    @Override
    public void take(UUID npc, List<Integer> taken) {
        List<ItemStack> stacks = byNpc.get(npc);
        if (stacks == null || stacks.size() != taken.size()) {
            throw new IllegalStateException("The upkeep stock of " + npc + " changed while it was being paid from");
        }
        List<ItemStack> left = new ArrayList<>(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            int amount = stack.getAmount() - taken.get(i);
            if (amount > 0) {
                ItemStack rest = stack.clone();
                rest.setAmount(amount);
                left.add(rest);
            }
        }
        replace(npc, left);
    }

    /** @return copies of the NPC's stocked stacks, in their order */
    public List<ItemStack> items(UUID npc) {
        return byNpc.getOrDefault(npc, List.of()).stream().map(ItemStack::clone).toList();
    }

    /**
     * Replaces the NPC's stocked stacks, such as after a player changed them in its upkeep chest.
     *
     * @param stacks the stacks, air and empty slots left out
     */
    public void replace(UUID npc, List<ItemStack> stacks) {
        List<ItemStack> kept = stacks.stream().filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::clone).toList();
        if (kept.isEmpty()) {
            if (byNpc.remove(npc) != null) {
                repository.delete(npc);
            }
            return;
        }
        byNpc.put(npc, new ArrayList<>(kept));
        repository.save(npc, ItemStackBytes.store(kept));
    }

    /**
     * Empties the NPC's stock.
     *
     * @return what it held, in their order
     */
    public List<ItemStack> takeAll(UUID npc) {
        List<ItemStack> held = items(npc);
        replace(npc, List.of());
        return held;
    }
}
