package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.Ledger;
import org.skysongdev.skysongEconomy.trade.LedgerRegistry;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;

import java.util.List;

/**
 * What the Ledgers windows share: the services they use, and how they open each other. Ledger
 * names, descriptions and lore come from the ledger files as MiniMessage.
 */
final class LedgerScreens {
    final BusinessMenus menus;
    final LedgerRegistry ledgers;
    final LedgerSelections selections;
    final EconomyLogisticsSettings settings;
    final Wallet wallet;
    final Messenger messenger;

    LedgerScreens(BusinessMenus menus, LedgerRegistry ledgers, LedgerSelections selections,
                  EconomyLogisticsSettings settings, Wallet wallet, Messenger messenger) {
        this.menus = menus;
        this.ledgers = ledgers;
        this.selections = selections;
        this.settings = settings;
        this.wallet = wallet;
        this.messenger = messenger;
    }

    void openList(Player player, Business business) {
        new LedgersMenu(this, menus.context(player, business)).open(player);
    }

    void openDetails(Player player, Business business, Ledger ledger) {
        new LedgerDetailsMenu(this, menus.context(player, business), ledger).open(player);
    }

    /** @return the placeholders {@code used}, {@code max} and {@code fine} */
    TagResolver slots(Business business) {
        int max = settings.limits(business).ledgers();
        return TagResolver.resolver(
                Placeholder.unparsed("used", String.valueOf(max - selections.freeSlots(business))),
                Placeholder.unparsed("max", String.valueOf(max)),
                Placeholder.unparsed("fine", wallet.format(selections.removalFine())));
    }

    /** @return the {@code ledger} placeholder: its name, with its formatting */
    static TagResolver name(Ledger ledger) {
        return Placeholder.parsed("ledger", ledger.displayName());
    }

    /** @return MiniMessage lines from a ledger file, rendered */
    List<Component> render(List<String> lines) {
        return lines.stream().map(messenger::renderText).toList();
    }

    /** @return the item material named in a ledger file, or paper when it isn't an item */
    static Material material(String name) {
        Material material = Material.matchMaterial(name);
        return material != null && material.isItem() ? material : Material.PAPER;
    }
}
