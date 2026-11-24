package org.skysongdev.skysongEconomy.data.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.ImportRoll;
import org.skysongdev.skysongEconomy.api.journal.JournalIcDate;
import org.skysongdev.skysongEconomy.api.journal.JournalQuery;
import org.skysongdev.skysongEconomy.api.journal.MoneyAccount;
import org.skysongdev.skysongEconomy.api.journal.MoneyMovement;
import org.skysongdev.skysongEconomy.api.journal.RecordItem;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;
import org.skysongdev.skysongEconomy.api.journal.TradeDetails;
import org.skysongdev.skysongEconomy.api.trade.AppliedTradeModifier;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.journal.JournalRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    public List<EconomyRecord> query(JournalQuery query) {
        JournalSql.Select select = JournalSql.select(query);
        try (PreparedStatement statement = connections.getConnection().prepareStatement(select.sql())) {
            for (int i = 0; i < select.parameters().size(); i++) {
                statement.setObject(i + 1, select.parameters().get(i));
            }
            List<EconomyRecord> records = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    records.add(read(rows));
                }
            }
            return records;
        } catch (SQLException | RuntimeException e) {
            throw new StorageException("Failed to read the economy journal", e);
        }
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

    private static EconomyRecord read(ResultSet row) throws SQLException {
        Optional<JournalIcDate> icDate = Optional.empty();
        int year = row.getInt("ic_year");
        if (!row.wasNull()) {
            boolean festival = row.getBoolean("ic_festival");
            icDate = Optional.of(JournalIcDate.of(year, row.getString("ic_period"), row.getInt("ic_day"),
                    row.getInt("ic_day_of_year"), festival));
        }
        String actorUuid = row.getString("actor_uuid");
        Optional<CharacterRef> actor = actorUuid == null ? Optional.empty()
                : Optional.of(new CharacterRef(UUID.fromString(actorUuid), row.getString("actor_profile")));
        String trade = row.getString("trade");
        return new EconomyRecord(row.getLong("sequence"), UUID.fromString(row.getString("record_id")),
                RecordKind.valueOf(row.getString("kind")), row.getString("subject_id"),
                Instant.ofEpochMilli(row.getLong("recorded_at")), icDate, actor, Optional.ofNullable(row.getString("business_id")),
                Optional.ofNullable(row.getString("business_name")),
                trade == null ? Optional.empty() : Optional.of(readTrade(JsonParser.parseString(trade).getAsJsonObject())),
                Optional.ofNullable(row.getString("node_id")), Optional.ofNullable(row.getString("claim_id")),
                readMoney(JsonParser.parseString(row.getString("money")).getAsJsonArray()),
                readItems(JsonParser.parseString(row.getString("items")).getAsJsonArray()),
                readDetails(JsonParser.parseString(row.getString("details")).getAsJsonObject()));
    }

    private static List<MoneyMovement> readMoney(JsonArray array) {
        List<MoneyMovement> money = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            money.add(new MoneyMovement(
                    new MoneyAccount(MoneyAccount.Type.valueOf(json.get("from_type").getAsString()), json.get("from").getAsString()),
                    new MoneyAccount(MoneyAccount.Type.valueOf(json.get("to_type").getAsString()), json.get("to").getAsString()),
                    new BigDecimal(json.get("amount").getAsString())));
        }
        return money;
    }

    private static List<RecordItem> readItems(JsonArray array) {
        List<RecordItem> items = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            items.add(new RecordItem(json.get("material").getAsString(), json.get("amount").getAsInt(),
                    RecordItem.Flow.valueOf(json.get("flow").getAsString())));
        }
        return items;
    }

    private static Map<String, String> readDetails(JsonObject json) {
        Map<String, String> details = new LinkedHashMap<>();
        json.entrySet().forEach(entry -> details.put(entry.getKey(), entry.getValue().getAsString()));
        return details;
    }

    private static TradeDetails readTrade(JsonObject json) {
        Set<String> tags = new LinkedHashSet<>();
        json.getAsJsonArray("tags").forEach(tag -> tags.add(tag.getAsString()));
        Optional<ImportRoll> roll = Optional.ofNullable(json.getAsJsonObject("roll")).map(rolled -> new ImportRoll(
                new BigDecimal(rolled.get("loss_chance").getAsString()), new BigDecimal(rolled.get("roll").getAsString()),
                rolled.get("lost_by_roll").getAsBoolean(), optional(rolled, "seize_reason")));
        return new TradeDetails(json.get("ledger").getAsString(), json.get("ledger_name").getAsString(),
                json.get("offer").getAsString(), json.get("offer_name").getAsString(), optional(json, "delivery_time"),
                optional(json, "pier"), optional(json, "pier_name"),
                Optional.ofNullable(json.getAsJsonObject("price")).map(SqlJournalRepository::readPrice), tags, roll);
    }

    private static TradePrice readPrice(JsonObject json) {
        List<AppliedTradeModifier> modifiers = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("modifiers")) {
            JsonObject applied = element.getAsJsonObject();
            modifiers.add(new AppliedTradeModifier(applied.get("id").getAsString(), applied.get("type").getAsString(),
                    applied.get("target").getAsString(), new BigDecimal(applied.get("percent").getAsString()),
                    new BigDecimal(applied.get("share").getAsString()), applied.get("display_name").getAsString()));
        }
        return new TradePrice(new BigDecimal(json.get("base").getAsString()), new BigDecimal(json.get("percent").getAsString()),
                new BigDecimal(json.get("paid").getAsString()), modifiers, json.get("tax_exempt").getAsBoolean());
    }

    private static Optional<String> optional(JsonObject json, String key) {
        return json.has(key) ? Optional.of(json.get(key).getAsString()) : Optional.empty();
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
