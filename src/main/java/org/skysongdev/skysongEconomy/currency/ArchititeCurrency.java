package org.skysongdev.skysongEconomy.currency;

import org.gestern.gringotts.Configuration;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Registers the Architite coins (AU) as Gringotts denominations. */
public final class ArchititeCurrency {
    private record Denomination(String itemFile, double value, String singular, String plural) {
    }

    private static final List<Denomination> DENOMINATIONS = List.of(
            new Denomination("architite_pinch", 0.1, "Pinch", "Pinches"),
            new Denomination("architite_pouch", 1, "Pouch", "Pouches"),
            new Denomination("architite_pound", 10, "Pound", "Pounds"));

    private final SkysongItemFactory coinItems;
    private final Logger logger;

    /** @param coinItems builds the coin items from the files in the architite_coins folder */
    public ArchititeCurrency(SkysongItemFactory coinItems, Logger logger) {
        this.coinItems = coinItems;
        this.logger = logger;
    }

    public void registerDenominations() {
        for (Denomination denomination : DENOMINATIONS) {
            try {
                Configuration.CONF.getCurrency().addDenomination(
                        coinItems.create(denomination.itemFile(), 1),
                        denomination.value(), denomination.singular(), denomination.plural());
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Could not register the " + denomination.singular() + " coin with Gringotts", e);
            }
        }
    }
}
