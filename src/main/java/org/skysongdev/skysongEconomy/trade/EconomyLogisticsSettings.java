package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.SkillPick;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The server's Economy and Logistics rules, from {@code economy-and-logistics.yml}.
 *
 * @param skills            skills whose picks turn Economy and Logistics on; each pick of any of
 *                          them counts as one level
 * @param slotsByLevel      limits for levels 1 to 5
 * @param deliveryTimes     by id, in file order (the order menus show them)
 * @param ledgerRemovalFine money the owner pays to remove a selected ledger
 */
public record EconomyLogisticsSettings(Set<String> skills, Map<Integer, SlotLimits> slotsByLevel,
                                       Map<String, DeliveryTime> deliveryTimes, BigDecimal ledgerRemovalFine) {
    public EconomyLogisticsSettings {
        skills = Set.copyOf(skills);
        slotsByLevel = Map.copyOf(slotsByLevel);
        deliveryTimes = Collections.unmodifiableMap(new LinkedHashMap<>(deliveryTimes));
        Objects.requireNonNull(ledgerRemovalFine, "ledgerRemovalFine");
    }

    /** @return how many of the business's picks are Economy and Logistics skills, 0 to 5 */
    public int level(Business business) {
        return level(business.picks());
    }

    /** @return how many of these picks are Economy and Logistics skills */
    public int level(List<SkillPick> picks) {
        return (int) picks.stream().filter(pick -> skills.contains(pick.skillId())).count();
    }

    /** @return whether the business has Economy and Logistics */
    public boolean enabled(Business business) {
        return level(business) > 0;
    }

    /**
     * @return the business's limits; {@link SlotLimits#NONE} without Economy and Logistics, and the
     *         highest configured level's limits if the file leaves its level out
     */
    public SlotLimits limits(Business business) {
        int level = level(business);
        for (int candidate = level; candidate > 0; candidate--) {
            SlotLimits limits = slotsByLevel.get(candidate);
            if (limits != null) {
                return limits;
            }
        }
        return SlotLimits.NONE;
    }

    public Optional<DeliveryTime> deliveryTime(String id) {
        return Optional.ofNullable(deliveryTimes.get(id));
    }

    /** @return the highest level the file sets limits for, at most {@link Business#MAX_TIER} */
    public int maxLevel() {
        return slotsByLevel.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }
}
