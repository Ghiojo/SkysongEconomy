package org.skysongdev.skysongEconomy.prices.modifier;

import org.skysongdev.skysongEconomy.api.prices.PriceModifierChange;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Every price modifier, and how they change prices. Staff commands and other features (such as
 * a future mechanic that sets taxes) use the same operations.
 * <p>
 * A target holds at most one modifier of each type: {@link #add} refuses a second one and
 * {@link #set} replaces it, so nothing stacks by accident. A trade's price is changed by the sum of
 * every applying modifier's percentage; Claims also take flat amounts, and only ever go up (see
 * {@code claims/ClaimPricing}). Expired modifiers stop applying at once and are deleted the next
 * time the list is read or changed. Kept in memory and saved right away; use it on the main thread.
 */
public final class PriceModifiers {
    /** Longest type key, in characters. */
    public static final int MAX_TYPE_LENGTH = 32;
    /** Longest display name, in characters. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 64;
    /** The lowest percentage a single modifier can have: -100% makes something free. */
    public static final BigDecimal MIN_PERCENT = BigDecimal.valueOf(-100);
    /** The highest percentage a single modifier can have. */
    public static final BigDecimal MAX_PERCENT = BigDecimal.valueOf(1000);
    /** The highest flat amount a single modifier can have, as the table stores it. */
    public static final BigDecimal MAX_FLAT = BigDecimal.valueOf(999_999);

    private static final Pattern TYPE = Pattern.compile("[a-z0-9_]{1," + MAX_TYPE_LENGTH + "}");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ModifierRepository repository;
    private final Clock clock;
    private final Map<String, PriceModifier> byId = new LinkedHashMap<>();
    private final List<ModifierListener> listeners = new ArrayList<>();
    private final List<Consumer<ModifierTarget>> targetChecks = new ArrayList<>();
    private long nextNumber = 1;

    public PriceModifiers(ModifierRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Tells the listener whenever a modifier starts or stops changing prices. */
    public void addListener(ModifierListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Adds a check that a new modifier's target exists, such as a feature that knows its own ids
     * ({@code claim-type:} types that SkysongClaims has).
     *
     * @param check throws a {@link PriceException} naming the problem when the target is unknown
     */
    public void addTargetCheck(Consumer<ModifierTarget> check) {
        targetChecks.add(Objects.requireNonNull(check, "check"));
    }

    /**
     * Loads every saved modifier and deletes the expired ones.
     *
     * @return how many still apply
     */
    public int loadAll() {
        byId.clear();
        for (PriceModifier modifier : repository.loadAll()) {
            byId.put(modifier.id(), modifier);
            nextNumber = Math.max(nextNumber, number(modifier.id()) + 1);
        }
        purgeExpired();
        return byId.size();
    }

    /** @return every modifier that still applies, in the order they were set */
    public List<PriceModifier> all() {
        purgeExpired();
        return List.copyOf(byId.values());
    }

    /** @return the modifiers attached to that target */
    public List<PriceModifier> on(ModifierTarget target) {
        return all().stream().filter(modifier -> modifier.target().equals(target)).toList();
    }

    public Optional<PriceModifier> find(String id) {
        PriceModifier modifier = byId.get(id.toLowerCase(Locale.ROOT));
        return modifier != null && modifier.isActive(clock.instant()) ? Optional.of(modifier) : Optional.empty();
    }

    /** @return the modifier of that type on that target */
    public Optional<PriceModifier> find(String type, ModifierTarget target) {
        return all().stream().filter(modifier -> modifier.type().equals(type) && modifier.target().equals(target)).findFirst();
    }

    /**
     * Adds a modifier.
     *
     * @throws PriceException when a value is invalid, or {@code prices.modifiers.exists} when the
     *                        target already has a modifier of that type
     */
    public PriceModifier add(ModifierDraft draft) {
        validate(draft);
        Optional<PriceModifier> existing = find(draft.type(), draft.target());
        if (existing.isPresent()) {
            throw new PriceException("prices.modifiers.exists", Map.of("id", existing.get().id(),
                    "type", draft.type(), "target", draft.target().key()));
        }
        return store("m" + nextNumber++, draft, Optional.empty());
    }

    /**
     * Adds a modifier, or replaces the one of the same type on the same target, which keeps its id.
     *
     * @return the new modifier and the one it replaced, if any
     * @throws PriceException when a value is invalid
     */
    public SetResult set(ModifierDraft draft) {
        validate(draft);
        Optional<PriceModifier> existing = find(draft.type(), draft.target());
        String id = existing.map(PriceModifier::id).orElseGet(() -> "m" + nextNumber++);
        return new SetResult(store(id, draft, existing), existing);
    }

    /** What {@link #set} did. */
    public record SetResult(PriceModifier modifier, Optional<PriceModifier> replaced) {
    }

    /**
     * Removes a modifier.
     *
     * @throws PriceException {@code prices.modifiers.not-found} when there is none with that id
     */
    public PriceModifier remove(String id) {
        PriceModifier modifier = find(id).orElseThrow(() ->
                new PriceException("prices.modifiers.not-found", Map.of("id", id)));
        byId.remove(modifier.id());
        repository.delete(modifier.id());
        changed(PriceModifierChange.Kind.REMOVED, modifier, Optional.empty());
        return modifier;
    }

    /**
     * Removes every modifier on that target, such as when its business is deleted.
     *
     * @return how many were removed
     */
    public int removeAll(ModifierTarget target) {
        List<PriceModifier> removed = byId.values().stream().filter(modifier -> modifier.target().equals(target)).toList();
        removed.forEach(modifier -> {
            byId.remove(modifier.id());
            repository.delete(modifier.id());
            changed(PriceModifierChange.Kind.REMOVED, modifier, Optional.empty());
        });
        return removed.size();
    }

    /**
     * @return the modifiers that change a price of that subject, in the order they were set. Those
     *         its tax-exempt ledger ignores are left out (see {@link #skipped}).
     */
    public List<PriceModifier> applying(PriceSubject subject) {
        return matching(subject).stream().filter(modifier -> !subject.isExemptFrom(modifier.target())).toList();
    }

    /** @return the modifiers that match that subject but are ignored because its ledger is tax exempt */
    public List<PriceModifier> skipped(PriceSubject subject) {
        return matching(subject).stream().filter(modifier -> subject.isExemptFrom(modifier.target())).toList();
    }

    /**
     * Applies every matching modifier to a trade's price: {@code value * (1 + sum of percentages / 100)},
     * truncated, and never below 0. Modifiers the subject is exempt from are skipped. Trades only
     * take percentages; Claims are priced by {@code claims/ClaimPricing}.
     */
    public ModifiedPrice price(BigDecimal value, PriceSubject subject, PriceSettings settings) {
        List<PriceModifier> applied = applying(subject);
        BigDecimal percent = applied.stream().map(PriceModifier::amount)
                .filter(ModifierAmount.Percent.class::isInstance)
                .map(ModifierAmount::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal factor = BigDecimal.ONE.add(percent.divide(HUNDRED, MathContext.DECIMAL64)).max(BigDecimal.ZERO);
        BigDecimal price = settings.truncate(value.multiply(factor));
        return new ModifiedPrice(value, applied, skipped(subject), percent, price);
    }

    private List<PriceModifier> matching(PriceSubject subject) {
        Instant now = clock.instant();
        return byId.values().stream()
                .filter(modifier -> modifier.isActive(now) && modifier.appliesTo(subject))
                .toList();
    }

    /** Deletes every expired modifier. */
    public void purgeExpired() {
        Instant now = clock.instant();
        List<PriceModifier> expired = byId.values().stream().filter(modifier -> !modifier.isActive(now)).toList();
        expired.forEach(modifier -> {
            byId.remove(modifier.id());
            repository.delete(modifier.id());
            changed(PriceModifierChange.Kind.EXPIRED, modifier, Optional.empty());
        });
    }

    private PriceModifier store(String id, ModifierDraft draft, Optional<PriceModifier> replaced) {
        String name = draft.displayName() == null || draft.displayName().isBlank()
                ? defaultName(draft) : draft.displayName().strip();
        PriceModifier modifier = new PriceModifier(id, draft.type(), draft.target(), draft.appliesTo(), draft.amount(),
                name, draft.expiresAt(), draft.createdBy(), clock.instant());
        byId.put(id, modifier);
        repository.save(modifier);
        changed(replaced.isPresent() ? PriceModifierChange.Kind.REPLACED : PriceModifierChange.Kind.ADDED, modifier, replaced);
        return modifier;
    }

    private void changed(PriceModifierChange.Kind kind, PriceModifier modifier, Optional<PriceModifier> previous) {
        listeners.forEach(listener -> listener.modifierChanged(kind, modifier, previous));
    }

    private void validate(ModifierDraft draft) {
        if (!TYPE.matcher(draft.type()).matches()) {
            throw new PriceException("prices.modifiers.invalid-type", Map.of("type", draft.type(),
                    "max", String.valueOf(MAX_TYPE_LENGTH)));
        }
        validateAmount(draft);
        validateClaims(draft);
        targetChecks.forEach(check -> check.accept(draft.target()));
        if (draft.displayName() != null && draft.displayName().strip().length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new PriceException("prices.modifiers.invalid-name", Map.of("max", String.valueOf(MAX_DISPLAY_NAME_LENGTH)));
        }
        if (draft.expiresAt() != null && !draft.expiresAt().isAfter(clock.instant())) {
            throw new PriceException("prices.modifiers.already-expired", Map.of());
        }
    }

    private static void validateAmount(ModifierDraft draft) {
        BigDecimal value = draft.amount().value();
        if (draft.amount() instanceof ModifierAmount.Percent) {
            if (value.compareTo(MIN_PERCENT) < 0 || value.compareTo(MAX_PERCENT) > 0 || value.signum() == 0) {
                throw new PriceException("prices.modifiers.invalid-percent", Map.of("percent", value.toPlainString(),
                        "min", MIN_PERCENT.toPlainString(), "max", MAX_PERCENT.toPlainString()));
            }
            return;
        }
        if (draft.appliesTo() != AppliesTo.CLAIMS) {
            throw new PriceException("prices.modifiers.flat-needs-claims", Map.of("amount", draft.amount().signed()));
        }
        if (value.signum() == 0 || value.abs().compareTo(MAX_FLAT) > 0) {
            throw new PriceException("prices.modifiers.invalid-flat", Map.of("amount", value.toPlainString(),
                    "max", MAX_FLAT.toPlainString()));
        }
    }

    /**
     * Claim modifiers lower prices only by a percentage (a flat discount could take a Claim below
     * nothing), and only go on targets that can match a Claim; trade
     * modifiers can't be put on a Claim target, where they would never apply.
     */
    private static void validateClaims(ModifierDraft draft) {
        boolean claimTarget = draft.target().kind().claimsOnly();
        if (draft.appliesTo() != AppliesTo.CLAIMS) {
            if (claimTarget) {
                throw new PriceException("prices.modifiers.claims-only-target", Map.of("target", draft.target().key()));
            }
            // Only imports go to a pier, so a pier modifier on exports would never apply.
            if (draft.target().kind() == TargetKind.PIER && draft.appliesTo() == AppliesTo.EXPORTS) {
                throw new PriceException("prices.modifiers.pier-imports-only", Map.of("target", draft.target().key()));
            }
            return;
        }
        if (!claimTarget && draft.target().kind() != TargetKind.GLOBAL) {
            throw new PriceException("prices.modifiers.not-a-claim-target", Map.of("target", draft.target().key()));
        }
        if (draft.amount() instanceof ModifierAmount.Flat && draft.amount().value().signum() < 0) {
            throw new PriceException("prices.modifiers.claim-discount", Map.of("amount", draft.amount().signed()));
        }
    }

    /** @return e.g. {@code port_tax +10%} or {@code registry_fee +50} */
    public static String defaultName(ModifierDraft draft) {
        return draft.type() + " " + draft.amount().signed();
    }

    /** @return the percentage with its sign, e.g. {@code +10} or {@code -5.5} */
    public static String signed(BigDecimal percent) {
        String plain = percent.stripTrailingZeros().toPlainString();
        return percent.signum() > 0 ? "+" + plain : plain;
    }

    private static long number(String id) {
        try {
            return Long.parseLong(id.substring(1));
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
