package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import org.skysongdev.skysongEconomy.gui.business.NpcMenuButtons;
import java.util.function.Function;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.npc.AbilityChoiceGiven;
import org.skysongdev.skysongEconomy.business.npc.NpcAccessGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcCreation;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcPending;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.PicksGiven;
import org.skysongdev.skysongEconomy.business.npc.PointsGiven;
import org.skysongdev.skysongEconomy.business.npc.SpawnPoint;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongnpcs.api.ability.NpcAbility;
import org.skysongdev.skysongnpcs.api.SkysongNpcsApi;
import org.skysongdev.skysongnpcs.api.access.AccessGrant;
import org.skysongdev.skysongnpcs.api.access.AccessGroup;
import org.skysongdev.skysongnpcs.api.access.GroupProvider;
import org.skysongdev.skysongnpcs.api.access.NpcAction;
import org.skysongdev.skysongnpcs.api.npc.NpcOwner;
import org.skysongdev.skysongnpcs.api.policy.AccessPolicy;
import org.skysongdev.skysongnpcs.api.policy.PolicyOwner;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Modifier;
import org.skysongdev.skysongstats.api.Proficiency;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongstats.api.Tier;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Connects the business features to SkysongNPCs, which owns the NPCs, their choices, abilities
 * and access, and to the SkysongStats sheet each NPC has.
 * <p>
 * This is the only class that talks to the SkysongNPCs API; everything else depends on the small
 * interfaces it implements. SkysongNPCs is optional, so nothing may load this class unless that
 * plugin is enabled. Like the API itself, it must be used on the main thread.
 */
public final class SkysongNpcsAdapter implements NpcGateway, NpcSheetGateway, NpcAccessGateway, NpcMenuButtons {
    /** The oldest SkysongNPCs API with everything used here: sources, respec by source, promotion, ability choices. */
    public static final ApiVersion REQUIRED_API = new ApiVersion(0, 23, 0);

    /** How far in front of a player to start looking for a free spot, in blocks. */
    private static final double IN_FRONT = 2.0;

    private final Plugin plugin;
    private final SkysongNpcsApi npcs;
    private final SkysongStatsApi stats;

    /**
     * @param plugin this plugin, the owner of the policies and group provider it registers
     */
    public SkysongNpcsAdapter(Plugin plugin, SkysongNpcsApi npcs, SkysongStatsApi stats) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.npcs = Objects.requireNonNull(npcs, "npcs");
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    /**
     * Connects to the running SkysongNPCs.
     *
     * @throws IllegalStateException when SkysongNPCs is not enabled, or is older than {@link #REQUIRED_API}
     */
    public static SkysongNpcsAdapter connect(Plugin plugin, SkysongStatsApi stats) {
        SkysongNpcsApi api = SkysongNpcsApi.get();
        if (!ApiVersion.parse(api.apiVersion()).isAtLeast(REQUIRED_API)) {
            throw new IllegalStateException("SkysongNPCs has API " + api.apiVersion() + ", but API " + REQUIRED_API
                    + " or newer is needed: update SkysongNPCs");
        }
        return new SkysongNpcsAdapter(plugin, api, stats);
    }

    /**
     * Lets SkysongNPCs' grants and policies name this plugin's groups as {@code <type>:<id>}.
     * SkysongNPCs drops the provider by itself when this plugin disables.
     *
     * @throws IllegalStateException when another plugin registered the type already
     */
    public void registerGroups(String type, GroupProvider provider) {
        npcs.access().registerGroupProvider(plugin, type, provider);
    }

    @Override
    public void addInfoButton(String name, int slot, Function<UUID, @Nullable ItemStack> icon) {
        npcs.menu().registerButton(plugin, name, slot, context -> icon.apply(context.npc().id()),
                context -> icon.apply(context.npc().id()) != null, click -> { });
    }

    // NpcGateway

    @Override
    public boolean exists(UUID npc) {
        return npcs.npcs().isManaged(npc);
    }

