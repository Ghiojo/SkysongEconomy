package org.skysongdev.skysongEconomy.prices.modifier;

import org.skysongdev.skysongEconomy.prices.PriceException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the words of {@code /se modifier add|set}:
 * {@code <type> <target> <percent> [for:<time>] [on:imports|exports|both] [display name...]}.
 * <p>
 * The percent is signed, like {@code +10} or {@code -5}, and may end in {@code %}. The time is
 * written like {@code 30m}, {@code 12h}, {@code 7d} or {@code 1d12h}. Without {@code for:} the
 * modifier never expires; without {@code on:} it changes imports only.
 */
public final class ModifierArguments {
    /** How many words come before the options. */
    public static final int REQUIRED = 3;

    private static final Pattern DURATION_PART = Pattern.compile("(\\d{1,6})([dhm])");

    private ModifierArguments() {
    }

    /**
     * @param args      the words after {@code add} or {@code set}, at least {@link #REQUIRED}
     * @param createdBy who runs the command
     * @param now       the current time, for the expiry
     * @throws PriceException naming the word at fault
     */
    public static ModifierDraft parse(String[] args, String createdBy, Instant now) {
        if (args.length < REQUIRED) {
            throw new IllegalArgumentException("Needs at least " + REQUIRED + " words");
        }
        String type = args[0].toLowerCase(Locale.ROOT);
        ModifierTarget target = ModifierTarget.parse(args[1]);
        BigDecimal percent = percent(args[2]);
        Instant expiresAt = null;
        AppliesTo appliesTo = AppliesTo.IMPORTS;
        int index = REQUIRED;
        for (; index < args.length; index++) {
            String word = args[index].toLowerCase(Locale.ROOT);
            if (word.startsWith("for:")) {
                expiresAt = now.plus(duration(args[index].substring("for:".length())));
            } else if (word.startsWith("on:")) {
                String value = args[index].substring("on:".length());
                appliesTo = AppliesTo.fromKey(value).orElseThrow(() ->
                        new PriceException("prices.modifiers.invalid-applies-to", Map.of("value", value)));
            } else {
                break;
            }
        }
        String name = String.join(" ", Arrays.copyOfRange(args, index, args.length));
        return new ModifierDraft(type, target, appliesTo, percent, name.isBlank() ? null : name, expiresAt, createdBy);
    }

    /**
     * @return the signed percentage in {@code text}, e.g. {@code +10}, {@code -5} or {@code 7.5%}
     * @throws PriceException {@code prices.modifiers.invalid-percent}
     */
    public static BigDecimal percent(String text) {
        String number = text.endsWith("%") ? text.substring(0, text.length() - 1) : text;
        try {
            return new BigDecimal(number.startsWith("+") ? number.substring(1) : number);
        } catch (NumberFormatException e) {
            throw new PriceException("prices.modifiers.invalid-percent", Map.of("percent", text,
                    "min", PriceModifiers.MIN_PERCENT.toPlainString(), "max", PriceModifiers.MAX_PERCENT.toPlainString()));
        }
    }

    /**
     * @return the time in {@code text}, e.g. {@code 7d}, {@code 12h}, {@code 30m} or {@code 1d12h}
     * @throws PriceException {@code prices.modifiers.invalid-duration}
     */
    public static Duration duration(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        Matcher parts = DURATION_PART.matcher(lower);
        Duration total = Duration.ZERO;
        int end = 0;
        while (parts.find() && parts.start() == end) {
            long amount = Long.parseLong(parts.group(1));
            total = total.plus(switch (parts.group(2)) {
                case "d" -> Duration.ofDays(amount);
                case "h" -> Duration.ofHours(amount);
                default -> Duration.ofMinutes(amount);
            });
            end = parts.end();
        }
        if (end == 0 || end != lower.length() || total.isZero()) {
            throw new PriceException("prices.modifiers.invalid-duration", Map.of("duration", text));
        }
        return total;
    }
}
