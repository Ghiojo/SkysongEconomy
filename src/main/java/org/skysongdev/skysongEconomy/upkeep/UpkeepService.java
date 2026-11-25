package org.skysongdev.skysongEconomy.upkeep;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Keeps every NPC's upkeep: which NPCs have one, settling their cycles, and their state.
 * <p>
 * Everything is kept in memory and written through to the {@link UpkeepRepository}. Must be used
 * from one thread, the server's main thread.
 */
public final class UpkeepService {
    /**
     * How many cycles one NPC settles at once, at most, so a very short cycle after a long
     * downtime can't stall the server. The rest are settled on the next check.
     */
    static final int MAX_CYCLES_PER_SETTLE = 1_000;

    private final UpkeepSettings settings;
    private final UpkeepNpcs npcs;
    private final UpkeepStock stock;
    private final UpkeepPayment payment;
    private final UpkeepRepository repository;
    private final UpkeepEvents events;
    private final Clock clock;
    private final Consumer<String> warnings;
    private final Map<UUID, NpcUpkeep> byNpc = new LinkedHashMap<>();
    private final List<Consumer<UUID>> lostListeners = new ArrayList<>();

    /**
     * @param warnings told about saved upkeeps whose profile left {@code npc-upkeep.yml}
     */
    public UpkeepService(UpkeepSettings settings, Dependencies dependencies, UpkeepRepository repository,
                        UpkeepEvents events, Clock clock, Consumer<String> warnings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.npcs = dependencies.npcs();
        this.stock = dependencies.stock();
        this.payment = dependencies.payment();
        this.repository = Objects.requireNonNull(repository, "repository");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /**
     * What the service works with.
     *
     * @param npcs    the NPCs SkysongNPCs manages
     * @param stock   the items stocked in them
     * @param payment works out what a cycle eats
     */
    public record Dependencies(UpkeepNpcs npcs, UpkeepStock stock, UpkeepPayment payment) {
        public Dependencies {
            Objects.requireNonNull(npcs, "npcs");
            Objects.requireNonNull(stock, "stock");
            Objects.requireNonNull(payment, "payment");
        }
    }

    /** @param listener told the UUID of every NPC that loses its upkeep, before it is forgotten */
    public void onLost(Consumer<UUID> listener) {
        lostListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** @return the settings in use */
    public UpkeepSettings settings() {
        return settings;
    }

    /** @return what works out a cycle's payment, for showing what a stock covers */
    public UpkeepPayment payment() {
        return payment;
    }

    /**
     * Reads every saved upkeep. Upkeeps whose profile left {@code npc-upkeep.yml} are kept as
     * they are, and do nothing until it is back or staff change it.
     *
     * @return how many were read
     */
    public int loadAll() {
        byNpc.clear();
        for (NpcUpkeep upkeep : repository.loadAll()) {
            byNpc.put(upkeep.npc(), upkeep);
            upkeep.profileId().filter(id -> settings.profile(id).isEmpty()).ifPresent(id -> warnings.accept(
                    "The NPC " + upkeep.npc() + " follows the upkeep profile '" + id
                            + "', which is not in npc-upkeep.yml: its cycles wait until it is back"));
        }
        return byNpc.size();
    }

    /**
     * Brings every NPC in line with the templates in {@code npc-upkeep.yml}: an NPC from a listed
     * template gets that profile, one whose template is no longer listed loses the upkeep its
     * template gave it, and NPCs that SkysongNPCs no longer has are forgotten. Upkeeps staff set by
     * hand are left as they are.
     *
     * @return how many NPCs gained, changed or lost an upkeep
     */
    public int syncTemplates() {
        Instant now = clock.instant();
        int changed = 0;
        Set<UUID> known = new HashSet<>();
        for (UpkeepNpcs.Known npc : npcs.all()) {
            known.add(npc.id());
            NpcUpkeep current = byNpc.get(npc.id());
            if (current != null && current.setByHand()) {
                continue;
            }
            Optional<UpkeepProfile> wanted = npc.template().flatMap(settings::forTemplate);
            if (wanted.isPresent() && (current == null || !wanted.get().id().equals(current.profile()))) {
                put(fresh(npc.id(), wanted.get(), false, current, now));
                changed++;
            } else if (wanted.isEmpty() && current != null) {
                forget(npc.id());
                changed++;
            }
        }
        for (UUID npc : List.copyOf(byNpc.keySet())) {
            if (!known.contains(npc)) {
                forget(npc);
                changed++;
            }
        }
        return changed;
    }

    /**
     * Settles every cycle that is due. A maintained NPC whose stock holds a cycle's cost eats it;
     * one whose stock doesn't becomes unmaintained and eats nothing. Unmaintained NPCs are skipped.
     * Cycles missed while the server was down are settled in order.
     *
     * @return how many NPCs became unmaintained
     */
    public int settle() {
        Instant now = clock.instant();
        int lapsed = 0;
        for (NpcUpkeep upkeep : List.copyOf(byNpc.values())) {
            if (upkeep.state() != UpkeepState.MAINTAINED || upkeep.nextDue().isAfter(now)) {
                continue;
            }
            Optional<UpkeepProfile> profile = upkeep.profileId().flatMap(settings::profile);
            if (profile.isEmpty()) {
                continue;
            }
            if (settleOne(upkeep, profile.get(), now)) {
                lapsed++;
            }
        }
        return lapsed;
    }

    /** @return whether the NPC became unmaintained */
    private boolean settleOne(NpcUpkeep upkeep, UpkeepProfile profile, Instant now) {
        Instant due = upkeep.nextDue();
        for (int cycle = 0; cycle < MAX_CYCLES_PER_SETTLE && !due.isAfter(now); cycle++) {
            List<StockStack> stacks = stock.view(upkeep.npc());
            UpkeepPayment.Outcome outcome = payment.plan(profile.cost(), stacks);
            if (outcome instanceof UpkeepPayment.Paid paid) {
                stock.take(upkeep.npc(), paid.taken());
                Instant paidDue = due;
                due = due.plus(profile.every());
                events.cyclePaid(upkeep.withNextDue(due), paidDue, eaten(stacks, paid.taken()));
            } else {
                NpcUpkeep lapsed = upkeep.withState(UpkeepState.UNMAINTAINED, due, now);
                put(lapsed);
                events.stateChanged(lapsed, UpkeepEvents.Cause.CYCLE);
                return true;
            }
        }
        put(upkeep.withNextDue(due));
        return false;
    }

    /** @return how many of each material the taken counts take from those stacks, each material once */
    static List<ItemAmount> eaten(List<StockStack> stacks, List<Integer> taken) {
        Map<String, Integer> byMaterial = new LinkedHashMap<>();
        for (int i = 0; i < taken.size() && i < stacks.size(); i++) {
            String materialId = stacks.get(i).materialId();
            if (taken.get(i) > 0 && materialId != null) {
                byMaterial.merge(materialId, taken.get(i), Integer::sum);
            }
        }
        return byMaterial.entrySet().stream().map(entry -> new ItemAmount(entry.getKey(), entry.getValue())).toList();
    }

    /**
     * Staff give an NPC an upkeep by hand, or take it away, whatever its template says.
     *
     * @param npc     a managed NPC
     * @param profile the profile's id, or {@code null} to leave it without upkeep
     * @return its upkeep now
     * @throws IllegalArgumentException when the NPC is not managed or there is no such profile
     */
    public NpcUpkeep setByHand(UUID npc, @Nullable String profile) {
        if (!npcs.exists(npc)) {
            throw new IllegalArgumentException("Not a managed NPC: " + npc);
        }
        Instant now = clock.instant();
        NpcUpkeep current = byNpc.get(npc);
        NpcUpkeep updated;
        if (profile == null) {
            if (current != null && current.hasUpkeep()) {
                notifyLost(npc);
            }
            updated = new NpcUpkeep(npc, null, true, UpkeepState.MAINTAINED, now, now);
        } else {
            UpkeepProfile found = settings.profile(profile)
                    .orElseThrow(() -> new IllegalArgumentException("No upkeep profile " + profile));
            updated = fresh(npc, found, true, current, now);
        }
        put(updated);
        return updated;
    }

    /**
     * Lets an NPC's template decide its upkeep again, undoing {@link #setByHand}.
     *
     * @return its upkeep now; empty when its template gives none
     */
    public Optional<NpcUpkeep> clearHand(UUID npc) {
        NpcUpkeep current = byNpc.get(npc);
        if (current != null && current.setByHand()) {
            if (current.hasUpkeep()) {
                notifyLost(npc);
            }
            byNpc.remove(npc);
            repository.delete(npc);
        }
        syncTemplates();
        return upkeep(npc);
    }

    /**
     * Marks an NPC maintained or unmaintained. Marking it maintained restarts its timer: its next
     * cycle comes one full cycle from now.
     *
     * @return its upkeep now; empty when it has none
     */
    public Optional<NpcUpkeep> mark(UUID npc, UpkeepState state, UpkeepEvents.Cause cause) {
        NpcUpkeep current = byNpc.get(npc);
        if (current == null || !current.hasUpkeep()) {
            return Optional.empty();
        }
        Optional<UpkeepProfile> profile = settings.profile(current.profile());
        if (current.state() == state && state == UpkeepState.UNMAINTAINED) {
            return Optional.of(current);
        }
        Instant now = clock.instant();
        Instant nextDue = state == UpkeepState.MAINTAINED && profile.isPresent()
                ? now.plus(profile.get().every()) : current.nextDue();
        NpcUpkeep updated = current.withState(state, nextDue, now);
        put(updated);
        if (current.state() != state) {
            events.stateChanged(updated, cause);
        }
        return Optional.of(updated);
    }

    /** Forgets the upkeep of an NPC that was removed for good. */
    public void npcRemoved(UUID npc) {
        if (byNpc.containsKey(npc)) {
            forget(npc);
        }
    }

    /** @return the NPC's upkeep; empty when it has none */
    public Optional<NpcUpkeep> upkeep(UUID npc) {
        return Optional.ofNullable(byNpc.get(npc)).filter(NpcUpkeep::hasUpkeep);
    }

    /**
     * @return whether the NPC has an upkeep and is unmaintained; an NPC whose upkeep staff took
     *         away is never unmaintained, whatever its saved row says
     */
    public boolean isUnmaintained(UUID npc) {
        return upkeep(npc).filter(found -> found.state() == UpkeepState.UNMAINTAINED).isPresent();
    }

    /** @return the NPC's saved row, also when staff took its upkeep away by hand */
    public Optional<NpcUpkeep> saved(UUID npc) {
        return Optional.ofNullable(byNpc.get(npc));
    }

    /** @return the NPC's profile; empty when it has no upkeep or its profile left the file */
    public Optional<UpkeepProfile> profile(UUID npc) {
        return upkeep(npc).flatMap(NpcUpkeep::profileId).flatMap(settings::profile);
    }

    /** @return every NPC with an upkeep, soonest cycle first */
    public List<NpcUpkeep> all() {
        return byNpc.values().stream().filter(NpcUpkeep::hasUpkeep)
                .sorted(Comparator.comparing(NpcUpkeep::nextDue)).toList();
    }

    /** @return every NPC with an upkeep in that state, soonest cycle first */
    public List<NpcUpkeep> all(UpkeepState state) {
        return all().stream().filter(upkeep -> upkeep.state() == state).collect(Collectors.toList());
    }

    /** @return the NPCs with an upkeep */
    public Collection<UUID> npcs() {
        return all().stream().map(NpcUpkeep::npc).toList();
    }

    private NpcUpkeep fresh(UUID npc, UpkeepProfile profile, boolean byHand, @Nullable NpcUpkeep current, Instant now) {
        // A new profile restarts the timer but never repairs: an unmaintained NPC stays so.
        UpkeepState state = current != null && current.hasUpkeep() ? current.state() : UpkeepState.MAINTAINED;
        Instant changed = current != null && current.hasUpkeep() ? current.changed() : now;
        return new NpcUpkeep(npc, profile.id(), byHand, state, now.plus(profile.every()), changed);
    }

    private void put(NpcUpkeep upkeep) {
        byNpc.put(upkeep.npc(), upkeep);
        repository.save(upkeep);
    }

    private void forget(UUID npc) {
        NpcUpkeep removed = byNpc.remove(npc);
        if (removed != null && removed.hasUpkeep()) {
            notifyLost(npc);
        }
        repository.delete(npc);
    }

    private void notifyLost(UUID npc) {
        lostListeners.forEach(listener -> listener.accept(npc));
    }
}