    @Override
    public NpcCreation create(String template, @Nullable CharacterRef owner, SpawnPoint where, int radius) {
        Spot spot = switch (where) {
            case SpawnPoint.InFrontOf inFront -> inFrontOf(inFront, radius);
            case SpawnPoint.NextTo nextTo -> nextTo(nextTo, radius);
        };
        if (spot.location() == null) {
            return NpcCreation.failed(spot.problem());
        }
        try {
            return NpcCreation.created(npcs.npcs().create(template, null, toOwner(owner), spot.location()).id());
        } catch (IllegalArgumentException unknownTemplate) {
            return NpcCreation.failed(NpcCreation.Outcome.UNKNOWN_TEMPLATE);
        } catch (IllegalStateException refused) {
            return NpcCreation.failed(NpcCreation.Outcome.REFUSED);
        }
    }

    @Override
    public boolean remove(UUID npc) {
        return npcs.npcs().remove(npc);
    }

    @Override
    public boolean setOwner(UUID npc, @Nullable CharacterRef owner) {
        if (!exists(npc)) {
            return false;
        }
        npcs.npcs().setOwner(npc, toOwner(owner));
        return true;
    }

    @Override
    public boolean isOwnedBy(UUID npc, @Nullable CharacterRef owner) {
        return npcs.npcs().get(npc).map(found -> found.owner().equals(toOwner(owner))).orElse(false);
    }

    @Override
    public boolean inSetup(UUID npc) {
        return npcs.choices().inSetup(npc);
    }

    @Override
    public Optional<String> name(UUID npc) {
        // SkysongNPCs keeps names as MiniMessage text.
        return exists(npc) ? Optional.of(MiniMessage.miniMessage().stripTags(npcs.appearance().name(npc))) : Optional.empty();
    }

    @Override
    public boolean openMenu(UUID player, UUID npc) {
        Player viewer = Bukkit.getPlayer(player);
        return viewer != null && npcs.menu().open(viewer, npc);
    }

    @Override
    public boolean openChoices(UUID player, UUID npc) {
        Player viewer = Bukkit.getPlayer(player);
        return viewer != null && npcs.choices().open(viewer, npc);
    }

    @Override
    public boolean teleportTo(UUID npc, UUID player) {
        Player target = Bukkit.getPlayer(player);
        return target != null && npcs.movement().teleport(npc, target.getLocation());
    }

    // NpcSheetGateway

    @Override
    public NpcPending pending(UUID npc) {
        int abilityChoices = npcs.abilities().choices(npc).stream().mapToInt(choice -> choice.remaining()).sum();
        return new NpcPending(npcs.choices().unspentPoints(npc), npcs.choices().unspentPicks(npc), abilityChoices);
    }

    @Override
    public List<PointsGiven> points(UUID npc, String source) {
        return npcs.choices().pointGrants(npc).stream()
                .filter(grant -> grant.source().equals(source))
                .map(grant -> new PointsGiven(grant.amount(), grant.spent(), grant.eligible()))
                .toList();
    }

    @Override
    public void grantPoints(UUID npc, String source, int amount, Collection<String> eligible) {
        npcs.choices().grantPoints(npc, source, amount, eligible);
    }

    @Override
    public int takeUnspentPoints(UUID npc, String source, int max) {
        return npcs.choices().takeUnspentPoints(npc, source, max);
    }

    @Override
    public List<PicksGiven> picks(UUID npc, String source) {
        return npcs.choices().pickGrants(npc).stream()
                .filter(grant -> grant.source().equals(source))
                .map(grant -> new PicksGiven(grant.skill(), grant.tier(), grant.count(), grant.unspent(),
                        grant.promoteTo(), grant.promoteTo() == null ? 0 : grant.promotionCost()))
                .toList();
    }

    @Override
    public void grantPicks(UUID npc, String source, String skill, String tier, int count, @Nullable String promoteTo,
                           int promotionCost) {
        if (promoteTo == null) {
            npcs.choices().grantPicks(npc, source, skill, tier, count);
        } else {
            npcs.choices().grantPicks(npc, source, skill, tier, count, promoteTo, promotionCost);
        }
    }

