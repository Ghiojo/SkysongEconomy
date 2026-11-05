package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.prices.modifier.ModifiedPrice;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;
import org.skysongdev.skysongEconomy.trade.Bundle;
import org.skysongdev.skysongEconomy.trade.BundlePart;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;
import org.skysongdev.skysongEconomy.trade.TradePricing;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryService;

import java.util.ArrayList;
import java.util.List;

/** What the Imports windows share: their services, and the lines that describe bundles and prices. */
final class ImportScreens {
    final BusinessMenus menus;
    final LedgerSelections selections;
    final DeliveryService deliveries;
    final TradePricing pricing;
    final EconomyLogisticsSettings settings;
    final Wallet wallet;
    final Messenger messenger;

    ImportScreens(BusinessMenus menus, LedgerSelections selections, DeliveryService deliveries, TradePricing pricing,
                  EconomyLogisticsSettings settings, Wallet wallet, Messenger messenger) {
        this.menus = menus;
        this.selections = selections;
        this.deliveries = deliveries;
        this.pricing = pricing;
        this.settings = settings;
        this.wallet = wallet;
        this.messenger = messenger;
    }

    void open(Player player, Business business) {
        new ImportsMenu(this, menus.context(player, business)).open(player);
    }

    /** @return one line per bundle part: fixed and random amounts, and weighted rolls with each chance */
    List<Component> contents(Bundle bundle) {
        List<Component> lines = new ArrayList<>();
        for (BundlePart part : bundle.parts()) {
            switch (part) {
                case BundlePart.Item item -> lines.add(messenger.render("business.imports.contents.item",
                        Placeholder.unparsed("amount", item.amount().toString()),
                        Placeholder.unparsed("item", item.materialId())));
                case BundlePart.Rolls rolls -> {
                    lines.add(messenger.render("business.imports.contents.rolls",
                            Placeholder.unparsed("rolls", rolls.rolls().toString())));
                    for (BundlePart.Rolls.Option option : rolls.options()) {
                        lines.add(messenger.render("business.imports.contents.option",
                                Placeholder.unparsed("amount", option.amount().toString()),
                                Placeholder.unparsed("item", option.materialId()),
                                Placeholder.unparsed("chance", String.valueOf(rolls.chance(option)))));
                    }
                }
            }
        }
        return lines;
    }

    /** @return the price, and when modifiers apply, the price before them and each modifier */
    List<Component> price(ModifiedPrice price) {
        List<Component> lines = new ArrayList<>();
        if (!price.applied().isEmpty()) {
            lines.add(messenger.render("business.imports.price.base", Placeholder.unparsed("price", wallet.format(price.base()))));
            for (PriceModifier modifier : price.applied()) {
                lines.add(messenger.render("business.imports.price.modifier", Placeholder.parsed("name", modifier.displayName()),
                        Placeholder.unparsed("percent", PriceModifiers.signed(modifier.percent()))));
            }
        }
        lines.add(messenger.render("business.imports.price.final", Placeholder.unparsed("price", wallet.format(price.price()))));
        return lines;
    }
}
