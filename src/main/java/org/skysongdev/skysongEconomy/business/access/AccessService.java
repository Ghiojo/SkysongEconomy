package org.skysongdev.skysongEconomy.business.access;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Who may train, order imports and sell exports at each business, and what training costs them.
 * <p>
 * Each business has the built-in groups {@code workers} and {@code everyone}, plus the groups its
 * owner adds. A character may do something when any group they belong to allows it, and pays the
 * lowest training price among the groups that let them train. The owner may do everything and
 * trains for free.
 * <p>
 * A business whose owner never changed its groups uses the defaults from
 * {@code businesses/access.yml}, so editing that file changes them too. The first change saves
 * all of its groups. Not thread-safe: use it on the main thread.
 */
public final class AccessService implements BusinessChangeListener {
    /** Longest group name, in characters. */
    public static final int MAX_GROUP_ID_LENGTH = 32;

    private static final Pattern GROUP_ID = Pattern.compile("[a-z0-9_-]{1," + MAX_GROUP_ID_LENGTH + "}");

    private final AccessDefaults defaults;
    private final AccessRepository repository;
    /** Saved groups by business id, built-in groups first. */
    private final Map<String, LinkedHashMap<String, AccessGroup>> saved = new HashMap<>();

    public AccessService(AccessDefaults defaults, AccessRepository repository) {
        this.defaults = defaults;
        this.repository = repository;
    }

    /** Loads the saved groups of every business. */
    public void loadAll() {
        saved.clear();
        repository.loadAll().forEach((businessId, groups) -> {
            LinkedHashMap<String, AccessGroup> byId = withDefaults();
            groups.forEach(group -> byId.put(group.id(), group));
            saved.put(businessId, byId);
        });
    }

    /** @return every group of the business: workers, everyone, then the owner's in the order they were made */
    public List<AccessGroup> groups(Business business) {
        return List.copyOf(groupMap(business).values());
    }

    /**
     * @throws BusinessException {@code business.access.no-such-group} when the business has no such group
     */
    public AccessGroup group(Business business, String groupId) {
        AccessGroup group = groupMap(business).get(groupId.toLowerCase(Locale.ROOT));
        if (group == null) {
            throw new BusinessException("business.access.no-such-group", Map.of("group", groupId));
        }
        return group;
    }

    /** @return the groups the character belongs to, built-in ones included; the owner needs none */
    public List<AccessGroup> groupsOf(Business business, CharacterRef character) {
        List<AccessGroup> groups = new ArrayList<>();
        for (AccessGroup group : groupMap(business).values()) {
            if (isIn(business, group, character)) {
                groups.add(group);
            }
        }
        return groups;
    }

    /** @return whether the character may do that at the business */
    public boolean allows(Business business, CharacterRef character, Permission permission) {
        return business.isOwner(character)
                || groupsOf(business, character).stream().anyMatch(group -> group.allows(permission));
    }

    /**
     * @return what the character pays for one training session: 0 for the owner, otherwise the
     *         lowest price among their groups that allow training; empty when they may not train
     */
    public Optional<BigDecimal> trainingPrice(Business business, CharacterRef character) {
        if (business.isOwner(character)) {
            return Optional.of(BigDecimal.ZERO);
        }
        return groupsOf(business, character).stream()
                .filter(group -> group.allows(Permission.TRAIN))
                .map(AccessGroup::trainingPrice)
                .min(Comparator.naturalOrder());
    }

    /**
     * Adds an empty group that allows nothing and charges the Everyone price for training.
     *
     * @throws BusinessException when the name is invalid or taken, or the business has too many groups
     */
    public AccessGroup createGroup(Business business, String groupId) {
        String id = groupId.toLowerCase(Locale.ROOT);
        if (!GROUP_ID.matcher(id).matches()) {
            throw new BusinessException("business.access.invalid-group-id",
                    Map.of("max", String.valueOf(MAX_GROUP_ID_LENGTH)));
        }
        LinkedHashMap<String, AccessGroup> groups = groupMap(business);
        if (groups.containsKey(id)) {
            throw new BusinessException("business.access.group-taken", Map.of("group", id));
        }
        long custom = groups.keySet().stream().filter(existing -> !AccessGroup.isBuiltIn(existing)).count();
        if (custom >= defaults.maxCustomGroups()) {
            throw new BusinessException("business.access.too-many-groups",
                    Map.of("max", String.valueOf(defaults.maxCustomGroups())));
        }
        AccessGroup group = new AccessGroup(id, Set.of(), defaults.everyone().trainingPrice(), Set.of());
        return put(business, group);
    }

