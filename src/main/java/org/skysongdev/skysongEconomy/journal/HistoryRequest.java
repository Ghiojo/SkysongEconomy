package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.api.journal.JournalQuery;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * What {@code /se history [business <id> | pier <id> | player <name> | kind <kind>] [page]} asks for:
 * one optional filter and a page of {@link #PAGE_SIZE} records, newest first.
 *
 * @param filter how the filter reads in the header, such as {@code business tavern}; {@code all} without one
 * @param page   the page, from 1
 * @param query  the journal query for that page
 */
public record HistoryRequest(String filter, int page, JournalQuery query) {
    /** Records on one page. */
    public static final int PAGE_SIZE = 10;
    /** The filters, as typed. */
    public static final List<String> FILTERS = List.of("business", "pier", "player", "kind");
    /** The last page that can be asked for. */
    public static final int MAX_PAGE = 100_000;

    /**
     * @param args    the words after {@code history}
     * @param players finds a player who has played here by name
     * @throws RefusedException {@code command.usage}, {@code history.unknown-kind},
     *                          {@code command.unknown-player} or {@code history.invalid-page}
     */
    public static HistoryRequest parse(String[] args, Function<String, Optional<UUID>> players) {
        JournalQuery.Builder query = JournalQuery.builder().newestFirst().limit(PAGE_SIZE);
        String filter = "all";
        int next = 0;
        if (args.length > 0 && FILTERS.contains(args[0].toLowerCase(Locale.ROOT))) {
            if (args.length < 2) {
                throw usageRefusal();
            }
            String kind = args[0].toLowerCase(Locale.ROOT);
            String value = args[1];
            switch (kind) {
                case "business" -> query.business(value.toLowerCase(Locale.ROOT));
                case "pier" -> query.pier(value.toLowerCase(Locale.ROOT));
                case "player" -> query.player(players.apply(value).orElseThrow(() ->
                        new RefusedException("command.unknown-player", Map.of("player", value))));
                default -> query.kinds(kind(value));
            }
            filter = kind + " " + value;
            next = 2;
        }
        int page = 1;
        if (args.length > next) {
            page = page(args[next]);
            next++;
        }
        if (args.length > next) {
            throw usageRefusal();
        }
        query.offset((page - 1) * PAGE_SIZE);
        return new HistoryRequest(filter, page, query.build());
    }

    /** @return the usage shown when the words don't fit */
    public static String usage() {
        return "[business <id> | pier <id> | player <name> | kind <kind>] [page]";
    }

    /** @return every record kind as typed, such as {@code import-ordered} */
    public static List<String> kindNames() {
        return Arrays.stream(RecordKind.values()).map(kind -> kind.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                .toList();
    }

    private static RefusedException usageRefusal() {
        return new RefusedException("command.usage", Map.of("usage", "/se history " + usage()));
    }

    private static RecordKind kind(String text) {
        String name = text.toUpperCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(RecordKind.values()).filter(kind -> kind.name().equals(name)).findFirst()
                .orElseThrow(() -> new RefusedException("history.unknown-kind", Map.of("kind", text,
                        "kinds", String.join(", ", kindNames()))));
    }

    private static int page(String text) {
        try {
            int page = Integer.parseInt(text);
            if (page >= 1 && page <= MAX_PAGE) {
                return page;
            }
        } catch (NumberFormatException ignored) {
            // Refused below, like a page out of range.
        }
        throw new RefusedException("history.invalid-page", Map.of("page", text,
                "max", String.valueOf(MAX_PAGE)));
    }
}
