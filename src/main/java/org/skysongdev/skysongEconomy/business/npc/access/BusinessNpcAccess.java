package org.skysongdev.skysongEconomy.business.npc.access;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.business.npc.BusinessGroupRef;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcAccessGateway;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * What each access group of a business may do with the business's NPCs.
 * <p>
 * Every business has one SkysongNPCs access policy, named by {@link PolicyIds}. Its grants name
 * the business's groups as {@code economy:<business>/<group>}, and every NPC of the business
 * follows it, so one change applies to all of them, including NPCs created later. SkysongNPCs
 * stores the policy; nothing is saved here. The policy is made the first time it is needed,
 * with the defaults from {@code businesses/npcs.yml}.
 * <p>
 * The owner needs no grant: SkysongNPCs lets an NPC's owner do everything. Not thread-safe: use
 * it on the main thread.
 */
public final class BusinessNpcAccess implements BusinessChangeListener, BusinessNpcEvents {
    private final NpcAccessDefaults defaults;
    private final BusinessService businesses;
    private final AccessService access;
    private final BusinessNpcs npcs;
    private final NpcAccessGateway gateway;
    private final NpcActions actions;
    private final Consumer<String> warnings;

    /**
     * What the service works with.
     *
     * @param businesses every business
     * @param access     the businesses' access groups
     * @param npcs       which NPCs each business has
     * @param gateway    SkysongNPCs' policies
     */
    public record Dependencies(BusinessService businesses, AccessService access, BusinessNpcs npcs, NpcAccessGateway gateway) {
    }

    /** @param warnings told about NPCs that could not be made to follow their policy, as console lines */
    public BusinessNpcAccess(NpcAccessDefaults defaults, Dependencies dependencies, Consumer<String> warnings) {
        this.defaults = defaults;
        this.businesses = dependencies.businesses();
        this.access = dependencies.access();
        this.npcs = dependencies.npcs();
        this.gateway = dependencies.gateway();
        this.actions = new NpcActions(gateway);
        this.warnings = warnings;
    }

    /**
     * Makes every business NPC follow its business's policy. Call it after
     * {@link BusinessNpcs#loadAll()}.
     *
     * @return how many NPCs were not following it yet
     */
    public int loadAll() {
        int assigned = 0;
        for (Business business : businesses.all()) {
            if (!npcs.of(business).isEmpty()) {
                assigned += sync(business);
            }
        }
        return assigned;
    }

    /** @return SkysongNPCs' action names and presets */
    public NpcActions actions() {
        return actions;
    }

    /**
     * @return whether the character may change what groups may do with the NPCs: the owner, or
     *         anyone in a group with {@code edit-npc-access}. Staff are the caller's to allow.
     */
    public boolean mayEdit(Business business, CharacterRef character) {
        return access.allows(business, character, Permission.EDIT_NPC_ACCESS);
    }

    /**
     * @return what each group of the business may do with its NPCs, by group id, in the order the
     *         business lists its groups; a group that may do nothing has an empty set
     */
    public Map<String, Set<String>> grants(Business business) {
        String policy = policy(business);
        Map<String, Set<String>> granted = gateway.grants(policy);
        Map<String, Set<String>> byGroup = new LinkedHashMap<>();
        for (AccessGroup group : access.groups(business)) {
            byGroup.put(group.id(), granted.getOrDefault(reference(business, group.id()), Set.of()));
        }
        return byGroup;
    }

    /**
     * @return what one group may do with the business's NPCs
     * @throws BusinessException when the business has no such group
     */
    public Set<String> allowed(Business business, String groupId) {
        String group = access.group(business, groupId).id();
        return gateway.grants(policy(business)).getOrDefault(reference(business, group), Set.of());
    }

    /**
     * Lets a group do something with the business's NPCs, or stops it.
     *
     * @param written an action name, or a SkysongNPCs preset that stands for several
     * @param allowed true to allow, false to deny
     * @return what the group may do now
     * @throws BusinessException when the business has no such group, or {@code written} is
     *                           neither an action nor a preset
     */
    public Set<String> set(Business business, String groupId, String written, boolean allowed) {
        Set<String> changed = actions.resolve(written).orElseThrow(() -> new BusinessException(
                "business.npcs.access.unknown-action", Map.of("action", written, "actions", String.join(", ", actions.all()))));
        String group = access.group(business, groupId).id();
        Set<String> now = new LinkedHashSet<>(allowed(business, group));
        if (allowed) {
            now.addAll(changed);
        } else {
            now.removeAll(changed);
        }
        gateway.setGrant(policy(business), reference(business, group), now);
        return Collections.unmodifiableSet(now);
    }

