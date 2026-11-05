package org.skysongdev.skysongEconomy.business.training;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.SkillPick;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.activity.Activity;
import org.skysongdev.skysongEconomy.business.activity.ActivityLock;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.character.ProficiencyTraining;
import org.skysongdev.skysongEconomy.character.SkillProgression;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Train/Learn.
 * <p>
 * Anyone the business's access groups allow can train, at their lowest group price; the owner
 * trains free. Starting pays the price into the business vault and spends downtime. A timer
 * adds online, non-AFK time with {@link #work}; when the session ends, every skill the
 * business picked gives the reward for its pick count: XP, and at Service businesses
 * proficiency points up to the tier's cap. Rewards follow the picks at the end of the session.
 * <p>
 * Not thread-safe: use it on the main thread.
 */
public final class TrainingService implements BusinessChangeListener {
    /** Collaborators the service charges and rewards through. */
    public record Dependencies(BusinessService businesses, BusinessTypeRegistry types, BusinessTiers tiers,
                               AccessService access, ActivityLock activities, BusinessVaults vaults, Wallet wallet,
                               DowntimeWallet downtime, SkillProgression progression) {
    }

    private final TrainingSettings settings;
    private final Dependencies deps;
    private final TrainingRepository repository;
    private final Clock clock;
    private final Consumer<String> warnings;
    /** The session each player is training now, by player. */
    private final Map<UUID, TrainingSession> active = new HashMap<>();

    public TrainingService(TrainingSettings settings, Dependencies deps, TrainingRepository repository, Clock clock,
                           Consumer<String> warnings) {
        this.settings = settings;
        this.deps = deps;
        this.repository = repository;
        this.clock = clock;
        this.warnings = warnings;
    }

    /** Loads the running sessions; they lock their players again. */
    public void loadAll() {
        active.values().forEach(this::unlock);
        active.clear();
        for (TrainingSession session : repository.loadAll()) {
            UUID player = session.character().player();
            if (active.containsKey(player) || deps.activities().current(player).isPresent()) {
                warnings.accept("Player " + player + " was busy twice; their training at " + session.businessId()
                        + " waits until the other activity ends");
                continue;
            }
            deps.activities().claim(player, activity(session));
            active.put(player, session);
        }
    }

    public TrainingSettings settings() {
        return settings;
    }

    /**
     * Starts a session and charges for it.
     *
     * @throws BusinessException when the business has no skills to train, the character may not
     *                           train there, the player is busy, or lacks downtime or money
     */
    public TrainingSession start(Business business, CharacterRef character) {
        if (business.picks().isEmpty()) {
            throw new BusinessException("business.training.no-skills", Map.of("business", business.displayName()));
        }
        BigDecimal price = price(business, character).orElseThrow(() ->
                new BusinessException("business.training.not-allowed", Map.of("business", business.displayName())));
        UUID player = character.player();
        deps.activities().requireFree(player);
        int cost = settings.downtimeCost();
        if (deps.downtime().available(player) < cost) {
            throw new BusinessException("business.training.no-downtime", Map.of("cost", String.valueOf(cost)));
        }
        boolean paid = price.signum() > 0;
        if (paid && !deps.wallet().withdraw(player, price)) {
            throw new BusinessException("business.training.cannot-afford", Map.of("price", deps.wallet().format(price)));
        }
        if (!deps.downtime().trySpend(player, cost)) {
            if (paid) {
                deps.wallet().deposit(player, price);
            }
            throw new BusinessException("business.training.no-downtime", Map.of("cost", String.valueOf(cost)));
        }
        if (paid) {
            deps.vaults().deposit(business, price);
        }
        TrainingSession session = TrainingSession.start(business.id(), character, clock.instant(), settings.duration(), price);
        deps.activities().claim(player, activity(session));
        active.put(player, session);
        repository.save(session);
        return session;
    }

    /** @return what the character pays to train there; empty when they may not */
    public Optional<BigDecimal> price(Business business, CharacterRef character) {
        return deps.access().trainingPrice(business, character);
    }

    /** @return the session the player is training now */
    public Optional<TrainingSession> active(UUID player) {
        return Optional.ofNullable(active.get(player));
    }

    /**
     * Adds online, non-AFK time to the player's session, if any.
     *
     * @return what the session gave, if this ended it
     */
    public Optional<TrainingResult> work(UUID player, Duration time) {
        TrainingSession current = active.get(player);
        if (current == null) {
            return Optional.empty();
        }
        TrainingSession worked = current.work(time);
        if (!worked.isDone()) {
            active.put(player, worked);
            repository.save(worked);
            return Optional.empty();
        }
        return Optional.of(finish(worked, deps.businesses().find(worked.businessId())));
    }

    /** A deleted business finishes its sessions at once, so nobody loses what they paid for. */
    @Override
    public void deleted(Business business) {
        for (TrainingSession session : List.copyOf(active.values())) {
            if (session.businessId().equals(business.id())) {
                finish(session, Optional.of(business));
            }
        }
    }

    private TrainingResult finish(TrainingSession session, Optional<Business> business) {
        active.remove(session.character().player());
        unlock(session);
        repository.delete(session.id());
        List<SkillTrained> skills = business.map(at -> reward(at, session.character())).orElse(List.of());
        return new TrainingResult(session, skills);
    }

    private List<SkillTrained> reward(Business business, CharacterRef character) {
        boolean proficiency = deps.types().find(business.typeId()).filter(type -> type.trainsProficiency()).isPresent();
        String maxTier = deps.tiers().get(business.tier()).maxProficiency();
        Set<String> skills = new LinkedHashSet<>();
        business.picks().stream().map(SkillPick::skillId).forEach(skills::add);
        List<SkillTrained> results = new ArrayList<>();
        for (String skill : skills) {
            int picks = business.pickCount(skill);
            TrainingReward reward = settings.reward(picks);
            int xp = reward.xp() > 0 && deps.progression().addXp(character, skill, reward.xp()) ? reward.xp() : 0;
            ProficiencyTraining trained = proficiency && reward.proficiency() > 0
                    ? deps.progression().trainProficiency(character, skill, reward.proficiency(), maxTier)
                    : null;
            results.add(new SkillTrained(skill, picks, xp, trained));
        }
        return results;
    }

    private void unlock(TrainingSession session) {
        deps.activities().release(session.character().player(), activity(session));
    }

    private static Activity activity(TrainingSession session) {
        return new Activity(Activity.Kind.TRAINING, session.businessId(), session.character());
    }
}
