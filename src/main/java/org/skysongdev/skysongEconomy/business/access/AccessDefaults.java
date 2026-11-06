package org.skysongdev.skysongEconomy.business.access;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * Settings from {@code businesses/access.yml}: what the built-in groups start with, and how many
 * groups an owner may add.
 * <pre>{@code
 * max-custom-groups: 8
 * defaults:
 *   workers:  {permissions: [train, sell-exports], training-price: 0}
 *   everyone: {permissions: [], training-price: 10}
 * }</pre>
 *
 * @param workers         the Workers group of a new business
 * @param everyone        the Everyone group of a new business
 * @param maxCustomGroups how many groups an owner may add besides the built-in ones
 */
public record AccessDefaults(AccessGroup workers, AccessGroup everyone, int maxCustomGroups) {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/access.yml";

    /**
     * @throws ConfigException when a permission is unknown or a number is out of range
     */
    public static AccessDefaults parse(ConfigSection root) {
        int max = root.integer("max-custom-groups", 8);
        if (max < 0) {
            throw root.error("max-custom-groups", "must be 0 or more");
        }
        ConfigSection defaults = root.section("defaults");
        return new AccessDefaults(
                group(defaults.section(AccessGroup.WORKERS), AccessGroup.WORKERS,
                        EnumSet.of(Permission.TRAIN, Permission.SELL_EXPORTS), BigDecimal.ZERO),
                group(defaults.section(AccessGroup.EVERYONE), AccessGroup.EVERYONE,
                        EnumSet.noneOf(Permission.class), BigDecimal.TEN),
                max);
    }

    private static AccessGroup group(ConfigSection section, String id, Set<Permission> fallbackPermissions,
                                     BigDecimal fallbackPrice) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        if (section.contains("permissions")) {
            for (String key : section.strings("permissions")) {
                permissions.add(Permission.fromKey(key)
                        .orElseThrow(() -> section.error("permissions", "has an unknown permission '" + key
                                + "': use train, order-imports, sell-exports or tend-farms")));
            }
        } else {
            permissions.addAll(fallbackPermissions);
        }
        double price = section.number("training-price", fallbackPrice.doubleValue());
        if (price < 0) {
            throw section.error("training-price", "must be 0 or more");
        }
        return new AccessGroup(id, permissions, BigDecimal.valueOf(price).stripTrailingZeros(), Set.of());
    }
}
