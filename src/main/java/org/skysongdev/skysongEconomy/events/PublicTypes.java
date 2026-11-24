package org.skysongdev.skysongEconomy.events;

import org.skysongdev.skysongEconomy.api.business.BusinessNpcInfo;
import org.skysongdev.skysongEconomy.api.business.ModulePickInfo;
import org.skysongdev.skysongEconomy.api.business.ModuleRemovalCause;
import org.skysongdev.skysongEconomy.api.business.NpcRemovalCause;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepInfo;
import org.skysongdev.skysongEconomy.api.business.ShiftInfo;
import org.skysongdev.skysongEconomy.api.business.UpkeepChangeCause;
import org.skysongdev.skysongEconomy.api.items.ItemCount;
import org.skysongdev.skysongEconomy.api.nodes.HarvestCostInfo;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.shift.Shift;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.trade.ItemAmount;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepEvents;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.util.List;

/**
 * Turns the plugin's internal types into the public ones in {@code api}, which events hand to
 * other plugins. It needs no server, so it is unit tested; the {@code Bukkit*Events} classes call it.
 */
public final class PublicTypes {
    private PublicTypes() {
    }

    public static ShiftInfo shift(Shift shift) {
        return new ShiftInfo(shift.id(), shift.businessId(), shift.character(), shift.startedAt(), shift.required(),
                shift.worked(), shift.pay().money(), shift.pay().tokens(), shift.vaultCut(), shift.finished());
    }

    public static BusinessNpcInfo npc(BusinessNpc npc) {
        return new BusinessNpcInfo(npc.npc(), npc.businessId(), npc.source().key(), npc.createdAt());
    }

    public static ModulePickInfo pick(ModulePick pick) {
        return new ModulePickInfo(pick.businessId(), pick.order(), pick.skill(), pick.moduleId(), pick.pickedAt());
    }

    public static NpcUpkeepInfo upkeep(NpcUpkeep upkeep) {
        return new NpcUpkeepInfo(upkeep.npc(), upkeep.profileId(), upkeep.setByHand(),
                upkeep.state() == UpkeepState.MAINTAINED, upkeep.nextDue(), upkeep.changed());
    }

    public static HarvestCostInfo harvestCost(HarvestCost cost) {
        return new HarvestCostInfo(cost.downtime(), cost.harvestTokens(), cost.money());
    }

    public static List<ItemCount> items(List<ItemAmount> items) {
        return items.stream().map(item -> new ItemCount(item.materialId(), item.amount())).toList();
    }

    /** @return the public cause with the same name */
    public static NpcRemovalCause cause(BusinessNpcEvents.RemovalCause cause) {
        return NpcRemovalCause.valueOf(cause.name());
    }

    /** @return the public cause with the same name */
    public static ModuleRemovalCause cause(BusinessModuleEvents.RemovalCause cause) {
        return ModuleRemovalCause.valueOf(cause.name());
    }

    /** @return the public cause with the same name */
    public static UpkeepChangeCause cause(UpkeepEvents.Cause cause) {
        return UpkeepChangeCause.valueOf(cause.name());
    }
}
