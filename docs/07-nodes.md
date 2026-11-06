# Nodes

A node is a protected block where characters gather materials. Staff place nodes; players right-click them to open
a menu and click a tier to harvest once, instantly. There are two kinds:

- **Random nodes** (mines, forests...) roll on a **random table** (`nodes/random/<id>.yml`).
- **Farm nodes** grow whatever **primer** item a player puts in them, from one **farm type**
  (`nodes/farm/types/<id>.yml`), which lists the **farm resources** it takes (`nodes/farm/resources/<id>.yml`).

Both roll their items with the shared **loot** system: guaranteed items, independent percent chances, and weighted
rolls on **loot groups** (`nodes/groups/<id>.yml`).

Code map:

| Part | Classes |
| --- | --- |
| Core | `nodes/Node`, `NodeService`, `NodeKind`, `NodeOwner`, `NodeSnapshot`, `NodeBlockGuard` |
| Settings | `nodes/NodeSettings`, `NodeSettingsParser` (`nodes/settings.yml`) |
| Node files | `nodes/NodeFiles` (reads them all), `NodeTables` (the loaded ones), `TierKeys`, `config/YamlFolder`, `config/FileRegistry` |
| Loot | `loot/*`: `LootParser`, `LootGroupLoader`, `LootGroup`, `LootOutput`, `LootRoller`, `LootOdds` |
| Random | `nodes/random/*`, `nodes/harvest/RandomHarvester` |
| Farm | `nodes/farm/*`: `FarmResource`, `FarmType`, `FarmPlots`, `FarmHarvester`, `PrimerReturns` |
| Paying | `nodes/harvest/HarvestCost`, `HarvestPayer`, `NodeAccess` |
| Menus | `gui/node/NodeMenus`, `NodeGatherMenu`, `FarmMenu`, opened by `listeners/NodeInteractListener` |
| Nameplates | `listeners/NodeNameplates` |
| Item tags | `listeners/NodeItemTags`, `items/PrimerItems` |
| Commands | `commands/node/*` |
| Wiring | `bootstrap/NodeFeature` |

Nodes need the business system: if businesses are disabled, so are nodes.

## Setting up a node

1. Create the files the node will use (a random table, or a farm type with its resources) and any loot groups they
   roll on, then `/se node reload`.
2. Look at a block and run `/se node create <id> <random|farm> <table or type>`. The node starts at tier 1. A random
   node starts **public**; a farm node starts **unowned**.
3. `/se node settier <id> <1-5>` sets the highest tier players can gather at. A tier 3 node offers the table's
   tiers 1, 2 and 3 (only those the file lists).
4. `/se node setowner <id> <business>` gives it to a business, or `/se node setowner <id> #public` makes a random
   node public. Only business types listed in `owner-business-types` (`gathering` by default) can own nodes. Farm
   nodes can't be public.

Other commands: `move <id>` (to the block you look at; also places a node whose block was broken),
`remove <id> confirm`, `settable <id> <table or type>` (switch to another table or type of the same kind),
`rename <id> <name...|#reset>` (MiniMessage allowed; `#reset` goes back to the table's or type's name),
`info [id]` (without an id, the node you look at), `list [business]`, `odds <table> <tier>`, `reload`.
Every node command needs `skysongeconomy.nodes`, which also lets staff break node blocks.

Node ids follow the business rule: lower-case letters, digits, `_`, `-`, up to 64 characters.

## Who may gather, and what it costs

| Owner | Who may gather | Cost per harvest |
| --- | --- | --- |
| A business | its owner and workers | the tier's `cost` in Harvest Tokens |
| Public (random nodes only) | everyone | the tier's `public-cost`; a tier without one can't be gathered on public nodes |
| Unowned | nobody | - |

