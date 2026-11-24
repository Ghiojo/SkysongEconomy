# Economy journal

SkysongEconomy keeps one record for every movement of money or goods it makes, forever by default. Other plugins
use it instead of keeping their own copies: SkysongPolitics writes its weekly ledger books from it, and SkysongMetrics
builds statistics from it. The rule behind it is in the project rules: plugins announce what happens, and Metrics
keeps the statistics; a plugin keeps history only when gameplay or staff read it back.

The journal does no statistics and knows nothing about what a tag means: a book that leaves out `contraband` trades
filters on the tag itself.

## How a record is made

1. A feature does something and fires its public event (`ExportSoldEvent`, `ShiftPayCollectedEvent`...).
2. `events/JournalListener` hears it last (`MONITOR`) and calls `journal/EconomyRecorder`, which needs no server and
   is unit tested.
3. The recorder numbers the record (`sequence`, carrying on from the highest saved), stamps the real time and the IC
   date, copies the names it needs, fires `EconomyRecordedEvent`, then queues the write.
4. `AsyncJournalRepository` writes queued records together in one batch on its own background thread; whatever is
   queued when the plugin stops is written before it closes.

A record that fails to be made is logged and never stops the trade, shift or sale behind it. When the journal can't
read the last sequence at start-up (no database), it records nothing that run rather than reuse numbers.

`JournalFeature` starts the journal after businesses and before nodes, since loading nodes can give farm primers back.

## Record kinds and where the money goes

Money moves between three kinds of `MoneyAccount`: `PLAYER` (a balance, id = UUID), `VAULT` (a business's vault, id =
business id) and `SERVER` (outside the economy: money created, such as shift pay, or removed, such as an import's
price). Items carry a flow: `IN` (given to a player), `OUT` (taken from a player) or `LOST`.

| Kind | Money | Items | Subject |
| --- | --- | --- | --- |
| `IMPORT_ORDERED` | player → server | expected items, in | shipment id |
| `IMPORT_DELIVERED` | none (paid at order) | what was handed over, in | shipment id |
| `IMPORT_SEIZED` | none | what it carried, lost | shipment id |
| `IMPORT_REFUNDED` | server → player (none when the refund failed) | none | shipment id |
| `EXPORT_SOLD` | server → seller | what was sold, out | sale id |
| `SHIFT_VAULT_CUT` | server → vault (only when there was a cut) | none | shift id |
| `SHIFT_PAY` | server → worker (only when it held money) | none | first shift id |
| `TRAINING_PAID` | player → vault (only when it cost money) | none | session id |
| `VAULT_COLLECTED` | vault → owner, or the staff member collecting a server business's vault | none | business id |
| `VAULT_CLOSED` | vault → owner; vault → server for a server business or a failed payment | none | business id |
| `LEDGER_FINE` | owner → server | none | business id |
| `HARVEST` | player → server, for any money it cost | what they got, in | node id |
| `FARM_PLANTED` | none | the primer, out | node id |
| `FARM_UNPLANTED` | none | the primer, in (taken out or given back) | node id |
| `UPKEEP_PAID` | none | what the NPC ate, out | NPC UUID |
| `CLAIM_BOUGHT` | buyer → each seller (resale) or buyer → server (server sale); buyer → server for modifier shares | none | `claim-sale:<id>` |
| `CLAIM_PAYOUT` | none (already counted in `CLAIM_BOUGHT`) | none | `claim-sale:<id>` |

Shift pay and the vault cut are money the server creates: nothing is withdrawn for them.

## Record fields (`api/journal/EconomyRecord`)

| Field | Meaning |
| --- | --- |
| `sequence` | Increasing number; a plugin that was off asks for the records after the last one it saw |
| `recordId`, `subjectId` | This record, and what it is about (see the table above) |
| `kind`, `at` | `RecordKind`, real time |
| `icDate` | `JournalIcDate(year, period, day, dayOfYear, week, festival)`; empty without SkysongCalendar |
| `actor` | The character who did it; empty for nobody (a deletion, an upkeep cycle, a seller paid on joining) |
| `businessId`, `businessName` | The business, name copied then (MiniMessage) |
| `trade` | `TradeDetails` on imports and exports: ledger and offer ids and names, delivery time, pier id and name, `TradePrice` (on orders and sales), tags, `ImportRoll` (on collected imports) |
| `nodeId`, `claimId` | On harvests and planting; on Claim records |
| `money` | `MoneyMovement(from, to, amount)` list |
| `items` | `RecordItem(materialId, amount, flow)` list |
| `details` | More facts by key, below |

