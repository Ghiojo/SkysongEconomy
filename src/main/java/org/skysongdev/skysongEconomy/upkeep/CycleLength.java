package org.skysongdev.skysongEconomy.upkeep;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads lengths of time the way staff type them: a number and a unit, combinable, such as
 * {@code 24h}, {@code 3d} or {@code 1d12h}. Units: {@code s}, {@code m}, {@code h}, {@code d}, {@code w}.
 */
public final class CycleLength {
    private static final Pattern PART = Pattern.compile("(\\d+)([smhdw])");
    private static final Map<String, Duration> UNITS = Map.of(
            "s", Duration.ofSeconds(1),
            "m", Duration.ofMinutes(1),
            "h", Duration.ofHours(1),
            "d", Duration.ofDays(1),
            "w", Duration.ofDays(7));

    private CycleLength() {
    }

    /**
     * @param text such as {@code 1d12h}; spaces and case are ignored
     * @return the length, longer than 0
     * @throws IllegalArgumentException when the text is not a length or is 0
     */
    public static Duration parse(String text) {
        String compact = text.replace(" ", "").toLowerCase(Locale.ROOT);
        Matcher matcher = PART.matcher(compact);
        Duration total = Duration.ZERO;
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) {
                break;
            }
            total = total.plus(UNITS.get(matcher.group(2)).multipliedBy(Long.parseLong(matcher.group(1))));
            end = matcher.end();
        }
        if (compact.isEmpty() || end != compact.length()) {
            throw new IllegalArgumentException("'" + text + "' is not a length of time such as 24h, 3d or 1d12h");
        }
        if (total.isZero()) {
            throw new IllegalArgumentException("'" + text + "' must be longer than 0");
        }
        return total;
    }
}
