package org.skysongdev.skysongEconomy.trade;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Writes durations the short way players read them: {@code 2d 4h}, {@code 3h 15m}, {@code 5m}. */
public final class Durations {
    private Durations() {
    }

    /**
     * @return at most the two largest units, rounded up to the minute; {@code 0m} when nothing is left
     */
    public static String format(Duration duration) {
        long minutes = Math.max(0, (duration.toSeconds() + 59) / 60);
        long days = minutes / (24 * 60);
        long hours = minutes / 60 % 24;
        long rest = minutes % 60;
        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + "d");
        }
        if (hours > 0) {
            parts.add(hours + "h");
        }
        if (rest > 0 && days == 0) {
            parts.add(rest + "m");
        }
        if (parts.isEmpty()) {
            return "0m";
        }
        return String.join(" ", parts.subList(0, Math.min(2, parts.size())));
    }
}
