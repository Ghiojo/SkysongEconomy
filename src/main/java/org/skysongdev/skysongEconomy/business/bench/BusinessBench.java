package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.time.Instant;
import java.util.Objects;

/**
 * A SkysongCrafting bench tied to a business. A bench belongs to at most one business.
 *
 * @param bench      the bench's block
 * @param businessId the business it belongs to
 * @param assignedBy who tied it: a player's name or a plugin's name
 * @param assignedAt when it was tied to this business
 */
public record BusinessBench(BlockPosition bench, String businessId, String assignedBy, Instant assignedAt) {
    public BusinessBench {
        Objects.requireNonNull(bench, "bench");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(assignedBy, "assignedBy");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
