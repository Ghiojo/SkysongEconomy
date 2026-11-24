package org.skysongdev.skysongEconomy.data.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.JournalIcDate;
import org.skysongdev.skysongEconomy.api.journal.MoneyMovement;
import org.skysongdev.skysongEconomy.api.journal.RecordItem;
import org.skysongdev.skysongEconomy.api.journal.TradeDetails;
import org.skysongdev.skysongEconomy.api.trade.AppliedTradeModifier;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.journal.JournalRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Keeps the economy journal in the {@code economy_journal} table. Money, items, details and the trade
 * are JSON text; the columns other plugins filter on (time, IC week, kind, business, pier, actor,
 * tags) are their own columns, with indexes.
 */
public final class SqlJournalRepository implements JournalRepository {
    private static final Gson GSON = new Gson();
    private static final String INSERT = "INSERT INTO economy_journal (sequence, record_id, kind, subject_id, recorded_at, "
            + "ic_year, ic_period, ic_day, ic_day_of_year, ic_week, ic_festival, actor_uuid, actor_profile, "
            + "business_id, business_name, pier_id, node_id, claim_id, tags, trade, money, items, details) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final ConnectionProvider connections;

    public SqlJournalRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public long lastSequence() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT COALESCE(MAX(sequence), 0) FROM economy_journal");
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getLong(1) : 0;
        } catch (SQLException e) {
            throw new StorageException("Failed to read the last economy journal record", e);
        }
    }

    @Override
    public void save(EconomyRecord record) {
        saveAll(List.of(record));
    }

    @Override
    public void saveAll(List<EconomyRecord> records) {
        SqlTransaction.run(connections, "Failed to save " + records.size() + " economy journal record(s)", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                for (EconomyRecord record : records) {
                    bind(statement, record);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public int deleteBefore(Instant time) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM economy_journal WHERE recorded_at < ?")) {
            statement.setLong(1, time.toEpochMilli());
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete old economy journal records", e);
        }
    }

    private static void bind(PreparedStatement statement, EconomyRecord record) throws SQLException {
        statement.setLong(1, record.sequence());
        statement.setString(2, record.recordId().toString());
        statement.setString(3, record.kind().name());
        statement.setString(4, record.subjectId());
        statement.setLong(5, record.at().toEpochMilli());
        Optional<JournalIcDate> ic = record.icDate();
        setInt(statement, 6, ic.map(JournalIcDate::year));
        statement.setString(7, ic.map(JournalIcDate::period).orElse(null));
        setInt(statement, 8, ic.map(JournalIcDate::day));
        setInt(statement, 9, ic.map(JournalIcDate::dayOfYear));
        setInt(statement, 10, ic.flatMap(date -> date.week().isPresent()
                ? Optional.of(date.week().getAsInt()) : Optional.empty()));
        if (ic.isPresent()) {
            statement.setBoolean(11, ic.get().festival());
        } else {
            statement.setNull(11, Types.BOOLEAN);
        }
        statement.setString(12, record.actor().map(actor -> actor.player().toString()).orElse(null));
        statement.setString(13, record.actor().map(actor -> actor.profile()).orElse(null));
        statement.setString(14, record.businessId().orElse(null));
        statement.setString(15, record.businessName().orElse(null));
        statement.setString(16, record.pierId().orElse(null));
        statement.setString(17, record.nodeId().orElse(null));
        statement.setString(18, record.claimId().orElse(null));
        statement.setString(19, record.trade().map(trade -> tagColumn(trade)).orElse(null));
        statement.setString(20, record.trade().map(trade -> GSON.toJson(trade(trade))).orElse(null));
        statement.setString(21, GSON.toJson(money(record.money())));
        statement.setString(22, GSON.toJson(items(record.items())));
        statement.setString(23, GSON.toJson(record.details()));
    }

    /** @return the tags between commas, such as {@code ,contraband,wine,}, so one tag is found with LIKE */
    static String tagColumn(TradeDetails trade) {
        return trade.tags().isEmpty() ? null : trade.tags().stream().sorted()
                .collect(Collectors.joining(",", ",", ","));
    }

    private static void setInt(PreparedStatement statement, int index, Optional<Integer> value) throws SQLException {
        if (value.isPresent()) {
            statement.setInt(index, value.get());
        } else {
            statement.setNull(index, Types.INTEGER);
        }
    }

    private static JsonArray money(List<MoneyMovement> movements) {
        JsonArray array = new JsonArray();
        for (MoneyMovement movement : movements) {
            JsonObject json = new JsonObject();
            json.addProperty("from_type", movement.from().type().name());
            json.addProperty("from", movement.from().id());
            json.addProperty("to_type", movement.to().type().name());
            json.addProperty("to", movement.to().id());
            json.addProperty("amount", movement.amount().toPlainString());
            array.add(json);
        }
        return array;
    }

    private static JsonArray items(List<RecordItem> items) {
        JsonArray array = new JsonArray();
        for (RecordItem item : items) {
            JsonObject json = new JsonObject();
            json.addProperty("material", item.materialId());
            json.addProperty("amount", item.amount());
            json.addProperty("flow", item.flow().name());
            array.add(json);
        }
        return array;
    }

    private static JsonObject trade(TradeDetails trade) {
        JsonObject json = new JsonObject();
        json.addProperty("ledger", trade.ledgerId());
        json.addProperty("ledger_name", trade.ledgerName());
        json.addProperty("offer", trade.offerId());
        json.addProperty("offer_name", trade.offerName());
        trade.deliveryTimeId().ifPresent(id -> json.addProperty("delivery_time", id));
        trade.pierId().ifPresent(id -> json.addProperty("pier", id));
        trade.pierName().ifPresent(name -> json.addProperty("pier_name", name));
        trade.price().ifPresent(price -> json.add("price", price(price)));
        JsonArray tags = new JsonArray();
        trade.tags().stream().sorted().forEach(tags::add);
        json.add("tags", tags);
        trade.roll().ifPresent(roll -> {
            JsonObject rolled = new JsonObject();
            rolled.addProperty("loss_chance", roll.lossChance().toPlainString());
            rolled.addProperty("roll", roll.roll().toPlainString());
            rolled.addProperty("lost_by_roll", roll.lostByRoll());
            roll.seizeReason().ifPresent(reason -> rolled.addProperty("seize_reason", reason));
            json.add("roll", rolled);
        });
        return json;
    }

    private static JsonObject price(TradePrice price) {
        JsonObject json = new JsonObject();
        json.addProperty("base", price.base().toPlainString());
        json.addProperty("percent", price.percent().toPlainString());
        json.addProperty("paid", price.paid().toPlainString());
        json.addProperty("tax_exempt", price.taxExempt());
        JsonArray modifiers = new JsonArray();
        for (AppliedTradeModifier modifier : price.modifiers()) {
            JsonObject applied = new JsonObject();
            applied.addProperty("id", modifier.id());
            applied.addProperty("type", modifier.type());
            applied.addProperty("target", modifier.target());
            applied.addProperty("percent", modifier.percent().toPlainString());
            applied.addProperty("share", modifier.share().toPlainString());
            applied.addProperty("display_name", modifier.displayName());
            modifiers.add(applied);
        }
        json.add("modifiers", modifiers);
        return json;
    }
}
