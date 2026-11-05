package org.skysongdev.skysongEconomy.business.shift;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reads {@code businesses/shifts.yml}:
 * <pre>{@code
 * duration-minutes: 30
 * downtime-cost: 1
 * default-message: "{character} started a shift at {business}"
 * pay:
 *   gathering: {1: {money: 5, tokens: 1}, 2: {money: 6, tokens: 1}}
 *   crafting:  {1: {money: 6}, 2: {money: 7}}
 * }</pre>
 * Types or tiers left out of {@code pay} pay nothing, with a warning so staff notice.
 */
public final class ShiftSettingsParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/shifts.yml";
    /** The shift message when the file doesn't set one. */
    public static final String DEFAULT_MESSAGE = "{character} started a shift at {business}";

    private final BusinessTypeRegistry types;
    private final Consumer<String> warnings;

    public ShiftSettingsParser(BusinessTypeRegistry types, Consumer<String> warnings) {
        this.types = types;
        this.warnings = warnings;
    }

    /**
     * @throws ConfigException when a value is out of range or a tier is not 1 to 5
     */
    public ShiftSettings parse(ConfigSection root) {
        int minutes = root.integer("duration-minutes", 30);
        if (minutes < 1) {
            throw root.error("duration-minutes", "must be 1 or more");
        }
        int cost = root.integer("downtime-cost", 1);
        if (cost < 0) {
            throw root.error("downtime-cost", "must be 0 or more");
        }
        String message = root.string("default-message", DEFAULT_MESSAGE);
        return new ShiftSettings(Duration.ofMinutes(minutes), cost, message, pay(root.section("pay")));
    }

    private Map<String, Map<Integer, ShiftPay>> pay(ConfigSection pay) {
        Map<String, Map<Integer, ShiftPay>> byType = new HashMap<>();
        for (String typeId : pay.keys()) {
            if (types.find(typeId).isEmpty()) {
                warnings.accept(FILE + ": pay." + typeId + " is not a business type, so it is ignored");
                continue;
            }
            ConfigSection tiers = pay.section(typeId);
            Map<Integer, ShiftPay> byTier = new HashMap<>();
            for (String key : tiers.keys()) {
                byTier.put(tier(tiers, key), shiftPay(tiers.section(key)));
            }
            byType.put(typeId, byTier);
        }
        types.all().forEach(type -> {
            Map<Integer, ShiftPay> byTier = byType.getOrDefault(type.id(), Map.of());
            List<Integer> missing = new ArrayList<>();
            for (int tier = Business.MIN_TIER; tier <= Business.MAX_TIER; tier++) {
                if (!byTier.containsKey(tier)) {
                    missing.add(tier);
                }
            }
            if (!missing.isEmpty()) {
                warnings.accept(FILE + ": " + type.id() + " shifts pay nothing at tier(s) " + missing);
            }
        });
        return byType;
    }

    private static ShiftPay shiftPay(ConfigSection section) {
        double money = section.number("money", 0);
        if (money < 0) {
            throw section.error("money", "must be 0 or more");
        }
        int tokens = section.integer("tokens", 0);
        if (tokens < 0) {
            throw section.error("tokens", "must be 0 or more");
        }
        return new ShiftPay(BigDecimal.valueOf(money).stripTrailingZeros(), tokens);
    }

    private static int tier(ConfigSection tiers, String key) {
        try {
            int tier = Integer.parseInt(key);
            if (tier >= Business.MIN_TIER && tier <= Business.MAX_TIER) {
                return tier;
            }
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw tiers.error(key, "is not a tier: use 1 to 5");
    }
}
