# NPC upkeep

An NPC registered for upkeep needs a supply of items every cycle. Players stock it through an **Upkeep chest** on
the NPC's menu, and every cycle the NPC eats its cost from that chest. If a cycle comes and the cost is not
there, the NPC becomes **Unmaintained**.

- Unmaintained is a **marker only**. The NPC keeps working exactly as before; staff decide by hand what it means.
- An unmaintained NPC **stops eating**: its cycles take nothing. Restocking does not fix it.
- The way back is a **SkysongCrafting recipe on the NPC's item** that removes the `unmaintained` trait, or a staff
  command. The NPC is then maintained, and its next cycle is one full cycle from that moment.

The design and the reasons behind it are in `design/npc-upkeep-design.md` (signed off 2026-11-17).

Where the marker shows:

| Where | How |
| --- | --- |
| In the world | A soft red `Unmaintained` line under the NPC's name |
| Character sheet | A soft red `Unmaintained` line under `/snpc sheet`, after the abilities line |
| NPC menu | The Upkeep button turns into a redstone block that says Unmaintained |
| NPC item (picked up) | The item gets the `unmaintained` trait, printed in its lore as `[Unmaintained]` |

Code map:

| Part | Classes |
| --- | --- |
| Settings | `upkeep/UpkeepSettings`, `UpkeepSettingsParser` (`npc-upkeep.yml`), `UpkeepProfile`, `UpkeepCost`, `CycleLength` |
| Core | `upkeep/UpkeepService` (clock, settling, marking), `NpcUpkeep`, `UpkeepState`, `UpkeepPayment` (all-or-nothing plan), `UpkeepEvents` |
| Stock | `upkeep/StockedItems`, `UpkeepStock`, `UpkeepChests` (one shared chest per NPC), `ChestMove`, `listeners/UpkeepChestListener` |
| Texts | `upkeep/UpkeepText` (state, next cycle, cost, cycles covered) |
| Item trait | `upkeep/ItemMark` (the rule), `integration/skysongitems/UpkeepItemMarks`, `listeners/UpkeepItemListener` |
| Name line | `integration/citizens/UpkeepNameLines`, `integration/citizens/CitizensNpcs` |
| SkysongNPCs | `integration/skysongnpcs/SkysongNpcsUpkeepAdapter`, `UpkeepMenuButton`, `UpkeepSheetLine`, `UpkeepNpcListener` |
| Database | `data/database/SqlUpkeepRepository`, `SqlUpkeepStockRepository` and their `Async*` wrappers |
| Event | `events/NpcUpkeepChangeEvent`, `events/BukkitUpkeepEvents` |
| Commands | `commands/upkeep/*` |
| Wiring | `bootstrap/UpkeepFeature` |

Upkeep needs **SkysongNPCs and Citizens**. Without either, the console says "SkysongNPCs or Citizens is not
installed: NPC upkeep is off" and nothing else changes. It does not need the business system.

## Which NPCs have an upkeep

Only NPCs registered for it:

- **By template**: `templates:` in `npc-upkeep.yml` maps a SkysongNPCs template id to a profile. Every NPC made
  from that template has the profile, business NPCs included (they are made from the template their skill names
  in `businesses/npcs.yml`). Templates are matched without case, and the list is applied again on every check, so new NPCs from a listed
  template get their upkeep within `check-every` seconds.
