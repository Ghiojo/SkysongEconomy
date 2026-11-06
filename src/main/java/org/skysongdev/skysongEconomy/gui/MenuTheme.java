package org.skysongdev.skysongEconomy.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.gui.layout.MenuIcons;
import org.skysongdev.skysongEconomy.messaging.MessageCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Builds menu items: the shared buttons from {@code menus.yml}, and any item named and
 * described by {@code messages.yml} texts. Names and lore are never italic, so they read clearly.
 */
public final class MenuTheme {
    private final Messenger messenger;
    private final MessageCatalog messages;
    private final Material border;
    private final Material previousPage;
    private final Material nextPage;
    private final Material back;
    private final Material close;
    private final Material confirm;
    private final Material cancel;

    /** Unknown material names fall back to a stone item, with a warning. */
    public MenuTheme(MenuIcons icons, Messenger messenger, MessageCatalog messages, Logger logger) {
        this.messenger = messenger;
        this.messages = messages;
        this.border = material(icons.border(), MenuIcons.FILE, logger);
        this.previousPage = material(icons.previousPage(), MenuIcons.FILE, logger);
        this.nextPage = material(icons.nextPage(), MenuIcons.FILE, logger);
        this.back = material(icons.back(), MenuIcons.FILE, logger);
        this.close = material(icons.close(), MenuIcons.FILE, logger);
        this.confirm = material(icons.confirm(), MenuIcons.FILE, logger);
        this.cancel = material(icons.cancel(), MenuIcons.FILE, logger);
    }

    /**
     * @param nameKey the {@code messages.yml} key of the item's name
     * @param loreKey the key of its lore lines, or {@code null} for none
     */
    public ItemStack item(Material material, String nameKey, @Nullable String loreKey, TagResolver... placeholders) {
        Component name = messenger.render(nameKey, placeholders);
        List<Component> lore = loreKey == null ? List.of() : messages.lines(loreKey).stream()
                .map(line -> messenger.renderText(line, placeholders))
                .toList();
        return item(material, name, lore);
    }

    /** @return an item with exactly this name and lore */
    public ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name));
        meta.lore(lore.stream().map(MenuTheme::plain).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    public MenuButton border() {
        return MenuButton.display(item(border, Component.empty(), List.of()));
    }

    public MenuButton previousPage(Consumer<MenuClick> onClick) {
        return MenuButton.of(item(previousPage, "menu.previous-page", null), onClick);
    }

    public MenuButton nextPage(Consumer<MenuClick> onClick) {
        return MenuButton.of(item(nextPage, "menu.next-page", null), onClick);
    }

    public MenuButton back(Consumer<MenuClick> onClick) {
        return MenuButton.of(item(back, "menu.back", null), onClick);
    }

    public MenuButton close() {
        return MenuButton.of(item(close, "menu.close", null), click -> click.player().closeInventory());
    }

    public MenuButton confirm(Consumer<MenuClick> onClick) {
        return MenuButton.of(item(confirm, "menu.confirm", null), onClick);
    }

    public MenuButton cancel(Consumer<MenuClick> onClick) {
        return MenuButton.of(item(cancel, "menu.cancel", null), onClick);
    }

    /** @return the menu title for a {@code messages.yml} key */
    public Component title(String key, TagResolver... placeholders) {
        return messenger.render(key, placeholders);
    }

    private static Component plain(Component text) {
        return text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * @param file the config file that names it, for the warning
     * @return the item with that Minecraft name; stone, with a warning, when there is none
     */
    public static Material material(String name, String file, Logger logger) {
        Material material = Material.matchMaterial(name);
        if (material == null || !material.isItem()) {
            logger.warning(file + ": '" + name + "' is not an item, using STONE");
            return Material.STONE;
        }
        return material;
    }
}
