package org.skysongdev.skysongEconomy.api.benches;

import java.util.List;
import java.util.Optional;

/**
 * Which SkysongCrafting benches belong to which business, and the way for other plugins to change
 * that. Get it with {@code Bukkit.getServicesManager().load(BusinessBenches.class)}; it is
 * registered while businesses are enabled. Use it on the main thread. Everything it returns is a
 * copy taken when it was called.
 * <p>
 * A bench belongs to at most one business; a business can have any number of benches. Every change
 * is saved at once and followed by a {@link BenchAssignEvent} or {@link BenchUnassignEvent}. A
 * refused change throws {@link BenchException}.
 */
public interface BusinessBenches {
    /** @return the link of the bench on that block; empty when the block isn't a tied bench */
    Optional<BenchLink> at(BenchPosition bench);

    /** @return the business's benches, oldest link first; empty for an unknown business */
    List<BenchLink> of(String businessId);

    /** @return every link, by business id, oldest link first within a business */
    List<BenchLink> all();

    /**
     * Ties the bench to the business. When it belongs to another business it moves to this one,
     * with a single {@link BenchAssignEvent} whose {@link BenchAssignEvent#getPrevious()} is the old link.
     *
     * @param by who did it, shown by {@code /se business bench info}: a player's name or a plugin's name
     * @return the new link
     * @throws BenchException {@code business.not-found}, {@code business.bench.no-crafting}
     *                        (SkysongCrafting isn't installed), {@code business.bench.not-a-bench} or
     *                        {@code business.bench.already-assigned} (it is already this business's)
     */
    BenchLink assign(BenchPosition bench, String businessId, String by);

    /**
     * Unties the bench from its business, with a {@link BenchUnassignEvent} whose cause is
     * {@link BenchUnassignCause#UNASSIGNED}.
     *
     * @param by who did it: a player's name or a plugin's name
     * @return the link that was removed; empty when the bench wasn't tied
     */
    Optional<BenchLink> unassign(BenchPosition bench, String by);
}
