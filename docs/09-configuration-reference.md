# Configuration reference

Every file in `plugins/SkysongEconomy/`, key by key. The bundled defaults are in `src/main/resources/`, each with
comments. The parser class named for each file is where its rules live, and every value it refuses names the file
and key in the console.

## `config.yml` (Bukkit config)

```yaml
database:
  host: "localhost"
  port: "3306"
  user: "root"
  password: ""
  database_name: "book_descriptions"   # set this to your real database
```

Read by `data/database/DatabaseSettings`. The bundled default database name is a leftover; if the key is missing the
code falls back to `skysong_economy`. The connection URL is `jdbc:mysql://host:port/database_name`.

## `prices.yml` (`prices/PriceSettings`)

| Key | Default | Meaning |
| --- | --- | --- |
| `decimals` | 1 | Decimals prices keep, 0 to 4. Truncated, never rounded. |

## `menus.yml` (`gui/layout/MenuIcons`)

`icons.border`, `previous-page`, `next-page`, `back`, `close`, `confirm`, `cancel`: Minecraft item names of the
buttons every menu shares. Their names and lore are under `menu` in `messages.yml`.

## `messages.yml` (`messaging/MessageCatalog`)

Every player-facing text, by key, in MiniMessage. See [Messages and menus](12-messages-and-menus.md).

## `economy-and-logistics.yml` (`trade/EconomyLogisticsSettingsParser`)

| Key | Meaning |
| --- | --- |
| `skills` | Skill ids whose picks turn E&L on; each pick is one level. At least one. Unknown skills are warned about. |
| `slots.<1-5>.imports` | Deliveries on the way at once at that level (0 or more) |
| `slots.<1-5>.ledgers` | Ledgers selected at once at that level (0 or more) |
| `delivery-times.<id>.name` | MiniMessage name in menus |
| `delivery-times.<id>.hours` | Real hours from order to arrival |
| `ledger-removal-fine` | Money the owner pays to remove a selected ledger |

## `ledgers/<id>.yml` (`trade/LedgerParser`)

