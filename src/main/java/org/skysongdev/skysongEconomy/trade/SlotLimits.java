package org.skysongdev.skysongEconomy.trade;

/**
 * How much an Economy and Logistics business can do at once.
 *
 * @param imports deliveries it can have on the way
 * @param ledgers ledgers it can have selected
 */
public record SlotLimits(int imports, int ledgers) {
    /** The limits of a business without Economy and Logistics. */
    public static final SlotLimits NONE = new SlotLimits(0, 0);

    public SlotLimits {
        if (imports < 0 || ledgers < 0) {
            throw new IllegalArgumentException("Slot limits can't be negative");
        }
    }
}
