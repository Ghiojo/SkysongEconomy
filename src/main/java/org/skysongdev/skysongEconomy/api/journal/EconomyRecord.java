package org.skysongdev.skysongEconomy.api.journal;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One movement of money or goods, as the economy journal keeps it. Records are never changed once
 * written.
 *
 * @param sequence     increasing with every record; a plugin that missed records (it was off) asks for
 *                     those after the last sequence it saw
 * @param recordId     this record
 * @param kind         what happened
 * @param subjectId    what it is about, shared by related records: the shipment for imports, the sale for
 *                     exports, the shift, the training session, {@code claim-sale:<id>} for Claims, the node
 *                     for harvests and planting, the NPC for upkeep, the business for vaults and fines
 * @param at           when it happened, real time
 * @param icDate       the IC date it happened on; empty when SkysongCalendar isn't installed
 * @param actor        the character who did it; empty when nobody did (such as a vault closed by a
 *                     deletion, an upkeep cycle, or a seller paid on joining, see {@link #details})
 * @param businessId   the business involved; empty when there is none
 * @param businessName its name then, in MiniMessage
 * @param trade        what was traded; only on import and export records
 * @param nodeId       the node; on harvests and planting
 * @param claimId      the Claim; on Claim records
 * @param money        the money that moved, in order; empty when none did
 * @param items        the items that moved; empty when none did
 * @param details      more facts about the record, by key, such as {@code tokens} on shift pay. See
 *                     {@code docs/17-economy-journal.md} for the keys of each kind; new keys may be added
 */
public record EconomyRecord(long sequence, UUID recordId, RecordKind kind, String subjectId, Instant at,
                            Optional<JournalIcDate> icDate, Optional<CharacterRef> actor, Optional<String> businessId,
                            Optional<String> businessName, Optional<TradeDetails> trade, Optional<String> nodeId,
                            Optional<String> claimId, List<MoneyMovement> money, List<RecordItem> items,
                            Map<String, String> details) {
    public EconomyRecord {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(icDate, "icDate");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(businessName, "businessName");
        Objects.requireNonNull(trade, "trade");
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(claimId, "claimId");
        money = List.copyOf(money);
        items = List.copyOf(items);
        details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    /** @return whether its trade carries that tag; {@code false} for records that aren't trades */
    public boolean hasTag(String tag) {
        return trade.map(traded -> traded.hasTag(tag)).orElse(false);
    }

    /** @return the import's pier; empty for every other record */
    public Optional<String> pierId() {
        return trade.flatMap(TradeDetails::pierId);
    }

    /** @return whether the player did it, or paid or received money in it */
    public boolean involves(UUID player) {
        return actor.map(character -> character.player().equals(player)).orElse(false)
                || money.stream().anyMatch(movement -> movement.from().playerId().filter(player::equals).isPresent()
                || movement.to().playerId().filter(player::equals).isPresent());
    }

    /** @return the detail with that key; empty when the record has none */
    public Optional<String> detail(String key) {
        return Optional.ofNullable(details.get(key));
    }
}
