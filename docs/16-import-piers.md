# Import piers

Every import is unloaded at a **receiving pier**. SkysongEconomy only keeps the skeleton: piers, which pier each
import goes to, a chance that a shipment is lost at its pier, and events where other plugins can step in. The
mechanics around it (guards stationed at piers, WorldGuard regions, contraband patrols) belong to another plugin,
SkysongPolitics, which manages piers through the API.

The agreed design is the project's design file `import-piers-design.md` (revision 3, signed off 2026-11-23).

## Piers

A pier has:

| Field | Meaning |
| --- | --- |
| id | Unique, lower case: letters, digits, `_`, `-` (up to 64) |
| name, icon, description | How it looks in menus (MiniMessage, a Minecraft material, MiniMessage lines) |
| open | Closed piers can't be picked for new orders. Shipments already on their way still arrive |
| data | Free-form text values for other plugins. SkysongEconomy never reads them |
| loss chance | The base percent (0 to 100, default 0) that a shipment collected there is lost |
| loss modifiers | Raise or lower that chance (below) |
| source | `config` for piers from `piers.yml`, otherwise the name of the plugin that created it |

Piers come from two places (`trade/pier/PierService` merges them):

- **`piers.yml`**: the file owns their name, icon and description, and they can't be removed through the API.
  Everything else can be changed through the API or `/se pier`; those changes are saved in the database (table
  `piers`, source `config`) on top of the file and survive `/se pier reload`. Data set this way wins over the
  file's; removing it shows the file's again.
- **Plugins**, with `Piers.create`: saved in the database and fully managed through the API.

If a plugin pier and a pier in the file share an id, the plugin pier wins and the file's is ignored with a warning.
Changes saved for a pier that was deleted from the file are kept, hidden, and come back if it is put back.

With no piers at all, imports work exactly as before: no Pier button, no pier on deliveries, nothing is ever lost.

## Loss chance and loss modifiers

A **loss modifier** (`api/piers/LossModifier`) is written like a price modifier:

- `+5` / `-5`: flat, adds or removes 5 percentage points;
- `+10%` / `-10%`: raises or lowers the chance by a tenth of itself.

It may be limited to one **tag**: then it only counts for shipments carrying that tag. A pier holds one modifier per
id; the same id replaces, different ids stack. The final chance (`Pier.finalLossChance(tags)`):

```
chance = (base + flat modifiers that apply) * (1 + sum of percentage modifiers that apply / 100), within 0..100
```

Example: base 2, `guards: +20` tagged `contraband` (four guards at +5, set by SkysongPolitics), `storm: +50%`.
Contraband is lost (2 + 20) x 1.5 = 33% of the time; honest cargo 2 x 1.5 = 3%.

Modifiers written in `piers.yml` can be replaced but not removed through the API; modifiers set through the API or
`/se pier modifier` are saved in `pier_loss_modifiers` and win over a file one with the same id.

## Tags

