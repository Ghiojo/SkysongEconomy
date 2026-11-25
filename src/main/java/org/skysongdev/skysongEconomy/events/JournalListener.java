package org.skysongdev.skysongEconomy.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepPaidEvent;
import org.skysongdev.skysongEconomy.api.business.ShiftEndEvent;
import org.skysongdev.skysongEconomy.api.business.ShiftPayCollectedEvent;
import org.skysongdev.skysongEconomy.api.business.TrainingStartEvent;
import org.skysongdev.skysongEconomy.api.business.VaultCloseEvent;
import org.skysongdev.skysongEconomy.api.business.VaultCollectEvent;
import org.skysongdev.skysongEconomy.api.claims.ClaimPayoutEvent;
import org.skysongdev.skysongEconomy.api.claims.ClaimPurchaseEvent;
import org.skysongdev.skysongEconomy.api.items.ItemCount;
import org.skysongdev.skysongEconomy.api.journal.ImportRoll;
import org.skysongdev.skysongEconomy.api.nodes.FarmPlantingChangeEvent;
import org.skysongdev.skysongEconomy.api.nodes.NodeHarvestEvent;
import org.skysongdev.skysongEconomy.api.piers.ShipmentCollectedEvent;
import org.skysongdev.skysongEconomy.api.piers.ShipmentOrderedEvent;
import org.skysongdev.skysongEconomy.api.piers.ShipmentOutcome;
import org.skysongdev.skysongEconomy.api.piers.ShipmentRefundedEvent;
import org.skysongdev.skysongEconomy.api.trade.ExportSoldEvent;
import org.skysongdev.skysongEconomy.api.trade.LedgerSelectionChangeEvent;
import org.skysongdev.skysongEconomy.journal.EconomyRecorder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hands SkysongEconomy's own events to the {@link EconomyRecorder}, last ({@code MONITOR}), so a
 * record shows what really happened after every other listener. A record that fails is logged and
 * never stops the trade, shift or sale that caused it.
 */
public final class JournalListener implements Listener {
    private final EconomyRecorder recorder;
    private final Function<ItemStack, Optional<String>> materialIds;
    private final Logger logger;

    /** @param materialIds reads an item's SkysongItems material id, for primers */
    public JournalListener(EconomyRecorder recorder, Function<ItemStack, Optional<String>> materialIds, Logger logger) {
        this.recorder = recorder;
        this.materialIds = materialIds;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onImportOrdered(ShipmentOrderedEvent event) {
        record("an import order", () -> recorder.importOrdered(event.getBusiness(), event.getShipment(),
                event.getPier(), event.getPrice()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onImportCollected(ShipmentCollectedEvent event) {
        record("an import collection", () -> recorder.importCollected(new EconomyRecorder.Collection(
                event.getShipment(), event.getPier(), event.getCollector(),
                event.getOutcome() == ShipmentOutcome.SEIZED, event.getItems(), event.getArrivedItems(),
                new ImportRoll(event.getLossChance(), event.getRoll(), event.isLostByRoll(), event.getSeizeMessage()))));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onImportRefunded(ShipmentRefundedEvent event) {
        record("an import refund", () -> recorder.importRefunded(event.getBusiness(), event.getShipment(),
                event.isPaidBack()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExportSold(ExportSoldEvent event) {
        record("an export sale", () -> recorder.exportSold(event.getBusiness(), event.getSale()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShiftEnd(ShiftEndEvent event) {
        record("a shift's vault cut", () -> recorder.shiftEnded(event.getBusiness(), event.getShift()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShiftPay(ShiftPayCollectedEvent event) {
        record("shift pay", () -> recorder.shiftPaid(event.getBusiness(), event.getWorker(), event.getShifts(),
                event.getMoney(), event.getTokens()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTrainingStart(TrainingStartEvent event) {
        record("a training payment", () -> recorder.trainingStarted(event.getBusiness(), event.getSession()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVaultCollect(VaultCollectEvent event) {
        record("a vault collection", () -> recorder.vaultCollected(event.getBusiness(), event.getPayee(),
                event.getAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVaultClose(VaultCloseEvent event) {
        record("a closed vault", () -> recorder.vaultClosed(event.getBusiness(), event.getPayee(), event.getAmount(),
                event.isPaid()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLedgerChange(LedgerSelectionChangeEvent event) {
        record("a ledger fine", () -> recorder.ledgerChanged(event.getBusiness(), event.getChange()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHarvest(NodeHarvestEvent event) {
        record("a harvest", () -> recorder.harvested(event.getNode(), event.getCharacter(), event.getTier(),
                event.getCost(), event.getItems(), event.getBooster(), event.getResource()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlanting(FarmPlantingChangeEvent event) {
        record("a farm planting", () -> recorder.plantingChanged(event.getChange(), counts(event.getPrimer())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpkeepPaid(NpcUpkeepPaidEvent event) {
        record("an upkeep payment", () -> recorder.upkeepPaid(event.getUpkeep(), event.getCycleDue(), event.getEaten()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaimPurchase(ClaimPurchaseEvent event) {
        record("a Claim purchase", () -> recorder.claimBought(event.getSale()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaimPayout(ClaimPayoutEvent event) {
        record("a Claim payout", () -> recorder.claimPayout(event.getAccount(), event.getClaimId(), event.getSaleId(),
                event.getAmount()));
    }

    /** @return how many of each material the stacks hold; stacks without a material id are left out */
    private List<ItemCount> counts(List<ItemStack> stacks) {
        Map<String, Integer> byMaterial = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            materialIds.apply(stack).ifPresent(id -> byMaterial.merge(id, stack.getAmount(), Integer::sum));
        }
        return byMaterial.entrySet().stream().map(entry -> new ItemCount(entry.getKey(), entry.getValue())).toList();
    }

    private void record(String what, Runnable recording) {
        try {
            recording.run();
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to record " + what + " in the economy journal", e);
        }
    }
}
