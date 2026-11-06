package org.skysongdev.skysongEconomy.commands.node;

import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeTables;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.util.Objects;

/**
 * What {@code /se node odds} reads to describe a table's tier.
 *
 * @param tables        the loaded random tables
 * @param settings      the skill level each tier needs
 * @param skills        skill names
 * @param levels        skill level names
 * @param prices        each item's base value, from {@code /se ledger}
 * @param priceSettings how many decimals values keep
 * @param wallet        how money amounts are written
 */
public record OddsSources(NodeTables tables, NodeSettings settings, SkillCatalog skills, SkillLevels levels,
                          PriceDatabase prices, PriceSettings priceSettings, Wallet wallet) {
    public OddsSources {
        Objects.requireNonNull(tables, "tables");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(levels, "levels");
        Objects.requireNonNull(prices, "prices");
        Objects.requireNonNull(priceSettings, "priceSettings");
        Objects.requireNonNull(wallet, "wallet");
    }
}
