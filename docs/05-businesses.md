# Businesses

A business is a protected block in the world, owned by one **character** (or by the server), worked by other
characters, with a menu that opens when the block is right-clicked. A business has a **type** (gathering,
crafting, service), a **tier** (1 to 5), **skill picks** (one per tier), **workers**, **access groups**, and a
**vault**. Workers work **shifts** for pay; anyone allowed can **train** there.

Everything a business does is per character, not per player account: if a player switches profile, they are a
different person to the business. Money and downtime, though, belong to the player account.

Code map:

| Part | Classes |
| --- | --- |
| Core | `business/Business`, `BusinessService`, `BusinessSnapshot`, `Hire`, `SkillPick`, `TierChange`, `BlockPosition` |
| Types and tiers | `business/type/*` (`businesses/types.yml`), `business/tier/*` (`businesses/tiers.yml`) |
| Access groups | `business/access/*` (`businesses/access.yml`) |
| Vault | `business/vault/BusinessVaults` |
| Shifts | `business/shift/*` (`businesses/shifts.yml`), `events/ShiftStartEvent`, `ShiftEndEvent` |
| Training | `business/training/*` (`businesses/training.yml`) |
| One activity at a time | `business/activity/ActivityLock`, `listeners/ActivityTimer` |
| Tokens | `tokens/*`, `integration/placeholderapi/TokenExpansion` |
| Block and nameplate | `business/BusinessBlockGuard`, `listeners/ProtectedBlockListener`, `listeners/BusinessNameplates`, `listeners/FloatingNames` (`businesses/block.yml`) |
| Menus | `gui/business/*`, opened by `listeners/BusinessInteractListener` |
| Commands | `commands/business/*`, `commands/tokens/*` |
| Wiring | `bootstrap/BusinessFeature` |

## Creating and managing a business

Staff look at a block and run `/se business create <id> <type> <owner|#server> [profile]`. The block becomes a
tier 1 business named after its id, with no workers and no picks.

- The **id** is permanent and is what every command uses: lower-case letters, digits, `_` and `-`, up to 64
  characters, unique. Players see the **display name** instead, which the owner or staff change with
  `/se business rename <id> <name...>` or by clicking the top item of the business menu (it asks in chat; type
  `cancel` to keep the name, and the question expires after a minute). Up to 48 characters, shown as typed
  (MiniMessage tags in it are not parsed).
- The **owner** is a character: `<player> [profile]`, where leaving out the profile means the character the player
  is playing now. Writing `#server` instead makes the business server-owned (see below).
- A block can hold only one business, and never a business and a node.

| Action | Command | Who |
| --- | --- | --- |
| Create | `/se business create <id> <type> <owner\|#server> [profile]` | Staff |
| Delete | `/se business delete <id> confirm` (without `confirm` it explains what will happen) | Staff |
| Change tier | `/se business settier <id> <tier>` | Staff |
| Change owner | `/se business setowner <id> <player\|#server> [profile]` | Staff |
| Clear a skill pick | `/se business clearpick <id> <number>` | Staff |
| List all | `/se business list` | Staff |
| Move to the block you look at | `/se business move <id>` | Owner or staff |
| Rename | `/se business rename <id> <name...>` | Owner or staff |
| Shift message | `/se business shiftmessage <id> <message...\|reset>` | Owner or staff |
| Hire / fire | `/se business hire\|fire <id> <player> [profile]` | Owner or staff |
| Access groups | `/se business group <id> ...` | Owner or staff |
| Vault | `/se business vault <id> [collect]` | Owner or staff |
| Info | `/se business info <id>` | Anyone with `skysongeconomy.business` |

"Staff" means `skysongeconomy.business.admin`. "Owner" means the sender's **active** character owns the business.
The checks are in `commands/business/BusinessCommandSupport.requireManager`.

## Server-owned businesses (`#server`)

A business can belong to the server instead of a character, so staff can run base NPC businesses without spending
anyone's character on them (`Business.isServerOwned()`, owner is empty). Then:

- nobody is the owner; staff do what an owner would (pick skills, select and remove ledgers, collect the vault)
  through the menu (`BusinessMenuContext.actsAsOwner()`);
- workers can still be hired and work shifts, and each worker shift's vault cut still collects in the vault;
- removing a ledger costs no fine;
- collecting its vault pays the staff member who collects it. The console can't collect it
  (`business.vault.server-owned`), since there is no one to pay;
- when it is deleted, what is left in its vault leaves the economy.

`/se business setowner <id> #server` gives an existing business to the server (its workers stay); `setowner` with a
player gives it back to a character. A character who becomes owner stops being a worker there.

## The block

- Right-clicking the block opens the business menu, and the block's own use (chest, crafting table...) is
  cancelled.
- Nobody can break it, and neither can explosions, fire, pistons or mobs. Staff with
  `skysongeconomy.business.break` can break it: the business keeps all its data but has no block until someone
  runs `/se business move <id>` while looking at a new block.
