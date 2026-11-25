package org.skysongdev.skysongEconomy.events;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.business.BusinessNpcInfo;
import org.skysongdev.skysongEconomy.api.business.ModulePickInfo;
import org.skysongdev.skysongEconomy.api.business.ModuleRemovalCause;
import org.skysongdev.skysongEconomy.api.business.NpcRemovalCause;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepInfo;
import org.skysongdev.skysongEconomy.api.business.ShiftInfo;
import org.skysongdev.skysongEconomy.api.business.UpkeepChangeCause;
import org.skysongdev.skysongEconomy.api.items.ItemCount;
import org.skysongdev.skysongEconomy.api.nodes.BoosterInfo;
import org.skysongdev.skysongEconomy.api.nodes.HarvestCostInfo;
import org.skysongdev.skysongEconomy.api.prices.PriceModifierChange;
import org.skysongdev.skysongEconomy.api.prices.PriceModifierInfo;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.shift.Shift;
import org.skysongdev.skysongEconomy.business.shift.ShiftPay;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.loot.YieldEffect;
import org.skysongdev.skysongEconomy.nodes.boost.Booster;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierAmount;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.trade.ItemAmount;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepEvents;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicTypesTest {
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final CharacterRef ALDRIC = new CharacterRef(UUID.nameUUIDFromBytes("a".getBytes()), "Aldric");

    @Test
    void copiesAShiftWithItsPayAndVaultCut() {
        Shift shift = new Shift(UUID.randomUUID(), "smithy", ALDRIC, NOW, Duration.ofMinutes(30), Duration.ofMinutes(30),
                new ShiftPay(new BigDecimal("12.5"), 3), new BigDecimal("4"), NOW.plusSeconds(1800));

        assertEquals(new ShiftInfo(shift.id(), "smithy", ALDRIC, NOW, Duration.ofMinutes(30), Duration.ofMinutes(30),
                new BigDecimal("12.5"), 3, new BigDecimal("4"), Optional.of(NOW.plusSeconds(1800))), PublicTypes.shift(shift));
    }

    @Test
    void copiesBusinessNpcsAndModulePicks() {
        UUID npc = UUID.randomUUID();

        assertEquals(new BusinessNpcInfo(npc, "smithy", "module:kennel", NOW),
                PublicTypes.npc(new BusinessNpc(npc, "smithy", SlotSource.module("kennel"), NOW)));
        assertEquals(new ModulePickInfo("smithy", 2, "cooking", "oven", NOW),
                PublicTypes.pick(new ModulePick("smithy", 2, "cooking", "oven", NOW)));
    }

    @Test
    void copiesUpkeepAsMaintainedOrNot() {
        UUID npc = UUID.randomUUID();
        NpcUpkeepInfo maintained = PublicTypes.upkeep(new NpcUpkeep(npc, "guard", true, UpkeepState.MAINTAINED, NOW, NOW));
        NpcUpkeepInfo unmaintained = PublicTypes.upkeep(new NpcUpkeep(npc, null, true, UpkeepState.UNMAINTAINED, NOW, NOW));

        assertTrue(maintained.maintained());
        assertEquals(Optional.of("guard"), maintained.profile());
        assertTrue(maintained.setByHand());
        assertFalse(unmaintained.maintained());
        assertEquals(Optional.empty(), unmaintained.profile());
    }

    @Test
    void copiesHarvestCostsAndItems() {
        HarvestCostInfo cost = PublicTypes.harvestCost(new HarvestCost(2, 1, BigDecimal.TEN));
        assertEquals(2, cost.downtime());
        assertEquals(1, cost.harvestTokens());
        assertEquals(0, BigDecimal.TEN.compareTo(cost.money()));
        assertEquals(List.of(new ItemCount("iron_ore", 3)), PublicTypes.items(List.of(new ItemAmount("iron_ore", 3))));
    }

    @Test
    void copiesPriceModifiersAndTheirChanges() {
        PriceModifier tax = new PriceModifier("m3", "port_tax", ModifierTarget.parse("ledger:smugglers"), AppliesTo.BOTH,
                new ModifierAmount.Percent(new BigDecimal("10")), "Port tax", null, "Staff", NOW);
        PriceModifier fee = new PriceModifier("m4", "registry_fee", ModifierTarget.GLOBAL, AppliesTo.CLAIMS,
                new ModifierAmount.Flat(new BigDecimal("50")), "Fee", NOW.plusSeconds(60), "CONSOLE", NOW);

        PriceModifierInfo info = PublicTypes.modifier(tax);
        assertEquals(new PriceModifierInfo("m3", "port_tax", "ledger:smugglers", PriceModifierInfo.AppliesTo.BOTH,
                new BigDecimal("10"), false, "Port tax", Optional.empty(), "Staff", NOW), info);
        assertTrue(PublicTypes.modifier(fee).flat());
        assertEquals(Optional.of(NOW.plusSeconds(60)), PublicTypes.modifier(fee).expiresAt());

        PriceModifierChange change = PublicTypes.modifierChange(PriceModifierChange.Kind.REPLACED, tax, Optional.of(tax), NOW);
        assertEquals(Optional.of(info), change.previous());
        for (AppliesTo appliesTo : AppliesTo.values()) {
            assertEquals(appliesTo.name(), PriceModifierInfo.AppliesTo.valueOf(appliesTo.name()).name());
        }
    }

    @Test
    void copiesBoostersWithTheirEffectAsWritten() {
        assertEquals(new BoosterInfo("crop", "max+25%; rolls+1"),
                PublicTypes.booster(new Booster("crop", YieldEffect.parse("max+25%; rolls+1"))));
    }

    @Test
    void everyInternalCauseHasAPublicOne() {
        for (BusinessNpcEvents.RemovalCause cause : BusinessNpcEvents.RemovalCause.values()) {
            assertEquals(cause.name(), PublicTypes.cause(cause).name());
        }
        for (BusinessModuleEvents.RemovalCause cause : BusinessModuleEvents.RemovalCause.values()) {
            assertEquals(cause.name(), PublicTypes.cause(cause).name());
        }
        for (UpkeepEvents.Cause cause : UpkeepEvents.Cause.values()) {
            assertEquals(cause.name(), PublicTypes.cause(cause).name());
        }
        assertEquals(BusinessNpcEvents.RemovalCause.values().length, NpcRemovalCause.values().length);
        assertEquals(BusinessModuleEvents.RemovalCause.values().length, ModuleRemovalCause.values().length);
        assertEquals(UpkeepEvents.Cause.values().length, UpkeepChangeCause.values().length);
    }
}
