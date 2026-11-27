package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.api.business.BusinessInfo;
import org.skysongdev.skysongEconomy.api.business.Businesses;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepInfo;
import org.skysongdev.skysongEconomy.api.business.ShiftInfo;
import org.skysongdev.skysongEconomy.api.business.TrainingInfo;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.api.items.ItemCount;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.ImportRoll;
import org.skysongdev.skysongEconomy.api.journal.JournalIcDate;
import org.skysongdev.skysongEconomy.api.journal.MoneyAccount;
import org.skysongdev.skysongEconomy.api.journal.MoneyMovement;
import org.skysongdev.skysongEconomy.api.journal.RecordItem;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;
import org.skysongdev.skysongEconomy.api.journal.TradeDetails;
import org.skysongdev.skysongEconomy.api.nodes.BoosterInfo;
import org.skysongdev.skysongEconomy.api.nodes.HarvestCostInfo;
import org.skysongdev.skysongEconomy.api.nodes.NodeInfo;
import org.skysongdev.skysongEconomy.api.nodes.PlantingChange;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.Shipment;
import org.skysongdev.skysongEconomy.api.piers.ShipmentItem;
import org.skysongdev.skysongEconomy.api.trade.ExportSale;
import org.skysongdev.skysongEconomy.api.trade.LedgerSelectionChange;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Turns what SkysongEconomy announces into economy journal records: one record per movement of money
 * or goods, numbered in order, stamped with the time and IC date, with names copied as they are now.
 * Each record is announced (as {@code EconomyRecordedEvent} on the server) and then saved.
 * <p>
 * It only reads public types, so it needs no server: on the server {@code events/JournalListener}
 * hands it SkysongEconomy's own events. Main thread only.
 */
public final class EconomyRecorder {
    /**
     * What the recorder needs.
     *
     * @param repository where records are saved
     * @param announce   told about every record before it is saved
     * @param calendar   the IC date, when SkysongCalendar is installed
     * @param businesses business names, for records that only carry a business id
     * @param names      ledger and offer names
     * @param clock      the time records are stamped with
     */
    public record Dependencies(JournalRepository repository, Consumer<EconomyRecord> announce, CalendarClock calendar,
                               Optional<Businesses> businesses, TradeNames names, Clock clock) {
        public Dependencies {
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(announce, "announce");
            Objects.requireNonNull(calendar, "calendar");
            Objects.requireNonNull(businesses, "businesses");
            Objects.requireNonNull(names, "names");
            Objects.requireNonNull(clock, "clock");
        }
    }

    private final Dependencies deps;
    private long sequence;

    public EconomyRecorder(Dependencies deps) {
        this.deps = Objects.requireNonNull(deps, "deps");
    }

    /** Carries on numbering after the last saved record. Call once at start-up, before recording. */
    public void loadSequence() {
        sequence = deps.repository().lastSequence();
    }

    /** @return the sequence of the last record made; 0 before the first */
    public long latestSequence() {
        return sequence;
    }

    // Imports

    /** An import was ordered and paid. */
    public EconomyRecord importOrdered(BusinessInfo business, Shipment shipment, Optional<Pier> pier, TradePrice price) {
        return record(RecordKind.IMPORT_ORDERED, shipment.id())
                .actor(shipment.orderedBy())
                .business(business)
                .trade(importDetails(shipment, pier, Optional.of(price), Optional.empty()))
                .pay(MoneyAccount.player(shipment.orderedBy().player()), MoneyAccount.SERVER, price.paid())
                .shipmentItems(shipment.items(), RecordItem.Flow.IN)
                .save();
    }

    /**
     * What happened to an import when it was collected.
     *
     * @param collector who collected it
     * @param seized    whether it was seized at its pier
     * @param items     what was handed over; ignored when seized
     * @param arrived   what it carried when it reached the pier, lost when seized
     * @param roll      the pier's loss roll
     */
    public record Collection(Shipment shipment, Optional<Pier> pier, CharacterRef collector, boolean seized,
                             List<ShipmentItem> items, List<ShipmentItem> arrived, ImportRoll roll) {
        public Collection {
            Objects.requireNonNull(shipment, "shipment");
            Objects.requireNonNull(pier, "pier");
            Objects.requireNonNull(collector, "collector");
            items = List.copyOf(items);
            arrived = List.copyOf(arrived);
            Objects.requireNonNull(roll, "roll");
        }
    }