- A **nameplate** (the display name, from `business.nameplate` in `messages.yml`) floats above the block, set up
  in `businesses/block.yml`. It is a text display that is never saved with the world and is shown again whenever
  the chunk loads.
- If two saved businesses claim the same block on start-up, the second one loses its block so staff can move it.

## Types: `businesses/types.yml`

Every business has exactly one type. A type decides which skills its owner can pick and which pay table its shifts
use.

```yaml
types:
  gathering:
    name: "<green>Gathering"
    skills: [animalhandling, farming, forestry, mining, mistgathering, woodprocessing]
    trains-proficiency: false
  service:
    name: "<gold>Service"
    skills: [economical, entertainment, martial, medical, tande]
    trains-proficiency: true    # Train/Learn here also trains proficiency
```

Skill ids must exist in SkysongStats, and a skill belongs to only one type. To add a type, add an entry and a pay
table in `shifts.yml`. The type of an existing business can't be changed (delete and recreate it).

## Tiers: `businesses/tiers.yml`

```yaml
tiers:
  1: {workers: 2, vault-cut: 1, max-proficiency: proficient}
  ...
  5: {workers: 6, vault-cut: 3, max-proficiency: master}
```

All five tiers must be listed. `workers` is how many workers it can hire (the owner doesn't count). `vault-cut` is
money each finished worker shift adds to the vault. `max-proficiency` caps what training at a Service business of
that tier can reach.

When staff lower a tier, the newest hires and the newest skill picks the new tier has no room for are removed
(`BusinessService.setTier` returns a `TierChange` listing them, and the command reports them). E&L ledger
selections that no longer fit are dropped too; deliveries already ordered stay.

## Workers

`/se business hire <id> <player> [profile]` hires a character; `fire` removes one. The owner can't be hired, a
character can't be hired twice, and the tier caps the count (`business.hire.full`). A character may work at several
businesses. Workers are kept oldest hire first.

## Skill picks

A business gets one skill pick per tier (so a tier 3 business has 3). The owner picks from the business type's
skills in the **Skill picks** menu: click a free slot, choose a skill, confirm. The same skill can be picked more
than once, and each pick counts. Picks are permanent: only staff can free one, with
`/se business clearpick <id> <number>` (numbered from 1, oldest first, as the menu shows).

Picks drive two things: what Train/Learn trains, and Economy and Logistics (each pick of an E&L skill, `economical`
by default, is one E&L level). Shift pay ignores picks.

## Access groups

Access groups decide who may do what at a business and what they pay to train (`business/access/AccessService`).
Every business has two built-in groups whose members are implied, plus up to `max-custom-groups` of the owner's
own:

| Group | Members |
| --- | --- |
| `workers` | the business's workers |
| `everyone` | every character |
| your own (e.g. `customers`) | characters you add |

Each group has a set of permissions and a training price:

| Permission | Lets members |
| --- | --- |
| `train` | use Train/Learn, at the group's price |
| `order-imports` | order E&L imports, and collect any delivery |
| `sell-exports` | sell E&L exports |
| `tend-farms` | plant and take out primers in the business's farm nodes |

A character may do something when **any** of their groups allows it, and pays the **lowest** training price among
their groups that allow training. The **owner** may always do everything and trains for free.

A business whose groups were never changed uses the defaults in `businesses/access.yml` live, so editing that file
changes them. The first change saves all of that business's groups to the database, and from then on the file no
longer affects it. A new custom group allows nothing and starts with the `everyone` default price.

```
/se business group <id> list
/se business group <id> create <group>          # lower-case letters, digits, _ and -, up to 32
/se business group <id> delete <group>          # not the built-in ones
/se business group <id> allow <group> <permission>
/se business group <id> deny <group> <permission>
/se business group <id> price <group> <amount>  # 0 or more
/se business group <id> add <group> <player> [profile]      # custom groups only
/se business group <id> remove <group> <player> [profile]
```

## The vault

The vault (`business/vault/BusinessVaults`) holds money the business earned for its owner:

- each finished **worker** shift adds the tier's `vault-cut` (the owner's own shifts add nothing);
- every paid **training fee** goes in.

It stays there until collected into the owner's own balance: the owner clicks the **Vault** button in the menu (only
the owner sees it, or staff on a server-owned business), or uses `/se business vault <id> collect`. Staff collecting
a character-owned vault still pays the owner. If paying fails, the vault keeps the money. When a business is deleted,
its vault is paid to the owner first.

## Work a Shift and Retrieve Pay

The owner and workers start a shift with the **Work a Shift** button (`business/shift/ShiftService`):

1. The player must be free (no other shift or training on that account, see the activity lock below) and have the
   `downtime-cost` from `shifts.yml` available.
2. `ShiftStartEvent` fires with the business's shift message; another plugin may cancel it, and then nothing is
   spent.
