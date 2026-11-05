package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.prices.PriceContext;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceExpression;
import org.skysongdev.skysongEconomy.prices.PriceSettings;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;
import org.skysongdev.skysongEconomy.prices.modifier.PriceSubject;
import org.skysongdev.skysongEconomy.prices.modifier.TradeDirection;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out what imports cost and exports pay: the base value from the price database (of the
 * bundle, or of the items sold), the price expression, then the modifiers, truncated.
 */
public final class TradePricing {
    private final PriceDatabase prices;
    private final PriceModifiers modifiers;
    private final PriceSettings settings;
    private final GoodTypes goodTypes;

    public TradePricing(PriceDatabase prices, PriceModifiers modifiers, PriceSettings settings, GoodTypes goodTypes) {
        this.prices = prices;
        this.modifiers = modifiers;
        this.settings = settings;
        this.goodTypes = goodTypes;
    }

    /**
     * @throws PriceException {@code business.imports.time-not-offered} when the import has no
     *                        price for that delivery time, or a price error when an item has no
     *                        base value or the expression can't be worked out
     */
    public ImportQuote quote(Business business, Ledger ledger, ImportOffer offer, DeliveryTime time) {
        PriceExpression expression = offer.price(time.id()).orElseThrow(() ->
                new PriceException("business.imports.time-not-offered", Map.of("time", time.id())));
        BigDecimal base = offer.contents().baseValue(prices.context(null));
        BigDecimal value = expression.evaluate(prices.context(base));
        PriceSubject subject = new PriceSubject(TradeDirection.IMPORT, business.id(), ledger.id(),
                offer.contents().materialIds(), goodTypes.of(offer.contents().materialIds()));
        return new ImportQuote(ledger, offer, time, modifiers.price(value, subject, settings));
    }

    /**
     * @param takes the items of the batch being sold
     * @throws PriceException when an item sold has no base value or the expression can't be worked out
     */
    public ExportQuote quote(Business business, Ledger ledger, ExportOffer offer, List<ExportBatch.Take> takes) {
        PriceContext items = prices.context(null);
        BigDecimal base = BigDecimal.ZERO;
        Set<String> materialIds = new LinkedHashSet<>();
        for (ExportBatch.Take take : takes) {
            base = base.add(items.item(take.materialId()).multiply(BigDecimal.valueOf(take.amount())));
            materialIds.add(take.materialId());
        }
        BigDecimal value = offer.price().evaluate(prices.context(base));
        PriceSubject subject = new PriceSubject(TradeDirection.EXPORT, business.id(), ledger.id(), materialIds,
                goodTypes.of(materialIds));
        return new ExportQuote(ledger, offer, takes, modifiers.price(value, subject, settings));
    }
}
