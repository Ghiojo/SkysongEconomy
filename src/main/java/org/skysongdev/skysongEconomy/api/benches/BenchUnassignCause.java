package org.skysongdev.skysongEconomy.api.benches;

/** Why a bench no longer belongs to a business. */
public enum BenchUnassignCause {
    /** Staff or a plugin untied it. */
    UNASSIGNED,
    /** Its business was deleted. */
    BUSINESS_DELETED,
    /** It was removed in SkysongCrafting (deleted, broken or replaced) while the server ran. */
    BENCH_REMOVED,
    /** It was found gone on startup, removed while SkysongEconomy wasn't running. */
    BENCH_MISSING
}
