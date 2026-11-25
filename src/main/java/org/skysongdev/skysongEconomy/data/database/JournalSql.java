package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.api.journal.JournalQuery;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns a {@link JournalQuery} into a SELECT on {@code economy_journal}, with every value a parameter.
 * It needs no database, so it is unit tested; it must agree with {@link JournalQuery#matches}.
 */
final class JournalSql {
    /** The columns {@code SqlJournalRepository} reads back, in order. */
    static final String COLUMNS = "sequence, record_id, kind, subject_id, recorded_at, ic_year, ic_period, ic_day, "
            + "ic_day_of_year, ic_week, ic_festival, actor_uuid, actor_profile, business_id, business_name, node_id, "
            + "claim_id, trade, money, items, details";

    /**
     * A statement ready to prepare.
     *
     * @param sql        the SELECT
     * @param parameters its values, in order: {@code String}, {@code Long}, {@code Integer} or {@code Boolean}
     */
    record Select(String sql, List<Object> parameters) {
        Select {
            parameters = List.copyOf(parameters);
        }
    }

    private JournalSql() {
    }

    static Select select(JournalQuery query) {
        List<String> where = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        if (!query.kinds().isEmpty()) {
            List<RecordKind> kinds = query.kinds().stream().sorted().toList();
            where.add("kind IN (" + kinds.stream().map(kind -> "?").collect(Collectors.joining(", ")) + ")");
            kinds.forEach(kind -> parameters.add(kind.name()));
        }
        query.businessId().ifPresent(id -> equal(where, parameters, "business_id", id));
        query.pierId().ifPresent(id -> equal(where, parameters, "pier_id", id));
        query.nodeId().ifPresent(id -> equal(where, parameters, "node_id", id));
        query.claimId().ifPresent(id -> equal(where, parameters, "claim_id", id));
        query.player().ifPresent(player -> {
            // A player is in the money JSON only as an account id, and UUIDs never collide with other text.
            where.add("(actor_uuid = ? OR money LIKE ?)");
            parameters.add(player.toString());
            parameters.add("%\"" + player + "\"%");
        });
        query.withTag().ifPresent(tag -> {
            where.add("tags LIKE ?");
            parameters.add(tagPattern(tag));
        });
        query.excludingTag().ifPresent(tag -> {
            where.add("(tags IS NULL OR tags NOT LIKE ?)");
            parameters.add(tagPattern(tag));
        });
        query.from().ifPresent(time -> {
            where.add("recorded_at >= ?");
            parameters.add(time.toEpochMilli());
        });
        query.to().ifPresent(time -> {
            where.add("recorded_at < ?");
            parameters.add(time.toEpochMilli());
        });
        query.afterSequence().ifPresent(sequence -> {
            where.add("sequence > ?");
            parameters.add(sequence);
        });
        query.icYear().ifPresent(year -> {
            where.add("ic_year = ?");
            parameters.add(year);
        });
        query.icWeek().ifPresent(week -> {
            where.add("ic_week = ?");
            parameters.add(week);
        });
        if (query.icFestival()) {
            where.add("ic_festival = ?");
            parameters.add(true);
        }
        query.icFrom().ifPresent(day -> {
            where.add("(ic_year > ? OR (ic_year = ? AND ic_day_of_year >= ?))");
            parameters.add(day.year());
            parameters.add(day.year());
            parameters.add(day.dayOfYear());
        });
        query.icTo().ifPresent(day -> {
            where.add("(ic_year < ? OR (ic_year = ? AND ic_day_of_year <= ?))");
            parameters.add(day.year());
            parameters.add(day.year());
            parameters.add(day.dayOfYear());
        });
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM economy_journal");
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        sql.append(" ORDER BY sequence ").append(query.newestFirst() ? "DESC" : "ASC").append(" LIMIT ? OFFSET ?");
        parameters.add(query.limit());
        parameters.add(query.offset());
        return new Select(sql.toString(), parameters);
    }

    /** @return a LIKE pattern finding one tag in the {@code ,a,b,} column; {@code _} and {@code %} match themselves */
    static String tagPattern(String tag) {
        String escaped = tag.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%," + escaped + ",%";
    }

    private static void equal(List<String> where, List<Object> parameters, String column, String value) {
        where.add(column + " = ?");
        parameters.add(value);
    }
}
