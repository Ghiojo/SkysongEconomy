# Commands and permissions

Everything is under `/se` (aliases: `/skysongeconomy`, `/seconomy`, `/seco`, `/skye`). Every command tab-completes
its arguments.

Notation: `<required>`, `[optional]`, `a|b` means one of them. `<player> [profile]` names a character: the player
(online, or offline but known to the server) and one of their SkysongStats profiles; without the profile, the
character they are playing now.

## How the tree works

`commands/SkysongEconomyCommand` is the Bukkit executor. It hands every call to a root `CommandGroup` named `se`.
A `CommandGroup` checks its children's permissions, prints usage when the word is missing or unknown, and completes
child names (only the ones the sender may use). A group's own permission applies to everything under it, and each
child may add a stricter one.

| Group | Class | Group permission |
| --- | --- | --- |
| `/se ledger` | `commands/ledger/LedgerCommand` | `skysongeconomy.ledger` |
| `/se good` | `commands/good/GoodCommand` | `skysongeconomy.good` |
| `/se business` | `commands/business/BusinessCommand` | `skysongeconomy.business` |
| `/se tokens` | `commands/tokens/TokensCommand` | `skysongeconomy.tokens` |
| `/se modifier` | `commands/modifier/ModifierCommand` | `skysongeconomy.modifiers` |
| `/se node` | `commands/node/NodeCommand` | `skysongeconomy.nodes` |

`business`, `tokens` and `modifier` only exist while the business system is on; `node` only while nodes are on.

## `/se ledger`: the price database

| Command | Extra permission | Does |
| --- | --- | --- |
| `view [materialId]` | | An item's base value and its expression; the held item without an id |
| `list` | | Every base value |
| `add [materialId] <expression>` | `skysongeconomy.ledger.add` | Sets a base value |
| `remove [materialId]` | `skysongeconomy.ledger.remove` | Removes one, unless another item uses it |

## `/se good`: good types

| Command | Extra permission | Does |
| --- | --- | --- |
| `add [materialId] <goodType>` | `skysongeconomy.good.manage` | Tags an item (the held item without an id) |
| `remove [materialId] <goodType>` | `skysongeconomy.good.manage` | Removes the tag |

## `/se business`

"Owner or staff" commands check that the sender's active character owns the business, or that they have
`skysongeconomy.business.admin`.

