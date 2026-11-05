package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Sells exports: one whole batch per sale, or nothing. Needs Economy and Logistics, the ledger
 * selected, and the owner or a group allowed to sell exports (Workers by default). The money
 * goes to the seller. Selling costs no downtime.
 */
public final class ExportService {
    private final EconomyLogisticsSettings settings;
    private final LedgerSelections selections;
    private final AccessService access;
    private final TradePricing pricing;
    private final GoodTypes goodTypes;
    private final Wallet wallet;

    public ExportService(EconomyLogisticsSettings settings, LedgerSelections selections, AccessService access,
                         TradePricing pricing, GoodTypes goodTypes, Wallet wallet) {
        this.settings = settings;
        this.selections = selections;
        this.access = access;
        this.pricing = pricing;
        this.goodTypes = goodTypes;
        this.wallet = wallet;
    }

    /** @return whether the character may sell exports at the business */
    public boolean canSell(Business business, CharacterRef character) {
        return access.allows(business, character, Permission.SELL_EXPORTS);
    }

    /** @return how many matching items the stacks hold */
    public int count(ExportOffer offer, List<ExportBatch.Stack> stacks) {
        return ExportBatch.count(offer, stacks, goodTypes);
    }

    /**
     * @return what selling one batch from these stacks would pay, or empty when they don't hold a whole batch
     * @throws org.skysongdev.skysongEconomy.prices.PriceException when an item has no base value
     */
    public Optional<ExportQuote> quote(Business business, Ledger ledger, ExportOffer offer, List<ExportBatch.Stack> stacks) {
        return ExportBatch.pick(offer, stacks, goodTypes).map(takes -> pricing.quote(business, ledger, offer, takes));
    }

    /**
     * Sells one batch: pays the seller, then has {@code remove} take the items.
     *
     * @param stacks the seller's inventory
     * @param remove takes the batch's items out of the inventory
     * @return what was sold and paid
     * @throws BusinessException when selling isn't allowed, the stacks don't hold a batch, or the
     *                           payment fails
     * @throws org.skysongdev.skysongEconomy.prices.PriceException when an item has no base value
     */
    public ExportQuote sell(Business business, CharacterRef seller, Ledger ledger, ExportOffer offer,
                            List<ExportBatch.Stack> stacks, Consumer<List<ExportBatch.Take>> remove) {
        if (!settings.enabled(business)) {
            throw new BusinessException("business.ledgers.not-enabled", Map.of("business", business.displayName()));
        }
        if (!selections.isSelected(business, ledger.id())) {
            throw new BusinessException("business.ledgers.not-selected", Map.of("ledger", ledger.id()));
        }
        if (!canSell(business, seller)) {
            throw new BusinessException("business.exports.not-allowed", Map.of("business", business.displayName()));
        }
        ExportQuote quote = quote(business, ledger, offer, stacks).orElseThrow(() -> new BusinessException(
                "business.exports.not-enough", Map.of("batch", String.valueOf(offer.batch()),
                "have", String.valueOf(count(offer, stacks)))));
        if (!wallet.deposit(seller.player(), quote.price().price())) {
            throw new BusinessException("business.exports.pay-failed", Map.of("price", wallet.format(quote.price().price())));
        }
        remove.accept(quote.takes());
        return quote;
    }
}
