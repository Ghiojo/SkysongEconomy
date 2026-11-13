package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * SkysongNPCs in memory, for unit tests: every gateway, behaving as the real plugin documents.
 * <p>
 * The public fields say what exists (templates, stats, proficiencies, abilities, presets) and
 * whether there is a free spot; tests change them freely. The methods at the bottom play the
 * player: assigning points, picking and promoting proficiencies, choosing abilities.
 */
public final class FakeSkysongNpcs implements NpcGateway, NpcSheetGateway, NpcAccessGateway {
    /** The ability source SkysongNPCs holds a chosen ability under. */
    public static final String CHOICE_PREFIX = "choice:";

    public final Set<String> templates = new LinkedHashSet<>(Set.of("guard"));
    public final Set<String> stats = new LinkedHashSet<>(List.of("strength", "dexterity", "wisdom"));
    /** Proficiency id to the skill it belongs to. */
    public final Map<String, String> proficiencies = new LinkedHashMap<>(Map.of(
            "swords", "martial", "shields", "martial", "perception", "economical", "survival", "economical"));
    public final List<String> tiers = List.of("untrained", "proficient", "adept", "master");
    public final Set<String> abilities = new LinkedHashSet<>(List.of("keen-eye", "pack-mule", "haggler"));
    public final Map<String, Set<String>> presets = new LinkedHashMap<>(Map.of("viewer", Set.of("info", "stats-view")));
    /** Whether {@link #create} finds a free spot. */
    public boolean freeSpot = true;
    /** Every {@link #create} call that reached the spot search, in order. */
    public final List<SpawnPoint> spawnPoints = new ArrayList<>();
    /** What was opened or teleported, in order, as {@code menu:1}, {@code choices:1} or {@code teleport:1} (the number of the NPC). */
    public final List<String> opened = new ArrayList<>();
    /** How often an NPC was given to another owner. */
    public int ownerChanges;

    private final Map<UUID, Npc> npcs = new LinkedHashMap<>();
    private final Map<String, Map<String, Set<String>>> policies = new LinkedHashMap<>();
    private int nextId = 1;

    private static final class Npc {
        final String template;
        @Nullable CharacterRef owner;
        boolean inSetup = true;
        @Nullable String policy;
        final List<Points> points = new ArrayList<>();
        final List<Picks> picks = new ArrayList<>();
        final List<Choice> choices = new ArrayList<>();
        final Map<String, Set<String>> abilityHolders = new LinkedHashMap<>();
        final Map<String, Modifier> modifiers = new LinkedHashMap<>();
        final Map<String, String> proficiencyTiers = new LinkedHashMap<>();

        Npc(String template, @Nullable CharacterRef owner) {
            this.template = template;
            this.owner = owner;
        }
    }

    private static final class Points {
        final String source;
        final Set<String> eligible;
        int amount;
        int spent;

        Points(String source, int amount, Collection<String> eligible) {
            this.source = source;
            this.amount = amount;
            this.eligible = Set.copyOf(eligible);
        }
    }

    private static final class Picks {
        final String source;
        final String skill;
        final String tier;
        final @Nullable String promoteTo;
        final int promotionCost;
        int count;
        /** Picked proficiency to the tier it had before. */
        final Map<String, String> picked = new LinkedHashMap<>();
        final List<String> promoted = new ArrayList<>();

        Picks(String source, String skill, String tier, int count, @Nullable String promoteTo, int promotionCost) {
            this.source = source;
            this.skill = skill;
            this.tier = tier;
            this.count = count;
            this.promoteTo = promoteTo;
            this.promotionCost = promoteTo == null ? 0 : promotionCost;
        }

        int unspent() {
            return count - picked.size() - promoted.size() * promotionCost;
        }
    }

    private static final class Choice {
        final String source;
        final List<String> options;
        int count;
        final List<String> chosen = new ArrayList<>();

        Choice(String source, List<String> options, int count) {
            this.source = source;
            this.options = List.copyOf(options);
            this.count = count;
        }
    }

    /** A baked stat bonus, as tests read it back with {@link #modifiers}. */
    public record Modifier(String stat, int amount) {
    }

    // NpcGateway

    @Override
    public boolean exists(UUID npc) {
        return npcs.containsKey(npc);
    }