| Command | Who | Does |
| --- | --- | --- |
| `create <id> <type> <owner\|#server> [profile]` | staff | Turns the looked-at block into a tier 1 business |
| `delete <id> [confirm]` | staff | Without `confirm`, explains; with it, deletes (see [Businesses](05-businesses.md#deleting-a-business)) |
| `settier <id> <tier>` | staff | 1 to 5; lowering removes the newest hires, picks and NPCs that no longer fit |
| `setowner <id> <player\|#server> [profile]` | staff | Gives the business to a character or to the server |
| `clearpick <id> <number>` | staff | Frees one skill pick, numbered from 1 (oldest); names the NPCs removed with their slots |
| `npcsync <id>` | staff | Brings every NPC of the business in line with what it is owed now and lists what changed. It happens by itself when a business changes; use it after changing an NPC by hand in SkysongNPCs. Only with SkysongNPCs |
| `npcaccess <id> [group] [allow\|deny <action>]` | owner, staff, or a group with `edit-npc-access` | What each access group may do with the business's NPCs. Alone it lists every group, with a group it shows that group, and `allow`/`deny` change it. `<action>` is a SkysongNPCs action (`info`, `inventory-view`, `teleport`, ...) or one of its presets (`customer`, ...). Only with SkysongNPCs |
| `dismissnpc <id> <npc>` | staff | Removes one of the business's NPCs for good and frees its slot; `<npc>` is its number in `npcs`, or its id. Only with SkysongNPCs |
| `list` | staff | Every business |
| `move <id>` | owner or staff | Moves it to the looked-at block (also places a broken one) |
| `rename <id> <name...>` | owner or staff | Display name, up to 48 characters |
| `shiftmessage <id> <message...\|reset>` | owner or staff | Text for `ShiftStartEvent`; `{character}`, `{player}`, `{business}` |
| `hire <id> <player> [profile]` | owner or staff | Hires a character |
| `fire <id> <player> [profile]` | owner or staff | Removes a worker |
| `group <id> list` | owner or staff | Access groups, members, permissions, prices |
| `group <id> create\|delete <group>` | owner or staff | Custom groups |
| `group <id> allow\|deny <group> <permission>` | owner or staff | `train`, `order-imports`, `sell-exports`, `tend-farms`, `manage-npcs`, `edit-npc-access` |
| `group <id> price <group> <amount>` | owner or staff | Training price |
| `group <id> add\|remove <group> <player> [profile]` | owner or staff | Members of custom groups |
| `vault <id> [collect]` | owner or staff | Shows the vault; `collect` pays it out |
| `npcs <id>` | owner or staff | NPC slots by source, and each NPC with its number and what it is waiting for. Only with SkysongNPCs |
| `info <id>` | anyone with the group permission | Type, tier, owner, workers, picks, block |

## `/se tokens`: Gathering Tokens

| Command | Permission | Does |
| --- | --- | --- |
| `balance` | `skysongeconomy.tokens` | Your active character's tokens |
| `balance <player> [profile]` | `skysongeconomy.tokens.admin` | Someone else's |
| `add <player> <amount> [profile]` | `skysongeconomy.tokens.admin` | Adds (amount above 0) |
| `remove <player> <amount> [profile]` | `skysongeconomy.tokens.admin` | Removes; refused below 0 |
| `set <player> <amount> [profile]` | `skysongeconomy.tokens.admin` | Sets (0 or more) |

## `/se modifier`: price modifiers

| Command | Permission |
| --- | --- |
| `list [target]` | `skysongeconomy.modifiers` |
| `add <type> <target> <percent> [for:<time>] [on:imports\|exports\|both] [name...]` | `skysongeconomy.modifiers.edit` |
| `set <type> <target> <percent> [for:<time>] [on:...] [name...]` | `skysongeconomy.modifiers.edit` |
| `remove <id>` | `skysongeconomy.modifiers.edit` |

Targets: `global`, `business:<id>`, `ledger:<id>`, `good-type:<type>`, `item:<material_id>`. Details in
[Prices](04-prices.md#price-modifiers-se-modifier).

## `/se node`: gathering nodes (all staff)

| Command | Does |
| --- | --- |
| `create <id> <random\|farm> <table or type>` | Turns the looked-at block into a tier 1 node |
| `remove <id> [confirm]` | Deletes it (primers go back) |
| `move <id>` | Moves it to the looked-at block |
| `settier <id> <tier>` | Highest tier players can gather at |
| `setowner <id> <business\|#public>` | Gives it to a business, or makes a random node public |
| `settable <id> <table or type>` | Switches its random table or farm type |
| `rename <id> <name...\|#reset>` | Its own name (MiniMessage), or back to the table's |
| `info [id]` | Details; without an id, the node you look at |
| `list [business]` | Every node, or a business's |
| `odds <table> <tier>` | Cost, level, each item's odds, average value |
| `reload` | Reads the node files again |

## Permissions

All are declared in `src/main/resources/plugin.yml`.

| Permission | Default | Allows |
| --- | --- | --- |
| `skysongeconomy.good` | everyone | The `/se good` group |
| `skysongeconomy.good.manage` | op | `/se good add` and `remove` |
| `skysongeconomy.ledger` | op | `/se ledger view` and `list` |
| `skysongeconomy.ledger.add` | op | `/se ledger add` |
| `skysongeconomy.ledger.remove` | op | `/se ledger remove` |
| `skysongeconomy.business` | everyone | `/se business`; owners can manage only their own businesses |
| `skysongeconomy.business.admin` | op | Every business command on every business; staff options in business menus |
| `skysongeconomy.business.break` | op | Breaking business blocks (the business keeps its data until moved) |
| `skysongeconomy.tokens` | everyone | `/se tokens balance` for yourself |
| `skysongeconomy.tokens.admin` | op | Seeing and changing anyone's tokens |
| `skysongeconomy.modifiers` | op | `/se modifier list` |
| `skysongeconomy.modifiers.edit` | op | `/se modifier add`, `set`, `remove` |
| `skysongeconomy.nodes` | op | Every `/se node` command, and breaking node blocks |
| `skysongeconomy.nodes.tend-bypass` | op | Plant and take out primers at every farm node, whatever its owner and groups allow |
| `skysongeconomy.*` | op | All of the above |

Players use most features through menus, not commands: right-click a business block or a node. Those menus check
the rules themselves (owner, member, access groups), not permissions, except that `skysongeconomy.business.admin`
marks a viewer as staff in business menus.

## Adding a command

1. Write a class implementing `SubCommand` (or extending the feature's base, such as `BusinessSubCommand`, which
   checks the argument count and turns a `BusinessException` into its message).
2. Register it in the feature's group (`BusinessCommand`, `NodeCommand`, ...), or return a new group from the
   feature's bootstrap so `SkysongEconomy.registerCommands` adds it under `/se`.
3. Put every text it sends in `messages.yml`.
4. If it needs a new permission, declare it in `plugin.yml` and add it to `skysongeconomy.*`.