    /**
     * Allows an action the group may not do, or denies one it may.
     *
     * @return whether the group may do it now
     */
    public boolean toggle(Business business, String groupId, String action) {
        boolean allow = !allowed(business, groupId).contains(action);
        set(business, groupId, action, allow);
        return allow;
    }

    /**
     * Brings the business's policy and NPCs in line: makes the policy if it is missing, drops
     * grants of groups the business no longer has, and makes every NPC follow the policy.
     *
     * @return how many NPCs were not following it yet
     */
    public int sync(Business business) {
        String policy = policy(business);
        Set<String> groups = new LinkedHashSet<>();
        access.groups(business).forEach(group -> groups.add(reference(business, group.id())));
        gateway.grants(policy).forEach((group, allowed) -> {
            if (!groups.contains(group) && isOf(business, group)) {
                gateway.setGrant(policy, group, Set.of());
            }
        });
        int assigned = 0;
        for (BusinessNpc npc : npcs.of(business)) {
            if (follow(npc.npc(), policy)) {
                assigned++;
            }
        }
        return assigned;
    }

    /**
     * Staff copied an NPC in SkysongNPCs. The copy of a business NPC is not a business NPC, so it
     * stops following the business's policy.
     */
    public void copied(UUID original, UUID copy) {
        npcs.find(original).ifPresent(found -> {
            if (gateway.policyOf(copy).filter(PolicyIds.of(found.businessId())::equals).isPresent()) {
                gateway.unassign(copy);
            }
        });
    }

    /**
     * The owner deleted a group: what it could do with the NPCs is forgotten, so a later group
     * of the same name starts with nothing.
     */
    public void groupDeleted(Business business, String groupId) {
        String policy = PolicyIds.of(business.id());
        if (gateway.hasPolicy(policy)) {
            gateway.setGrant(policy, reference(business, groupId), Set.of());
        }
    }

    /** A deleted business's policy is deleted with it. */
    @Override
    public void deleted(Business business) {
        gateway.deletePolicy(PolicyIds.of(business.id()));
    }

    /** A new NPC follows its business's policy from the start. */
    @Override
    public void created(Business business, BusinessNpc npc) {
        follow(npc.npc(), policy(business));
    }

    /** An NPC that stays in the world after leaving the business (released in SkysongNPCs) stops following the policy. */
    @Override
    public void removed(Business business, BusinessNpc npc, RemovalCause cause) {
        if (cause == RemovalCause.REMOVED_ELSEWHERE
                && gateway.policyOf(npc.npc()).filter(PolicyIds.of(business.id())::equals).isPresent()) {
            gateway.unassign(npc.npc());
        }
    }

    /** @return the id of the business's policy, made now with the defaults when it did not exist */
    private String policy(Business business) {
        String policy = PolicyIds.of(business.id());
        if (gateway.createPolicy(policy)) {
            defaults.byGroup().forEach((group, allowed) -> {
                if (!allowed.isEmpty()) {
                    gateway.setGrant(policy, reference(business, group), allowed);
                }
            });
        }
        return policy;
    }

    /** @return whether the NPC was made to follow the policy now */
    private boolean follow(UUID npc, String policy) {
        if (gateway.policyOf(npc).filter(policy::equals).isPresent()) {
            return false;
        }
        try {
            gateway.assign(npc, policy);
            return true;
        } catch (RuntimeException e) {
            warnings.accept("The NPC " + npc + " could not be made to follow the access policy " + policy + ": " + e);
            return false;
        }
    }

    private static String reference(Business business, String groupId) {
        return new BusinessGroupRef(business.id(), groupId).toString();
    }

    /** @return whether a grant's group is one of this business's own, whether or not it still exists */
    private static boolean isOf(Business business, String group) {
        String prefix = BusinessGroupRef.TYPE + ":";
        return group.startsWith(prefix) && BusinessGroupRef.parseId(group.substring(prefix.length()))
                .filter(reference -> reference.businessId().equals(business.id())).isPresent();
    }
}