    @Override
    public NpcCreation create(String template, @Nullable CharacterRef owner, SpawnPoint where, int radius) {
        spawnPoints.add(where);
        if (!freeSpot) {
            return NpcCreation.failed(NpcCreation.Outcome.NO_SPOT);
        }
        if (!templates.contains(template)) {
            return NpcCreation.failed(NpcCreation.Outcome.UNKNOWN_TEMPLATE);
        }
        UUID id = new UUID(0, nextId++);
        npcs.put(id, new Npc(template, owner));
        return NpcCreation.created(id);
    }

    @Override
    public boolean remove(UUID npc) {
        return npcs.remove(npc) != null;
    }

    @Override
    public boolean setOwner(UUID npc, @Nullable CharacterRef owner) {
        Npc found = npcs.get(npc);
        if (found == null) {
            return false;
        }
        found.owner = owner;
        ownerChanges++;
        return true;
    }

    @Override
    public boolean isOwnedBy(UUID npc, @Nullable CharacterRef owner) {
        Npc found = npcs.get(npc);
        return found != null && Objects.equals(found.owner, owner);
    }

    @Override
    public boolean inSetup(UUID npc) {
        Npc found = npcs.get(npc);
        return found != null && found.inSetup;
    }

    @Override
    public Optional<String> name(UUID npc) {
        return find(npc).map(found -> found.template + " " + npc.getLeastSignificantBits());
    }

    @Override
    public boolean openMenu(UUID player, UUID npc) {
        return exists(npc) && opened.add("menu:" + npc.getLeastSignificantBits());
    }

    @Override
    public boolean openChoices(UUID player, UUID npc) {
        return exists(npc) && opened.add("choices:" + npc.getLeastSignificantBits());
    }

    @Override
    public boolean teleportTo(UUID npc, UUID player) {
        return exists(npc) && opened.add("teleport:" + npc.getLeastSignificantBits());
    }

    // NpcSheetGateway

    @Override
    public NpcPending pending(UUID npc) {
        return find(npc).map(found -> new NpcPending(
                found.points.stream().mapToInt(grant -> grant.amount - grant.spent).sum(),
                found.picks.stream().mapToInt(Picks::unspent).sum(),
                found.choices.stream().mapToInt(choice -> choice.count - choice.chosen.size()).sum()))
                .orElse(NpcPending.NONE);
    }

    @Override
    public List<PointsGiven> points(UUID npc, String source) {
        return find(npc).map(found -> found.points.stream()
                .filter(grant -> grant.source.equals(source))
                .map(grant -> new PointsGiven(grant.amount, grant.spent, grant.eligible))
                .toList()).orElse(List.of());
    }

    @Override
    public void grantPoints(UUID npc, String source, int amount, Collection<String> eligible) {
        checkSource(source);
        checkAmount(amount);
        eligible.forEach(stat -> require(stats.contains(stat), "no stat " + stat));
        managed(npc).points.add(new Points(source, amount, eligible));
    }

    @Override
    public int takeUnspentPoints(UUID npc, String source, int max) {
        Optional<Npc> found = find(npc);
        if (found.isEmpty()) {
            return 0;
        }
        int taken = 0;
        List<Points> grants = found.get().points;
        for (int i = grants.size() - 1; i >= 0 && taken < max; i--) {
            Points grant = grants.get(i);
            if (grant.source.equals(source)) {
                int take = Math.min(max - taken, grant.amount - grant.spent);
                grant.amount -= take;
                taken += take;
                if (grant.amount == 0) {
                    grants.remove(i);
                }
            }
        }
        return taken;
    }

    @Override
    public List<PicksGiven> picks(UUID npc, String source) {
        return find(npc).map(found -> found.picks.stream()
                .filter(grant -> grant.source.equals(source))
                .map(grant -> new PicksGiven(grant.skill, grant.tier, grant.count, grant.unspent(), grant.promoteTo,
                        grant.promotionCost))
                .toList()).orElse(List.of());
    }

