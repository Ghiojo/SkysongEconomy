package org.skysongdev.skysongEconomy.api.journal;

import org.skysongdev.skysongEconomy.api.trade.TradePrice;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a trade record (an import or an export) was. Names are copied when it happened, in MiniMessage,
 * so a renamed or removed ledger still reads as it did.
 *
 * @param ledgerId       the ledger
 * @param ledgerName     its name then; its id when it was no longer loaded
 * @param offerId        the import or export
 * @param offerName      its name then; its id when it was no longer loaded
 * @param deliveryTimeId the import's delivery time; empty for an export
 * @param pierId         the import's pier; empty for an export, or without piers
 * @param pierName       the pier's name then
 * @param price          the price breakdown; on imports only when ordered, and on exports
 * @param tags           the ledger's and the offer's tags, such as {@code contraband}
 * @param roll           the loss roll; only on collected imports
 */
public record TradeDetails(String ledgerId, String ledgerName, String offerId, String offerName,
                           Optional<String> deliveryTimeId, Optional<String> pierId, Optional<String> pierName,
                           Optional<TradePrice> price, Set<String> tags, Optional<ImportRoll> roll) {
    public TradeDetails {
        Objects.requireNonNull(ledgerId, "ledgerId");
        Objects.requireNonNull(ledgerName, "ledgerName");
        Objects.requireNonNull(offerId, "offerId");
        Objects.requireNonNull(offerName, "offerName");
        Objects.requireNonNull(deliveryTimeId, "deliveryTimeId");
        Objects.requireNonNull(pierId, "pierId");
        Objects.requireNonNull(pierName, "pierName");
        Objects.requireNonNull(price, "price");
        tags = Set.copyOf(tags);
        Objects.requireNonNull(roll, "roll");
    }

    /** @return whether it carries that tag */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