See [Economy and Logistics](06-economy-and-logistics.md#ledgers) for a full example.

| Key | Meaning |
| --- | --- |
| `name`, `icon`, `description` | Ledger button: MiniMessage name, Minecraft material (default `PAPER`), MiniMessage lines |
| `imports.<id>.name`, `icon`, `lore` | The import's button |
| `imports.<id>.contents` | List of `{item, amount}` and `{rolls, from: [{item, weight, amount}]}` |
| `imports.<id>.prices.<delivery time>` | Price expression per delivery time offered |
| `exports.<id>.name`, `icon`, `lore` | The export's button |
| `exports.<id>.item` or `good-type` | What it buys (exactly one) |
| `exports.<id>.batch` | Items per sale, 1 or more (default 1) |
| `exports.<id>.price` | Price expression for one batch |

Amounts are a number or `"min-max"`.

## `businesses/types.yml` (`business/type/BusinessTypesParser`)

| Key | Meaning |
| --- | --- |
| `types.<id>.name` | MiniMessage name |
| `types.<id>.skills` | SkysongStats skill ids this type offers; a skill can be in only one type |
| `types.<id>.trains-proficiency` | Whether Train/Learn here also trains proficiency |

## `businesses/tiers.yml` (`business/tier/BusinessTiersParser`)

All five tiers must be listed.

| Key | Default | Meaning |
| --- | --- | --- |
| `tiers.<1-5>.workers` | | Workers it can hire (the owner doesn't count) |
| `tiers.<1-5>.vault-cut` | 0 | Money each finished worker shift adds to the vault |
| `tiers.<1-5>.max-proficiency` | `proficient` | Highest SkysongStats proficiency tier training here can reach (Service types) |

## `businesses/access.yml` (`business/access/AccessDefaults`)

| Key | Meaning |
| --- | --- |
| `max-custom-groups` | Groups an owner may add besides `workers` and `everyone` |
| `defaults.workers.permissions` | Default permissions of the `workers` group |
| `defaults.workers.training-price` | Default training price for workers |
| `defaults.everyone.permissions` | Default permissions of the `everyone` group |
| `defaults.everyone.training-price` | Default training price for everyone, also the starting price of new custom groups |

Permissions: `train`, `order-imports`, `sell-exports`, `tend-farms`, `manage-npcs`, `edit-npc-access`. Defaults apply live to businesses whose owner
never changed their groups.

## `businesses/shifts.yml` (`business/shift/ShiftSettingsParser`)

| Key | Meaning |
| --- | --- |
| `duration-minutes` | Online, non-AFK minutes a shift takes |
| `downtime-cost` | Downtime spent to start one |
| `default-message` | Shift message of businesses without their own; `{character}`, `{player}`, `{business}` |
| `pay.<type>.<tier>.money` | Money paid for one shift |
| `pay.<type>.<tier>.tokens` | Gathering Tokens paid for one shift |

Types and tiers left out pay nothing.

## `businesses/training.yml` (`business/training/TrainingSettingsParser`)

| Key | Meaning |
| --- | --- |
| `duration-minutes` | Online, non-AFK minutes a session takes |
| `downtime-cost` | Downtime spent to start one |
| `rewards.<picks>.xp` | Skill XP for a skill picked that many times |
| `rewards.<picks>.proficiency` | Proficiency points for it (Service types only, capped by the tier) |

## `businesses/npcs.yml` (`business/npc/NpcSettingsParser`)

Only read when SkysongNPCs is installed. See [Business NPCs](05-businesses.md#business-npcs).

| Key | Meaning |
| --- | --- |
| `spawn-at` | Where a new NPC appears: `player` (a free spot in front of whoever creates it, the default) or `business` (a free spot next to the business block) |
| `spawn-radius` | How many blocks around that place SkysongNPCs looks for a free spot. Default 3 |
| `default-access.workers`, `default-access.everyone` | What the built-in groups may do with a business's NPCs until its owner changes it: a list of SkysongNPCs actions or presets. Written into the business's access policy when it is made (with its first NPC), so a later edit only reaches businesses that have no policy yet. Left out: nothing. Read by `business/npc/access/NpcAccessDefaults` |
| `skills.<skill>.slots` | How many NPCs a business can have for this skill: an [amount](#amounts-by-level-and-tier), so a number, a formula with `{level}` and `{tier}`, or a table |
| `skills.<skill>.template` | The SkysongNPCs template new NPCs are made from. It is checked when an NPC is created, not on load |

| `skills.<skill>.progression` | Optional: what each NPC of the skill receives, a benefit block (below) that may also have `at` |

A **benefit block** (`business/npc/benefit/BenefitParser`). Every key is optional, and every stat, skill,
proficiency, tier and ability id is checked on load:

| Key | Meaning |
| --- | --- |
| `stat-points.amount` | Points the owner assigns, an amount |
| `stat-points.eligible` | The stats they may go on; left out, the NPC's usual stats |
| `stats.<stat>` | A baked bonus to that stat, an amount |
| `proficiency-picks` | One block or a list of blocks with `skill`, `amount`, `max-tier`, `promote-cost` |
| `proficiency-picks[].max-tier` | A tier (the tier a pick gives; `proficient` when left out), or a table `{by: level, 1: proficient, 4: adept}`: picks give the table's lowest tier and may be promoted up to the tier reached |
| `proficiency-picks[].promote-cost` | Picks one promotion step costs; left out, nothing is promoted |
| `proficiencies.<proficiency>` | The tier that proficiency is raised to at least |
| `abilities` | Fixed ability ids from SkysongNPCs' `abilities.yml` |
| `ability-picks.amount`, `ability-picks.from` | How many abilities the owner chooses, and the list to choose from |
| `at` | Progressions only: `{by: level\|tier, <step>: <benefit block>}`. Every step reached adds to the ones below; a proficiency keeps its highest tier |

`<skill>` is a SkysongStats skill id; an unknown one is warned about and ignored. The bundled file has
`skills: {}`, so no business has NPC slots until you add a skill.

## `businesses/modules.yml` (`business/module/ModuleSettingsParser`)

See [Modules](05-businesses.md#modules).

| Key | Meaning |
| --- | --- |
| `skills.<skill>.picks` | How many module picks a business has for this skill in total: an [amount](#amounts-by-level-and-tier). Required |
| `skills.<skill>.modules` | The modules those picks can take: file names from `modules/` without `.yml`. A name with no file is warned about and left out |

`<skill>` is a SkysongStats skill id; an unknown one is warned about and ignored. The bundled file has
`skills: {}`, so no business has module picks until you add a skill.

## `modules/<id>.yml` (`business/module/ModuleParser`)

One file per module; the file name is its id. A file that can't be read is skipped with a warning. The bundled
`sworn_oath.yml` is an example and is in no skill's pool.

| Key | Meaning |
| --- | --- |
| `name` | Required. MiniMessage |
| `icon` | The Minecraft item shown in menus. Default `PAPER` |
| `description` | One text or a list of lines, MiniMessage |
| `max-picks` | How many times one business may pick it, across all its skills. Default 1 |
| `requires.tier` | The business tier needed to pick it, 1 to 5 |
| `requires.level` | How many picks of the skill it is being picked for the business needs |
| `requires.modules` | Modules the business must already have, by id |
| `effects` | A list of effects, each with a `type`; all apply together. None when left out |

Effects (see [Module effects](05-businesses.md#module-effects)). In an [amount](#amounts-by-level-and-tier) here,
`{level}` is how many picks the business has of the skill the module was picked for.

| `type` | Keys |
| --- | --- |
| `worker-slots` | `amount`: required, an amount. That many more workers can be hired |
| `npc-slots` | `amount`: required, an amount. `template`: required, the SkysongNPCs template of the module's own NPCs. `progression`: optional, what each of those NPCs receives, written like a skill's `progression` in `npcs.yml`. At most one `npc-slots` effect per module |
| `npc-benefit` | `benefit`: required, a benefit block (`stat-points`, `stats`, `proficiency-picks`, `proficiencies`, `abilities`, `ability-picks`, as in `npcs.yml`). `target`: `all` (default) or how many NPCs the owner chooses, 1 or more. `from`: optional, a skill id (only that skill's NPCs count) or `module` (only this module's own NPCs, which needs an `npc-slots` effect in the same file) |

An unknown `type`, a second `npc-slots`, or a wrong `target` or `from` skips the whole module file with a warning.
The NPC effects need SkysongNPCs: without it they are read but do nothing, and the console says so.

```yaml
effects:
  - type: npc-slots
    amount: 1
    template: war_hound
    progression:
      stats: {strength: 2}
  - type: npc-benefit
    target: all
    from: martial
    benefit:
      proficiency-picks:
        - {skill: martial, amount: 1}
  - type: npc-benefit
    target: 2
    benefit:
      stat-points: {amount: 2}
  - type: worker-slots
    amount: 1
```

## `businesses/block.yml` (`business/display/NameplateSettingsParser`)

| Key | Meaning |
| --- | --- |
| `nameplate.enabled` | `false` hides every business nameplate |
| `nameplate.height` | Blocks above the top of the block |
| `nameplate.view-distance` | Blocks away it can be seen |
| `nameplate.shadow` | Text shadow |
| `nameplate.background` | The default dark box behind the text |

The text is `business.nameplate` in `messages.yml`.

## `nodes/settings.yml` (`nodes/NodeSettingsParser`)

| Key | Meaning |
| --- | --- |
| `owner-business-types` | Business types that can own nodes |
| `tier-skill-levels.<1-5>` | SkysongStats level id each tier needs |
| `menu.info`, `tier`, `locked-tier`, `empty-plot`, `take-out` | Node menu items |
| `farm.primer-removal-downtime` | Downtime taking a primer out costs; 0 is free |
| `nameplate.*` | As in `businesses/block.yml`; text is `node.nameplate` (`-farm`, `-unavailable`) |

## Node folders

| Folder | Parser | Format |
| --- | --- | --- |
| `nodes/groups/<id>.yml` | `loot/LootParser.parseGroups` | `entries:` list of `{item\|group\|from\|nothing, weight, amount}` |
| `nodes/random/<id>.yml` | `nodes/random/RandomTableParser` | `name`, `type`, `skill`, `tiers.<n>.{cost, public-cost, guaranteed, chances, rolls}` |
| `nodes/farm/resources/<id>.yml` | `nodes/farm/FarmResourceParser` | `name`, `primer.{item, amount}`, `tiers.<n>.{cost, guaranteed, chances, rolls}` |
| `nodes/farm/types/<id>.yml` | `nodes/farm/FarmTypeParser` | `name`, `skill`, `resources` |

Full examples are in [Nodes](07-nodes.md). File names must be valid ids (lower-case letters, digits, `_`, `-`).

## `architite_coins/*.yml` and `items/*.yml`

Skysong_Items templates, in that plugin's own format. `architite_coins/` holds the three coins; `items/` holds one
template per material id that ledgers, loot and primers may name.

## Amounts by level and tier

Some values follow how far a business has come (`business/scale`). They can use two numbers:

- `{level}`: the skill pick tier, which is how many of the business's skill picks are the skill in question (the
  same count Economy and Logistics calls its level);
- `{tier}`: the business's overall tier, 1 to 5.

An amount (`business/scale/AmountParser`) is written in one of three ways:

```yaml
amount: 4                               # a fixed number
amount: "3 + {level}"                   # a formula: numbers, + - * /, parentheses, {level} and {tier}
amount: {by: tier, 1: 1, 3: 2, 5: 3}    # a table: a row applies from its number upward
```

- A formula must be in quotes when it starts with `{`. Its result loses its decimals (`{level} / 2` is 1 at level
  3), and a result below 0 counts as 0. A formula that would divide by zero at some level and tier is refused when
  the file loads.
- A table follows `level` unless it says `by: tier`. Its rows are numbered 1 to 5 and give 0 below the first row:
  the example gives 1 at tiers 1 and 2, 2 at tiers 3 and 4, and 3 at tier 5.

The same table form holds other kinds of values (`business/scale/StepsParser`), for example
`{by: level, 1: proficient, 4: adept}`.

A functionality that is switched on per skill keeps a `skills:` map in its own file
(`business/scale/SkillSections`): a skill listed there has the functionality with the settings written under it, and
a skill left out does not. Unknown skills are skipped with a console warning. Each functionality has its own file, so
one skill can have any combination of them.

No config file uses these yet: they are the base for the business NPC and module files.
