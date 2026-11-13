# Setup and dependencies

## Plugins the server needs

| Plugin | Required? | What SkysongEconomy uses it for | Code that touches it |
| --- | --- | --- | --- |
| Gringotts 3.0.1 | Yes (`depend`) | Money: balances, paying, charging, formatting amounts. The Architite coins are registered as its denominations. | `integration/gringotts/GringottsWallet`, `currency/ArchititeCurrency` |
| Skysong_Items (1.8.0 on the server) | Yes (`depend`) | Building items from template files, reading the `material_id` tag and other item tags, registering the `node_primer` tag. Only the public API in `org.skysongdev.skysongitems.api` is used. | `items/*`, `listeners/NodeItemTags` |
| SkysongStats | Yes (`depend`) | Characters (profiles), skills and skill levels, XP, proficiency training, downtime. | `integration/skysongstats/SkysongStatsAdapter` (the only class that calls its API) |
| SkysongNPCs (API 0.23.0 or newer) | Optional (`softdepend`) | Business NPCs, and letting NPC access name business groups as `economy:<business>/<group>`. Without it the console says "SkysongNPCs is not installed: business NPCs are off" and everything else works. An older SkysongNPCs is refused with a console error that names the API it has. | `integration/skysongnpcs/SkysongNpcsAdapter` (the only class that calls its API), `integration/skysongnpcs/BusinessGroupProvider` |
| PlaceholderAPI | Optional (`softdepend`) | `%skysongeconomy_tokens%` placeholders. | `integration/placeholderapi/TokenExpansion` |
| CMI | Optional (`softdepend`) | Whether a player is AFK, so shift and training time stops. Called by reflection because CMI has no Maven artifact. | `integration/cmi/CmiAfkCheck` |

The server runs Paper 1.21.11. The plugin also needs a MySQL database.

## Building

The project builds with Gradle (wrapper included): `./gradlew build` produces the jar in `build/libs/`, and
`./gradlew test` runs the unit tests. `./gradlew runServer` starts a local Paper 1.21 test server with the
plugin (from the `run-paper` Gradle plugin).

Dependencies in `build.gradle` are all `compileOnly`, because the plugins above provide them at runtime:

- `io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT` from repo.papermc.io
- `libs/gringotts-3.0.1.jar` (a local jar, kept in the repository)
- `com.github.Ghiojo:Skysong-Special-Items:1.0.1`, `com.github.Ghiojo:SkysongStats:1.4.0` and
  `com.github.Ghiojo:SkysongNPCs:1.0.0` (API 0.23.0) from JitPack.
  Ghiojo repositories are always `com.github.Ghiojo:<RepoName>:<tag>`, with no `:api` module.
- `me.clip:placeholderapi:2.11.6` from repo.extendedclip.com

Tests use JUnit 5 and SnakeYAML only.

## The data folder

Everything lives in `plugins/SkysongEconomy/`. Files marked "copied" are written from the jar the first time
the plugin needs them and never overwritten after, so your edits are kept. Delete a file to get the default back.

```
plugins/SkysongEconomy/
├── config.yml                      copied   MySQL connection
├── messages.yml                    copied   every player-facing text
├── menus.yml                       copied   shared menu button icons
├── prices.yml                      copied   how many decimals prices keep
├── economy-and-logistics.yml       copied   E&L skills, slots, delivery times, ledger fine
├── architite_coins/                copied   the three coin item templates
│   ├── architite_pinch.yml
│   ├── architite_pouch.yml
│   └── architite_pound.yml
├── items/                          yours    one Skysong item template per material id
│   └── <material_id>.yml
├── ledgers/                        example  one trade ledger per file
│   └── smugglers.yml                        only created when the folder doesn't exist
├── businesses/
│   ├── types.yml                   copied
│   ├── tiers.yml                   copied
│   ├── access.yml                  copied
│   ├── shifts.yml                  copied
│   ├── training.yml                copied
│   └── block.yml                   copied   business nameplate
└── nodes/
    ├── settings.yml                copied
    ├── groups/<id>.yml             yours    loot groups
    ├── random/<id>.yml             yours    random node tables
    └── farm/
        ├── resources/<id>.yml      yours    farm resources (what a primer grows)
        └── types/<id>.yml          yours    farm types (which resources a farm node takes)
```

- **`items/`** is important: imports, exports, loot and primers can only name items that have a template file
  here (`<material_id>.yml`, in the Skysong_Items template format). The plugin checks this when it reads ledgers
  and node files, and skips entries that name a missing item, with a console warning. The folder is
  `PluginPaths.items()`.
- The node folders have no bundled examples. Until you create files there, no random table or farm type exists
  and `/se node create` has nothing to use. See [Nodes](07-nodes.md) for each file's format.
- Every key of every file is described in the [Configuration reference](09-configuration-reference.md).

## What needs a restart, and what reloads

| Change | How to apply it |
| --- | --- |
| `nodes/groups`, `nodes/random`, `nodes/farm/resources`, `nodes/farm/types` | `/se node reload` |
| `nodes/settings.yml` | Restart |
| Anything in `businesses/`, `economy-and-logistics.yml`, `ledgers/` | Restart |
| `messages.yml`, `menus.yml`, `prices.yml`, `config.yml` | Restart |
| `items/` templates | Read when an item is made, so new templates work at once; but ledgers and node files only accept an item if its file existed when they were read, so reload or restart after adding one |

## Database connections

The plugin opens three MySQL connections, all from `config.yml`: one for the price database and good types
(main), one for the business system and one for the nodes system. Each is opened lazily and reopened if MySQL
closed it (`ConnectionProvider`). Tables are created on start-up; see [Database](10-database.md).

If the database can't be reached at start-up, the plugin logs a SEVERE error and keeps running with nothing
loaded; repositories reconnect on their next query.

## When a feature turns itself off

A feature that can't read its config logs one SEVERE line saying which file and key is wrong, then stays off
while the rest of the plugin runs:

- "Businesses are disabled until this is fixed: ..." means one of `businesses/types.yml`, `tiers.yml`,
  `access.yml`, `shifts.yml`, `training.yml` or `economy-and-logistics.yml` is broken. No `/se business`,
  `/se tokens` or `/se modifier` commands exist then, and nodes are off too.
- "Nodes are disabled until this is fixed: ..." means `nodes/settings.yml` is broken.
- Smaller files fall back instead of disabling anything: a broken `prices.yml` keeps 1 decimal, a broken
  `menus.yml` uses default icons, a broken `businesses/block.yml` turns business nameplates off, and a broken
  `messages.yml` uses the bundled texts.
