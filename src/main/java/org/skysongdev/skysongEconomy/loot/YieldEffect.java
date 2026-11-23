package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.trade.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A change to what one harvest gives, written like {@code min+1; max+25%; rolls+1; chance-50%}.
 * Booster items carry one in their {@code node_booster_effect} tag, and calendar node modifiers
 * carry one in their file.
 * <p>
 * Each part changes the minimum or maximum amount of every item (guaranteed, chance and rolled),
 * flat or in whole percent; the number of rolls in every roll line, flat; or the percent of every
 * chance item, in whole percent of itself ({@code chance-50%} turns 20% into 10%). Weights in loot
 * groups never change.
 * <p>
 * Percent changes apply first, then flat ones; amounts are truncated, never rounded up; nothing
 * goes below 0; a minimum that ends above the maximum is lowered to it; and a chance stays between
 * 0 and 100%. Several parts on the same thing add up, so {@code max+10%; max+15%} is
 * {@code max+25%}, and so do several effects on one harvest ({@link #plus}).
 *
 * @param minPercent    percent added to every minimum amount
 * @param minFlat       added to every minimum amount, after the percent
 * @param maxPercent    percent added to every maximum amount
 * @param maxFlat       added to every maximum amount, after the percent
 * @param rolls         added to the number of rolls of every roll line
 * @param chancePercent percent of itself added to every chance item's percent
 */
public record YieldEffect(int minPercent, int minFlat, int maxPercent, int maxFlat, int rolls, int chancePercent)
        implements YieldAdjuster {
    /** Changes nothing. */
    public static final YieldEffect NONE = new YieldEffect(0, 0, 0, 0, 0, 0);

    private static final Pattern PART = Pattern.compile("(min|max|rolls|chance)\\s*([+-])\\s*(\\d{1,6})\\s*(%?)");

    /**
     * @param text the effect, such as {@code min+1; max+25%; rolls+1}
     * @throws IllegalArgumentException naming the part that isn't understood
     */
    public static YieldEffect parse(String text) {
        YieldEffect effect = NONE;
        int parts = 0;
        for (String part : (text == null ? "" : text).split(";")) {
            String trimmed = part.strip().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                continue;
            }
            parts++;
            effect = effect.plus(parsePart(part.strip(), trimmed));
        }
        if (parts == 0) {
            throw new IllegalArgumentException("The effect is empty: write something like min+1; max+25%; rolls+1");
        }
        return effect;
    }

    private static YieldEffect parsePart(String written, String trimmed) {
        Matcher match = PART.matcher(trimmed);
        if (!match.matches()) {
            throw new IllegalArgumentException("'" + written
                    + "' isn't an effect: use min, max, rolls or chance, then + or - and a whole number, like max+25%");
        }
        int value = Integer.parseInt(match.group(3)) * (match.group(2).equals("-") ? -1 : 1);
        boolean percent = !match.group(4).isEmpty();
        return switch (match.group(1)) {
            case "min" -> percent ? new YieldEffect(value, 0, 0, 0, 0, 0) : new YieldEffect(0, value, 0, 0, 0, 0);
            case "max" -> percent ? new YieldEffect(0, 0, value, 0, 0, 0) : new YieldEffect(0, 0, 0, value, 0, 0);
            case "rolls" -> {
                if (percent) {
                    throw new IllegalArgumentException("'" + written + "': rolls only change by a whole number, like rolls+1");
                }
                yield new YieldEffect(0, 0, 0, 0, value, 0);
            }
            default -> {
                if (!percent) {
                    throw new IllegalArgumentException("'" + written + "': chances only change in percent, like chance-50%");
                }
                yield new YieldEffect(0, 0, 0, 0, 0, value);
            }
        };
    }

    /** @return this effect and {@code other} together, each part added up */
    public YieldEffect plus(YieldEffect other) {
        return new YieldEffect(minPercent + other.minPercent, minFlat + other.minFlat, maxPercent + other.maxPercent,
                maxFlat + other.maxFlat, rolls + other.rolls, chancePercent + other.chancePercent);
    }

    /** @return whether it changes nothing */
    public boolean isNone() {
        return equals(NONE);
    }

    /**
     * @return the effect written the way {@link #parse} reads it, such as {@code max-50%; rolls-1};
     *         empty for {@link #NONE}
     */
    public String text() {
        List<String> parts = new ArrayList<>();
        addPart(parts, "min", minPercent, "%");
        addPart(parts, "min", minFlat, "");
        addPart(parts, "max", maxPercent, "%");
        addPart(parts, "max", maxFlat, "");
        addPart(parts, "rolls", rolls, "");
        addPart(parts, "chance", chancePercent, "%");
        return String.join("; ", parts);
    }

    private static void addPart(List<String> parts, String name, int value, String unit) {
        if (value != 0) {
            parts.add(name + (value > 0 ? "+" : "") + value + unit);
        }
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

    @Override
    public double chance(double percent) {
        return Math.max(0, Math.min(100, percent * (100 + chancePercent) / 100));
    }

    /** Percent first, truncated, then flat; never below 0. */
    private static int change(int value, int percent, int flat) {
        long changed = (long) value * (100 + percent) / 100 + flat;
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, changed));
    }
}
