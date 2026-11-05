package org.skysongdev.skysongEconomy.bootstrap;

import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceSettings;
import org.skysongdev.skysongEconomy.trade.GoodTypes;

/**
 * What Economy and Logistics takes from the rest of the plugin.
 *
 * @param items         the Skysong items that ledgers trade
 * @param prices        every item's base value
 * @param priceSettings how prices are truncated
 * @param goodTypes     every item's good types
 * @param materialIds   tells which Skysong item an item stack is, for exports
 */
public record TradeInputs(SkysongItemFactory items, PriceDatabase prices, PriceSettings priceSettings,
                          GoodTypes goodTypes, MaterialIdResolver materialIds) {
}