    @Override
    public void grantPicks(UUID npc, String source, String skill, String tier, int count, @Nullable String promoteTo,
                           int promotionCost) {
        checkSource(source);
        checkAmount(count);
        require(proficiencies.containsValue(skill), "no skill " + skill);
        require(tiers.contains(tier), "no tier " + tier);
        if (promoteTo != null) {
            require(tiers.indexOf(promoteTo) > tiers.indexOf(tier), promoteTo + " is not above " + tier);
            require(promotionCost >= 1, "promotion cost below 1");
        }
        Picks wanted = new Picks(source, skill, tier, count, promoteTo, promotionCost);
        for (Picks grant : managed(npc).picks) {
            if (grant.source.equals(source) && grant.skill.equals(skill) && grant.tier.equals(tier)
                    && Objects.equals(grant.promoteTo, wanted.promoteTo) && grant.promotionCost == wanted.promotionCost) {
                grant.count += count;
                return;
            }
        }
        managed(npc).picks.add(wanted);
    }

    @Override
    public int takeUnspentPicks(UUID npc, String source, int max) {
        Optional<Npc> found = find(npc);
        if (found.isEmpty()) {
            return 0;
        }
        int taken = 0;
        List<Picks> grants = found.get().picks;
        for (int i = grants.size() - 1; i >= 0 && taken < max; i--) {
            Picks grant = grants.get(i);
            if (grant.source.equals(source)) {
                int take = Math.min(max - taken, grant.unspent());
                grant.count -= take;
                taken += take;
                if (grant.count == 0) {
                    grants.remove(i);
                }
            }
        }
        return taken;
    }

    @Override
    public int respec(UUID npc, String source) {
        Optional<Npc> maybe = find(npc);
        if (maybe.isEmpty()) {
            return 0;
        }
        Npc found = maybe.get();
        int givenBack = 0;
        for (Points grant : found.points) {
            if (grant.source.equals(source)) {
                givenBack += grant.spent;
                grant.spent = 0;
            }
        }
        for (Picks grant : found.picks) {
            if (grant.source.equals(source)) {
                givenBack += grant.picked.size() + grant.promoted.size();
                grant.picked.forEach(found.proficiencyTiers::put);
                grant.picked.clear();
                grant.promoted.clear();
            }
        }
        for (Choice choice : found.choices) {
            if (choice.source.equals(source)) {
                givenBack += choice.chosen.size();
                choice.chosen.forEach(ability -> revokeAbility(npc, ability, CHOICE_PREFIX + source));
                choice.chosen.clear();
            }
        }
        return givenBack;
    }

    @Override
    public boolean holdsAbility(UUID npc, String ability, String source) {
        return find(npc).map(found -> found.abilityHolders.getOrDefault(ability, Set.of()).contains(source))
                .orElse(false);
    }

    @Override
    public Set<String> abilitiesHeldBy(UUID npc, String source) {
        Set<String> held = new LinkedHashSet<>();
        find(npc).ifPresent(found -> found.abilityHolders.forEach((ability, holders) -> {
            if (holders.contains(source)) {
                held.add(ability);
            }
        }));
        return held;
    }

    @Override
    public boolean grantAbility(UUID npc, String ability, String source) {
        require(abilities.contains(ability), "no ability " + ability);
        require(!source.isBlank(), "blank source");
        return managed(npc).abilityHolders.computeIfAbsent(ability, key -> new LinkedHashSet<>()).add(source);
    }

    @Override
    public boolean revokeAbility(UUID npc, String ability, String source) {
        Optional<Npc> found = find(npc);
        if (found.isEmpty()) {
            return false;
        }
        Set<String> holders = found.get().abilityHolders.get(ability);
        if (holders == null || !holders.remove(source)) {
            return false;
        }
        if (holders.isEmpty()) {
            found.get().abilityHolders.remove(ability);
        }
        return true;
    }

    @Override
    public List<AbilityChoiceGiven> abilityChoices(UUID npc, String source) {
        return find(npc).map(found -> found.choices.stream()
                .filter(choice -> choice.source.equals(source))
                .map(choice -> new AbilityChoiceGiven(choice.options, choice.count, choice.chosen))
                .toList()).orElse(List.of());
    }

    @Override
    public void grantAbilityChoice(UUID npc, String source, List<String> options, int count) {
        checkSource(source);
        checkAmount(count);
        options.forEach(ability -> require(abilities.contains(ability), "no ability " + ability));
        List<String> distinct = List.copyOf(new LinkedHashSet<>(options));
        for (Choice choice : managed(npc).choices) {
            if (choice.source.equals(source) && choice.options.equals(distinct)) {
                choice.count += count;
                return;
            }
        }
        managed(npc).choices.add(new Choice(source, distinct, count));
    }

