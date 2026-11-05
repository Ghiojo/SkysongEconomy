package org.skysongdev.skysongEconomy.business.shift;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.activity.Activity;
import org.skysongdev.skysongEconomy.business.activity.ActivityLock;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Work a Shift and Retrieve Pay.
 * <p>
 * The owner and workers start shifts. Starting spends downtime and fixes the pay from the pay
 * table. A timer adds online, non-AFK time with {@link #work}; once the shift has enough it
 * ends, its vault cut goes to the business vault, and the character can retrieve the pay. Pay
 * stays owed after the character leaves the business. A deleted business pays every shift
 * first, finished or not.
 * <p>
 * Not thread-safe: use it on the main thread.
 */
public final class ShiftService implements BusinessChangeListener {
    /** Collaborators the service pays and charges through. */
    public record Dependencies(BusinessService businesses, BusinessTiers tiers, ActivityLock activities,
                               BusinessVaults vaults, GatheringTokens tokens, Wallet wallet,
                               DowntimeWallet downtime, ShiftEvents events) {
    }

    private final ShiftSettings settings;
    private final Dependencies deps;
    private final ShiftRepository repository;
    private final Clock clock;
    private final Consumer<String> warnings;
    private final Map<UUID, Shift> shifts = new LinkedHashMap<>();
    /** The shift each player is working now, by player. */
    private final Map<UUID, Shift> active = new HashMap<>();

    /** @param warnings where to report pay that could not be paid out */
    public ShiftService(ShiftSettings settings, Dependencies deps, ShiftRepository repository, Clock clock,
                        Consumer<String> warnings) {
        this.settings = settings;
        this.deps = deps;
        this.repository = repository;
        this.clock = clock;
        this.warnings = warnings;
    }

    /** Loads every saved shift; shifts being worked lock their players again. */
    public void loadAll() {
        shifts.values().stream().filter(shift -> !shift.isFinished()).forEach(this::unlock);
        shifts.clear();
        active.clear();
        for (Shift shift : repository.loadAll()) {
            shifts.put(shift.id(), shift);
            if (shift.isFinished()) {
                continue;
            }
            UUID player = shift.character().player();
            if (active.containsKey(player)) {
                warnings.accept("Player " + player + " had two shifts running; only one counts until it ends");
                continue;
            }
            deps.activities().claim(player, activity(shift));
            active.put(player, shift);
        }
    }

    public ShiftSettings settings() {
        return settings;
    }

    /**
     * Starts a shift.
     *
     * @param playerName the character's player, for the {@code {player}} placeholder
     * @throws BusinessException when the character doesn't work there, the player is busy, has
     *                           too little downtime, or a plugin cancelled the shift
     */
    public Shift start(Business business, CharacterRef character, String playerName) {
        if (!business.isMember(character)) {
            throw new BusinessException("business.shift.not-worker", Map.of("business", business.displayName()));
        }
        UUID player = character.player();
        deps.activities().requireFree(player);
        int cost = settings.downtimeCost();
        if (deps.downtime().available(player) < cost) {
            throw new BusinessException("business.shift.no-downtime", Map.of("cost", String.valueOf(cost)));
        }
        if (!deps.events().starting(business, character, message(business, character, playerName))) {
            throw new BusinessException("business.shift.cancelled");
        }
        if (!deps.downtime().trySpend(player, cost)) {
            throw new BusinessException("business.shift.no-downtime", Map.of("cost", String.valueOf(cost)));
        }
        BigDecimal cut = business.isOwner(character) ? BigDecimal.ZERO : deps.tiers().get(business.tier()).vaultCut();
        Shift shift = Shift.start(business.id(), character, clock.instant(), settings.duration(),
                settings.pay(business.typeId(), business.tier()), cut);
        deps.activities().claim(player, activity(shift));
        active.put(player, shift);
        store(shift);
        return shift;
    }

    /** @return the business's shift message with its placeholders filled in */
    public String message(Business business, CharacterRef character, String playerName) {
        return business.shiftMessage().orElse(settings.defaultMessage())
                .replace("{character}", character.profile())
                .replace("{player}", playerName)
                .replace("{business}", business.displayName());
    }

    /** @return the shift the player is working now */
    public Optional<Shift> active(UUID player) {
        return Optional.ofNullable(active.get(player));
    }

    /**
     * Adds online, non-AFK time to the shift the player is working, if any.
     *
     * @return the shift, if this ended it
     */
    public Optional<Shift> work(UUID player, Duration time) {
        Shift current = active.get(player);
        if (current == null) {
            return Optional.empty();
        }
        Shift worked = current.work(time, clock.instant());
        store(worked);
        if (!worked.isFinished()) {
            active.put(player, worked);
            return Optional.empty();
        }
        active.remove(player);
        unlock(worked);
        deps.businesses().find(worked.businessId()).ifPresent(business -> {
            deps.vaults().deposit(business, worked.vaultCut());
            deps.events().ended(business, worked);
        });
        return Optional.of(worked);
    }

    /** @return what the character can retrieve from the business now */
    public ShiftPay owed(Business business, CharacterRef character) {
        return finished(business, character).stream().map(Shift::pay).reduce(ShiftPay.NONE, ShiftPay::plus);
    }

    /**
     * Pays the character every finished shift at the business: money to their player, tokens to
     * the character.
     *
     * @return what was paid
     * @throws BusinessException {@code business.shift.nothing-owed} when nothing is owed, or
     *                           {@code business.shift.pay-failed} when the money couldn't be
     *                           paid (nothing is paid then)
     */
    public ShiftPay retrieve(Business business, CharacterRef character) {
        List<Shift> owed = finished(business, character);
        if (owed.isEmpty()) {
            throw new BusinessException("business.shift.nothing-owed", Map.of("business", business.displayName()));
        }
        ShiftPay total = owed.stream().map(Shift::pay).reduce(ShiftPay.NONE, ShiftPay::plus);
        if (!pay(character, total)) {
            throw new BusinessException("business.shift.pay-failed", Map.of("amount", deps.wallet().format(total.money())));
        }
        owed.forEach(this::remove);
        return total;
    }

    /** Pays every shift of a deleted business, finished or not, since its pay was promised. */
    @Override
    public void deleted(Business business) {
        for (Shift shift : List.copyOf(shifts.values())) {
            if (!shift.businessId().equals(business.id())) {
                continue;
            }
            if (!shift.isFinished()) {
                active.remove(shift.character().player());
                unlock(shift);
            }
            if (!pay(shift.character(), shift.pay())) {
                warnings.accept("Could not pay " + shift.pay().money().toPlainString() + " for a shift at deleted business "
                        + business.id() + " to " + shift.character() + "; pay them by hand");
            }
            shifts.remove(shift.id());
        }
    }

    private List<Shift> finished(Business business, CharacterRef character) {
        List<Shift> result = new ArrayList<>();
        for (Shift shift : shifts.values()) {
            if (shift.isFinished() && shift.businessId().equals(business.id()) && shift.character().equals(character)) {
                result.add(shift);
            }
        }
        return result;
    }

    /** @return whether the money was paid; tokens are only given when it was */
    private boolean pay(CharacterRef character, ShiftPay pay) {
        if (pay.money().signum() > 0 && !deps.wallet().deposit(character.player(), pay.money())) {
            return false;
        }
        if (pay.tokens() > 0) {
            deps.tokens().add(character, pay.tokens());
        }
        return true;
    }

    private void store(Shift shift) {
        shifts.put(shift.id(), shift);
        repository.save(shift);
    }

    private void remove(Shift shift) {
        shifts.remove(shift.id());
        repository.delete(shift.id());
    }

    private void unlock(Shift shift) {
        deps.activities().release(shift.character().player(), activity(shift));
    }

    private static Activity activity(Shift shift) {
        return new Activity(Activity.Kind.SHIFT, shift.businessId(), shift.character());
    }
}
