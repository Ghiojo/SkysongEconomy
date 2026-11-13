# Architecture

SkysongEconomy is a Paper 1.21 plugin written in Java 21. All code lives under
`src/main/java/org/skysongdev/skysongEconomy`. This page explains how it starts and stops, what each
package is for, and the few patterns every feature follows. Once you know these patterns, every feature
reads the same way.

## Start-up

`SkysongEconomy.onEnable()` (the plugin's main class) only builds services and connects them. In order:

1. `saveDefaultConfig()` writes `config.yml` if it is missing, and `ResourceExtractor` copies the
   `architite_coins/` folder out of the jar (never overwriting edits).
2. `setUpDatabase()` opens the shared `ConnectionProvider`, runs `DatabaseSchema.createTables` (creates
   every table, adds or relaxes columns added later) and loads the good types into `CachedGoodTypes`.
   If MySQL is unreachable the error is logged and the plugin keeps running.
3. `ArchititeCurrency.registerDenominations()` registers the three coins with Gringotts.
4. The shared inputs are built: `SkysongStatsAdapter` (characters, skills, downtime), `YamlFiles` (config
   loading), `MessageCatalog` + `Messenger` (texts), `PriceSettings` + `PriceDatabase` (prices),
   `MaterialIdResolver`, `GringottsWallet`, and `TradeInputs` (what trade needs).
5. `BusinessFeature.start(...)` builds the whole business system (see below). If its config can't be read,
   it logs why and returns a disabled feature with no commands. The rest of the plugin still runs.
6. `NodeFeature.start(...)` builds the nodes system. Nodes need businesses, so they are off when businesses
   are off, or when `nodes/settings.yml` is broken.
7. `registerCommands()` builds the `/se` command tree: `ledger` and `good` always, plus whatever commands
   each feature returns (`business`, `tokens`, `modifier`, `node`).

### Feature bootstraps

`bootstrap/BusinessFeature` and `bootstrap/NodeFeature` are the only places where a feature's classes are
created and wired together. Reading one top to bottom tells you everything the feature uses. Each one:

- reads its config files into immutable settings records (a `ConfigException` turns the feature off),
- opens its **own** `ConnectionProvider` and one `BackgroundWriter` thread,
- creates services, each with an `Async...Repository` wrapping a `Sql...Repository`,
- registers listeners between services (see "Change listeners" below),
- calls `loadAll()` on every service,
- registers Bukkit listeners, menus and the activity timer,
- returns the `/se` sub-commands and a list of `AutoCloseable` resources.

`BusinessFeature.system()` returns a `BusinessSystem` record holding every running business service. The
nodes feature and other plugins use it to reach businesses.

## Shutdown

`onDisable()` runs in this order, and the order matters:

1. `MenuListener.closeAll(server)` closes every open plugin menu, which gives back items held in input slots
   (a primer in a planting slot, for example).
2. `nodeFeature.close()`, then `businessFeature.close()`. Nodes listen to businesses, so they close first.
   Each feature closes its resources in reverse order: the timer stops, nameplates are removed, the
   `BackgroundWriter` finishes queued writes (waiting up to 10 seconds), then the connection closes.
3. The main `ConnectionProvider` closes.

## Packages

| Package | What it holds |
| --- | --- |
| *(root)* | `SkysongEconomy`, the plugin class. |
| `bootstrap` | `BusinessFeature`, `NodeFeature`, `BusinessSystem`, `TradeInputs`: wiring only, no rules. |
| `business` | `Business` (state), `BusinessService` (rules), `BusinessRepository`, `BusinessChangeListener`, `BusinessException`, `Hire`, `SkillPick`, `TierChange`, `BlockPosition`, `BusinessBlockGuard`. |
| `business.type` | Business types from `businesses/types.yml`. |
| `business.tier` | Tier settings from `businesses/tiers.yml`. |
| `business.access` | Access groups and permissions (`train`, `order-imports`, `sell-exports`, `tend-farms`), defaults from `businesses/access.yml`. |
| `business.vault` | The owner's earnings per business. |
| `business.activity` | `ActivityLock`: one shift or training session per player account. |
| `business.shift` | Work a Shift and Retrieve Pay, `businesses/shifts.yml`. |
| `business.training` | Train/Learn, `businesses/training.yml`. |
| `business.display` | Nameplate settings from `businesses/block.yml`. |
| `business.scale` | Values that follow a business's level and tier: `BusinessScale`, `Amount` (number, formula or table), `Steps` (a table by level or tier), `SkillSections` (the `skills:` map of a functionality's file). See [Amounts by level and tier](09-configuration-reference.md#amounts-by-level-and-tier). |
| `trade` | Economy and Logistics: ledgers, bundles, slots, pricing, exports, ledger selection. |
| `trade.delivery` | Ordered imports until they are collected. |
| `expression` | `Formula`: arithmetic with placeholders, shared by price expressions and amounts. |
| `prices` | The price database and price expressions. |
| `prices.modifier` | Percentage price modifiers. |
| `tokens` | Gathering Tokens and their placeholder texts. |
| `nodes` | `Node`, `NodeService`, node settings, the loaded node files (`NodeFiles`, `NodeTables`). |
| `nodes.random` | Random tables (`nodes/random/*.yml`). |
| `nodes.farm` | Farm resources and types, plantings, primer returns, `FarmHarvester`. |
| `nodes.harvest` | `RandomHarvester`, `HarvestCost`, `HarvestPayer`, `NodeAccess`. |
| `loot` | Weighted loot: groups, outputs, the roller, and exact odds. No server classes. |
| `blocks` | `BlockGuard` / `GuardedBlock`: blocks a feature holds, protected by one listener. |
| `character` | Small interfaces over SkysongStats: `CharacterRef`, `CharacterDirectory`, `DowntimeWallet`, `SkillCatalog`, `SkillLevels`, `SkillProgression`. |
| `money` | `Wallet`: a player's money. |
| `currency` | Registers the Architite coins with Gringotts. |
| `items` | Building Skysong items from files, reading `material_id`, handing items to players, serialising stacks. |
| `config` | `ConfigSection`, `YamlFiles`, `YamlFolder`, `FileRegistry`, `PluginPaths`, `ConfigException`. |
| `messaging` | `MessageCatalog`, `Messenger`, `RefusedException`, `NumberText`. |
| `data.database` | MySQL: schema, connection, every `Sql...` and `Async...` repository, `BackgroundWriter`. |
| `commands` (+ sub-packages) | The `/se` command tree. |
| `gui` (+ `layout`, `business`, `node`) | The chest-menu framework and every menu. |
| `listeners` | Bukkit listeners: block protection, interaction, nameplates, timers, item tags, primer returns. |
| `events` | Bukkit events other plugins can listen to. |
| `integration` | The only classes that touch other plugins' APIs: SkysongStats, Gringotts, PlaceholderAPI, CMI. |

## Patterns every feature follows

### State, service, repository

Each feature splits into three kinds of class:

- **State** (`Business`, `Node`): holds data and checks its own invariants (a tier is 1 to 5). Its setters are
  package-private, so only the service changes it.
- **Service** (`BusinessService`, `NodeService`, `AccessService`, `ShiftService`, ...): every rule. It keeps
  everything in memory (maps by id, and by block position), changes it, and passes each change to its
  repository right away.
- **Repository** (`BusinessRepository`, `NodeRepository`, ...): an interface with `loadAll`, `save` and
  `delete`. On the server it is an `Async...Repository` (writes go to the `BackgroundWriter` thread) wrapping a
  `Sql...Repository` (JDBC). In tests it is an in-memory map.

Data is loaded once at start-up (`loadAll`, on the main thread) and then only read from memory. The database
is never queried while players play, except by the price database and good types (see
[Testing and maintenance](13-testing-and-maintenance.md#known-gaps)).

Services are **not thread-safe** and must only be used on the server's main thread. Only writes leave it.

### Background writes, in order

`BackgroundWriter` is one daemon thread per feature. Every async repository of that feature shares it, so rows
are written in the order they changed (a business before its access groups). A failed write is logged at
SEVERE and not retried.

### Change listeners

Services tell each other about changes through listener interfaces instead of calling each other:

- `BusinessChangeListener` has `loaded(all)`, `changed(business, previousPosition)` and `deleted(business)`.
  `BusinessFeature` registers, in this order: modifier cleanup, `AccessService`, `ShiftService`,
  `TrainingService`, `BusinessVaults`, `LedgerSelections`, `DeliveryService`, then `BusinessNameplates`.
  `NodeFeature` adds `NodeService` (a deleted business leaves its nodes unowned).
- `NodeChangeListener` is the same for nodes. `NodeFeature` registers `NodeNameplates` then `FarmPlots`.

The order is deliberate: on deletion, shifts and training pay out before the vault is emptied, and deliveries
are refunded before other rows go. Keep it when adding listeners.

### Refusals become messages

When a player asks for something the rules don't allow, the service throws a subclass of
`messaging/RefusedException` (`BusinessException`, `NodeException`, `PriceException`). It carries a
`messages.yml` key and placeholder values. Commands and menus catch it and call
`messenger.send(sender, refusal)`, which shows that message. So a rule never sends chat itself, and every
player-facing text lives in `messages.yml`.

### Config parsing

Config files are read with SnakeYAML into plain maps, wrapped in `config/ConfigSection`. Its typed getters
(`integer`, `string`, `section`, `sections`, `strings`, `requireString`, ...) throw a `ConfigException` that
names the file and key, for example `businesses/tiers.yml: tiers.3.workers must be ...`. Each config file has a
parser class (`BusinessTiersParser`, `RandomTableParser`, ...) that returns an immutable record. Parsers have no
server classes, so they are unit tested.

- `YamlFiles.load(path)` reads one file in the data folder, copying the default out of the jar first if it is
  missing.
- `YamlFolder.parseEach(folder, parser)` reads a folder where each `<id>.yml` is one thing, skipping broken
  files with a warning, and returns a `FileRegistry<T>`.

### Server glue at the edges

Rules depend on small interfaces (`Wallet`, `DowntimeWallet`, `CharacterDirectory`, `SkillLevels`,
`SkysongItemFactory`, `ShiftEvents`, `NodeHarvestEvents`, `PrimerDelivery`) instead of Bukkit or other plugins.
The real implementations live in `integration/`, `events/`, `items/` and `listeners/`. That is why most rules can
be unit tested without a server, and why swapping, say, Gringotts would only touch `GringottsWallet`.

### Commands

`commands/SubCommand` is one node of the tree: a `name`, an optional `permission`, a `usage`, `execute` and
`tabComplete`. `commands/CommandGroup` is a node whose first argument picks a child. It checks the child's
permission, prints usage for unknown or missing words and completes child names. Feature commands extend a
small base (`BusinessSubCommand`, `NodeSubCommand`) that checks the argument count and turns refusals into
messages. See [Commands and permissions](08-commands-and-permissions.md).

### Menus

Every menu extends `gui/Menu` (a chest inventory made of `MenuButton`s). `gui/MenuListener` cancels every click
so the server never moves items on its own, and runs the clicked button. The business main menu is built from
`MainMenuEntry` objects that each feature adds, so adding a feature never edits the main menu. See
[Messages and menus](12-messages-and-menus.md).

### Floating names and protected blocks

`listeners/FloatingNames` shows a text display above a block. Displays are never saved with the world; they are
remembered and shown again when their chunk loads, so a crash can't leave stray text. `listeners/ProtectedBlockListener`
asks each `BlockGuard` (businesses, nodes) whether a block is held, and stops players, explosions, fire, pistons
and mobs from breaking it. Staff with the guard's break permission can break it; the feature then keeps its data
without a block until it is moved. Features also check each other's guards, so a business and a node never share
a block.
