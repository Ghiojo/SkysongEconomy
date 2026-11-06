package org.skysongdev.skysongEconomy.integration.skysongstats;

import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.character.ProficiencyTraining;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.character.SkillProgression;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Downtime;
import org.skysongdev.skysongstats.api.DowntimeTarget;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.SkillLevel;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongstats.api.Tier;
import org.skysongdev.skysongstats.api.TrainingResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Connects the business features to SkysongStats, which owns characters, skills, proficiencies
 * and downtime.
 * <p>
 * This is the only class that talks to the SkysongStats API; everything else depends on the
 * small interfaces it implements. Like the API itself, it must be used on the main thread.
 */
public final class SkysongStatsAdapter implements CharacterDirectory, DowntimeWallet, SkillCatalog, SkillLevels,
        SkillProgression {
    private final SkysongStatsApi api;

    public SkysongStatsAdapter(SkysongStatsApi api) {
        this.api = Objects.requireNonNull(api, "api");
    }

    @Override
    public Optional<CharacterRef> active(UUID player) {
        return api.profiles().active(player).map(SkysongStatsAdapter::toRef);
    }

    @Override
    public List<CharacterRef> all(UUID player) {
        return api.profiles().all(player).stream().map(SkysongStatsAdapter::toRef).toList();
    }

    @Override
    public boolean exists(CharacterRef character) {
        return api.profiles().exists(toId(character));
    }

    @Override
    public int available(UUID player) {
        return preferredPool(player).map(pool -> api.downtime().get(pool).orElse(0)).orElse(0);
    }

    @Override
    public boolean trySpend(UUID player, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        Optional<DowntimeTarget> pool = preferredPool(player);
        if (pool.isEmpty()) {
            return false;
        }
        Downtime downtime = api.downtime();
        OptionalInt available = downtime.get(pool.get());
        // SkysongStats stops at 0 instead of refusing, so check the balance first.
        if (available.isEmpty() || available.getAsInt() < amount) {
            return false;
        }
        return amount == 0 || downtime.remove(pool.get(), amount).isPresent();
    }

    @Override
    public boolean exists(String skillId) {
        return api.skills().find(skillId).isPresent();
    }

    @Override
    public String displayName(String skillId) {
        return api.skills().find(skillId).map(skill -> skill.color() + skill.name()).orElse(skillId);
    }

    @Override
    public List<String> levelIds() {
        return api.skills().levels().stream().map(SkillLevel::id).toList();
    }

    @Override
    public Optional<String> levelOf(CharacterRef character, String skillId) {
        return api.skills().level(toId(character), skillId).map(SkillLevel::id);
    }

    @Override
    public String levelName(String levelId) {
        return api.skills().levels().stream()
                .filter(level -> level.id().equals(levelId))
                .findFirst()
                .map(level -> level.color() + level.name())
                .orElse(levelId);
    }

    @Override
    public boolean addXp(CharacterRef character, String skillId, int amount) {
        return api.skills().addXp(toId(character), skillId, amount).isPresent();
    }

    @Override
    public ProficiencyTraining trainProficiency(CharacterRef character, String skillId, int points, String maxTierId) {
        Optional<Skill> skill = api.skills().find(skillId);
        Optional<Tier> maxTier = api.proficiencies().findTier(maxTierId);
        if (skill.isEmpty() || maxTier.isEmpty()) {
            return ProficiencyTraining.UNAVAILABLE;
        }
        return api.proficiencies().train(toId(character), skill.get(), points, maxTier.get())
                .map(SkysongStatsAdapter::toTraining)
                .orElse(ProficiencyTraining.UNAVAILABLE);
    }

    /** The pool {@code Downtime#spend} would use: global, or the active character's. */
    private Optional<DowntimeTarget> preferredPool(UUID player) {
        Optional<Boolean> spendsGlobal = api.downtime().spendsGlobal(player);
        if (spendsGlobal.isEmpty()) {
            return Optional.empty();
        }
        if (spendsGlobal.get()) {
            return Optional.of(DowntimeTarget.global(player));
        }
        return api.profiles().active(player).map(DowntimeTarget::character);
    }

    private static ProficiencyTraining toTraining(TrainingResult result) {
        return switch (result.outcome()) {
            case ADDED, TIER_UP -> ProficiencyTraining.TRAINED;
            case NO_FOCUS -> ProficiencyTraining.NO_FOCUS;
            case OTHER_SKILL -> ProficiencyTraining.OTHER_SKILL;
            case AT_MAX_TIER -> ProficiencyTraining.AT_MAX_TIER;
        };
    }

    private static CharacterRef toRef(CharacterId id) {
        return new CharacterRef(id.player(), id.profile());
    }

    private static CharacterId toId(CharacterRef character) {
        return new CharacterId(character.player(), character.profile());
    }
}
