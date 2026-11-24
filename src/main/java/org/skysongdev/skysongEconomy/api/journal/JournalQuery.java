package org.skysongdev.skysongEconomy.api.journal;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/**
 * Which economy journal records to read. Every filter is optional and they all apply together; with
 * none, the query reads the first {@link #DEFAULT_LIMIT} records. Build one with {@link #builder()}:
 *
 * <pre>{@code
 * JournalQuery week = JournalQuery.builder()
 *         .kinds(RecordKind.IMPORT_ORDERED, RecordKind.EXPORT_SOLD)
 *         .icWeek(1203, 7)
 *         .excludingTag("contraband")
 *         .build();
 * }</pre>
 *
 * Immutable; {@link #matches} says whether one record passes the filters, as the database does.
 */
public final class JournalQuery {
    /** How many records a query reads when it sets no limit. */
    public static final int DEFAULT_LIMIT = 500;
    /** The most records one query reads; page with {@link Builder#afterSequence} for more. */
    public static final int MAX_LIMIT = 5_000;

    private final Set<RecordKind> kinds;
    private final Optional<String> businessId;
    private final Optional<String> pierId;
    private final Optional<String> nodeId;
    private final Optional<String> claimId;
    private final Optional<UUID> player;
    private final Optional<String> withTag;
    private final Optional<String> excludingTag;
    private final Optional<Instant> from;
    private final Optional<Instant> to;
    private final OptionalInt icYear;
    private final OptionalInt icWeek;
    private final boolean icFestival;
    private final Optional<IcDay> icFrom;
    private final Optional<IcDay> icTo;
    private final OptionalLong afterSequence;
    private final int limit;
    private final int offset;
    private final boolean newestFirst;

    private JournalQuery(Builder builder) {
        kinds = builder.kinds.isEmpty() ? Set.of() : Set.copyOf(builder.kinds);
        businessId = builder.businessId;
        pierId = builder.pierId;
        nodeId = builder.nodeId;
        claimId = builder.claimId;
        player = builder.player;
        withTag = builder.withTag;
        excludingTag = builder.excludingTag;
        from = builder.from;
        to = builder.to;
        icYear = builder.icYear;
        icWeek = builder.icWeek;
        icFestival = builder.icFestival;
        icFrom = builder.icFrom;
        icTo = builder.icTo;
        afterSequence = builder.afterSequence;
        limit = builder.limit;
        offset = builder.offset;
        newestFirst = builder.newestFirst;
    }

    /** @return a query with no filters, to add them to */
    public static Builder builder() {
        return new Builder();
    }

    /** One IC day, for {@link Builder#icFrom} and {@link Builder#icTo}: a year and its day, counting from 0. */
    public record IcDay(int year, int dayOfYear) {
        /** @return whether this day comes before the other */
        public boolean isBefore(IcDay other) {
            return year < other.year || (year == other.year && dayOfYear < other.dayOfYear);
        }
    }

    /** @return the kinds wanted; empty for every kind */
    public Set<RecordKind> kinds() {
        return kinds;
    }

    public Optional<String> businessId() {
        return businessId;
    }

    public Optional<String> pierId() {
        return pierId;
    }

    public Optional<String> nodeId() {
        return nodeId;
    }

    public Optional<String> claimId() {
        return claimId;
    }

    /** @return the player who must be the actor, or pay or receive money in the record */
    public Optional<UUID> player() {
        return player;
    }

    public Optional<String> withTag() {
        return withTag;
    }

    public Optional<String> excludingTag() {
        return excludingTag;
    }

    /** @return the earliest real time, included */
    public Optional<Instant> from() {
        return from;
    }

    /** @return the real time records must be before, excluded */
    public Optional<Instant> to() {
        return to;
    }

    /** @return the IC year, set by {@link Builder#icWeek} or {@link Builder#icFestival} */
    public OptionalInt icYear() {
        return icYear;
    }

    public OptionalInt icWeek() {
        return icWeek;
    }

    /** @return whether only festival days of {@link #icYear()} are wanted */
    public boolean icFestival() {
        return icFestival;
    }

    /** @return the first IC day, included */
    public Optional<IcDay> icFrom() {
        return icFrom;
    }

    /** @return the last IC day, included */
    public Optional<IcDay> icTo() {
        return icTo;
    }

    /** @return the sequence records must come after */
    public OptionalLong afterSequence() {
        return afterSequence;
    }

    public int limit() {
        return limit;
    }

    /** @return how many matching records to skip first, for pages */
    public int offset() {
        return offset;
    }

    /** @return whether the newest records come first; otherwise the oldest do */
    public boolean newestFirst() {
        return newestFirst;
    }

    /** @return whether the record passes every filter (limit, offset and order aside) */
    public boolean matches(EconomyRecord record) {
        return (kinds.isEmpty() || kinds.contains(record.kind()))
                && matches(businessId, record.businessId())
                && matches(pierId, record.pierId())
                && matches(nodeId, record.nodeId())
                && matches(claimId, record.claimId())
                && player.map(record::involves).orElse(true)
                && withTag.map(record::hasTag).orElse(true)
                && excludingTag.map(tag -> !record.hasTag(tag)).orElse(true)
                && from.map(time -> !record.at().isBefore(time)).orElse(true)
                && to.map(record.at()::isBefore).orElse(true)
                && afterSequence.stream().allMatch(sequence -> record.sequence() > sequence)
                && matchesIc(record.icDate());
    }

