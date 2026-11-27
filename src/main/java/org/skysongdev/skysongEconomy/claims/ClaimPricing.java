package org.skysongdev.skysongEconomy.claims;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceSettings;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierAmount;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierDraft;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;
import org.skysongdev.skysongEconomy.prices.modifier.PriceSubject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What Claims cost with their modifiers, and the API for changing Claim type modifiers. Other code
 * (a district tax, a festival) uses {@link #setTypeModifier} with its own type key, never the table.
 * <p>
 * Every modifier's share is worked out from the asking price alone, never from another modifier,
 * so each one has an exact amount that can be reported:
 * <pre>
 * share of a percentage = asking price x percent / 100, truncated
 * share of a flat       = its amount, truncated
 * modifier total        = every share added up, but never below minus the asking price
 * total                 = asking price + modifier total, so never below 0
 * </pre>
 * A percentage can be negative (a discount, such as a council's -10% on a Claim type), so its share
 * is negative; flat amounts are always positive. When
 * businesses are disabled there are no price modifiers, and Claims cost their asking price.
 * Main thread only.
 */
public final class ClaimPricing {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ClaimGateway claims;
    private final ClaimPrices prices;
    private final @Nullable PriceModifiers modifiers;
    private final PriceSettings settings;
    private final Clock clock;

    /** @param modifiers the price modifiers, or {@code null} when businesses (which keep them) are disabled */
    public ClaimPricing(ClaimGateway claims, ClaimPrices prices, @Nullable PriceModifiers modifiers,
                        PriceSettings settings, Clock clock) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.prices = Objects.requireNonNull(prices, "prices");
        this.modifiers = modifiers;
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * @return what buying the Claim costs now
     * @throws ClaimException {@code claims.unknown-claim}, {@code claims.free} or {@code claims.no-price}
     * @throws PriceException when its asking price can't be worked out
     */
    public ClaimQuote quote(String claimId) {
        ClaimView claim = claims.find(claimId).orElseThrow(() -> ClaimException.about("claims.unknown-claim", claimId));
        if (claim.free()) {
            throw ClaimException.about("claims.free", claim.id());
        }
        BigDecimal asking = prices.value(claim.id()).orElseThrow(() -> ClaimException.about("claims.no-price", claim.id()));
        List<ClaimQuote.Share> shares = applying(claim).stream()
                .map(modifier -> new ClaimQuote.Share(modifier, share(asking, modifier.amount()))).toList();
        BigDecimal modifierTotal = shares.stream().map(ClaimQuote.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(asking.negate());
        return new ClaimQuote(claim.id(), claim.typeId(), asking, shares, modifierTotal, asking.add(modifierTotal));
    }

    /** @return the modifiers that change the Claim's price now, in the order they were set */
    public List<PriceModifier> applying(ClaimView claim) {
        return modifiers == null ? List.of() : modifiers.applying(PriceSubject.claim(claim.id(), claim.typeId()));
    }

    /**
     * Adds a modifier on every Claim of a type, or replaces the one with the same key there.
     *
     * @param key         the modifier's type, such as {@code district_tax}
     * @param amount      a percentage (negative for a discount) or a positive flat amount
     * @param duration    how long it lasts, or {@code null} for ever
     * @param displayName shown when players look at a price, or {@code null} for one made from the key and amount
     * @param by          who set it, such as a plugin's name
     * @return the modifier, and the one it replaced
     * @throws PriceException when a value is invalid, or the type is unknown to SkysongClaims
     * @throws ClaimException {@code claims.modifiers-off} when businesses, which keep modifiers, are disabled
     */
    public PriceModifiers.SetResult setTypeModifier(String key, String claimTypeId, ModifierAmount amount,
                                                    @Nullable Duration duration, @Nullable String displayName, String by) {
        Instant expiresAt = duration == null ? null : clock.instant().plus(duration);
        return requireModifiers().set(new ModifierDraft(key, typeTarget(claimTypeId), AppliesTo.CLAIMS, amount,
                displayName, expiresAt, by));
    }

    /** @return the modifier with that key on the type, which is now gone; empty when there was none */
    public Optional<PriceModifier> removeTypeModifier(String key, String claimTypeId) {
        Optional<PriceModifier> found = modifiersOf(claimTypeId).stream().filter(modifier -> modifier.type().equals(key))
                .findFirst();
        found.ifPresent(modifier -> requireModifiers().remove(modifier.id()));
        return found;
    }

    /** @return every modifier on the type itself (not {@code global} ones), in the order they were set */
    public List<PriceModifier> modifiersOf(String claimTypeId) {
        return modifiers == null ? List.of() : modifiers.on(typeTarget(claimTypeId));
    }

    /**
     * Refuses a {@code claim-type:} or {@code claim:} modifier target SkysongClaims doesn't have, so
     * a typo can't make a modifier that never applies. Registered with {@link PriceModifiers#addTargetCheck}.
     *
     * @throws PriceException {@code prices.modifiers.unknown-claim-type} or {@code prices.modifiers.unknown-claim}
     */
    public void checkTarget(ModifierTarget target) {
        if (target instanceof ModifierTarget.ClaimType type && !claims.typeExists(type.typeId())) {
            throw new PriceException("prices.modifiers.unknown-claim-type", Map.of("type", type.typeId()));
        }
        if (target instanceof ModifierTarget.Claim claim && claims.find(claim.claimId()).isEmpty()) {
            throw new PriceException("prices.modifiers.unknown-claim", Map.of("claim", claim.claimId()));
        }
    }

    /** A Claim was deleted in SkysongClaims: the modifiers on it go too. */
    public void deleted(String claimId) {
        if (modifiers != null) {
            modifiers.removeAll(new ModifierTarget.Claim(claimId.toLowerCase(Locale.ROOT)));
        }
    }

    private BigDecimal share(BigDecimal asking, ModifierAmount amount) {
        BigDecimal share = switch (amount) {
            case ModifierAmount.Percent percent -> asking.multiply(percent.value()).divide(HUNDRED, MathContext.DECIMAL64);
            case ModifierAmount.Flat flat -> flat.value();
        };
        return settings.truncate(share);
    }

    private PriceModifiers requireModifiers() {
        if (modifiers == null) {
            throw new ClaimException("claims.modifiers-off", Map.of());
        }
        return modifiers;
    }

    private static ModifierTarget typeTarget(String claimTypeId) {
        return new ModifierTarget.ClaimType(claimTypeId.toLowerCase(Locale.ROOT));
    }
}
