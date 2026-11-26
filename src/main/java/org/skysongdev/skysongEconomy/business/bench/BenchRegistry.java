package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.api.benches.BenchException;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Which SkysongCrafting benches belong to which business, kept in memory and saved on every change.
 * <ul>
 *     <li>A bench belongs to at most one business; a business can have any number of benches.</li>
 *     <li>Only real benches can be tied, so nothing can be tied without SkysongCrafting.</li>
 *     <li>Tying a bench that belongs to another business moves it in one step.</li>
 *     <li>A bench goes with its business when the business is deleted, and is untied when
 *     SkysongCrafting removes it.</li>
 * </ul>
 * Use it on the main thread.
 */
public final class BenchRegistry implements BusinessChangeListener {
    private static final Comparator<BusinessBench> OLDEST_FIRST = Comparator.comparing(BusinessBench::assignedAt);

    private final BusinessService businesses;
    private final BenchRepository repository;
    private final BenchLookup lookup;
    private final Clock clock;
    private final Map<BlockPosition, BusinessBench> byBench = new LinkedHashMap<>();
    private final List<BenchEvents> listeners = new ArrayList<>();

    public BenchRegistry(BusinessService businesses, BenchRepository repository, BenchLookup lookup, Clock clock) {
        this.businesses = businesses;
        this.repository = repository;
        this.lookup = lookup;
        this.clock = clock;
    }

    /** Tells the listener about every change from now on. */
    public void addListener(BenchEvents listener) {
        listeners.add(listener);
    }

    /**
     * Loads the saved links, replacing what is in memory.
     *
     * @return how many were loaded
     */
    public int loadAll() {
        byBench.clear();
        repository.loadAll().forEach(link -> byBench.put(link.bench(), link));
        return byBench.size();
    }

    /** @return whether SkysongCrafting is installed, so benches can be tied */
    public boolean benchesAvailable() {
        return lookup.installed();
    }

    /** @return the type id of the bench on that block; empty when it isn't a bench or SkysongCrafting is missing */
    public Optional<String> benchType(BlockPosition block) {
        return lookup.typeAt(block);
    }

    /** @return the link of the bench on that block; empty when it isn't tied */
    public Optional<BusinessBench> at(BlockPosition bench) {
        return Optional.ofNullable(byBench.get(bench));
    }

    /** @return the business's benches, oldest link first */
    public List<BusinessBench> of(String businessId) {
        return byBench.values().stream()
                .filter(link -> link.businessId().equalsIgnoreCase(businessId))
                .sorted(OLDEST_FIRST)
                .toList();
    }

    /** @return every link, by business id, oldest link first within a business */
    public List<BusinessBench> all() {
        return byBench.values().stream()
                .sorted(Comparator.comparing(BusinessBench::businessId).thenComparing(OLDEST_FIRST))
                .toList();
    }

    /**
     * Ties the bench to the business, moving it when it belongs to another one.
     *
     * @param by who did it: a player's name or a plugin's name
     * @return the new link, and the one it replaced
     * @throws BenchException {@code business.not-found}, {@code business.bench.no-crafting},
     *                        {@code business.bench.not-a-bench} or {@code business.bench.already-assigned}
     */
    public Assignment assign(BlockPosition bench, String businessId, String by) {
        Objects.requireNonNull(bench, "bench");
        Objects.requireNonNull(by, "by");
        Business business = businesses.find(businessId)
                .orElseThrow(() -> new BenchException("business.not-found", Map.of("id", businessId)));
        if (!lookup.installed()) {
            throw new BenchException("business.bench.no-crafting", Map.of());
        }
        if (lookup.typeAt(bench).isEmpty()) {
            throw new BenchException("business.bench.not-a-bench", Map.of("block", bench.toString()));
        }
        Optional<BusinessBench> previous = at(bench);
        if (previous.filter(link -> link.businessId().equals(business.id())).isPresent()) {
            throw new BenchException("business.bench.already-assigned",
                    Map.of("business", business.displayName(), "id", business.id()));
        }
        BusinessBench link = new BusinessBench(bench, business.id(), by, clock.instant());
        byBench.put(bench, link);
        repository.save(link);
        listeners.forEach(listener -> listener.assigned(link, previous));
        return new Assignment(link, previous);
    }

    /**
     * Unties the bench from its business.
     *
     * @param by who did it: a player's name or a plugin's name
     * @return the link that was removed; empty when the bench wasn't tied
     */
    public Optional<BusinessBench> unassign(BlockPosition bench, String by) {
        Objects.requireNonNull(by, "by");
        return remove(bench, UnassignCause.UNASSIGNED, Optional.of(by));
    }

    /** SkysongCrafting removed the bench on that block: its link, if any, goes with it. */
    public void benchRemoved(BlockPosition bench) {
        remove(bench, UnassignCause.BENCH_REMOVED, Optional.empty());
    }

    /**
     * Unties every bench SkysongCrafting no longer has, such as one removed while this plugin was off.
     * Does nothing without SkysongCrafting, so links aren't lost while it is missing.
     *
     * @param removed told about each link that was removed
     * @return how many were removed
     */
    public int removeMissing(Consumer<BusinessBench> removed) {
        if (!lookup.installed()) {
            return 0;
        }
        List<BusinessBench> missing = byBench.values().stream()
                .filter(link -> lookup.typeAt(link.bench()).isEmpty())
                .toList();
        for (BusinessBench link : missing) {
            remove(link.bench(), UnassignCause.BENCH_MISSING, Optional.empty());
            removed.accept(link);
        }
        return missing.size();
    }

    /** A deleted business's benches go with it; the database drops their rows itself. */
    @Override
    public void deleted(Business business) {
        List<BusinessBench> gone = of(business.id());
        gone.forEach(link -> byBench.remove(link.bench()));
        gone.forEach(link -> listeners.forEach(listener ->
                listener.unassigned(link, UnassignCause.BUSINESS_DELETED, Optional.empty())));
    }

    private Optional<BusinessBench> remove(BlockPosition bench, UnassignCause cause, Optional<String> by) {
        BusinessBench link = byBench.remove(bench);
        if (link == null) {
            return Optional.empty();
        }
        repository.delete(bench);
        listeners.forEach(listener -> listener.unassigned(link, cause, by));
        return Optional.of(link);
    }

    /**
     * What tying a bench did.
     *
     * @param link     the new link
     * @param previous the link to another business it replaced; empty when the bench had none
     */
    public record Assignment(BusinessBench link, Optional<BusinessBench> previous) {
    }
}