    @Override
    public int takeUnspentPicks(UUID npc, String source, int max) {
        return npcs.choices().takeUnspentPicks(npc, source, max);
    }

    @Override
    public int respec(UUID npc, String source) {
        return npcs.choices().respec(npc, source);
    }

    @Override
    public boolean holdsAbility(UUID npc, String ability, String source) {
        return npcs.abilities().sources(npc, ability).contains(source);
    }

    @Override
    public Set<String> abilitiesHeldBy(UUID npc, String source) {
        Set<String> held = new LinkedHashSet<>();
        for (NpcAbility ability : npcs.abilities().of(npc)) {
            if (npcs.abilities().sources(npc, ability.id()).contains(source)) {
                held.add(ability.id());
            }
        }
        return held;
    }

    @Override
    public boolean grantAbility(UUID npc, String ability, String source) {
        return npcs.abilities().grant(npc, ability, source);
    }

    @Override
    public boolean revokeAbility(UUID npc, String ability, String source) {
        return npcs.abilities().revoke(npc, ability, source);
    }

    @Override
    public List<AbilityChoiceGiven> abilityChoices(UUID npc, String source) {
        return npcs.abilities().choices(npc).stream()
                .filter(choice -> choice.source().equals(source))
                .map(choice -> new AbilityChoiceGiven(choice.options(), choice.count(), choice.chosen()))
                .toList();
    }

    @Override
    public void grantAbilityChoice(UUID npc, String source, List<String> options, int count) {
        npcs.abilities().grantChoice(npc, source, options, count);
    }

    @Override
    public int takeUnspentAbilityChoices(UUID npc, String source) {
        return npcs.abilities().takeUnspentChoices(npc, source);
    }

    @Override
    public Optional<String> addModifier(UUID npc, String stat, int amount) {
        if (!isStat(stat)) {
            return Optional.empty();
        }
        return sheet(npc).flatMap(character -> stats.modifiers().add(character, stat, amount)).map(Modifier::id);
    }

    @Override
    public boolean removeModifier(UUID npc, String modifierId) {
        return sheet(npc).map(character -> stats.modifiers().remove(character, modifierId)).orElse(false);
    }

    @Override
    public boolean hasModifier(UUID npc, String modifierId) {
        return sheet(npc)
                .map(character -> stats.modifiers().list(character).stream()
                        .anyMatch(modifier -> modifier.id().equals(modifierId)))
                .orElse(false);
    }

    @Override
    public Optional<String> proficiencyTier(UUID npc, String proficiency) {
        Optional<Proficiency> found = stats.proficiencies().find(proficiency);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return sheet(npc).flatMap(character -> stats.proficiencies().tier(character, found.get())).map(Tier::id);
    }

    @Override
    public boolean setProficiencyTier(UUID npc, String proficiency, String tier) {
        Optional<CharacterId> character = sheet(npc);
        Optional<Proficiency> found = stats.proficiencies().find(proficiency);
        Optional<Tier> foundTier = stats.proficiencies().findTier(tier);
        if (character.isEmpty() || found.isEmpty() || foundTier.isEmpty()) {
            return false;
        }
        stats.proficiencies().setTier(character.get(), found.get(), foundTier.get());
        return true;
    }

    @Override
    public List<String> tierIds() {
        return stats.proficiencies().tiers().stream().map(Tier::id).toList();
    }

    @Override
    public boolean isStat(String stat) {
        return stats.stats().find(stat).isPresent();
    }

    @Override
    public boolean isProficiency(String proficiency) {
        return stats.proficiencies().find(proficiency).isPresent();
    }

    @Override
    public boolean isAbility(String ability) {
        return npcs.abilities().find(ability).isPresent();
    }

    // NpcAccessGateway

    @Override
    public Set<String> actions() {
        Set<String> keys = new LinkedHashSet<>();
        for (NpcAction action : NpcAction.values()) {
            keys.add(action.key());
        }
        return keys;
    }