### IC weeks

SkysongCalendar's year has 8 months of 45 days and closing festival days. An IC week is 9 IC days (3 real days):
Celestine 1 to 9 is week 1, and the month days make weeks 1 to 40 (`week = dayOfYear / 9 + 1`, with `dayOfYear`
counting from 0 as SkysongCalendar does). Festival days have no week and `festival = true`, so a weekly book can
treat the festival as its own book.

### Details by kind

| Kind | Keys |
| --- | --- |
| `IMPORT_REFUNDED` | `paid-back` |
| `SHIFT_PAY` | `shifts` (how many), `tokens` (Gathering Tokens paid with it) |
| `VAULT_CLOSED` | `paid` (`false` when nobody received it) |
| `LEDGER_FINE` | `ledger`, `ledger-name` |
| `HARVEST` | `node-kind`, `node-source`, `tier`, `downtime`, `harvest-tokens`; `resource` at a farm node; `booster-type`, `booster-effect` when one was used |
| `FARM_PLANTED`, `FARM_UNPLANTED` | `resource`, `farm-type`, `planter` (`uuid/profile`); on unplanted also `cause` (`taken-out` or `returned`) and `downtime` |
| `UPKEEP_PAID` | `npc`, `cycle-due`, `profile` |
| `CLAIM_BOUGHT` | `sale`, `claim-type`, `asking-price`, `modifier-total`, `seller-paid-later` |
| `CLAIM_PAYOUT` | `sale`, `account`, `amount` |

New kinds and keys may be added; readers skip what they don't know.

## Reading the journal

`api.journal.EconomyJournal` is registered with Bukkit's services manager while the journal is on (`load` returns
`null` when it is off). Call it on the main thread.

```java
EconomyJournal journal = Bukkit.getServicesManager().load(EconomyJournal.class);
journal.query(JournalQuery.builder()
                .kinds(RecordKind.IMPORT_ORDERED, RecordKind.IMPORT_DELIVERED, RecordKind.EXPORT_SOLD)
                .icWeek(1203, 7)
                .excludingTag("contraband")
                .build())
        .thenAccept(records -> book.write(records));   // runs on the main thread
```

- `query(JournalQuery)` reads on the journal's own background thread, **after** the records still waiting to be
  written, so a record made just before the call is included. The future completes on the main thread, or
  exceptionally when the database can't be read.
- `latestSequence()` is the newest record's sequence. A plugin that was off asks for
  `afterSequence(lastSeen)` and pages on with the last sequence it got.

`JournalQuery` filters, all optional and combined: `kinds`, `business`, `pier`, `node`, `claim`, `player` (the
actor, or either side of a payment), `withTag`, `excludingTag` (records that aren't trades stay), `from` (included) /
`to` (excluded) real time, `icWeek(year, week)` (leaves the festival out), `icFestival(year)`, `icFrom` / `icTo`
(IC year and day of year, both included), `afterSequence`. Then `limit` (default 500, at most 5,000), `offset` and
`newestFirst()`. Results are in sequence order, oldest first unless `newestFirst()`. `JournalQuery.matches(record)`
applies the same filters to one record, for plugins that filter `EconomyRecordedEvent`s live.

`data/database/JournalSql` turns a query into one parameterised SELECT; it is unit tested against the same cases as
`matches`.

## Configuration

`journal.yml`: `enabled` (default `true`) and `keep-days` (default `0`, forever). See the
[configuration reference](09-configuration-reference.md#journalyml-journaljournalsettings).

## Listening

```java
@EventHandler
public void onRecord(EconomyRecordedEvent event) {
    EconomyRecord record = event.getRecord();
    if (record.trade().isPresent() && !record.hasTag("contraband")) {
        book.add(record);
    }
}
```

The event fires on the main thread right before the record is written. The table is `economy_journal`
([Database](10-database.md)).