    private boolean matchesIc(Optional<JournalIcDate> date) {
        if (icYear.isEmpty() && icFrom.isEmpty() && icTo.isEmpty()) {
            return true;
        }
        if (date.isEmpty()) {
            return false;
        }
        JournalIcDate ic = date.get();
        IcDay day = new IcDay(ic.year(), ic.dayOfYear());
        return (icYear.isEmpty() || icYear.getAsInt() == ic.year())
                && (icWeek.isEmpty() || ic.week().equals(icWeek))
                && (!icFestival || ic.festival())
                && icFrom.map(first -> !day.isBefore(first)).orElse(true)
                && icTo.map(last -> !last.isBefore(day)).orElse(true);
    }

    private static boolean matches(Optional<String> wanted, Optional<String> actual) {
        return wanted.isEmpty() || wanted.equals(actual);
    }

    /** Sets a query's filters. Each call replaces that filter. */
    public static final class Builder {
        private final Set<RecordKind> kinds = EnumSet.noneOf(RecordKind.class);
        private Optional<String> businessId = Optional.empty();
        private Optional<String> pierId = Optional.empty();
        private Optional<String> nodeId = Optional.empty();
        private Optional<String> claimId = Optional.empty();
        private Optional<UUID> player = Optional.empty();
        private Optional<String> withTag = Optional.empty();
        private Optional<String> excludingTag = Optional.empty();
        private Optional<Instant> from = Optional.empty();
        private Optional<Instant> to = Optional.empty();
        private OptionalInt icYear = OptionalInt.empty();
        private OptionalInt icWeek = OptionalInt.empty();
        private boolean icFestival;
        private Optional<IcDay> icFrom = Optional.empty();
        private Optional<IcDay> icTo = Optional.empty();
        private OptionalLong afterSequence = OptionalLong.empty();
        private int limit = DEFAULT_LIMIT;
        private int offset;
        private boolean newestFirst;

        private Builder() {
        }

        /** Only these kinds. */
        public Builder kinds(RecordKind... wanted) {
            kinds.clear();
            kinds.addAll(Set.of(wanted));
            return this;
        }

        public Builder business(String id) {
            businessId = Optional.of(Objects.requireNonNull(id, "id"));
            return this;
        }

        /** Only imports to that pier. */
        public Builder pier(String id) {
            pierId = Optional.of(Objects.requireNonNull(id, "id"));
            return this;
        }

        public Builder node(String id) {
            nodeId = Optional.of(Objects.requireNonNull(id, "id"));
            return this;
        }

        public Builder claim(String id) {
            claimId = Optional.of(Objects.requireNonNull(id, "id"));
            return this;
        }

        /** Only records the player did, or paid or received money in. */
        public Builder player(UUID id) {
            player = Optional.of(Objects.requireNonNull(id, "id"));
            return this;
        }

        /** Only trades with that tag. */
        public Builder withTag(String tag) {
            withTag = Optional.of(Objects.requireNonNull(tag, "tag"));
            return this;
        }

        /** Leaves out trades with that tag; records that aren't trades stay. */
        public Builder excludingTag(String tag) {
            excludingTag = Optional.of(Objects.requireNonNull(tag, "tag"));
            return this;
        }

        /** From that real time, included. */
        public Builder from(Instant time) {
            from = Optional.of(Objects.requireNonNull(time, "time"));
            return this;
        }

        /** Before that real time, excluded. */
        public Builder to(Instant time) {
            to = Optional.of(Objects.requireNonNull(time, "time"));
            return this;
        }

        /** Only that IC week (1 to 40) of that IC year; leaves out the festival. */
        public Builder icWeek(int year, int week) {
            if (week < 1) {
                throw new IllegalArgumentException("IC weeks count from 1: " + week);
            }
            icYear = OptionalInt.of(year);
            icWeek = OptionalInt.of(week);
            icFestival = false;
            return this;
        }

        /** Only the festival days that close that IC year. */
        public Builder icFestival(int year) {
            icYear = OptionalInt.of(year);
            icWeek = OptionalInt.empty();
            icFestival = true;
            return this;
        }

        /** From that IC day, included; {@code dayOfYear} counts from 0. */
        public Builder icFrom(int year, int dayOfYear) {
            icFrom = Optional.of(new IcDay(year, dayOfYear));
            return this;
        }

        /** Up to that IC day, included; {@code dayOfYear} counts from 0. */
        public Builder icTo(int year, int dayOfYear) {
            icTo = Optional.of(new IcDay(year, dayOfYear));
            return this;
        }

        /** Only records after that sequence, such as the last one a plugin saw before it stopped. */
        public Builder afterSequence(long sequence) {
            afterSequence = OptionalLong.of(sequence);
            return this;
        }

        /** At most that many records, 1 to {@link #MAX_LIMIT}. */
        public Builder limit(int count) {
            if (count < 1 || count > MAX_LIMIT) {
                throw new IllegalArgumentException("A query reads 1 to " + MAX_LIMIT + " records, not " + count);
            }
            limit = count;
            return this;
        }

        /** Skips that many matching records first, for pages. */
        public Builder offset(int count) {
            if (count < 0) {
                throw new IllegalArgumentException("offset can't be negative: " + count);
            }
            offset = count;
            return this;
        }

        /** The newest records first, instead of the oldest. */
        public Builder newestFirst() {
            newestFirst = true;
            return this;
        }

        public JournalQuery build() {
            return new JournalQuery(this);
        }
    }
}
