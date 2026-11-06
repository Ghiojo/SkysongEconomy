package org.skysongdev.skysongEconomy.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A chest menu made of {@link MenuButton}s. Subclasses only say what goes in which slot;
 * {@link MenuListener} stops players from taking or placing items and runs the clicked button,
 * or {@link #clickedOwnInventory} for a click on the player's own inventory.
 * <p>
 * Each opening gets its own menu object, so a menu can keep state such as its page.
 */
public abstract class Menu implements InventoryHolder {
    private final int size;
    private final Component title;
    private final Map<Integer, MenuButton> buttons = new HashMap<>();
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
     * A click on the player's own inventory below the menu. Nothing moves either way; a menu
     * that takes items, such as a farm node's planting slot, overrides this to read the clicked
     * stack from {@code click.player().getInventory()}.
     *
     * @param slot the clicked slot of the player's inventory
     */
    protected void clickedOwnInventory(MenuClick click, int slot) {
    }

    /** Passes a click on the player's own inventory to {@link #clickedOwnInventory}. */
    final void clickOwn(int slot, Player player, ClickType type) {
        clickedOwnInventory(new MenuClick(player, type), slot);
    }

    @Override
    public final @NotNull Inventory getInventory() {
        return inventory;
    }
}
