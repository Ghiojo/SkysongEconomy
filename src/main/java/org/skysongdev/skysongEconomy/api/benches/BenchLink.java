package org.skysongdev.skysongEconomy.api.benches;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A bench tied to a business, at the moment it was read. It is a copy: it doesn't change when the
 * link does.
 *
 * @param bench      the bench's block
 * @param businessId the business it belongs to
 * @param benchType  its SkysongCrafting type id ({@code workstations.yml} key) when it was read;
 *                   empty when SkysongCrafting isn't installed or the bench is gone
 * @param assignedBy who tied it: a player's name or a plugin's name
 * @param assignedAt when it was tied to this business
 */
public record BenchLink(BenchPosition bench, String businessId, Optional<String> benchType, String assignedBy,
                        Instant assignedAt) {
    public BenchLink {
        Objects.requireNonNull(bench, "bench");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(benchType, "benchType");
        Objects.requireNonNull(assignedBy, "assignedBy");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
