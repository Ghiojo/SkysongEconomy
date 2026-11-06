# Events, API and integrations

## Bukkit events (`events/`)

Other plugins can listen to these like any Bukkit event.

| Event | When | Cancellable | Getters |
| --- | --- | --- | --- |
| `ShiftStartEvent` | A player is about to start a shift, after every check passed and before downtime is spent | Yes: nothing is spent and the player is told the shift was cancelled | `getBusiness()`, `getPlayer()`, `getCharacter()`, `getMessage()` (the business's shift message, placeholders filled in) |
| `ShiftEndEvent` | A shift has its full time; its pay can now be retrieved. The player may be offline. | No | `getBusiness()`, `getShift()` (pay, times, character) |
| `NodeHarvestEvent` | Someone harvested at a node: paid and rolled, items handed over right after | No | `getPlayer()`, `getNode()`, `getCharacter()`, `getTier()`, `getPaid()` (`HarvestCost`), `getItems()` |

The shift message is not shown in game; `ShiftStartEvent.getMessage()` is meant for a Discord bridge or similar.

The rules don't fire Bukkit events themselves: they call `business/shift/ShiftEvents` and
`nodes/harvest/NodeHarvestEvents`, whose server implementations (`events/BukkitShiftEvents`,
`BukkitNodeHarvestEvents`) fire the events. Tests use fakes. To add an event, follow the same shape.

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

`BusinessSystem` (in `bootstrap/`) is a record of every running business service: `businesses`, `types`, `access`,
`vaults`, `tokens`, `activities`, `shifts`, `training`, `economyLogistics`, `ledgers`, `ledgerSelections`,
`modifiers`, `pricing`, `deliveries`, `exports`, and `menus` (to add a main menu button with
`menus.addEntry(MainMenuEntry)`).

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