    @Override
    public int takeUnspentAbilityChoices(UUID npc, String source) {
        Optional<Npc> found = find(npc);
        if (found.isEmpty()) {
            return 0;
        }
        int taken = 0;
        List<Choice> choices = found.get().choices;
        for (int i = choices.size() - 1; i >= 0; i--) {
            Choice choice = choices.get(i);
            if (choice.source.equals(source)) {
                taken += choice.count - choice.chosen.size();
                choice.count = choice.chosen.size();
                if (choice.count == 0) {
                    choices.remove(i);
                }
            }
        }
        return taken;
    }

    @Override
    public Optional<String> addModifier(UUID npc, String stat, int amount) {
        if (!stats.contains(stat)) {
            return Optional.empty();
        }
        return find(npc).map(found -> {
            String id = "m" + nextId++;
            found.modifiers.put(id, new Modifier(stat, amount));
            return id;
        });
    }

    @Override
    public boolean removeModifier(UUID npc, String modifierId) {
        return find(npc).map(found -> found.modifiers.remove(modifierId) != null).orElse(false);
    }

    @Override
    public boolean hasModifier(UUID npc, String modifierId) {
        return find(npc).map(found -> found.modifiers.containsKey(modifierId)).orElse(false);
    }

    @Override
    public Optional<String> proficiencyTier(UUID npc, String proficiency) {
        if (!proficiencies.containsKey(proficiency)) {
            return Optional.empty();
        }
        return find(npc).map(found -> found.proficiencyTiers.getOrDefault(proficiency, tiers.get(0)));
    }

    @Override
    public boolean setProficiencyTier(UUID npc, String proficiency, String tier) {
        Optional<Npc> found = find(npc);
        if (found.isEmpty() || !proficiencies.containsKey(proficiency) || !tiers.contains(tier)) {
            return false;
        }
        found.get().proficiencyTiers.put(proficiency, tier);
        return true;
    }

    @Override
    public List<String> tierIds() {
        return tiers;
    }

    @Override
    public boolean isStat(String stat) {
        return stats.contains(stat);
    }

    @Override
    public boolean isProficiency(String proficiency) {
        return proficiencies.containsKey(proficiency);
    }

    @Override
    public boolean isAbility(String ability) {
        return abilities.contains(ability);
    }

    // NpcAccessGateway

    @Override
    public Set<String> actions() {
        return new LinkedHashSet<>(List.of("info", "stats-view", "abilities-view", "wardrobe-view", "inventory-view",
                "equipment-view", "teleport", "follow", "look", "wardrobe-edit", "inventory-edit", "equipment-edit",
                "rename", "skin", "appearance", "choose", "pickup", "manage"));
    }

    @Override
    public Optional<Set<String>> preset(String name) {
        return Optional.ofNullable(presets.get(name));
    }

    @Override
    public boolean hasPolicy(String policyId) {
        return policies.containsKey(policyId);
    }

    @Override
    public boolean createPolicy(String policyId) {
        require(policyId.matches("[a-z0-9_.-]{1," + MAX_POLICY_ID_LENGTH + "}"), "not a policy id: " + policyId);
        return policies.putIfAbsent(policyId, new LinkedHashMap<>()) == null;
    }

    @Override
    public boolean deletePolicy(String policyId) {
        npcs.values().forEach(npc -> {
            if (policyId.equals(npc.policy)) {
                npc.policy = null;
            }
        });
        return policies.remove(policyId) != null;
    }

    @Override
    public Map<String, Set<String>> grants(String policyId) {
        return new LinkedHashMap<>(policies.getOrDefault(policyId, Map.of()));
    }

    @Override
    public void setGrant(String policyId, String group, Set<String> actions) {
        require(policies.containsKey(policyId), "no policy " + policyId);
        actions.forEach(action -> require(actions().contains(action), "not an NPC action: " + action));
        if (actions.isEmpty()) {
            policies.get(policyId).remove(group);
        } else {
            policies.get(policyId).put(group, Set.copyOf(actions));
        }
    }

    @Override
    public void assign(UUID npc, String policyId) {
        require(policies.containsKey(policyId), "no policy " + policyId);
        managed(npc).policy = policyId;
    }

    @Override
    public boolean unassign(UUID npc) {
        Optional<Npc> found = find(npc);
        if (found.isEmpty() || found.get().policy == null) {
            return false;
        }
        found.get().policy = null;
        return true;
    }

