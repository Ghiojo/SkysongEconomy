package org.skysongdev.skysongEconomy.gui.business;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

/** Where this plugin adds buttons to the menu SkysongNPCs opens when an NPC is right-clicked. */
public interface NpcMenuButtons {
    /**
     * Adds a button that only shows something.
     *
     * @param name the button's name; its id in SkysongNPCs' menus.yml is {@code skysongeconomy:<name>}
     * @param slot where it goes unless menus.yml says otherwise, counted from 0 at the top left
     * @param icon the item for an NPC, {@code null} to leave the button out of that NPC's menu
     */
    void addInfoButton(String name, int slot, Function<UUID, @Nullable ItemStack> icon);
}
