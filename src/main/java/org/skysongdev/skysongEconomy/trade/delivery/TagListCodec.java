package org.skysongdev.skysongEconomy.trade.delivery;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Writes a delivery's tags as one text column, sorted: {@code contraband,weapons}. */
public final class TagListCodec {
    private TagListCodec() {
    }

    public static String encode(Set<String> tags) {
        return tags.stream().sorted().collect(Collectors.joining(","));
    }

    /** @param text what {@link #encode} wrote; {@code null} or empty means no tags */
    public static Set<String> decode(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Set.copyOf(Arrays.stream(text.split(",")).map(String::strip).filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new)));
    }
}
