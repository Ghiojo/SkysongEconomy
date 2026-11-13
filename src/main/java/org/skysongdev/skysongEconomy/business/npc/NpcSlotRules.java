package org.skysongdev.skysongEconomy.business.npc;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.benefit.Entitlement;
import org.skysongdev.skysongEconomy.business.npc.benefit.TierOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Something that gives businesses NPC slots: the skills in {@code businesses/npcs.yml}, or
 * picked modules. Each knows its own slot sources and says nothing about the others'.
 */
public interface NpcSlotRules {
    /**
     * @return how many NPC slots each of its sources gives the business right now, in a steady
     *         order. A source that gives the business nothing may be left out.
     */
    Map<SlotSource, Integer> capacities(Business business);

    /**
     * @return how many slots that source gives the business right now; empty when the source is
     *         not one of its own, or its config is gone, so nothing can be said about it
     */
    OptionalInt capacity(Business business, SlotSource source);

    /** @return the SkysongNPCs template the source's NPCs are created from, empty when it is not one of its own */
    Optional<String> template(SlotSource source);

    /**
     * @param tiers the proficiency tiers, to tell when picks may promote
     * @return what the source's own progression owes each of its NPCs at this business right
     *         now; empty when the source is not one of its own, so its NPCs are left as they are
     */
    Optional<Entitlement> entitlement(Business business, SlotSource source, TierOrder tiers);

    /** @return rules that ask each of the given ones in turn; the first that knows a source answers for it */
    static NpcSlotRules all(List<NpcSlotRules> rules) {
        List<NpcSlotRules> each = List.copyOf(rules);
        return new NpcSlotRules() {
            @Override
            public Map<SlotSource, Integer> capacities(Business business) {
                Map<SlotSource, Integer> capacities = new LinkedHashMap<>();
                each.forEach(one -> one.capacities(business).forEach(capacities::putIfAbsent));
                return capacities;
            }

            @Override
            public OptionalInt capacity(Business business, SlotSource source) {
                return each.stream().map(one -> one.capacity(business, source)).filter(OptionalInt::isPresent)
                        .findFirst().orElse(OptionalInt.empty());
            }

            @Override
            public Optional<String> template(SlotSource source) {
                return each.stream().flatMap(one -> one.template(source).stream()).findFirst();
            }

            @Override
            public Optional<Entitlement> entitlement(Business business, SlotSource source, TierOrder tiers) {
                return each.stream().flatMap(one -> one.entitlement(business, source, tiers).stream()).findFirst();
            }
        };
    }
}
