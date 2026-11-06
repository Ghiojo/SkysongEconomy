package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.ChatPrompts;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens business menus and holds what they share: the main menu's entries, the theme, and
 * renaming through chat.
 */
public final class BusinessMenus {
    /** Same as the /se business staff permission: staff may manage every business. */
    public static final String STAFF_PERMISSION = "skysongeconomy.business.admin";

    private final BusinessService businesses;
    private final BusinessTypeRegistry types;
    private final CharacterDirectory characters;
    private final MenuTheme theme;
    private final ChatPrompts prompts;
    private final Messenger messenger;
    private final List<MainMenuEntry> entries = new ArrayList<>();

    public BusinessMenus(BusinessService businesses, BusinessTypeRegistry types, CharacterDirectory characters,
                         MenuTheme theme, ChatPrompts prompts, Messenger messenger) {
        this.businesses = businesses;
        this.types = types;
        this.characters = characters;
        this.theme = theme;
        this.prompts = prompts;
        this.messenger = messenger;
    }

    /** Adds a button to every business's main menu. */
    public void addEntry(MainMenuEntry entry) {
        entries.add(entry);
    }

    /** Shows the business's main menu to the player. */
    public void openMain(Player player, Business business) {
        new BusinessMainMenu(this, context(player, business)).open(player);
    }

    /** @return who the player is to the business */
    public BusinessMenuContext context(Player player, Business business) {
        return new BusinessMenuContext(player, characters.active(player.getUniqueId()), business,
                player.hasPermission(STAFF_PERMISSION));
    }

    /** @return the shared menu look, which other features' menus reuse */
    public MenuTheme theme() {
        return theme;
    }

    BusinessTypeRegistry types() {
        return types;
    }

    List<MainMenuEntry> entries() {
        return List.copyOf(entries);
    }

    /** @return the placeholders of the header item: business, id, type, tier, owner, workers, max */
    TagResolver[] headerPlaceholders(Business business) {
        return new TagResolver[]{
                Placeholder.unparsed("business", business.displayName()),
                Placeholder.unparsed("id", business.id()),
                Placeholder.parsed("type", types.find(business.typeId()).map(BusinessType::displayName).orElse(business.typeId())),
                Placeholder.unparsed("tier", String.valueOf(business.tier())),
                Placeholder.unparsed("owner", business.owner().map(BusinessMenus::characterName)
                        .orElseGet(() -> messenger.plainText("business.server-owner"))),
                Placeholder.unparsed("workers", String.valueOf(business.workers().size())),
                Placeholder.unparsed("max", String.valueOf(businesses.maxWorkers(business)))};
    }

    /** Asks the owner for a new name in chat, then reopens the menu. */
    void askNewName(Player player, Business business) {
        prompts.ask(player, "prompt.rename", answer -> {
            try {
                businesses.rename(business, answer);
                messenger.send(player, "business.renamed", Placeholder.unparsed("business", business.displayName()));
            } catch (BusinessException refusal) {
                messenger.send(player, refusal);
            }
            openMain(player, business);
        });
    }

    private static String characterName(CharacterRef character) {
        String player = Bukkit.getOfflinePlayer(character.player()).getName();
        return player == null || player.equalsIgnoreCase(character.profile())
                ? character.profile()
                : character.profile() + " (" + player + ")";
    }
}