    @Override
    public Optional<Set<String>> preset(String name) {
        return npcs.access().preset(name).map(SkysongNpcsAdapter::keys);
    }

    @Override
    public boolean hasPolicy(String policyId) {
        return npcs.policies().get(policyId).isPresent();
    }

    @Override
    public boolean createPolicy(String policyId) {
        if (hasPolicy(policyId)) {
            return false;
        }
        npcs.policies().create(policyId, PolicyOwner.plugin(plugin));
        return true;
    }

    @Override
    public boolean deletePolicy(String policyId) {
        return npcs.policies().delete(policyId);
    }

    @Override
    public Map<String, Set<String>> grants(String policyId) {
        Map<String, Set<String>> byGroup = new LinkedHashMap<>();
        npcs.policies().get(policyId).map(AccessPolicy::grants).orElse(List.of())
                .forEach(grant -> byGroup.put(grant.group().toString(), keys(grant)));
        return byGroup;
    }

    @Override
    public void setGrant(String policyId, String group, Set<String> actions) {
        if (!hasPolicy(policyId)) {
            throw new IllegalArgumentException("There is no NPC access policy " + policyId);
        }
        Set<NpcAction> allowed = EnumSet.noneOf(NpcAction.class);
        for (String key : actions) {
            allowed.add(NpcAction.fromKey(key)
                    .orElseThrow(() -> new IllegalArgumentException("Not an NPC action: " + key)));
        }
        npcs.policies().setGrant(policyId, AccessGroup.parse(group), allowed);
    }

    @Override
    public void assign(UUID npc, String policyId) {
        npcs.policies().assign(npc, policyId);
    }

    @Override
    public boolean unassign(UUID npc) {
        return npcs.policies().unassign(npc);
    }

    @Override
    public Optional<String> policyOf(UUID npc) {
        return npcs.policies().policyOf(npc).map(AccessPolicy::id);
    }

    /** A free spot for a new NPC, or why there is none. */
    private record Spot(@Nullable Location location, NpcCreation.Outcome problem) {
        static Spot of(Optional<Location> found) {
            return new Spot(found.orElse(null), NpcCreation.Outcome.NO_SPOT);
        }

        static Spot nowhere() {
            return new Spot(null, NpcCreation.Outcome.NOWHERE);
        }
    }

    private Spot inFrontOf(SpawnPoint.InFrontOf where, int radius) {
        Player player = Bukkit.getPlayer(where.player());
        if (player == null) {
            return Spot.nowhere();
        }
        Location near = player.getLocation();
        Vector ahead = near.getDirection().setY(0);
        if (ahead.lengthSquared() > 0) {
            near.add(ahead.normalize().multiply(IN_FRONT));
        }
        return Spot.of(npcs.npcs().findSpot(near, radius, player));
    }

    private Spot nextTo(SpawnPoint.NextTo where, int radius) {
        World world = Bukkit.getWorld(where.block().world());
        if (world == null) {
            return Spot.nowhere();
        }
        Location near = new Location(world, where.block().x() + 0.5, where.block().y(), where.block().z() + 0.5);
        if (where.placer() == null) {
            return Spot.of(npcs.npcs().findSpot(near, radius));
        }
        Player placer = Bukkit.getPlayer(where.placer());
        return placer == null ? Spot.nowhere() : Spot.of(npcs.npcs().findSpot(near, radius, placer));
    }

    /** The NPC's SkysongStats character, empty when it has no sheet. */
    private Optional<CharacterId> sheet(UUID npc) {
        return npcs.sheets().character(npc);
    }

    private static NpcOwner toOwner(@Nullable CharacterRef owner) {
        return owner == null ? NpcOwner.server() : NpcOwner.character(owner.player(), owner.profile());
    }

    private static Set<String> keys(AccessGrant grant) {
        return keys(grant.actions());
    }

    private static Set<String> keys(Set<NpcAction> actions) {
        Set<String> keys = new LinkedHashSet<>();
        actions.forEach(action -> keys.add(action.key()));
        return keys;
    }
}
