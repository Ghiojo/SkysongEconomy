package org.skysongdev.skysongEconomy.business.bench;

/**
 * How benches tied to businesses behave, from {@code businesses/benches.yml}.
 *
 * @param businessStaffCanUse whether a business's owner and workers may open its private benches
 */
public record BenchSettings(boolean businessStaffCanUse) {
    /** Used when the file can't be read: nobody gets extra access. */
    public static final BenchSettings OFF = new BenchSettings(false);
}
