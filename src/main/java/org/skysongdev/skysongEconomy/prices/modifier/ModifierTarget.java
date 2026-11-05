package org.skysongdev.skysongEconomy.prices.modifier;

import org.skysongdev.skysongEconomy.prices.PriceException;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * What a modifier is attached to. Written as {@code global}, {@code business:<id>},
 * {@code ledger:<id>}, {@code good-type:<type>} or {@code item:<material_id>}.
 */
public sealed interface ModifierTarget {
    /** Every price. */
    ModifierTarget GLOBAL = new Global();

    /** @return the target as commands and storage write it, e.g. {@code ledger:smugglers} */
    String key();

    /** @return whether a price of that subject is affected */
    boolean matches(PriceSubject subject);

    record Global() implements ModifierTarget {
        @Override
        public String key() {
            return "global";
        }

        @Override
        public boolean matches(PriceSubject subject) {
            return true;
        }
    }

    record Business(String businessId) implements ModifierTarget {
        public Business {
            Objects.requireNonNull(businessId, "businessId");
        }

        @Override
        public String key() {
            return "business:" + businessId;
        }

        @Override
        public boolean matches(PriceSubject subject) {
            return businessId.equals(subject.businessId());
        }
    }

    record Ledger(String ledgerId) implements ModifierTarget {
        public Ledger {
            Objects.requireNonNull(ledgerId, "ledgerId");
        }

        @Override
        public String key() {
            return "ledger:" + ledgerId;
        }

        @Override
        public boolean matches(PriceSubject subject) {
            return ledgerId.equals(subject.ledgerId());
        }
    }

    record GoodType(String goodType) implements ModifierTarget {
        public GoodType {
            Objects.requireNonNull(goodType, "goodType");
        }

        @Override
        public String key() {
            return "good-type:" + goodType;
        }

        @Override
        public boolean matches(PriceSubject subject) {
            return subject.goodTypes().contains(goodType);
        }
    }

    record Item(String materialId) implements ModifierTarget {
        public Item {
            Objects.requireNonNull(materialId, "materialId");
        }

        @Override
        public String key() {
            return "item:" + materialId;
        }

        @Override
        public boolean matches(PriceSubject subject) {
            return subject.materialIds().contains(materialId);
        }
    }

    /**
     * Reads a target, ignoring case.
     *
     * @throws PriceException {@code prices.modifiers.invalid-target} when it isn't one of the forms above
     */
    static ModifierTarget parse(String text) {
        String lower = text.strip().toLowerCase(Locale.ROOT);
        if (lower.equals("global")) {
            return GLOBAL;
        }
        int colon = lower.indexOf(':');
        String value = colon < 0 ? "" : lower.substring(colon + 1);
        if (Patterns.ID.matcher(value).matches()) {
            switch (lower.substring(0, colon)) {
                case "business" -> {
                    return new Business(value);
                }
                case "ledger" -> {
                    return new Ledger(value);
                }
                case "good-type" -> {
                    return new GoodType(value);
                }
                case "item" -> {
                    return new Item(value);
                }
                default -> {
                    // Reported below.
                }
            }
        }
        throw new PriceException("prices.modifiers.invalid-target", Map.of("target", text));
    }

    /** Holder for the id pattern, since interfaces can't have private fields. */
    final class Patterns {
        static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,100}");

        private Patterns() {
        }
    }
}