    @Override
    public Optional<String> policyOf(UUID npc) {
        return find(npc).map(found -> found.policy);
    }

    // What tests read and what a player would do

    /** @return every NPC's id, oldest first */
    public List<UUID> all() {
        return List.copyOf(npcs.keySet());
    }

    /** @return the NPC's owner, empty for the server */
    public Optional<CharacterRef> ownerOf(UUID npc) {
        return Optional.ofNullable(managed(npc).owner);
    }

    public String templateOf(UUID npc) {
        return managed(npc).template;
    }

    /** @return the NPC's baked stat bonuses by modifier id */
    public Map<String, Modifier> modifiers(UUID npc) {
        return Map.copyOf(managed(npc).modifiers);
    }

    /** @return whether the NPC has the ability from any source */
    public boolean hasAbility(UUID npc, String ability) {
        return managed(npc).abilityHolders.containsKey(ability);
    }

    /** The player finishes the NPC's first setup. */
    public void completeSetup(UUID npc) {
        managed(npc).inSetup = false;
    }

    /** The player assigns points from the source's oldest grant that has enough. */
    public void spendPoints(UUID npc, String source, int amount) {
        Points grant = managed(npc).points.stream()
                .filter(candidate -> candidate.source.equals(source) && candidate.amount - candidate.spent >= amount)
                .findFirst().orElseThrow(() -> new IllegalStateException(source + " has no " + amount + " unspent points"));
        grant.spent += amount;
    }

    /** The player spends one of the source's picks on a proficiency, raising it to the grant's tier. */
    public void spendPick(UUID npc, String source, String proficiency) {
        Npc found = managed(npc);
        Picks grant = found.picks.stream()
                .filter(candidate -> candidate.source.equals(source) && candidate.unspent() >= 1
                        && candidate.skill.equals(proficiencies.get(proficiency)) && !candidate.picked.containsKey(proficiency))
                .findFirst().orElseThrow(() -> new IllegalStateException(source + " has no pick for " + proficiency));
        grant.picked.put(proficiency, found.proficiencyTiers.getOrDefault(proficiency, tiers.get(0)));
        found.proficiencyTiers.put(proficiency, grant.tier);
    }

    /**
     * The player promotes a proficiency the source's grant picked, one tier up.
     *
     * @return false when no grant of that source picked it, may promote it further and has the picks
     */
    public boolean promote(UUID npc, String source, String proficiency) {
        Npc found = managed(npc);
        for (Picks grant : found.picks) {
            String current = found.proficiencyTiers.get(proficiency);
            if (grant.source.equals(source) && grant.picked.containsKey(proficiency) && grant.promoteTo != null
                    && tiers.indexOf(current) < tiers.indexOf(grant.promoteTo) && grant.unspent() >= grant.promotionCost) {
                grant.promoted.add(proficiency);
                found.proficiencyTiers.put(proficiency, tiers.get(tiers.indexOf(current) + 1));
                return true;
            }
        }
        return false;
    }

    /** The player chooses an ability from one of the source's choices. */
    public void chooseAbility(UUID npc, String source, String ability) {
        Choice choice = managed(npc).choices.stream()
                .filter(candidate -> candidate.source.equals(source) && candidate.options.contains(ability)
                        && candidate.chosen.size() < candidate.count && !candidate.chosen.contains(ability))
                .findFirst().orElseThrow(() -> new IllegalStateException(source + " offers no " + ability));
        choice.chosen.add(ability);
        grantAbility(npc, ability, CHOICE_PREFIX + source);
    }

    private Optional<Npc> find(UUID npc) {
        return Optional.ofNullable(npcs.get(npc));
    }

    private Npc managed(UUID npc) {
        Npc found = npcs.get(npc);
        require(found != null, "not a managed NPC: " + npc);
        return found;
    }

    private static void checkSource(String source) {
        require(!source.isBlank() && source.length() <= MAX_SOURCE_LENGTH && !source.contains(" "),
                "not a source: " + source);
    }

    private static void checkAmount(int amount) {
        require(amount >= 1, "amount below 1: " + amount);
    }

    private static void require(boolean condition, String problem) {
        if (!condition) {
            throw new IllegalArgumentException(problem);
        }
    }
}