    /**
     * Deletes one of the owner's groups.
     *
     * @throws BusinessException when there is no such group or it is built in
     */
    public void deleteGroup(Business business, String groupId) {
        AccessGroup group = requireCustom(business, groupId, "business.access.built-in-delete");
        LinkedHashMap<String, AccessGroup> groups = editable(business);
        groups.remove(group.id());
        save(business, groups);
    }

    /** Lets the group's members do that, or stops them. */
    public AccessGroup setPermission(Business business, String groupId, Permission permission, boolean allowed) {
        return put(business, group(business, groupId).withPermission(permission, allowed));
    }

    /**
     * @throws BusinessException {@code business.access.invalid-price} when the price is negative
     */
    public AccessGroup setTrainingPrice(Business business, String groupId, BigDecimal price) {
        if (price.signum() < 0) {
            throw new BusinessException("business.access.invalid-price", Map.of("price", price.toPlainString()));
        }
        return put(business, group(business, groupId).withTrainingPrice(price));
    }

    /**
     * Adds a character to one of the owner's groups.
     *
     * @throws BusinessException when the group is built in, or they are already in it
     */
    public AccessGroup addMember(Business business, String groupId, CharacterRef character) {
        AccessGroup group = requireCustom(business, groupId, "business.access.built-in-members");
        if (group.members().contains(character)) {
            throw new BusinessException("business.access.already-member",
                    Map.of("character", character.profile(), "group", group.id()));
        }
        return put(business, group.withMember(character, true));
    }

    /**
     * Removes a character from one of the owner's groups.
     *
     * @throws BusinessException when the group is built in, or they are not in it
     */
    public AccessGroup removeMember(Business business, String groupId, CharacterRef character) {
        AccessGroup group = requireCustom(business, groupId, "business.access.built-in-members");
        if (!group.members().contains(character)) {
            throw new BusinessException("business.access.not-member",
                    Map.of("character", character.profile(), "group", group.id()));
        }
        return put(business, group.withMember(character, false));
    }

    /** A deleted business's groups go with it; the database removes their rows itself. */
    @Override
    public void deleted(Business business) {
        saved.remove(business.id());
    }

    private static boolean isIn(Business business, AccessGroup group, CharacterRef character) {
        return switch (group.id()) {
            case AccessGroup.EVERYONE -> true;
            case AccessGroup.WORKERS -> business.isWorker(character);
            default -> group.members().contains(character);
        };
    }

    private AccessGroup requireCustom(Business business, String groupId, String builtInKey) {
        AccessGroup group = group(business, groupId);
        if (AccessGroup.isBuiltIn(group.id())) {
            throw new BusinessException(builtInKey, Map.of("group", group.id()));
        }
        return group;
    }

    private AccessGroup put(Business business, AccessGroup group) {
        LinkedHashMap<String, AccessGroup> groups = editable(business);
        groups.put(group.id(), group);
        save(business, groups);
        return group;
    }

    private void save(Business business, LinkedHashMap<String, AccessGroup> groups) {
        saved.put(business.id(), groups);
        repository.save(business.id(), List.copyOf(groups.values()));
    }

    /** @return the business's groups, the defaults when it has none saved; don't change the result */
    private LinkedHashMap<String, AccessGroup> groupMap(Business business) {
        LinkedHashMap<String, AccessGroup> groups = saved.get(business.id());
        return groups != null ? groups : withDefaults();
    }

    /** @return a copy of the business's groups to change and save */
    private LinkedHashMap<String, AccessGroup> editable(Business business) {
        return new LinkedHashMap<>(groupMap(business));
    }

    private LinkedHashMap<String, AccessGroup> withDefaults() {
        LinkedHashMap<String, AccessGroup> groups = new LinkedHashMap<>();
        groups.put(AccessGroup.WORKERS, defaults.workers());
        groups.put(AccessGroup.EVERYONE, defaults.everyone());
        return groups;
    }
}