3. The downtime is spent, and the **pay is fixed now** from the pay table for the business's type and tier:
   `money` (to the player's balance) and `tokens` (Gathering Tokens for the character). The vault cut is fixed too
   (0 for the owner).
4. The shift counts **online, non-AFK** time only (AFK only when CMI is installed). The player doesn't need to stay
   near the business. `listeners/ActivityTimer` runs every 10 seconds on the main thread and adds 10 seconds to
   every online, non-AFK player's shift, so a shift can end up to 10 seconds late.
5. When it has `duration-minutes` of time, it ends: the vault cut goes to the vault, `ShiftEndEvent` fires and the
   player is told.
6. The pay waits until the character clicks **Retrieve Pay**, which pays every finished shift at that business at
   once. Everyone sees that button, so a fired worker still collects what they are owed.

The shift message is set per business with `/se business shiftmessage <id> <message...>` (up to 200 characters, or
`reset` for the default in `shifts.yml`). It may use `{character}`, `{player}` and `{business}`. It isn't shown in
game: it is `ShiftStartEvent.getMessage()`, for a Discord bridge or similar.

Shifts are saved from their start until their pay is retrieved (`business_shifts`), so a restart continues them.

## Train/Learn

Anyone a group allows (and the owner) trains with the **Train/Learn** button (`business/training/TrainingService`):

1. The business must have at least one skill pick, the character must be allowed to train, the player must be free
   and have the `downtime-cost` from `training.yml`.
2. The training price (the lowest of their groups', 0 for the owner) is taken from the player's balance and put in
   the vault. If spending the downtime then fails, the money is given back.
3. The session counts online, non-AFK time like a shift, for `duration-minutes`.
4. When it ends, **every skill the business has picked** gives the reward row for how many times it was picked
   (`rewards` in `training.yml`): a business that picked mining twice and smithing once gives the row 2 for mining
   and row 1 for smithing. Each row gives `xp` (skill XP) and, only at a business whose type has
   `trains-proficiency: true`, `proficiency` points, capped at the tier's `max-proficiency`. SkysongStats decides
   which proficiency focus gets them; the player is told the outcome per skill (trained, no focus, focus in another
   skill, already at the cap, unavailable).

Rewards follow the picks **at the end** of the session. Running sessions are saved (`business_training`); finished
ones are deleted.

## One activity at a time

`business/activity/ActivityLock` makes a player **account** do one shift or one training session at a time,
whichever character they play. Shifts and training share it. A busy player gets `business.activity.busy-shift` or
`busy-training`.

## Gathering Tokens

A whole-number balance per **character** (`tokens/GatheringTokens`, table `gathering_tokens`). Characters earn them
from shift pay (the Gathering pay table gives `tokens` by default) and spend them at gathering nodes, where they are
called Harvest Tokens.

| Command | Permission |
| --- | --- |
| `/se tokens balance` | `skysongeconomy.tokens` (your active character) |
| `/se tokens balance <player> [profile]` | `skysongeconomy.tokens.admin` |
| `/se tokens add\|remove\|set <player> <amount> [profile]` | `skysongeconomy.tokens.admin` |

With PlaceholderAPI installed: `%skysongeconomy_tokens%` (the viewer's active character) and
`%skysongeconomy_tokens_<player>%` (that player's active character). A player without a character shows 0.

## The business menu

Right-clicking the block opens `BusinessMainMenu`. The top item shows the business (owner and staff can click it to
rename). Each feature adds its own button (`MainMenuEntry`) in a fixed slot (`MainMenuSlots`):

| Row | Button | Shown to |
| --- | --- | --- |
| 2 | Work a Shift | everyone; greyed out with the reason for anyone who can't start one |
| 2 | Retrieve Pay | everyone |
| 2 | Train/Learn | everyone; greyed out with the reason for anyone who can't train |
| 2 | Vault | the owner, or staff on a server-owned business |
| 2 | Skill picks | everyone; only the owner (or staff on a server-owned business) can pick |
| 3 | Imports | businesses with E&L |
| 3 | Exports | businesses with E&L |
| 3 | Slots | businesses with E&L |
| 3 | Ledgers | businesses with E&L; everyone can browse, only the owner selects |

The E&L buttons are described in [Economy and Logistics](06-economy-and-logistics.md).

## Deleting a business

`/se business delete <id> confirm` removes it for good. Each listener cleans up its part, in this order:

1. price modifiers on `business:<id>` are removed;
2. its access groups go;
3. **every shift is paid**, finished or not (its pay was promised), and running shifts stop;
4. running training sessions finish at once and give their rewards;
5. the vault is paid to the owner (or dropped for a server-owned business);
6. ledger selections are removed;
7. every delivery is **refunded** to whoever paid for it;
8. the nameplate is removed;
9. its gathering nodes become **unowned** (nobody can gather until staff give them a new owner), and primers planted
   in its farm nodes go back to their planters.

A payment that fails during deletion is logged as a warning asking staff to pay by hand. The database removes the
child rows itself (foreign keys with `ON DELETE CASCADE`).