    /** An import was handed over, or seized, at collection. */
    public EconomyRecord importCollected(Collection collection) {
        Shipment shipment = collection.shipment();
        return record(collection.seized() ? RecordKind.IMPORT_SEIZED : RecordKind.IMPORT_DELIVERED, shipment.id())
                .actor(collection.collector())
                .business(shipment.businessId())
                .trade(importDetails(shipment, collection.pier(), Optional.empty(), Optional.of(collection.roll())))
                .shipmentItems(collection.seized() ? collection.arrived() : collection.items(),
                        collection.seized() ? RecordItem.Flow.LOST : RecordItem.Flow.IN)
                .save();
    }

    /**
     * A deleted business's import still on its way was refunded.
     *
     * @param paidBack whether the money reached the player; when it didn't, no money is recorded
     */
    public EconomyRecord importRefunded(BusinessInfo business, Shipment shipment, boolean paidBack) {
        Builder record = record(RecordKind.IMPORT_REFUNDED, shipment.id())
                .business(business)
                .trade(importDetails(shipment, Optional.empty(), Optional.empty(), Optional.empty()))
                .detail("paid-back", String.valueOf(paidBack));
        if (paidBack) {
            record.pay(MoneyAccount.SERVER, MoneyAccount.player(shipment.orderedBy().player()), shipment.price());
        }
        return record.save();
    }

    // Exports

    /** A batch of an export was sold. */
    public EconomyRecord exportSold(BusinessInfo business, ExportSale sale) {
        TradeDetails trade = new TradeDetails(sale.ledgerId(), ledgerName(sale.ledgerId()), sale.exportId(),
                deps.names().exportOffer(sale.ledgerId(), sale.exportId()).orElse(sale.exportId()), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(sale.price()), sale.tags(), Optional.empty());
        return record(RecordKind.EXPORT_SOLD, sale.id())
                .actor(sale.seller())
                .business(business)
                .trade(trade)
                .pay(MoneyAccount.SERVER, MoneyAccount.player(sale.seller().player()), sale.price().paid())
                .items(sale.items(), RecordItem.Flow.OUT)
                .save();
    }

    // Shifts, training and vaults

