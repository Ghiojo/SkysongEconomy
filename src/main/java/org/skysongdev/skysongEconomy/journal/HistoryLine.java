package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.MoneyAccount;
import org.skysongdev.skysongEconomy.api.journal.MoneyMovement;
import org.skysongdev.skysongEconomy.api.journal.RecordItem;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * One record as a {@code /se history} line shows it. Every part is plain text except
 * {@link #business}, which is the business's MiniMessage name.
 *
 * @param time     the real time, such as {@code 2026-11-24 12:00}
 * @param ic       the IC date, such as {@code Idalia 3, 1203}; empty without one
 * @param kind     the kind in lower case, such as {@code import-ordered}
 * @param who      the character who did it; {@code -} for nobody
 * @param business the business's name; empty without one
 * @param items    such as {@code 4 red_wine in, 2 bread out}; {@code -} without items
 * @param money    such as {@code 120 Bram → server}; {@code -} without money
 */
public record HistoryLine(String time, String ic, String kind, String who, String business, String items,
                          String money) {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final String NONE = "-";

    /**
     * @param players a player's name, by UUID
     * @param amounts how money reads, such as {@code 120 architite}
     * @param zone    the time zone times are shown in
     */
    public static HistoryLine of(EconomyRecord record, Function<UUID, String> players, Function<BigDecimal, String> amounts,
                                 ZoneId zone) {
        String ic = record.icDate().map(date -> date.period() + " " + date.day() + ", " + date.year()).orElse("");
        String items = record.items().isEmpty() ? NONE : record.items().stream().map(HistoryLine::item)
                .collect(Collectors.joining(", "));
        String money = record.money().isEmpty() ? NONE : record.money().stream()
                .map(movement -> movement(movement, players, amounts)).collect(Collectors.joining(", "));
        return new HistoryLine(TIME.format(record.at().atZone(zone)), ic,
                record.kind().name().toLowerCase(Locale.ROOT).replace('_', '-'),
                record.actor().map(actor -> actor.profile()).orElse(NONE),
                record.businessName().or(record::businessId).orElse(""), items, money);
    }

    private static String item(RecordItem item) {
        return item.amount() + " " + item.materialId() + " " + item.flow().name().toLowerCase(Locale.ROOT);
    }

    private static String movement(MoneyMovement movement, Function<UUID, String> players,
                                   Function<BigDecimal, String> amounts) {
        return amounts.apply(movement.amount()) + " " + account(movement.from(), players) + " → "
                + account(movement.to(), players);
    }

    private static String account(MoneyAccount account, Function<UUID, String> players) {
        return switch (account.type()) {
            case PLAYER -> players.apply(account.playerId().orElseThrow());
            case VAULT -> "vault of " + account.id();
            case SERVER -> "server";
        };
    }
}
