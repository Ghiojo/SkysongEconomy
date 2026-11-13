# Database

The plugin stores everything in MySQL (InnoDB), over plain JDBC. Tables are created on start-up by
`data/database/DatabaseSchema.createTables`, which also adds columns added after a table first shipped
(`ADDED_COLUMNS`) and makes columns optional when that changed later (`RELAXED_COLUMNS`). It never drops anything:
the old `import_slots` and `economy_businesses` tables are no longer used or created, and their rows are left alone.

## How data moves

- **Load once.** Each service calls its repository's `loadAll()` on start-up, on the main thread, and keeps
  everything in memory.
- **Write through, in the background.** Every change calls `save` or `delete` right away. In the business and node
  systems these are `Async...Repository` wrappers that queue the JDBC call on the feature's `BackgroundWriter`
  thread, in order. On shutdown the writer gets up to 10 seconds to finish.
- **Exceptions.** The price database (`PriceLedgerRepository`) and good types (`GoodTypeRepository`) write on the main
  thread, synchronously.
- **Connections.** Three `ConnectionProvider`s (main, business, nodes), each holding one connection that is reopened
  when MySQL closes it.
- **Cascades.** Child rows of a business use `ON DELETE CASCADE`, so deleting the business row removes them.
  Nodes deliberately have no foreign key to businesses: a deleted business makes its nodes unowned instead.

Times are stored as epoch milliseconds (`BIGINT`), money as `DECIMAL(19, 4)`, characters as `player_uuid` +
`profile`.

## Tables

| Table | Holds | Repository | Key columns |
| --- | --- | --- | --- |
| `price_ledger` | Base value of each item | `PriceLedgerRepository` | `materialId` (PK), `expression`, `cost` (value at the last change) |
| `good_types` | Good type tags | `GoodTypeRepository` | `goodType`, `materialId` |
| `businesses` | Every business | `SqlBusinessRepository` | `id` (PK), `display_name`, `type_id`, `tier`, `owner_uuid`/`owner_profile` (NULL for server-owned), `world`/`x`/`y`/`z` (NULL without a block), `shift_message` |
| `business_workers` | Hires | `SqlBusinessRepository` | (`business_id`, `player_uuid`, `profile`), `hired_at` |
| `business_skill_picks` | Picks (a skill can repeat) | `SqlBusinessRepository` | `id` (auto), `business_id`, `skill_id`, `picked_at` |
| `business_access_groups` | Saved access groups | `SqlAccessRepository` | (`business_id`, `group_id`), `position` (order), `permissions` (comma list), `training_price` |
| `business_access_members` | Members of custom groups | `SqlAccessRepository` | (`business_id`, `group_id`, `player_uuid`, `profile`) |
| `business_vaults` | Vault balances | `SqlVaultRepository` | `business_id` (PK), `balance` |
| `gathering_tokens` | Token balances | `SqlTokenRepository` | (`player_uuid`, `profile`), `balance` |
| `business_ledgers` | Selected ledgers | `SqlLedgerSelectionRepository` | (`business_id`, `ledger_id`), `selected_at` |
| `business_deliveries` | Ordered imports until collected | `SqlDeliveryRepository` | `id` (UUID), `business_id`, `ledger_id`, `import_id`, `delivery_time_id`, `items` (`id:amount;...`), orderer, `price`, `ordered_at`, `arrives_at` |
| `price_modifiers` | Price modifiers | `SqlModifierRepository` | `id` (`m1`...), `type`, `target` (e.g. `ledger:smugglers`), `applies_to`, `percent`, `display_name`, `expires_at` (NULL = never), `created_by`, `created_at` |
| `business_shifts` | Shifts from start until pay is retrieved | `SqlShiftRepository` | `id`, `business_id`, character, `started_at`, `required_seconds`, `worked_seconds`, `money`, `tokens`, `vault_cut`, `finished_at` (NULL while running) |
| `business_training` | Running training sessions only | `SqlTrainingRepository` | `id`, `business_id`, character, `started_at`, `required_seconds`, `worked_seconds`, `price` |
| `nodes` | Every node | `SqlNodeRepository` | `id` (PK), `kind` (`random`/`farm`), `source` (table or type id), `tier`, `owner_kind` (public/unowned/business), `owner_business`, `display_name`, block position |
| `node_plantings` | What is planted in each farm node | `SqlPlantingRepository` | `node_id` (PK, FK to nodes), `resource_id`, `farm_type`, `business_id`, planter, `items` (serialised stacks, `MEDIUMBLOB`) |
| `node_primer_returns` | Primers waiting for an offline planter | `SqlPrimerReturnRepository` | `id`, `player_uuid`, `items` |
| `business_modules` | Which modules each business picked | `SqlModulePickRepository` | (`business_id`, `pick_order`) PK: `pick_order` counts up within a business, higher is newer; `business_id` FK, rows go with the business; `skill`, `module_id`, `picked_at` |
| `business_npc_applied` | What the plugin changed on each business NPC's SkysongStats sheet, to undo exactly that | `SqlNpcAppliedRepository` | (`npc_uuid`, `kind`, `target`): `kind` `stat` with `modifier_id` and `amount`, or `kind` `proficiency` with `tier_before` and `tier_applied`. Rows go with the `business_npcs` row |
| `business_npcs` | Which SkysongNPCs belong to which business | `SqlBusinessNpcRepository` | `npc_uuid` (PK, the SkysongNPCs id), `business_id` (FK, rows go with the business), `source` (the slot it fills, e.g. `skill:martial`), `created_at` |

## Editing data by hand

Stop the server first: everything is cached in memory and the next change would overwrite your edit. Keep the
formats above (for example a delivery's `items` text, or a modifier's `target`). Rows that don't make sense on load
are handled where possible: two businesses (or nodes) on one block make the second lose its block, a planting whose
node is gone gives the primer back, and a broken price database entry is skipped with a warning.

## Adding a table

1. Add its `CREATE TABLE IF NOT EXISTS` to `DatabaseSchema.TABLES` (after any table it references).
2. To add a column to an existing table, add it to `ADDED_COLUMNS` as well, so servers that already have the table
   get it.
3. Write a repository interface in the feature package, a `Sql...Repository` in `data/database`, and an
   `Async...Repository` wrapper, and wire them in the feature's bootstrap with the feature's `BackgroundWriter`.