Ledgers and their imports can carry `tags:` (see [Economy and Logistics](06-economy-and-logistics.md#ledgers)). A
shipment carries its ledger's and its import's tags as they were when it was ordered, so editing the ledger later
doesn't change shipments on their way. SkysongEconomy doesn't know what tags mean; `contraband` is just a word the
example ledger and SkysongPolitics agree on.

## Ordering: choosing a pier

Whenever there is any pier, the import confirmation has a **Pier** button under the summary. It opens the list of
open piers (`gui/business/PierMenu`); the chosen one is marked. Confirm refuses ("Choose a receiving pier for this
import first") until one is chosen. Every pier takes every import, and anyone who may order imports may pick any open
pier. Once ordered, the pier can't be changed.

The confirmation prices the import again for the chosen pier, so price modifiers on `pier:<id>` show and are charged
(see [Prices](04-prices.md)). None exist by default.

`DeliveryService.order` refuses a missing pier (`business.imports.choose-pier`), one that doesn't exist any more
(`pier-gone`) or a closed one (`pier-closed`). Then it fires `ShipmentOrderEvent` **before anything is paid**; a
listener that cancels it stops the order, and the player sees its message or `business.imports.order-cancelled`.

The Slots menu shows "Arriving at <pier>" on each delivery.

## Collecting: the shipment reaches its pier

A shipment "arrives" when someone **collects** it (there is no timer). In `DeliveryService.collect`, after the usual
checks:

1. The pier's final loss chance for the shipment's tags is rolled once: a number from 0 to 100 (two decimals); the
   shipment is lost when the roll is below the chance. No pier, or a removed pier, means 0%.
2. `ShipmentArriveEvent` fires, **already cancelled when the roll lost it**. Cancelled means seized. Listeners can
   seize or save it, change the items, and set the seize message.
3. Seized: nothing is handed over, the slot is freed, **nothing is refunded**. The collector sees `piers.seized`
   ("Your cargo was seized!") or `piers.seized-reason` with the listener's text.
4. Otherwise the items are handed over (what doesn't fit drops at the player's feet). A shipment a listener left
   empty just frees its slot.
5. `ShipmentCollectedEvent` reports the outcome (`DELIVERED` or `SEIZED`).

## The API (`api/piers`)

Add SkysongEconomy as `compileOnly 'com.github.Ghiojo:SkysongEconomy:<tag>'` and as a `softdepend`. Get the service
with `Bukkit.getServicesManager().load(Piers.class)` (or `SkysongEconomy.getPierFeature().piers()`). Main thread only.

| Method | What it does |
| --- | --- |
| `all()`, `find(id)`, `open()` | Read piers: file piers in file order, then plugin piers in creation order |
| `inbound(pierId)` | Shipments sent there that haven't been collected, soonest arrival first |
| `create(ownerName, id, PierDetails)` | A plugin pier: open, no data, 0% chance |
| `update(id, PierDetails)`, `remove(id)` | Plugin piers only. Removing one also removes its `pier:<id>` price modifiers; shipments sent there keep its id and still arrive |
| `setOpen(id, open)` | Open or close any pier |
| `setData(id, key, value)` | Set (or with `null` remove) one free-form value |
| `setLossChance(id, percent)` | The base chance, 0 to 100 |
| `setLossModifier(id, LossModifier)`, `removeLossModifier(id, modifierId)` | Add/replace or remove a loss modifier |

A refused change throws `PierException`, a `RuntimeException` naming a `piers.*` message. Every change is saved at
once and fires `PierChangeEvent` (before and after; also on `/se pier reload`).

Records: `Pier`, `PierDetails`, `LossModifier`, `Shipment` (business, ledger, import, delivery time, pier, items,
orderer, price, times, tags), `ShipmentItem`, `ShipmentOutcome`.

| Event | When | Cancellable | Can change |
| --- | --- | --- | --- |
| `PierChangeEvent` | A pier was created, changed or removed | No | |
| `ShipmentOrderEvent` | An import is about to be ordered, nothing paid yet | Yes: nothing is paid | `setCancelMessage` |
| `ShipmentArriveEvent` | A delivery is being collected, after the loss roll | Yes: cancelled = seized; starts cancelled when the roll lost it | `setItems`, `setSeizeMessage` |
| `ShipmentCollectedEvent` | The shipment was handed over or seized, slot freed | No | |

Example, guards that catch contraband:

```java
Piers piers = Bukkit.getServicesManager().load(Piers.class);
piers.setLossModifier("harbour", new LossModifier("guards", BigDecimal.valueOf(5L * guards), false,
        Optional.of("contraband")));
// when they leave:
piers.removeLossModifier("harbour", "guards");
```

## Code map

| Where | What |
| --- | --- |
| `api/piers/` | The public types and events above |
| `trade/pier/` | `PierSettingsParser` (`piers.yml`), `ConfiguredPier`, `PierService` (implements `Piers`), `PierRepository`, `PierPriceModifiers` (`pier:` price modifier checks and clean-up) |
| `trade/delivery/` | `DeliveryService.order` (pier, tags, order event) and `collect` (roll, arrival events, `Collection`), `ShipmentEvents` (what the rules ask; `Arrival`, `Outcome`), `Shipments`, `TagListCodec`, `OrderCancelledException` |
| `events/BukkitShipmentEvents` | Fires the shipment events |
| `data/database/SqlPierRepository` | Tables `piers` and `pier_loss_modifiers` |
| `bootstrap/PierFeature` | Loads piers, registers `Piers` with the services manager, `/se pier`. Starts before businesses and works without them |
| `commands/pier/` | `/se pier` |
| `gui/business/PierMenu`, `ImportsMenu`, `SlotsMenu` | Choosing a pier, showing it, the seize message |
