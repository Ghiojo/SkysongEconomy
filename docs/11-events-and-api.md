# Events, API and integrations

## Bukkit events (`api/`)

Other plugins can listen to these like any Bukkit event. Every event lives in a public `api` package and hands out
only public types (`api/business/BusinessInfo`, `ShiftInfo`, `NodeInfo`, `ItemCount`…), never internal classes, so
code built against them keeps compiling when the internals change. Business, shift, NPC, module and upkeep events
are in `api/business`, harvests in `api/nodes`, Claims in `api/claims`, piers and shipments in `api/piers`, exports and ledgers in `api/trade`, price
modifiers in `api/prices`.

| Event | When | Cancellable | Getters |
| --- | --- | --- | --- |
| `BusinessChangeEvent` (in `api/business`) | A business was created, deleted, renamed, moved, unplaced, re-tiered, changed owner, was given to the server, picked or lost a skill, or hired or lost a worker. A lower tier fires `TIER_CHANGED`, then one `WORKER_FIRED` / `SKILL_CLEARED` per worker and pick it cost | No | `getChange()` (`BusinessChange`: `kind`, `before` and `after` as `BusinessInfo`, `character` for workers, `skill` for picks), `getKind()`, `getBusinessId()` |
| `ShiftStartEvent` | A player is about to start a shift, after every check passed and before downtime is spent | Yes: nothing is spent and the player is told the shift was cancelled | `getBusiness()`, `getPlayer()`, `getCharacter()`, `getMessage()` (the business's shift message, placeholders filled in) |
| `ShiftEndEvent` | A shift has its full time; its vault cut is in the vault and its pay can now be retrieved. The player may be offline. | No | `getBusiness()`, `getShift()` (`ShiftInfo`: character, times, `payMoney`, `payTokens`, `vaultCut`) |
| `ShiftPayCollectedEvent` (in `api/business`) | A worker was paid for finished shifts: when they collect, and for each shift of a deleted business | No | `getBusiness()`, `getWorker()`, `getShifts()`, `getMoney()`, `getTokens()`, `isBusinessDeleted()` |
| `TrainingStartEvent` (in `api/business`) | Someone paid for a training session (price into the vault) and started it | No | `getBusiness()`, `getSession()` (`TrainingInfo`: who, when, how long, price) |
| `TrainingFinishEvent` (in `api/business`) | A training session finished and gave its rewards | No | `getBusiness()` (empty when it no longer exists), `getSession()`, `getSkills()` (`SkillTrainedInfo`: skill, picks, XP, proficiency) |
| `VaultCollectEvent` (in `api/business`) | A vault was collected into its owner's (or, for a server business, the collecting staff member's) balance | No | `getBusiness()`, `getPayee()`, `getAmount()` |
| `VaultCloseEvent` (in `api/business`) | A deleted business's vault held money: paid to its owner, or left the economy for a server business | No | `getBusiness()`, `getPayee()` (empty for a server business), `getAmount()`, `isPaid()` |
| `BusinessNpcCreateEvent` | A business created an NPC in one of its slots; it is waiting for its first setup | No | `getBusiness()`, `getNpc()` (`BusinessNpcInfo`: SkysongNPCs id, business, slot source, creation time) |
| `BusinessNpcRemoveEvent` | A business no longer has an NPC and its slot is free. Unless the cause is `REMOVED_ELSEWHERE`, the NPC still exists while this fires and is removed right after | No | `getBusiness()`, `getNpc()`, `getCause()` (`DISMISSED`, `SLOT_LOST`, `BUSINESS_DELETED`, `REMOVED_ELSEWHERE`) |
| `BusinessModulePickEvent` | A business picked a module; the pick is saved | No | `getBusiness()`, `getPick()` (`ModulePickInfo`: module id, skill, order, time) |
| `BusinessModuleRemoveEvent` | A business no longer has a module it picked | No | `getBusiness()`, `getPick()`, `getCause()` (`CLEARED`, `PICK_LOST`, `BUSINESS_DELETED`) |
| `NpcUpkeepChangeEvent` | An NPC became maintained or unmaintained | No | `getNpc()`, `isMaintained()`, `getUpkeep()` (`NpcUpkeepInfo`: profile, maintained, next cycle), `getCause()` (`CYCLE`, `REPAIR`, `STAFF`) |
| `NpcUpkeepPaidEvent` (in `api/business`) | An NPC's upkeep cycle was paid from its stock; once per cycle, also for cycles missed while the server was down. An unpaid cycle fires `NpcUpkeepChangeEvent` instead | No | `getUpkeep()` (after paying, with the next cycle), `getCycleDue()`, `getEaten()` (`ItemCount`s) |
| `ClaimPurchaseAttemptEvent` (in `api/claims`) | A character tries to buy a Claim on sale: as the confirmation opens and on Confirm, before SkysongClaims' checks and before money moves | Yes, with a message | `getBuyer()`, `getClaimId()`, `getClaimTypeId()`, `setCancelled(Component)`, `getReason()`. See [Claims](15-claims.md#claimpurchaseattemptevent) |
| `ClaimPurchaseEvent` (in `api/claims`) | A Claim was bought: money taken, owner added, sale recorded | No | `getSale()` (`ClaimSale`: Claim, buyer, asking price, each modifier's share, total paid). See [Claims](15-claims.md#claimpurchaseevent) |
| `ClaimPayoutEvent` (in `api/claims`) | A seller's share of a Claim sale that waited for them was paid, usually as they joined | No | `getAccount()`, `getClaimId()`, `getSaleId()`, `getAmount()`, `getSoldAt()` |
| `ClaimSaleChangeEvent` (in `api/claims`) | A Claim's asking price changed, or SkysongEconomy put it on or took it off sale (also on start-up, matching SkysongClaims) | No | `getChange()` (`ClaimSaleChange`: `kind`, Claim, `price` after the change, `listedBy`, time), `getKind()`. See [Claims](15-claims.md#other-claim-events) |
| `PierChangeEvent` (in `api/piers`) | A pier was created, changed or removed (also on `/se pier reload`) | No | `getBefore()`, `getAfter()`, `getPierId()`. See [Import piers](16-import-piers.md#the-api-apipiers) |
| `ShipmentOrderEvent` (in `api/piers`) | An import is about to be ordered; nothing paid yet | Yes: nothing is paid, the player sees `getCancelMessage()` or the default | `getShipment()`, `getPier()`, `getPlayer()`, `getPrice()` (`TradePrice`), `setCancelMessage(...)` |
| `ShipmentOrderedEvent` (in `api/piers`) | The import was paid and its delivery slot booked | No | `getBusiness()`, `getShipment()`, `getPier()`, `getPrice()` (`TradePrice` paid), `getPlayer()` |
| `ShipmentArriveEvent` (in `api/piers`) | A delivery is being collected, right after its pier's loss roll | Yes: cancelled = seized (lost, no refund); starts cancelled when the roll lost it | `getShipment()`, `getPier()`, `getCollector()`, `getLossChance()`, `getRoll()`, `isLostByRoll()`, `getItems()`/`setItems(...)`, `setSeizeMessage(...)` |
| `ShipmentCollectedEvent` (in `api/piers`) | The shipment was handed over or seized; slot freed | No | `getShipment()`, `getPier()`, `getCollector()`, `getOutcome()`, `getItems()`, `getArrivedItems()` (before listeners changed them), `getSeizeMessage()`, `getLossChance()`, `isLostByRoll()` |
| `ShipmentRefundedEvent` (in `api/piers`) | A deleted business refunded an import still on its way | No | `getBusiness()`, `getShipment()`, `isPaidBack()` |
| `ExportSellEvent` (in `api/trade`) | A batch of an export is about to be sold; nothing paid or taken yet | Yes: nothing is paid or taken, the seller sees `getCancelMessage()` or `business.exports.sale-cancelled` | `getBusiness()`, `getSale()` (`ExportSale`: ledger, export, seller, `ItemCount`s, `TradePrice`, time, tags), `getPlayer()`, `setCancelMessage(...)` |
| `ExportSoldEvent` (in `api/trade`) | The seller was paid and the items taken | No | `getBusiness()`, `getSale()`, `getPlayer()` |
| `LedgerSelectionChangeEvent` (in `api/trade`) | A ledger was selected, removed (fine paid) or dropped because the business lost the slot. Deleting a business fires `BusinessChangeEvent`, not this | No | `getBusiness()`, `getChange()` (`LedgerSelectionChange`: `kind` `SELECTED`/`REMOVED`/`DROPPED`, ledger, `fine`, `paidBy`, time) |
| `PriceModifierChangeEvent` (in `api/prices`) | A price modifier was added, replaced, removed (also when its business or pier goes) or expired. Expiry is noticed the next time modifiers are read, so it can come a little late | No | `getChange()` (`PriceModifierChange`: `kind` `ADDED`/`REPLACED`/`REMOVED`/`EXPIRED`, `modifier` and `previous` as `PriceModifierInfo`, time) |
| `EconomyRecordedEvent` (in `api/journal`) | A record was added to the economy journal: once per movement of money or goods, in sequence order | No | `getRecord()` (`EconomyRecord`), `getKind()`. See [Economy journal](17-economy-journal.md) |
| `NodeHarvestEvent` (in `api/nodes`) | Someone harvested at a node: paid and rolled, items handed over right after | No | `getPlayer()`, `getNode()` (`NodeInfo`), `getCharacter()`, `getTier()`, `getCost()` (`HarvestCostInfo`: downtime, Harvest Tokens, money), `getItems()` (`ItemCount`s), `getModifiers()` (ids of the calendar modifiers that changed it), `getBooster()` (`BoosterInfo`: type and effect, when one was used up), `getResource()` (the farm resource, at a farm node) |
| `FarmPlantingChangeEvent` (in `api/nodes`) | A primer was planted in a farm node, taken out (removal downtime paid), or given back to its planter because its node went or changed owner or type (also when the plugin starts) | No | `getChange()` (`PlantingChange`: `kind` `PLANTED`/`TAKEN_OUT`/`RETURNED`, node, resource, farm type, business, planter, who acted, `downtimePaid`, time), `getKind()`, `getPrimer()` (copies of the exact items) |
| `BenchAssignEvent` (in `api/benches`) | A SkysongCrafting bench was tied to a business, or moved to it from another business | No | `getLink()` (`BenchLink`: bench block, business id, bench type, who, when), `getPrevious()` (the old link when it moved) |
| `BenchUnassignEvent` (in `api/benches`) | A bench no longer belongs to its business | No | `getLink()`, `getCause()` (`UNASSIGNED`, `BUSINESS_DELETED`, `BENCH_REMOVED` in SkysongCrafting, `BENCH_MISSING` on startup), `getBy()` (who, when `UNASSIGNED`) |

The shift message is not shown in game; `ShiftStartEvent.getMessage()` is meant for a Discord bridge or similar.

The rules don't fire Bukkit events themselves: they call `business/shift/ShiftEvents`,
`business/npc/BusinessNpcEvents` and `nodes/harvest/NodeHarvestEvents`, whose server implementations
(`events/BukkitShiftEvents`, `BukkitBusinessNpcEvents`, `BukkitNodeHarvestEvents`…) turn the internal types into the
public ones and fire the events. Tests use fakes. To add an event, follow the same shape: the event and its public
types in `api/`, the Bukkit implementation in `events/`.

## Calling the plugin from another plugin

Get the plugin instance (`(SkysongEconomy) Bukkit.getPluginManager().getPlugin("SkysongEconomy")`), then:

| Getter | Returns |
| --- | --- |
| `getPrices()` | `PriceDatabase`: every item's base value (`value(materialId)`, `set`, `remove`, `context`). To only read values, use `ItemCatalog` |
| `getPriceSettings()` | `PriceSettings`: decimals and `truncate` |
| `getGoodTypes()` | `CachedGoodTypes`: `of(materialId)`, `all()` and changes. To only read good types, use `ItemCatalog` |
| `getImportItems()` | `SkysongItemFactory` for the `items/` folder |
| `getSkysongStats()` | `SkysongStatsAdapter` |
| `getBusinessFeature().system()` | `Optional<BusinessSystem>`, empty when businesses are disabled |
| `getNodeFeature().nodes()` | `Optional<NodeService>`, empty when nodes are disabled |
| `getModuleFeature().modules()` | `Optional<BusinessModules>`: the modules each business picked (`of`, `has`, `groups`, `pool`, `pick`, `clear`) and every module file with its effects (`module`, `modules`); empty when businesses are disabled or `businesses/modules.yml` can't be read |
| `getPierFeature().piers()` | `PierService`, which implements `api.piers.Piers`. Other plugins should rather `Bukkit.getServicesManager().load(Piers.class)` |

| `getClaimFeature().gateway()` | `Optional<ClaimGateway>`: the way into SkysongClaims, empty when it is not installed or too old |
| `getClaimFeature().pricing()` | `Optional<ClaimPricing>`: what a Claim costs with its modifiers (`quote`), and Claim type modifiers (`setTypeModifier`, `removeTypeModifier`, `modifiersOf`) |
| `getClaimFeature().sales()` | `Optional<ClaimSales>`: Claim asking prices (`prices()`), server sales (`sell`, `unsell`, `listed`), empty whenever `gateway()` is |
| `getNpcFeature().gateways()` | `Optional<NpcFeature.Gateways>`: the gateways into SkysongNPCs (NPCs, sheets, access, and `menu` for buttons in its NPC menu), empty when it is not installed or businesses are disabled |
| `getNpcFeature().progression()` | `Optional<NpcProgression>`: what each business NPC is owed (`owed`) and bringing it in line (`sync`) |
| `getNpcFeature().npcAccess()` | `Optional<BusinessNpcAccess>`: what each access group may do with a business's NPCs (`grants`, `allowed`, `set`, `toggle`) |
| `getNpcFeature().businessNpcs()` | `Optional<BusinessNpcs>`: which NPCs each business has and the slots they fill; also empty when `businesses/npcs.yml` can't be read |

Services other plugins should use instead of the getters above, from `Bukkit.getServicesManager().load(...)`:
`api.piers.Piers`, `api.business.Businesses` (read-only `BusinessInfo` copies: `find`, `all`, `ownedBy`),
`api.items.ItemCatalog` (read-only base values and good types, see below), `api.items.PriceFormulas` (price formulas, see below), `api.benches.BusinessBenches`
(crafting benches tied to businesses, see below; registered while businesses are enabled) and
`api.journal.EconomyJournal` (reads the [economy journal](17-economy-journal.md#reading-the-journal); not registered
while the journal is off).

### Item values and good types: `api.items.ItemCatalog`

The stable, read-only way to read the price database and the good types; use it rather than `getPrices()` and
`getGoodTypes()`. It is registered for as long as the plugin is enabled. Implementation: `prices/ItemLookup` (Bukkit-free, tested), wrapped by `prices/CatalogService`.

| Method | Returns |
| --- | --- |
| `baseValue(materialId)` | The item's base value, truncated to `prices.yml` decimals as players see it; empty when it has none or it can't be worked out |
| `baseValues()` | Every base value by lower-case material id, alphabetically (values that can't be worked out are left out) |
| `decimals()` | How many decimals prices keep |
| `goodTypes(materialId)` | The item's good types, alphabetically |
| `hasGoodType(materialId, goodType)` | Whether the item has that good type |
| `allGoodTypes()` | Every good type at least one item has |
| `itemsOf(goodType)` | The lower-case material ids that have that good type |
| `materialId(itemStack)` | The Skysong material id of a stack; empty for air or a non-Skysong item |

Material ids and good types ignore case and surrounding spaces, and everything returned is an immutable copy.
Values are worked out on every call, so they follow `/se ledger` and `/se good` changes at once. Call it on the
main thread.

```java
ItemCatalog items = Bukkit.getServicesManager().load(ItemCatalog.class);
Optional<BigDecimal> worth = items.baseValue("steel_sword");
boolean weapon = items.hasGoodType("steel_sword", "weapon");
```

### Price formulas: `api.items.PriceFormulas`

Lets another plugin price things in its own config with the same formulas ledgers use, instead of copying the parser.
Registered for as long as the plugin is enabled. Implementation: `prices/FormulaService` (Bukkit-free, tested) over
`PriceExpression`, `PriceDatabase` and `PriceSettings`.

| Method | Returns |
| --- | --- |
| `parse(text)` | A `PriceFormula`; throws `InvalidPriceFormulaException` (`formula()`, `problem()`) when the text isn't valid. Item references are not checked here |
| `PriceFormula.text()` | The formula as written |
| `PriceFormula.items()` | The lower-case material ids its `{item:...}` references name |
| `PriceFormula.usesBase()` | Whether it uses `{base}` |
| `PriceFormula.value()` | Its value without `{base}`, truncated to `prices.yml` decimals and never below 0; empty when it uses `{base}`, names an item without a base value, or divides by zero |
| `PriceFormula.value(base)` | The same, with `{base}` standing for the given value |

A formula allows numbers, `+ - * /`, parentheses, a leading minus, `{base}` and `{item:<material_id>}`, exactly as in
[price expressions](04-prices.md#price-expressions). What `{base}` means is up to the caller. Values are worked out on
every call, so they follow `/se ledger` changes at once. Call it on the main thread.

```java
PriceFormulas formulas = Bukkit.getServicesManager().load(PriceFormulas.class);
PriceFormula price = formulas.parse("{item:steel_ingot} * 1.2 + {base}");
Optional<BigDecimal> now = price.value(new BigDecimal("3"));
```

### Crafting benches tied to businesses: `api.benches.BusinessBenches`

Which SkysongCrafting benches belong to which business. A bench (identified by its block, `BenchPosition`) belongs to
at most one business; a business can have any number. Implementation: `business/bench/BenchRegistry` (rules, Bukkit-free,
tested) wrapped by `business/bench/BenchService`; saved in `business_benches`.

| Method | Does |
| --- | --- |
| `at(bench)` | The bench's `BenchLink`; empty when it isn't tied |
| `of(businessId)` | The business's benches, oldest first |
| `all()` | Every link, by business id |
| `assign(bench, businessId, by)` | Ties it; a bench of another business moves in one step. Throws `BenchException` (`business.not-found`, `business.bench.no-crafting`, `business.bench.not-a-bench`, `business.bench.already-assigned`) |
| `unassign(bench, by)` | Unties it; returns the removed link |

`by` is a player's or plugin's name. Links go with a deleted business, and are untied when SkysongCrafting removes the
bench (or it is found gone on startup). Without SkysongCrafting, links can be read and untied but not made.

```java
BusinessBenches benches = Bukkit.getServicesManager().load(BusinessBenches.class);
benches.at(BenchPosition.of(block)).ifPresent(link -> { /* link.businessId() */ });
```

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
- To react to business changes, implement `BusinessChangeListener` (`changed`, `deleted`, and `described` for what
  exactly changed) and call `system.businesses().addListener(...)`;
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
| `NpcSheetGateway` | Stat points (`points`, `grantPoints`, `takeUnspentPoints`), proficiency picks (`picks`, `grantPicks` with optional promotion, `takeUnspentPicks`), `respec(npc, source)`, abilities (`holdsAbility`, `abilitiesHeldBy`, `grantAbility`, `revokeAbility`), ability choices, baked stats (`addModifier`, `removeModifier`, `hasModifier`), fixed proficiencies (`proficiencyTier`, `setProficiencyTier`), and `tierIds`, `isStat`, `isProficiency`, `isAbility` for checking config | What an NPC is given. Everything in SkysongNPCs is tracked per source text (at most 64 characters, no spaces), so Economy only counts and takes back its own. Modifiers and proficiency tiers go on the NPC's SkysongStats sheet, which has no sources. |
| `NpcAccessGateway` | `actions`, `preset`, `hasPolicy`, `createPolicy`, `deletePolicy`, `grants`, `setGrant`, `assign`, `unassign`, `policyOf` | SkysongNPCs access policies. Policies made here are owned by the SkysongEconomy plugin. Actions and groups are plain texts (`inventory-view`, `economy:smithy/workers`). |
| `NpcMenuButtons` (in `gui/business`) | `addInfoButton(name, slot, icon)` | A display-only button in the menu SkysongNPCs opens on an NPC, shown only where the icon is not null. Used for the business card, `skysongeconomy:business` |

**Business groups in SkysongNPCs.** On enable, `BusinessGroupProvider` is registered under the group type `economy`,
so any SkysongNPCs grant or policy can name `economy:<business id>/<group>`: `workers`, `everyone`, or one of the
owner's groups. SkysongNPCs asks live on every check, and `business/npc/BusinessGroupDirectory` answers from the
businesses and `AccessService` in memory, so hiring, firing and group changes count at once. Membership belongs to
the character being played, not the account. The owner is in no group, because SkysongNPCs already lets an NPC's
owner do everything. `/snpc access` suggests the groups of the businesses the player's current character owns.

The unit tests use `FakeSkysongNpcs` (in `src/test`, package `business.npc`), which implements all three gateways
in memory the way SkysongNPCs' API documents them, with helpers that play the player: `spendPoints`, `spendPick`,
`promote`, `chooseAbility`, `completeSetup`.

**NPC upkeep** goes through its own small adapter, `SkysongNpcsUpkeepAdapter` (`upkeep/UpkeepNpcs`: every managed
NPC with its template, `exists`, `name`), checked against the same `REQUIRED_API`. It also uses the API directly for
the Upkeep menu button (`UpkeepMenuButton`, `menu().registerButton`, `access().may(..., INVENTORY_EDIT)`), NPC items
(`items().isCurrent`, `storedNpc`), the events `NpcDeleteEvent`, `NpcPickupEvent` and `NpcPlaceEvent`, and the
sheet footer `skysongeconomy:upkeep` (`UpkeepSheetLine`, `sheets().registerFooter`, API 0.24.0 or newer, skipped
with a warning on older ones). See [NPC upkeep](14-npc-upkeep.md).

### SkysongClaims: `integration/skysongclaims/SkysongClaimsAdapter`

SkysongClaims is optional. `bootstrap/ClaimFeature` checks that the plugin is enabled before anything loads the
adapter, and `SkysongClaimsAdapter.connect` refuses an API older than `REQUIRED_API` (1.0.0). The adapter implements
`claims/ClaimGateway` (read Claims as `ClaimView`, `canOwn`, `addOwner`, `clearOwners`, `setForSale`, the sign price
line), which is all the Claim rules see. Version checks for both SkysongNPCs and SkysongClaims use
`integration/ApiVersion`. See [Claims](15-claims.md).

### SkysongCrafting: `integration/skysongcrafting/SkysongCraftingBenches`

SkysongCrafting is optional. `bootstrap/BenchFeature` checks that it is enabled before loading anything from it, then
`SkysongCraftingBenches` (the only class that reads SkysongCrafting's `Workstations` service) implements
`business/bench/BenchLookup` (`typeAt(block)`). `WorkstationRemovalListener` unties a bench on `WorkstationRemoveEvent`.
Without it, or with one older than 1.0.0 (no `Workstations` service), the lookup is `BenchLookup.NONE`.

### SkysongCalendar: `integration/skysongcalendar/SkysongCalendarAdapter`

SkysongCalendar is optional. `bootstrap/CalendarConnection` checks that the plugin is enabled before anything loads the
adapter, and `SkysongCalendarAdapter.connect` refuses an API older than `REQUIRED_API` (1.0.0); either way the plugin
gets `CalendarClock.NONE` instead and nothing changes with the date. The adapter implements `calendar/CalendarClock`:
`today()` (from `calendar().today()`, as a `CalendarDay` of season id, month id or festival, and day), `names()` (the
seasons and months in order, with their display names, built from SkysongCalendar's `Season` and `Month`, so config
files are always checked against SkysongCalendar's own names) and `whenDayChanges` (a `CalendarDayListener` on
`IcDayChangeEvent` and `CalendarReloadEvent`). Ids are the enum names in lower case (`VINSE_ER` is `vinse_er`). See
[calendar modifiers](07-nodes.md#calendar-modifiers-nodesmodifiersidyml).

### Citizens: `integration/citizens/CitizensNpcs`

Optional, and only for NPC upkeep. `CitizensNpcs` finds the NPC a player looks at (within 8 blocks), where an NPC
stands (for dropping its stock when it is removed) and its body in the world (`entity`), which
`UpkeepNameLines` rides a text display on. It is compiled against `citizens-main` 2.0.43-SNAPSHOT, the same
SkysongNPCs uses, and never loaded unless Citizens is enabled.

### Gringotts: `integration/gringotts/GringottsWallet`

The `Wallet` over Gringotts player accounts. See [Currency and money](03-currency-and-money.md).

### SkysongItems (Skysong_Items)

Only the public API is used (`org.skysongdev.skysongitems.api.SkysongItemsApi`): `templates()` builds items from
template files (`items/FileSkysongItemFactory`), `tags()` reads item tags (`items/MaterialIdResolver` for
`material_id`, `items/PrimerItems` for `node_primer`), and `SkysongItemsRegisterEvent` registers the plugin's own
tags (`listeners/NodeItemTags`). Never use Skysong_Items' internal classes: they change between versions (1.8.0
removed `utils.Utils`, which once crashed this plugin on enable). The project rule is that the Skysong-Special-Items
repository is not edited from here.

Item "properties" are now called **traits** (API 1.4.0, tag `1.2.0`). The old names (`ItemAccess.properties()`,
`ItemProperties`, `PropertyDefinition`, `SkysongItemsRegisterEvent.registerProperty`) are deprecated for removal, so
new code uses `ItemAccess.traits()`, `ItemTraits`, `TraitDefinition` and `registerTrait`. The build compiles with
`-Xlint:deprecation`, so any use of the old names shows up as a warning.

NPC upkeep registers the printable trait `unmaintained` and the hidden bool tag `upkeep_marked`, and adds or removes
them with `traits()` and `tags()` (`integration/skysongitems/UpkeepItemMarks`). SkysongItems fires no event when a
trait is removed, which is why the tag exists; see [NPC upkeep](14-npc-upkeep.md). Both are registered with
`OnTransform.KEEP` (API 1.16.0, tag `1.3.0`): from that version a transform drops every other plugin's tags and traits
unless they are kept, the same reason SkysongNPCs keeps `npc_id` and `npc_ticket` on its NPC items.

### PlaceholderAPI: `integration/placeholderapi/TokenExpansion`

Registered only when PlaceholderAPI is installed (the class can't load without it). The texts come from
`tokens/TokenPlaceholders`, which is testable:

- `%skysongeconomy_tokens%`: the viewer's active character's tokens
- `%skysongeconomy_tokens_<player>%`: that player's active character's tokens

### CMI: `integration/cmi/CmiAfkCheck`

Calls `CMI.getInstance().getPlayerManager().getUser(player).isAfk()` by reflection, since CMI isn't on a Maven
repository. Without CMI, `AfkCheck.NONE` counts nobody as AFK. If the reflective call breaks after a CMI update, the
error is logged once and nobody counts as AFK.
