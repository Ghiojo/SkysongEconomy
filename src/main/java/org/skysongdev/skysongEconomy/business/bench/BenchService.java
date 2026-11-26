package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.api.benches.BenchLink;
import org.skysongdev.skysongEconomy.api.benches.BenchPosition;
import org.skysongdev.skysongEconomy.api.benches.BenchUnassignCause;
import org.skysongdev.skysongEconomy.api.benches.BusinessBenches;
import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.List;
import java.util.Optional;

/**
 * The public {@link BusinessBenches} service: {@link BenchRegistry} in the API's own types. It also
 * turns the registry's links into {@link BenchLink}s for the events.
 */
public final class BenchService implements BusinessBenches {
    private final BenchRegistry registry;

    public BenchService(BenchRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Optional<BenchLink> at(BenchPosition bench) {
        return registry.at(block(bench)).map(this::link);
    }

    @Override
    public List<BenchLink> of(String businessId) {
        return registry.of(businessId).stream().map(this::link).toList();
    }

    @Override
    public List<BenchLink> all() {
        return registry.all().stream().map(this::link).toList();
    }

    @Override
    public BenchLink assign(BenchPosition bench, String businessId, String by) {
        return link(registry.assign(block(bench), businessId, by).link());
    }

    @Override
    public Optional<BenchLink> unassign(BenchPosition bench, String by) {
        return registry.unassign(block(bench), by).map(this::link);
    }

    /** @return the link as other plugins see it, with the bench's current SkysongCrafting type */
    public BenchLink link(BusinessBench link) {
        return new BenchLink(position(link.bench()), link.businessId(), registry.benchType(link.bench()),
                link.assignedBy(), link.assignedAt());
    }

    /** @return the public cause for an unassignment */
    public static BenchUnassignCause cause(UnassignCause cause) {
        return switch (cause) {
            case UNASSIGNED -> BenchUnassignCause.UNASSIGNED;
            case BUSINESS_DELETED -> BenchUnassignCause.BUSINESS_DELETED;
            case BENCH_REMOVED -> BenchUnassignCause.BENCH_REMOVED;
            case BENCH_MISSING -> BenchUnassignCause.BENCH_MISSING;
        };
    }

    /** @return the plugin's block position for a bench */
    public static BlockPosition block(BenchPosition bench) {
        return new BlockPosition(bench.world(), bench.x(), bench.y(), bench.z());
    }

    /** @return the public bench position for a block */
    public static BenchPosition position(BlockPosition block) {
        return new BenchPosition(block.world(), block.x(), block.y(), block.z());
    }
}