    /** A shift finished; records its vault cut, when it had one. */
    public Optional<EconomyRecord> shiftEnded(BusinessInfo business, ShiftInfo shift) {
        if (shift.vaultCut().signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(record(RecordKind.SHIFT_VAULT_CUT, shift.id().toString())
                .actor(shift.character())
                .business(business)
                .pay(MoneyAccount.SERVER, MoneyAccount.vault(business.id()), shift.vaultCut())
                .save());
    }

    /** A worker collected shift pay; records it when it held money. Tokens are kept as a detail. */
    public Optional<EconomyRecord> shiftPaid(BusinessInfo business, CharacterRef worker, List<ShiftInfo> shifts,
                                             BigDecimal money, long tokens) {
        if (money.signum() <= 0) {
            return Optional.empty();
        }
        String subject = shifts.isEmpty() ? UUID.randomUUID().toString() : shifts.get(0).id().toString();
        return Optional.of(record(RecordKind.SHIFT_PAY, subject)
                .actor(worker)
                .business(business)
                .pay(MoneyAccount.SERVER, MoneyAccount.player(worker.player()), money)
                .detail("shifts", String.valueOf(shifts.size()))
                .detail("tokens", String.valueOf(tokens))
                .save());
    }

    /** Someone paid for a training session; records it when it cost money. */
    public Optional<EconomyRecord> trainingStarted(BusinessInfo business, TrainingInfo session) {
        if (session.price().signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(record(RecordKind.TRAINING_PAID, session.id().toString())
                .actor(session.character())
                .business(business)
                .pay(MoneyAccount.player(session.character().player()), MoneyAccount.vault(business.id()), session.price())
                .save());
    }

    /** A vault was collected. */
    public EconomyRecord vaultCollected(BusinessInfo business, UUID payee, BigDecimal amount) {
        return record(RecordKind.VAULT_COLLECTED, business.id())
                .business(business)
                .pay(MoneyAccount.vault(business.id()), MoneyAccount.player(payee), amount)
                .save();
    }

    /**
     * A deleted business's vault was emptied. Money nobody received (a server business, or a payment
     * that failed) is recorded as going to the server, since it left the economy.
     */
    public EconomyRecord vaultClosed(BusinessInfo business, Optional<UUID> payee, BigDecimal amount, boolean paid) {
        MoneyAccount to = paid && payee.isPresent() ? MoneyAccount.player(payee.get()) : MoneyAccount.SERVER;
        return record(RecordKind.VAULT_CLOSED, business.id())
                .business(business)
                .pay(MoneyAccount.vault(business.id()), to, amount)
                .detail("paid", String.valueOf(paid))
                .save();
    }

    // Ledgers

    /** A ledger selection changed; records the removal fine, when one was paid. */
    public Optional<EconomyRecord> ledgerChanged(BusinessInfo business, LedgerSelectionChange change) {
        if (change.kind() != LedgerSelectionChange.Kind.REMOVED || change.fine().signum() <= 0
                || change.paidBy().isEmpty()) {
            return Optional.empty();
        }
        CharacterRef owner = change.paidBy().get();
        return Optional.of(record(RecordKind.LEDGER_FINE, business.id())
                .actor(owner)
                .business(business)
                .pay(MoneyAccount.player(owner.player()), MoneyAccount.SERVER, change.fine())
                .detail("ledger", change.ledgerId())
                .detail("ledger-name", ledgerName(change.ledgerId()))
                .save());
    }

    // Nodes

    /**
     * Someone harvested at a node.
     *
     * @param resource the farm resource; empty at a random node
     */
    public EconomyRecord harvested(NodeInfo node, CharacterRef character, int tier, HarvestCostInfo cost,
                                   List<ItemCount> items, Optional<BoosterInfo> booster, Optional<String> resource) {
        Builder record = record(RecordKind.HARVEST, node.id())
                .actor(character)
                .node(node.id())
                .pay(MoneyAccount.player(character.player()), MoneyAccount.SERVER, cost.money())
                .items(items, RecordItem.Flow.IN)
                .detail("node-kind", node.kind())
                .detail("node-source", node.source())
                .detail("tier", String.valueOf(tier))
                .detail("downtime", String.valueOf(cost.downtime()))
                .detail("harvest-tokens", String.valueOf(cost.harvestTokens()));
        node.owningBusiness().ifPresent(record::business);
        resource.ifPresent(id -> record.detail("resource", id));
        booster.ifPresent(used -> record.detail("booster-type", used.type()).detail("booster-effect", used.effect()));
        return record.save();
    }

    /** A primer was planted, taken out or given back. */
    public EconomyRecord plantingChanged(PlantingChange change, List<ItemCount> primer) {
        boolean planted = change.kind() == PlantingChange.Kind.PLANTED;
        Builder record = record(planted ? RecordKind.FARM_PLANTED : RecordKind.FARM_UNPLANTED, change.nodeId())
                .actor(change.actor())
                .business(change.businessId())
                .node(change.nodeId())
                .items(primer, planted ? RecordItem.Flow.OUT : RecordItem.Flow.IN)
                .detail("resource", change.resourceId())
                .detail("farm-type", change.farmType())
                .detail("planter", change.planter().player() + "/" + change.planter().profile());
        if (!planted) {
            record.detail("cause", change.kind() == PlantingChange.Kind.TAKEN_OUT ? "taken-out" : "returned")
                    .detail("downtime", String.valueOf(change.downtimePaid()));
        }
        return record.save();
    }

    // Upkeep

    /** An NPC's upkeep cycle was eaten from its stock. */
    public EconomyRecord upkeepPaid(NpcUpkeepInfo upkeep, Instant cycleDue, List<ItemCount> eaten) {
        Builder record = record(RecordKind.UPKEEP_PAID, upkeep.npc().toString())
                .items(eaten, RecordItem.Flow.OUT)
                .detail("npc", upkeep.npc().toString())
                .detail("cycle-due", cycleDue.toString());
        upkeep.profile().ifPresent(profile -> record.detail("profile", profile));
        return record.save();
    }

    // Claims

    /**
     * A Claim was bought: buyer to each seller's account in a resale, or to the server in a server
     * sale, and buyer to server for the modifiers' shares. When discounts make the modifier total
     * negative, the server pays the buyer that much instead, since the sellers still get the whole
     * asking price.
     */
    public EconomyRecord claimBought(ClaimSale sale) {
        MoneyAccount buyer = MoneyAccount.player(sale.buyer().player());
        Builder record = record(RecordKind.CLAIM_BOUGHT, "claim-sale:" + sale.id())
                .actor(sale.buyer())
                .claim(sale.claimId())
                .detail("sale", String.valueOf(sale.id()))
                .detail("claim-type", sale.claimTypeId())
                .detail("asking-price", sale.askingPrice().toPlainString())
                .detail("modifier-total", sale.modifierTotal().toPlainString())
                .detail("seller-paid-later", String.valueOf(sale.sellerPaidLater()));
        sale.seller().ifPresentOrElse(
                seller -> seller.payouts().forEach((account, amount) -> record.pay(buyer, MoneyAccount.player(account), amount)),
                () -> record.pay(buyer, MoneyAccount.SERVER, sale.askingPrice()));
        record.pay(buyer, MoneyAccount.SERVER, sale.modifierTotal());
        record.pay(MoneyAccount.SERVER, buyer, sale.modifierTotal().negate());
        return record.save();
    }

    /** A seller's share of a Claim sale that waited for them was paid. */
    public EconomyRecord claimPayout(UUID account, String claimId, long saleId, BigDecimal amount) {
        return record(RecordKind.CLAIM_PAYOUT, "claim-sale:" + saleId)
                .claim(claimId)
                .detail("sale", String.valueOf(saleId))
                .detail("account", account.toString())
                .detail("amount", amount.toPlainString())
                .save();
    }

    private TradeDetails importDetails(Shipment shipment, Optional<Pier> pier, Optional<TradePrice> price,
                                       Optional<ImportRoll> roll) {
        return new TradeDetails(shipment.ledgerId(), ledgerName(shipment.ledgerId()), shipment.importId(),
                deps.names().importOffer(shipment.ledgerId(), shipment.importId()).orElse(shipment.importId()),
                Optional.of(shipment.deliveryTimeId()), shipment.pierId(),
                pier.map(found -> found.details().displayName()), price, shipment.tags(), roll);
    }

    private String ledgerName(String ledgerId) {
        return deps.names().ledger(ledgerId).orElse(ledgerId);
    }

    private Builder record(RecordKind kind, String subjectId) {
        return new Builder(kind, subjectId);
    }

    private Optional<JournalIcDate> icDate() {
        return deps.calendar().date().map(date ->
                JournalIcDate.of(date.year(), date.period(), date.day(), date.dayOfYear(), date.festival()));
    }

    /** Collects one record's facts, then numbers, announces and saves it. */
    private final class Builder {
        private final RecordKind kind;
        private final String subjectId;
        private Optional<CharacterRef> actor = Optional.empty();
        private Optional<String> businessId = Optional.empty();
        private Optional<String> businessName = Optional.empty();
        private Optional<TradeDetails> trade = Optional.empty();
        private Optional<String> nodeId = Optional.empty();
        private Optional<String> claimId = Optional.empty();
        private final List<MoneyMovement> money = new ArrayList<>();
        private final List<RecordItem> items = new ArrayList<>();
        private final Map<String, String> details = new LinkedHashMap<>();

        private Builder(RecordKind kind, String subjectId) {
            this.kind = kind;
            this.subjectId = subjectId;
        }

        Builder actor(CharacterRef character) {
            actor = Optional.of(character);
            return this;
        }

        Builder business(BusinessInfo business) {
            businessId = Optional.of(business.id());
            businessName = Optional.of(business.displayName());
            return this;
        }

        /** The business by id; its name is looked up, and is its id when it no longer exists. */
        Builder business(String id) {
            businessId = Optional.of(id);
            businessName = Optional.of(deps.businesses().flatMap(businesses -> businesses.find(id))
                    .map(BusinessInfo::displayName).orElse(id));
            return this;
        }

        Builder trade(TradeDetails details) {
            trade = Optional.of(details);
            return this;
        }

        Builder node(String id) {
            nodeId = Optional.of(id);
            return this;
        }

        Builder claim(String id) {
            claimId = Optional.of(id);
            return this;
        }

        /** Adds a movement; nothing for an amount of 0. */
        Builder pay(MoneyAccount from, MoneyAccount to, BigDecimal amount) {
            if (amount.signum() > 0) {
                money.add(new MoneyMovement(from, to, amount));
            }
            return this;
        }

        Builder items(List<ItemCount> counts, RecordItem.Flow flow) {
            counts.forEach(count -> items.add(new RecordItem(count.materialId(), count.amount(), flow)));
            return this;
        }

        Builder shipmentItems(List<ShipmentItem> shipped, RecordItem.Flow flow) {
            shipped.forEach(item -> items.add(new RecordItem(item.materialId(), item.amount(), flow)));
            return this;
        }

        Builder detail(String key, String value) {
            details.put(key, value);
            return this;
        }

        EconomyRecord save() {
            EconomyRecord record = new EconomyRecord(++sequence, UUID.randomUUID(), kind, subjectId,
                    deps.clock().instant(), icDate(), actor, businessId, businessName, trade, nodeId, claimId, money,
                    items, details);
            deps.announce().accept(record);
            deps.repository().save(record);
            return record;
        }
    }
}
