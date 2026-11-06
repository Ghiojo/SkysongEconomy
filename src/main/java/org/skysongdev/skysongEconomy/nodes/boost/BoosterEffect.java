package org.skysongdev.skysongEconomy.nodes.boost;

import org.skysongdev.skysongEconomy.loot.YieldAdjuster;
import org.skysongdev.skysongEconomy.trade.IntRange;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a booster changes in one harvest, read from its {@code node_booster_effect} tag such as
 * {@code min+1; max+25%; rolls+1}. Each part changes the minimum or maximum amount of every item
 * (guaranteed, chance and rolled), flat or in whole percent, or the number of rolls in every roll
 * line, flat. Chance odds don't change.
 * <p>
 * Percent changes apply first, then flat ones; results are truncated, never rounded up; nothing
 * goes below 0; and a minimum that ends above the maximum is lowered to it. Several parts on the
 * same thing add up, so {@code max+10%; max+15%} is {@code max+25%}.
 *
 * @param minPercent percent added to every minimum amount
 * @param minFlat    added to every minimum amount, after the percent
 * @param maxPercent percent added to every maximum amount
 * @param maxFlat    added to every maximum amount, after the percent
 * @param rolls      added to the number of rolls of every roll line
 */
public record BoosterEffect(int minPercent, int minFlat, int maxPercent, int maxFlat, int rolls) implements YieldAdjuster {
    /** Changes nothing. */
    public static final BoosterEffect NONE = new BoosterEffect(0, 0, 0, 0, 0);

    private static final Pattern PART = Pattern.compile("(min|max|rolls)\\s*([+-])\\s*(\\d{1,6})\\s*(%?)");

    /**
     * @param text the tag, such as {@code min+1; max+25%; rolls+1}
     * @throws IllegalArgumentException naming the part that isn't understood
     */
    public static BoosterEffect parse(String text) {
        int minPercent = 0;
        int minFlat = 0;
        int maxPercent = 0;
        int maxFlat = 0;
        int rolls = 0;
        int parts = 0;
        for (String part : (text == null ? "" : text).split(";")) {
            String trimmed = part.strip().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                continue;
            }
            parts++;
            Matcher match = PART.matcher(trimmed);
            if (!match.matches()) {
                throw new IllegalArgumentException("'" + part.strip()
                        + "' isn't an effect: use min, max or rolls, then + or - and a whole number, like max+25%");
            }
            int value = Integer.parseInt(match.group(3)) * (match.group(2).equals("-") ? -1 : 1);
            boolean percent = !match.group(4).isEmpty();
            switch (match.group(1)) {
                case "min" -> {
                    if (percent) {
                        minPercent += value;
                    } else {
                        minFlat += value;
                    }
                }
                case "max" -> {
                    if (percent) {
                        maxPercent += value;
                    } else {
                        maxFlat += value;
                    }
                }
                default -> {
                    if (percent) {
                        throw new IllegalArgumentException("'" + part.strip() + "': rolls only change by a whole number, like rolls+1");
                    }
                    rolls += value;
                }
            }
        }
        if (parts == 0) {
            throw new IllegalArgumentException("The effect is empty: write something like min+1; max+25%; rolls+1");
        }
        return new BoosterEffect(minPercent, minFlat, maxPercent, maxFlat, rolls);
    }

    @Override
    public IntRange amount(IntRange amount) {
        int min = change(amount.min(), minPercent, minFlat);
        int max = change(amount.max(), maxPercent, maxFlat);
        return new IntRange(Math.min(min, max), max);
    }

    @Override
    public int rolls(int count) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, (long) count + rolls));
    }

    /** Percent first, truncated, then flat; never below 0. */
    private static int change(int value, int percent, int flat) {
        long changed = (long) value * (100 + percent) / 100 + flat;
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, changed));
    }
}
