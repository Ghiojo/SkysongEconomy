# Events, API and integrations

## Bukkit events (`events/`)

Other plugins can listen to these like any Bukkit event.

| Event | When | Cancellable | Getters |
| --- | --- | --- | --- |
| `ShiftStartEvent` | A player is about to start a shift, after every check passed and before downtime is spent | Yes: nothing is spent and the player is told the shift was cancelled | `getBusiness()`, `getPlayer()`, `getCharacter()`, `getMessage()` (the business's shift message, placeholders filled in) |
| `ShiftEndEvent` | A shift has its full time; its pay can now be retrieved. The player may be offline. | No | `getBusiness()`, `getShift()` (pay, times, character) |
| `BusinessNpcCreateEvent` | A business created an NPC in one of its slots; it is waiting for its first setup | No | `getBusiness()`, `getNpc()` (`BusinessNpc`: SkysongNPCs id, business, slot source, creation time) |
| `BusinessNpcRemoveEvent` | A business no longer has an NPC and its slot is free. Unless the cause is `REMOVED_ELSEWHERE`, the NPC still exists while this fires and is removed right after | No | `getBusiness()`, `getNpc()`, `getCause()` (`DISMISSED`, `SLOT_LOST`, `BUSINESS_DELETED`, `REMOVED_ELSEWHERE`) |
| `NodeHarvestEvent` | Someone harvested at a node: paid and rolled, items handed over right after | No | `getPlayer()`, `getNode()`, `getCharacter()`, `getTier()`, `getPaid()` (`HarvestCost`), `getItems()` |

The shift message is not shown in game; `ShiftStartEvent.getMessage()` is meant for a Discord bridge or similar.

The rules don't fire Bukkit events themselves: they call `business/shift/ShiftEvents`,
`business/npc/BusinessNpcEvents` and `nodes/harvest/NodeHarvestEvents`, whose server implementations
(`events/BukkitShiftEvents`, `BukkitBusinessNpcEvents`, `BukkitNodeHarvestEvents`) fire the events. Tests use fakes. To add an event, follow the same shape.

## Calling the plugin from another plugin

Get the plugin instance (`(SkysongEconomy) Bukkit.getPluginManager().getPlugin("SkysongEconomy")`), then:

| Getter | Returns |
| --- | --- |
| `getPrices()` | `PriceDatabase`: every item's base value (`value(materialId)`, `set`, `remove`, `context`) |
| `getPriceSettings()` | `PriceSettings`: decimals and `truncate` |
| `getGoodTypes()` | `CachedGoodTypes`: `of(materialId)` and changes |
| `getImportItems()` | `SkysongItemFactory` for the `items/` folder |
| `getSkysongStats()` | `SkysongStatsAdapter` |
| `getBusinessFeature().system()` | `Optional<BusinessSystem>`, empty when businesses are disabled |
| `getNodeFeature().nodes()` | `Optional<NodeService>`, empty when nodes are disabled |
| `getNpcFeature().gateways()` | `Optional<NpcFeature.Gateways>`: the three gateways into SkysongNPCs, empty when it is not installed or businesses are disabled |
| `getNpcFeature().businessNpcs()` | `Optional<BusinessNpcs>`: which NPCs each business has and the slots they fill; also empty when `businesses/npcs.yml` can't be read |

`BusinessSystem` (in `bootstrap/`) is a record of every running business service: `businesses`, `types`, `access`,
`vaults`, `tokens`, `activities`, `shifts`, `training`, `economyLogistics`, `ledgers`, `ledgerSelections`,
`modifiers`, `pricing`, `deliveries`, `exports`, `menus` (to add a main menu button with
`menus.addEntry(MainMenuEntry)`) and `command` (the `/se business` command, to add sub-commands to).

Rules for callers:

- Use every service on the **main thread**.
- Change things through the services (for example `PriceModifiers.set` with your own modifier type), never through
  the tables, so the in-memory state and the listeners stay right.
- A refused action throws a `RefusedException` subclass; show it with the plugin's `Messenger` or read its
  `messageKey()` and `placeholders()`.
- To react to business changes, implement `BusinessChangeListener` and call `system.businesses().addListener(...)`;
  for nodes, `NodeChangeListener` and `NodeService.addListener`.
- To keep a feature's blocks away from businesses, implement `BlockGuard` and call `businesses().addBlockGuard(...)`.

This is not a published, versioned API yet. If another plugin starts depending on it, consider a small `api`
package with stable interfaces.

## Integrations (`integration/`)

Only these classes talk to other plugins, so a change in one of them only touches one file.

### SkysongStats: `integration/skysongstats/SkysongStatsAdapter`

Implements every `character/` interface over `SkysongStatsApi.get()`:

| Interface | Methods | Used for |
| --- | --- | --- |
| `CharacterDirectory` | `active(uuid)`, `all(uuid)`, `exists(character)` | Who a player is playing; `<player> [profile]` arguments |
| `DowntimeWallet` | `available(uuid)`, `trySpend(uuid, amount)` | Shifts, training, public node costs, taking primers out; uses the player's `/downtime preference` pool |
| `SkillCatalog` | `exists(skill)`, `displayName(skill)` | Checking config, showing names |
| `SkillLevels` | `levelIds()`, `levelOf(character, skill)`, `levelName(level)`, `reaches(...)` | Node tier requirements |
| `SkillProgression` | `addXp(character, skill, xp)`, `trainProficiency(character, skill, points, maxTier)` | Train/Learn rewards |

`CharacterRef` mirrors SkysongStats' `CharacterId` without depending on it, so rules are testable.

### SkysongNPCs: `integration/skysongnpcs/SkysongNpcsAdapter`

SkysongNPCs is optional. `bootstrap/NpcFeature` checks that the plugin is enabled before anything loads the
adapter, and `SkysongNpcsAdapter.connect` refuses an API older than `REQUIRED_API` (0.23.0), so a server with an old
SkysongNPCs gets one clear console error instead of failing halfway through a command.

The adapter implements the three interfaces in `business/npc`, which is all the business code sees:

| Interface | Methods | Used for |
| --- | --- | --- |
| `NpcGateway` | `exists`, `create(template, owner, where, radius)`, `remove`, `setOwner`, `isOwnedBy`, `inSetup`, `name` (plain text), `openMenu`, `openChoices`, `teleportTo` | A business NPC's life. `create` asks SkysongNPCs for a free spot (`SpawnPoint.InFrontOf` a player or `NextTo` a block) and answers with an `NpcCreation`: created, no spot, nowhere (player offline or world not loaded), unknown template, or refused. The owner is a character, or `null` for the server. |
| `NpcSheetGateway` | Stat points (`points`, `grantPoints`, `takeUnspentPoints`), proficiency picks (`picks`, `grantPicks` with optional promotion, `takeUnspentPicks`), `respec(npc, source)`, abilities (`holdsAbility`, `grantAbility`, `revokeAbility`), ability choices, baked stats (`addModifier`, `removeModifier`, `hasModifier`), fixed proficiencies (`proficiencyTier`, `setProficiencyTier`), and `tierIds`, `isStat`, `isProficiency`, `isAbility` for checking config | What an NPC is given. Everything in SkysongNPCs is tracked per source text (at most 64 characters, no spaces), so Economy only counts and takes back its own. Modifiers and proficiency tiers go on the NPC's SkysongStats sheet, which has no sources. |
| `NpcAccessGateway` | `actions`, `preset`, `hasPolicy`, `createPolicy`, `deletePolicy`, `grants`, `setGrant`, `assign`, `unassign`, `policyOf` | SkysongNPCs access policies. Policies made here are owned by the SkysongEconomy plugin. Actions and groups are plain texts (`inventory-view`, `economy:smithy/workers`). |

**Business groups in SkysongNPCs.** On enable, `BusinessGroupProvider` is registered under the group type `economy`,
so any SkysongNPCs grant or policy can name `economy:<business id>/<group>`: `workers`, `everyone`, or one of the
owner's groups. SkysongNPCs asks live on every check, and `business/npc/BusinessGroupDirectory` answers from the
businesses and `AccessService` in memory, so hiring, firing and group changes count at once. Membership belongs to
the character being played, not the account. The owner is in no group, because SkysongNPCs already lets an NPC's
owner do everything. `/snpc access` suggests the groups of the businesses the player's current character owns.

The unit tests use `FakeSkysongNpcs` (in `src/test`, package `business.npc`), which implements all three gateways
in memory the way SkysongNPCs' API documents them, with helpers that play the player: `spendPoints`, `spendPick`,
`promote`, `chooseAbility`, `completeSetup`.

### Gringotts: `integration/gringotts/GringottsWallet`

The `Wallet` over Gringotts player accounts. See [Currency and money](03-currency-and-money.md).

### SkysongItems (Skysong_Items)

Only the public API is used (`org.skysongdev.skysongitems.api.SkysongItemsApi`): `templates()` builds items from
template files (`items/FileSkysongItemFactory`), `tags()` reads item tags (`items/MaterialIdResolver` for
`material_id`, `items/PrimerItems` for `node_primer`), and `SkysongItemsRegisterEvent` registers the plugin's own
tags (`listeners/NodeItemTags`). Never use Skysong_Items' internal classes: they change between versions (1.8.0
removed `utils.Utils`, which once crashed this plugin on enable). The project rule is that the Skysong-Special-Items
repository is not edited from here.

### PlaceholderAPI: `integration/placeholderapi/TokenExpansion`

Registered only when PlaceholderAPI is installed (the class can't load without it). The texts come from
`tokens/TokenPlaceholders`, which is testable:

- `%skysongeconomy_tokens%`: the viewer's active character's tokens
- `%skysongeconomy_tokens_<player>%`: that player's active character's tokens

### CMI: `integration/cmi/CmiAfkCheck`

Calls `CMI.getInstance().getPlayerManager().getUser(player).isAfk()` by reflection, since CMI isn't on a Maven
repository. Without CMI, `AfkCheck.NONE` counts nobody as AFK. If the reflective call breaks after a CMI update, the
error is logged once and nobody counts as AFK.