- **By hand**: staff look at the NPC and run `/se upkeep set <profile>`. A profile set by hand wins over the
  template's. `/se upkeep set none` takes the upkeep away whatever the template says, and `/se upkeep set
  template` lets the template decide again.

A **profile** says how long a cycle lasts (`every`), how big the Upkeep chest is (`slots`) and what each cycle
costs (`cost`). See `npc-upkeep.yml` in the [configuration reference](09-configuration-reference.md).

Each cost entry is either one item by its `material_id` (`item:`) or any item of a good type (`good-type:`, the
item categories of `/se good`). A cycle can ask for several entries. Entries are paid in the order written and one
item pays only one entry, so put single items before the categories they belong to.

## The cycle

- An NPC that gets an upkeep starts **maintained**, and its first cycle is due one `every` later. Changing the
  profile keeps the state (it never repairs an unmaintained NPC).
- Every `check-every` seconds (default 60), `UpkeepService.settle` handles every cycle that is due:
  - **Maintained** and the chest holds every entry: those items are taken and the next cycle is one `every` later.
  - **Maintained** and any entry is short: **nothing** is taken and the NPC becomes **unmaintained** (cause `CYCLE`).
  - **Unmaintained**: nothing happens.
- Time is real time, saved as a "next due" timestamp, so it counts while the server is off and while the NPC is
  picked up as an item. Cycles missed while the server was down are settled in order at the first check, at most
  1000 for one NPC in one check.
- **Marking maintained** (repair or staff) sets the next cycle one `every` from that moment.
- When an NPC is removed for good (`NpcDeleteEvent`), its upkeep row is forgotten and what is left in its chest
  drops at its last known spot. If it was never seen in a loaded world, the stock stays saved instead, with a
  warning. When an NPC is picked up, its stock stays with it.

## The Upkeep chest

`UpkeepMenuButton` adds the button `skysongeconomy:upkeep` (slot 23 by default, movable in SkysongNPCs'
`menus.yml`) to the NPC menu, only for NPCs that have an upkeep. Its icon is a barrel while maintained and a
redstone block while unmaintained, and lists the cost per cycle, when the next cycle is due and how many cycles the
stock covers.

Clicking opens a chest of the profile's `slots`. It is kept by SkysongEconomy in MySQL, separate from Citizens'
NPC inventory, so it never counts toward the NPC's item-tag stats.

- **Everyone viewing the same NPC shares one chest** (`UpkeepChests`), so two players never overwrite each other.
  It is saved a tick after every change, saved before each check and refreshed after it.
- **Putting in**: anyone who can open the NPC's menu. Only items that match a cost entry are accepted.
- **Taking out**: only players with the NPC's `inventory-edit` action (the owner and staff always have it). The
  rule for each click is `ChestMove.allowed`; refused clicks say why (`upkeep.chest-refused-item`,
  `upkeep.chest-deposit-only`).

## The `unmaintained` item trait and its repair

`UpkeepItemMarks` registers, through `SkysongItemsRegisterEvent`, the printable trait **`unmaintained`** (category
NPC, texts under `upkeep.trait.*`) and a hidden bool tag **`upkeep_marked`**. Staff use the trait id in the
SkysongCrafting recipe that repairs an NPC item.

**Putting it on.** SkysongNPCs has no hook for when an NPC item is made, and the clock runs while the NPC is an
item, so items are brought up to date wherever the plugin can see them (`UpkeepItemListener`):

- a tick after a pickup (`NpcPickupEvent`);
- when a player joins;
- when a container opens (its items and the player's), except SkysongEconomy's own menus and chests;
- a tick after any inventory closes, so a repair counts right after the crafting menu closes;
- when the item places its NPC (`NpcPlaceEvent`);
- on every upkeep check, for all online players' inventories.

Only the **current** item of an NPC counts (`NpcItems.isCurrent`); blueprints and old copies from before a
`/snpc reissue` are never touched.

**Noticing it was taken off.** SkysongItems fires no event when a trait is removed, so the hidden tag is the signal.
`ItemMark.decide(state, hasTrait, marked)`:

| NPC state | Trait | Tag | Result |
| --- | --- | --- | --- |
| maintained or no upkeep | any | any | `UNMARK`: both are taken off (if there) |
| unmaintained | yes | no | `MARK`: the tag is added |
| unmaintained | yes | yes | `NONE` |
| unmaintained | no | no | `MARK`: not marked yet, so trait and tag are added |
| unmaintained | no | yes | `REPAIR`: the NPC becomes maintained (cause `REPAIR`), the tag is cleared |

This relies on the repair recipe giving back the same item with the trait removed and its other tags kept
(`npc_id`, `npc_ticket`, `upkeep_marked`). Smoke test 12j.4 checks it.

**Transforms keep the mark.** Since SkysongItems 1.3.0 an item that transforms into another form loses every tag and
trait the new form doesn't set, unless they were registered to be kept. The trait and the tag are both registered with
`OnTransform.KEEP`, so an unmaintained NPC item stays marked through its forms, just as SkysongNPCs keeps the NPC it
holds (`npc_id`, `npc_ticket`). Keeping both together matters: if only the tag were lost the item would be marked
again, and if only the trait were lost it would look repaired.

**Lore.** SkysongNPCs 1.2.0 writes its own "Holds / Owner" lines through SkysongItems' lore API, and every NPC item is a
SkysongItems item (a plain `pickup.item` material is turned into one). So the `[Unmaintained]` line can be added to
any NPC item, and SkysongNPCs' lines stay when adding or removing it makes SkysongItems redraw the lore. With
`pickup.lore-position: after-traits` (SkysongNPCs' default) the NPC lines sit right under the trait line.

## The line under the name

`UpkeepNameLines` puts a text display (no armour stand) on every spawned unmaintained NPC, as a passenger so it
follows the NPC as it walks. It is billboarded and lifted by `name-line-height` blocks (default 0.2) to sit just
above the head, under the name. The text is `upkeep.name-line`.

The display is **never saved with the world**. It is put back every 2 seconds and on every `NpcUpkeepChangeEvent`,
which covers restarts, chunk reloads and the NPC being placed again, and it is removed when the NPC is maintained,
picked up, despawned or loses its upkeep. All of them are removed when the plugin stops. The NPC's body is found
through Citizens (`CitizensNpcs.entity`).

## The line under the sheet

`UpkeepSheetLine` registers the SkysongNPCs sheet footer `skysongeconomy:upkeep`
(`sheets().registerFooter`, SkysongNPCs API 0.24.0). While the NPC is unmaintained, `upkeep.sheet-line` shows under
its sheet after the abilities line, from `/snpc sheet` and the menu's Sheet button alike. With an older SkysongNPCs
the line is skipped with one warning, and the rest of upkeep works.

## Staff commands

All need `skysongeconomy.upkeep.admin`. Commands on "the NPC" act on the one you look at, within 8 blocks.

| Command | Does |
| --- | --- |
| `/se upkeep list [unmaintained]` | Every NPC with an upkeep: profile, state, next cycle |
| `/se upkeep info` | The NPC's profile (and whether it came from its template or by hand), state, cost per cycle, next cycle, and how many cycles the stock covers |
| `/se upkeep set <profile\|none\|template>` | Gives the NPC a profile by hand, takes its upkeep away, or lets its template decide |
| `/se upkeep mark <maintained\|unmaintained>` | Sets the state (cause `STAFF`); maintained restarts the timer, as a repair does |

## Adding to it

- **A new place the marker shows**: listen to `NpcUpkeepChangeEvent` (state and cause) or ask
  `UpkeepService.isUnmaintained(npc)`.
- **A new kind of cost entry**: add a case to `UpkeepCost.Match`, parse it in `UpkeepSettingsParser`, and match it in
  `UpkeepPayment` (both the plan and `accepts`, which the chest uses to filter deposits).
