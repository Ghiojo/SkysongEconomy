package org.skysongdev.skysongEconomy.api.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Who sold a Claim in a player resale, and what each of them received. The asking price is split
 * equally between everyone who owned the Claim when it sold; any remainder from truncation goes to
 * the owner who put it on sale.
 *
 * @param listedBy the owner who put it on sale
 * @param owners   everyone who owned it when it sold
 * @param payouts  what each player account received (or will receive on joining, see
 *                 {@link ClaimSale#sellerPaidLater()}), summed when one player had several owning characters;
 *                 together they make up the asking price
 */
public record Seller(CharacterRef listedBy, List<CharacterRef> owners, Map<UUID, BigDecimal> payouts) {
    public Seller {
        Objects.requireNonNull(listedBy, "listedBy");
        owners = List.copyOf(owners);
        payouts = Collections.unmodifiableMap(new LinkedHashMap<>(payouts));
    }
}
