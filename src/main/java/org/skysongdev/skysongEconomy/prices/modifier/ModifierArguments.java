package org.skysongdev.skysongEconomy.prices.modifier;

import org.skysongdev.skysongEconomy.prices.PriceException;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the words of {@code /se modifier add|set}:
 * {@code <type> <target> <amount> [for:<time>] [on:imports|exports|both|claims] [display name...]}.
 * <p>
 * The amount is signed. With {@code %} it is a percentage, like {@code +10%} or {@code -5%};
 * without, a flat amount of money, like {@code +50}, which only Claims take. The time is written
 * like {@code 30m}, {@code 12h}, {@code 7d} or {@code 1d12h}. Without {@code for:} the modifier
 * never expires. Without {@code on:} it changes imports only, except on a {@code claim-type:} or
 * {@code claim:} target, where it changes Claims.
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
        ModifierAmount amount = ModifierAmount.parse(args[2]);
        Instant expiresAt = null;
        AppliesTo appliesTo = target.kind().claimsOnly() ? AppliesTo.CLAIMS : AppliesTo.IMPORTS;
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
        return new ModifierDraft(type, target, appliesTo, amount, name.isBlank() ? null : name, expiresAt, createdBy);
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