Every harvest also needs the character's level in the table's (or farm type's) `skill` to reach the level that
tier requires (`tier-skill-levels` in `nodes/settings.yml`: by default unskilled, novice, competent, journeyman,
expert for tiers 1 to 5). Level names come from SkysongStats.

### What a harvest costs

`HarvestCost` has three parts: `downtime`, `harvest-tokens` (the character's Gathering Tokens) and `money` (the
player's balance). On a business node only tokens are charged. A public cost charges every part it sets, together:

```yaml
public-cost: {downtime: 1, harvest-tokens: 0, money: 5}
```

`HarvestPayer` checks every part before taking any. Downtime can't be given back, so it is taken last; if it then
fails, the tokens and money are refunded. Nothing is charged when a harvest is refused.

### What a harvest gives

The items are rolled from the tier's output, put in the player's inventory (whatever doesn't fit drops at their
feet) and listed in chat. A harvest that rolled nothing says so. `NodeHarvestEvent` fires for each harvest. There
is no cooldown and no XP; the menu stays open so the player can click again.

## Loot groups: `nodes/groups/<id>.yml`

One group per file, named by its file. A group is a weighted list; a roll on it picks exactly one entry.

```yaml
entries:
  - {item: copper, weight: 10, amount: "1-3"}   # an item; amount fixed or "min-max", default 1
  - {group: rare-pebbles, weight: 1}            # roll once inside nodes/groups/rare-pebbles.yml
  - weight: 2                                   # roll once inside a list written here
    from:
      - {item: tin}
      - {item: coal, weight: 3, amount: 2}
  - {nothing: true, weight: 5}                  # a wasted roll
```

Each entry has exactly one of `item`, `group`, `from` or `nothing: true`, and a whole `weight` of 1 or more (default
1). Weights are relative: an entry's chance is its weight divided by the group's total. Groups can be shared by any
number of tables and can nest; a group that loops back to itself is refused, and so is any group that points to a
broken one, with a warning each.

## Outputs (used by every tier)

A tier's output can have any of these three lists, as long as at least one is set:

```yaml
guaranteed:
  - {item: wheat, amount: "2-4"}               # always given
chances:
  - {item: golden_seed, chance: 2.5, amount: "1-2"}   # each rolled on its own, in percent (0 < chance <= 100)
rolls:
  - times: "6-8"                               # how many rolls, default 1
    from:                                      # the same entries as a group file
      - {group: unskilled-ores, weight: 60}
      - {item: wheat, weight: 10, amount: "1-3"}
  - {times: 2, group: novice-ores}             # or roll on a group file
```

`loot/LootRoller` rolls it and adds up amounts per item. `loot/LootOdds` works out the exact chance of getting each
item at least once and its average amount, which `/se node odds` shows.

## Random tables: `nodes/random/<id>.yml`

```yaml
name: "<gray>Mine"            # what players see; default the file name (MiniMessage)
type: mining                   # what boosters match; default the file name
skill: mining                  # required; the skill whose level each tier checks
tiers:                         # only the tiers listed exist, 1 to 5
  1:
    cost: 1                    # required: Harvest Tokens on business-owned nodes (0 or more)
    public-cost: {downtime: 1, money: 5}     # optional: what public nodes charge
    rolls:
      - times: 8
        from:
          - {group: unskilled-ores, weight: 60}
          - {group: novice-ores, weight: 30}
    chances:
      - {item: geode, chance: 2.5}
  3:
    cost: 3
    rolls:
      - {times: "8-10", group: competent-ores}
```

The nameplate above the node reads like "Mine, Tier 3" (`node.nameplate` in `messages.yml`).

Use `/se node odds <table> <tier>` while balancing: it shows the tier's cost and level, each item's chance per
harvest and average amount, and what one harvest is worth on average by the price database (items without a base
value are listed separately).

## Farm nodes

A farm node belongs to one business and one **farm type**. A tender puts a **primer** in it (a seed, an animal
token...), which plants one **farm resource**; members then harvest that resource as often as they like, and the
primer stays until someone takes it out.

### Farm resources: `nodes/farm/resources/<id>.yml`

```yaml
name: "<white>Sheep"                       # default the file name
primer: {item: sheep_token, amount: 2}     # required: what planting takes; amount default 1
tiers:                                     # only the tiers listed exist
  3:
    cost: 3                                # Harvest Tokens per harvest
    guaranteed: [{item: wool, amount: "2-3"}]
    chances: [{item: mutton, chance: 25, amount: "1-2"}]
```

A resource that only lists tier 3 and up can only be harvested by characters at the tier 3 level or more.

### Farm types: `nodes/farm/types/<id>.yml`

```yaml
name: "<gold>Animal pen"            # default the file name
skill: animalhandling                # required; every harvest checks this skill
resources: [sheep, cow, chicken]     # files in nodes/farm/resources
```

A listed resource that is missing or didn't load is left out with a warning, so one broken resource doesn't take
the type down.

### Which resource an item plants

1. If the item has the **`node_primer`** SkysongItems tag, its text value is the resource id it plants. That
   resource must be one the node's farm type lists.
2. Otherwise, the first resource of the type (in the file's order) whose `primer.item` is the item's material id.

The plugin registers the `node_primer` tag with SkysongItems on its register event (`listeners/NodeItemTags`), so
staff can set it with `/si tag set` without editing SkysongItems' config. If two resources of a type share a primer
item, the type warns that untagged items plant the first.

### Planting, taking out, harvesting

The farm menu shows the node, one button per tier of what is planted, a **planting slot** and a **take out** button.

- **Planting** (free): a tender drags the primer into the planting slot. Once the slot holds the whole primer
  (both of a pair, say), it is planted. The exact item stacks are saved (`items/StoredItems`, Paper's own
  serialised format), so the very same items come back later. Anything left in the slot goes back when the menu
  closes, the player quits, or the server stops.
- **Tenders** are the owning business's owner, anyone in an access group with `tend-farms`, and staff with
  `skysongeconomy.nodes.tend-bypass` (`FarmPlots.BYPASS_PERMISSION`), who tend every farm node. Server-owned
  businesses have no owner character, so staff tend their farms through this permission. The bypass is checked on
  the online player (`NodeFeature` passes `FarmPlots` a `Predicate<UUID>`); it doesn't make an unowned farm
  plantable, and taking out still costs the removal downtime.
- **Taking out** costs `farm.primer-removal-downtime` (1 by default, 0 makes it free) and gives the exact items back.
- **Harvesting** is for the owner and workers, like a random node: pick a tier (up to the node's tier and among the
  resource's tiers), need the level in the farm type's skill, pay the tier's Harvest Tokens.

A primer goes back to whoever planted it, automatically, when its node is removed, changes owner or farm type, or
its business is deleted, and when a restart finds its node gone or changed. If the planter is offline, the primer
waits (`node_primer_returns`) and is given the next time they join (`listeners/PrimerReturnListener`).

The farm nameplate has a second line showing what is planted, or "Empty" (`node.nameplate-farm`).

## Reloading

`/se node reload` reads the loot groups, random tables, farm resources and farm types again, in that order, and
swaps them in (`NodeTables.replace`). Broken files are skipped with a console warning and the reply says how many
warnings there were. Nodes look their table up on every use, so they pick up the new files at once. A node whose
table or type no longer loads shows `node.nameplate-unavailable` and can't be used until it loads again.
`nodes/settings.yml` is only read on start-up.

## `nodes/settings.yml`

```yaml
owner-business-types: [gathering]   # business types that can own nodes
tier-skill-levels:                  # level each tier needs; tiers left out keep these defaults
  1: unskilled
  2: novice
  3: competent
  4: journeyman
  5: expert
menu:                               # Minecraft item names for the node menus
  info: BOOK
  tier: IRON_PICKAXE
  locked-tier: GRAY_DYE
  empty-plot: DIRT
  take-out: SHEARS
farm:
  primer-removal-downtime: 1
nameplate:                          # same options as businesses/block.yml
  enabled: true
  height: 0.4
  view-distance: 16
  shadow: true
  background: false
```

## Boosters (not on master yet)

When this was written, boosters were finished on the branch `feature/node-boosters` but not merged. From that
branch: booster items carry the SkysongItems tags `node_booster_type` (a random table's `type`, or a farm type's
id) and `node_booster_effect` (for example `min+1; max+25%; rolls+1`). Both node menus get a booster slot: a player
drags one booster in, each harvest they click is boosted and uses it once (a charge, or the item itself), and
closing the menu gives back what is left. The hook it uses already exists on master: `loot/YieldAdjuster`, which
`LootRoller.roll(output, adjuster)` applies to amounts and roll counts. Update this section when it merges.
