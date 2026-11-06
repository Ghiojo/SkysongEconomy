package org.skysongdev.skysongEconomy.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A chest menu made of {@link MenuButton}s. Subclasses only say what goes in which slot;
 * {@link MenuListener} stops players from taking or placing items and runs the clicked button.
 * <p>
 * A menu may also have input slots ({@link #isInputSlot}), where players put items in: the menu
 * decides how much of a stack it takes ({@link #accepts}), keeps it until it uses it, and gives
 * back whatever it still holds when it closes. Nothing is ever moved by the server itself, so
 * items can't be lost or copied by odd clicks.
 * <p>
 * Each opening gets its own menu object, so a menu can keep state such as its page.
 */
public abstract class Menu implements InventoryHolder {
    private final int size;
    private final Component title;
    private final Map<Integer, MenuButton> buttons = new HashMap<>();
    /** What players put in input slots, by slot. */
    private final Map<Integer, List<ItemStack>> held = new HashMap<>();
    private Inventory inventory;

    /**
     * @param size the number of slots, a multiple of 9 up to 54
     */
    protected Menu(int size, Component title) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("A menu has 9 to 54 slots, in rows of 9: " + size);
        }
        this.size = size;
        this.title = title;
    }

    /** Puts every button in place. Called on open and on {@link #redraw()}. */
    protected abstract void draw();

    /** Shows the menu to the player. */
    public final void open(Player player) {
        inventory = Bukkit.createInventory(this, size, title);
        redraw();
        player.openInventory(inventory);
    }

    /** Clears the menu and draws it again, e.g. after its data changed. */
    public final void redraw() {
        buttons.clear();
        inventory.clear();
        draw();
        held.forEach((slot, stacks) -> inventory.setItem(slot, stacks.getFirst().asQuantity(
                stacks.stream().mapToInt(ItemStack::getAmount).sum())));
    }

    /** Puts a button in a slot, replacing what was there. */
    protected final void set(int slot, MenuButton button) {
        buttons.put(slot, button);
        inventory.setItem(slot, button.icon());
    }

    protected final int size() {
        return size;
    }

    /** Runs the button in {@code slot}, if it has an action. */
    final void click(int slot, Player player, ClickType type) {
        MenuButton button = buttons.get(slot);
        if (button != null && button.onClick() != null) {
            button.onClick().accept(new MenuClick(player, type));
        }
    }

    /**
     * @return whether players may put items in that slot, such as a farm node's planting slot.
     *         None by default; such a slot keeps what was put in until the menu uses it, and
     *         gives the rest back when the menu closes.
     */
    protected boolean isInputSlot(int slot) {
        return false;
    }

    /**
     * Asked when a player puts a stack in an input slot, from their cursor or with a shift-click.
     * Tell the player why when taking none.
     *
     * @param offered the stack offered; don't change it
     * @param held    what the slot already holds
     * @return how many of {@code offered} to take, from 0 to its amount
     */
    protected int accepts(Player player, int slot, ItemStack offered, List<ItemStack> held) {
        return 0;
    }

    /** Told after items were put in an input slot, such as to plant once a pair is in. */
    protected void received(Player player, int slot) {
    }

    /** @return copies of what an input slot holds */
    protected final List<ItemStack> held(int slot) {
        return held.getOrDefault(slot, List.of()).stream().map(ItemStack::clone).toList();
    }

    /** Empties an input slot without giving anything back, once its items were used. */
    protected final void useHeld(int slot) {
        held.remove(slot);
    }

    /** Puts part of {@code offered} in the slot. @return what is left of it, or null when nothing is */
    final @Nullable ItemStack offer(int slot, Player player, ItemStack offered) {
        if (!isInputSlot(slot)) {
            return offered;
        }
        int take = Math.min(offered.getAmount(), Math.max(0, accepts(player, slot, offered.clone(), held(slot))));
        if (take == 0) {
            return offered;
        }
        held.computeIfAbsent(slot, ignored -> new ArrayList<>()).add(offered.asQuantity(take));
        received(player, slot);
        redraw();
        return take == offered.getAmount() ? null : offered.asQuantity(offered.getAmount() - take);
    }

    /** @return whether the slot holds anything */
    final boolean holds(int slot) {
        return held.containsKey(slot);
    }

    /** Gives everything an input slot holds back to the player. */
    final void giveBack(int slot, Player player) {
        List<ItemStack> stacks = held.remove(slot);
        if (stacks != null) {
            give(player, stacks);
            redraw();
        }
    }

    /** Gives everything every input slot holds back to the player, as the menu closes. */
    public final void giveEverythingBack(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        held.values().forEach(stacks::addAll);
        held.clear();
        give(player, stacks);
    }

    /** @return the input slots, lowest first, for a shift-click from the player's inventory */
    final List<Integer> inputSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (isInputSlot(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static void give(Player player, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return;
        }
        player.getInventory().addItem(stacks.toArray(ItemStack[]::new)).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    @Override
    public final @NotNull Inventory getInventory() {
        return inventory;
    }
}
