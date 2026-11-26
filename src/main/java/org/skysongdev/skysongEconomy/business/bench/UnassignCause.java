package org.skysongdev.skysongEconomy.business.bench;

/** Why a bench no longer belongs to a business. */
public enum UnassignCause {
    /** Staff or a plugin untied it. */
    UNASSIGNED,
    /** Its business was deleted. */
    BUSINESS_DELETED,
    /** It was removed in SkysongCrafting while the server ran. */
    BENCH_REMOVED,
    /** It was found gone on startup. */
    BENCH_MISSING
}
