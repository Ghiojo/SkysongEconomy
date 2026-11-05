package org.skysongdev.skysongEconomy.business.access;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A set of characters, what they may do at one business, and what they pay to train there.
 * <p>
 * Every business has two built-in groups whose members are implied: {@link #WORKERS} (its
 * workers) and {@link #EVERYONE}. Owners add their own groups, such as customers or a special
 * deal, and choose their members.
 *
 * @param id            lower case; unique within the business
 * @param permissions   what members may do
 * @param trainingPrice what members pay for one training session, 0 or more
 * @param members       the chosen members; always empty for the built-in groups
 */
public record AccessGroup(String id, Set<Permission> permissions, BigDecimal trainingPrice, Set<CharacterRef> members) {
    /** The built-in group of the business's workers. */
    public static final String WORKERS = "workers";
    /** The built-in group every character belongs to. */
    public static final String EVERYONE = "everyone";

    public AccessGroup {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(trainingPrice, "trainingPrice");
        if (trainingPrice.signum() < 0) {
            throw new IllegalArgumentException("trainingPrice must not be negative: " + trainingPrice);
        }
        permissions = permissions.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(permissions));
        members = Set.copyOf(members);
        if (isBuiltIn(id) && !members.isEmpty()) {
            throw new IllegalArgumentException("The built-in group " + id + " can't have chosen members");
        }
    }

    /** @return whether {@code id} names a built-in group */
    public static boolean isBuiltIn(String id) {
        return WORKERS.equals(id) || EVERYONE.equals(id);
    }

    public boolean allows(Permission permission) {
        return permissions.contains(permission);
    }

    public AccessGroup withPermission(Permission permission, boolean allowed) {
        Set<Permission> changed = permissions.isEmpty() ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(permissions);
        if (allowed) {
            changed.add(permission);
        } else {
            changed.remove(permission);
        }
        return new AccessGroup(id, changed, trainingPrice, members);
    }

    public AccessGroup withTrainingPrice(BigDecimal price) {
        return new AccessGroup(id, permissions, price, members);
    }

    public AccessGroup withMember(CharacterRef character, boolean member) {
        Set<CharacterRef> changed = new HashSet<>(members);
        if (member) {
            changed.add(character);
        } else {
            changed.remove(character);
        }
        return new AccessGroup(id, permissions, trainingPrice, changed);
    }
}
