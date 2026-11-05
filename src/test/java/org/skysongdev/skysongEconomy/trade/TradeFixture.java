package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.access.AccessDefaults;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.InMemoryAccessRepository;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.MapWallet;
import org.skysongdev.skysongEconomy.prices.BaseValueRepository;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceExpression;
import org.skysongdev.skysongEconomy.prices.PriceSettings;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierRepository;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * A tier 3 trading business with one Economy and Logistics pick (2 import slots, 1 ledger slot),
 * a selected ledger with one import, and priced items: what trade tests build on.
 */
final class TradeFixture {
    static final CharacterRef OWNER = character("owner");
    static final CharacterRef WORKER = character("worker");
    static final CharacterRef STRANGER = character("stranger");
    static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");
    static final DeliveryTime EXPRESS = new DeliveryTime("express", "Express", Duration.ofHours(24));
    static final DeliveryTime SLOW = new DeliveryTime("slow", "Slow", Duration.ofDays(7));

    /** 2 swords and 1-3 spears; express costs {base} * 2, slow isn't offered. */
    static final ImportOffer CRATE = new ImportOffer("crate", "Crate", "CHEST", List.of(), new Bundle(List.of(
            new BundlePart.Item("sword", IntRange.exactly(2)),
            new BundlePart.Item("spear", new IntRange(1, 3)))),
            Map.of("express", PriceExpression.parse("{base} * 2")));
    static final Ledger SMUGGLERS = new Ledger("smugglers", "Smugglers", "CHEST", List.of(),
            Map.of("crate", CRATE), Map.of());

    final MapWallet wallet = new MapWallet();
    final Map<String, Set<String>> goodTypes = new HashMap<>(Map.of("sword", Set.of("weapon")));
    final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    final BusinessService businesses;
    final AccessService access;
    final LedgerSelections selections;
    final PriceDatabase prices;
    final PriceModifiers modifiers;
    final TradePricing pricing;
    final EconomyLogisticsSettings settings;
    final Business shop;

    TradeFixture() {
        BusinessTypeRegistry types = new BusinessTypeRegistry(List.of(
                new BusinessType("trading", "Trading", Set.of("economical"), false)));
        BusinessTiers tiers = new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, 3)).toList());
        businesses = new BusinessService(types, tiers, new InMemoryBusinessRepository(), clock);
        access = new AccessService(new AccessDefaults(
                new AccessGroup(AccessGroup.WORKERS, EnumSet.noneOf(Permission.class), BigDecimal.ZERO, Set.of()),
                new AccessGroup(AccessGroup.EVERYONE, EnumSet.noneOf(Permission.class), BigDecimal.TEN, Set.of()), 4),
                new InMemoryAccessRepository());
        settings = new EconomyLogisticsSettings(Set.of("economical"), Map.of(1, new SlotLimits(2, 1)),
                new LinkedHashMap<>(Map.of("express", EXPRESS, "slow", SLOW)), BigDecimal.TEN);
        selections = new LedgerSelections(settings, new LedgerRegistry(List.of(SMUGGLERS)), new NoSelectionStorage(),
                wallet, clock);
        prices = new PriceDatabase(new NoPriceStorage());
        prices.set("sword", "10");
        prices.set("spear", "4");
        modifiers = new PriceModifiers(new NoModifierStorage(), clock);
        pricing = new TradePricing(prices, modifiers, new PriceSettings(1), id -> goodTypes.getOrDefault(id, Set.of()));
        businesses.addListener(access);
        businesses.addListener(selections);
        shop = businesses.create("shop", "trading", OWNER, new BlockPosition("world", 0, 64, 0));
        businesses.setTier(shop, 3);
        businesses.hire(shop, WORKER);
        businesses.pick(shop, "economical");
        selections.select(shop, "smugglers");
    }

    static CharacterRef character(String name) {
        return new CharacterRef(UUID.nameUUIDFromBytes(name.getBytes()), "main");
    }

    private static final class NoPriceStorage implements BaseValueRepository {
        @Override
        public Map<String, String> loadAll() {
            return Map.of();
        }

        @Override
        public void save(String materialId, String expression, BigDecimal valueNow) {
        }

        @Override
        public void delete(String materialId) {
        }
    }

    private static final class NoSelectionStorage implements LedgerSelectionRepository {
        @Override
        public List<SelectedLedger> loadAll() {
            return List.of();
        }

        @Override
        public void save(SelectedLedger selection) {
        }

        @Override
        public void delete(String businessId, String ledgerId) {
        }
    }

    private static final class NoModifierStorage implements ModifierRepository {
        @Override
        public List<PriceModifier> loadAll() {
            return List.of();
        }

        @Override
        public void save(PriceModifier modifier) {
        }

        @Override
        public void delete(String id) {
        }
    }
}
