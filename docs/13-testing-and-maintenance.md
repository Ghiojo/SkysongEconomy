# Testing, known gaps and how-tos

## Unit tests

`./gradlew test` runs the JUnit 5 tests in `src/test/java`. They cover the logic that has no server classes:

| Area | Tests |
| --- | --- |
| Config | `ConfigSectionTest`, `FileRegistryTest`, every `*ParserTest` (types, tiers, access, shifts, training, nameplates, E&L, ledgers, node settings, random tables, farm resources and types) |
| Businesses | `BusinessServiceTest`, `AccessServiceTest`, `AccessGroupTest`, `PermissionTest`, `BusinessVaultsTest`, `ShiftServiceTest`, `TrainingServiceTest`, `ActivityLockTest`, `BusinessScaleTest`, `AmountParserTest`, `StepsParserTest`, `SkillSectionsTest` |
| Prices and trade | `FormulaTest`, `PriceExpressionTest`, `PriceDatabaseTest`, `PriceSettingsTest`, `PriceModifiersTest`, `ModifierArgumentsTest`, `TradePricingTest`, `BundleTest`, `LedgerLoaderTest`, `LedgerSelectionsTest`, `DeliveryServiceTest`, `ExportServiceTest`, `ItemListCodecTest`, `DurationsTest`, `IntRangeTest` |
| Business NPCs | `BenefitParserTest`, `NpcReconcilerTest` (growing, shrinking, promotion, sheet changes, other sources left alone), `NpcProgressionTest` (the martial example end to end), `BusinessNpcAccessTest` (the policy per business, defaults, allowing and denying, deleted groups and businesses, copies, reloading), `BusinessNpcsTest` (slots, creating, dismissing, lost slots, owner changes, reloading), `NpcSettingsParserTest`, `SlotSourceTest`, `NpcStatusTest`, `BusinessGroupRefTest`, `BusinessGroupDirectoryTest`, `FakeSkysongNpcsTest` (pins down how the fake behaves), `ApiVersionTest` |
| Tokens | `GatheringTokensTest`, `TokenPlaceholdersTest` |
| Loot and nodes | `LootParserTest`, `LootGroupTest`, `LootGroupLoaderTest`, `LootRollerTest`, `LootOddsTest`, `NodeTest`, `NodeServiceTest`, `NodeFilesTest`, `NodeTablesTest`, `RandomHarvesterTest`, `FarmPlotsTest`, `FarmHarvesterTest`, `PrimerReturnsTest` |
| Other | `MessageCatalogTest`, `StoredItemsTest`, `SkillLevelsTest`, menu layout tests |

Tests use in-memory fakes instead of the server: `InMemoryBusinessRepository`, `InMemoryAccessRepository`,
`InMemoryNodeRepository`, `InMemoryTokenRepository`, `InMemoryModifierRepository`, `MapWallet`, `MapPrices`,
`TradeFixture`, `FarmWorld`, `FakeSkysongNpcs`. When you add a rule, add it to a service with a test next to the existing ones.

**Not covered by unit tests**: anything that needs a running server: commands, menus and `MenuListener`,
listeners (block protection, nameplates, timers), the `Sql...` repositories and `DatabaseSchema`, and the
integrations (Gringotts, SkysongStats, SkysongNPCs, SkysongItems, PlaceholderAPI, CMI). Those are checked on a test server with
the smoke test checklist.

The cloud environment these features were written in can't download every dependency (PlaceholderAPI's
repository is blocked there, and for a while Paper's was too), so build with `./gradlew build` on a normal machine
before deploying.

## Known gaps

These are known and deliberate to leave for later, or worth fixing when you are nearby:

- **Main-thread writes.** The price database (`/se ledger add|remove`) and good types (`/se good`) write to MySQL on
  the main thread, unlike everything else. A slow database briefly freezes the server on those commands. Moving
  `PriceLedgerRepository` and `GoodTypeRepository` behind a `BackgroundWriter` would fix it.
- **Default database name.** The bundled `config.yml` has `database_name: "book_descriptions"`, a leftover. Set the
  real name on each server.
- **`/se good`** has no `list` command and still uses hard-coded texts instead of `messages.yml`.
- **No reload** for business config, `economy-and-logistics.yml` or ledgers; they need a restart. Only node files
  reload.
- **Ledger file removed while selected**: the selection is ignored and takes no slot, but its row stays in the
  database until the business is deleted, so putting the file back selects it again.
- **A failed background write** is logged at SEVERE and not retried; the in-memory state keeps the change, so it
  is lost on restart. Watch the console for "Failed to ...".
- **Boosters** are on the `feature/node-boosters` branch, not on master yet.

## How-tos

### Add a business type

1. Add it under `types` in `businesses/types.yml`, moving skills into it (a skill can only be in one type).
2. Add its pay table under `pay` in `businesses/shifts.yml` (otherwise its shifts pay nothing).
3. To let it own nodes, add it to `owner-business-types` in `nodes/settings.yml`.
4. Restart.

### Make another skill unlock Economy and Logistics

Add the skill id under `skills` in `economy-and-logistics.yml` and restart. Each pick of any listed skill is one level.

### Add a coin

Add its template to `src/main/resources/architite_coins/`, add a `Denomination` line in
`currency/ArchititeCurrency.DENOMINATIONS`, and rebuild.

### Add a random node

1. Write the loot groups it rolls on in `nodes/groups/`.
2. Write `nodes/random/<id>.yml` (see [Nodes](07-nodes.md#random-tables-nodesrandomidyml)).
3. `/se node reload`, check the console for warnings, then `/se node odds <id> <tier>` to check the balance.
4. Look at a block: `/se node create <node id> random <id>`, `/se node settier ...`, `/se node setowner ...`.

### Add a farm node

1. Make sure the primer items have templates in `items/` (and, if you want, the `node_primer` tag).
2. Write each resource in `nodes/farm/resources/` and the type in `nodes/farm/types/`.
3. `/se node reload`, then `/se node create <node id> farm <type>` and `/se node setowner <node id> <business>`.

### Add a new feature

Follow the existing shape (see [Architecture](01-architecture.md#patterns-every-feature-follows)):

1. A state class and a service with the rules, throwing a `RefusedException` subclass for refusals. No Bukkit classes
   in it, so it can be tested.
2. A repository interface, a `Sql...Repository`, an `Async...Repository` and its table in `DatabaseSchema`.
3. A config file with a parser returning an immutable record, and its default in `src/main/resources`.
4. Texts in `messages.yml`; commands under `/se`; a `MainMenuEntry` if it belongs in the business menu.
5. Wire it in a bootstrap class (or a new `...Feature` next to `BusinessFeature`), registering listeners in the order
   cleanup must happen.
6. Unit tests for the service and parser, and a section in these docs.

### Project rules

The owner's standing rules for this repository: one readable pull request per functionality on its own clearly
named branch, every pull request merges into `master`, new systems get a design document approved before any code,
code is documented (Javadoc) and unit tested, separate config files per functionality with every player-facing text
in `messages.yml`, and the Skysong-Special-Items repository is never edited from this project.
