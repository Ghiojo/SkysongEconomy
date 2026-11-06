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

Permissions: `train`, `order-imports`, `sell-exports`, `tend-farms`. Defaults apply live to businesses whose owner
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
