# SkysongEconomy documentation

This folder explains every part of the SkysongEconomy plugin: what each feature does in game, how it is
configured, and how the code behind it is organised. It is written so that someone who has never seen the
code can maintain and extend it.

The documentation describes the code on `master` as of 2026-11-06 (after PR #37). Node boosters were still
in review at that time (branch `feature/node-boosters`); see [Nodes](07-nodes.md#boosters-not-on-master-yet).

## Reading order

If you are new to the code, read these in order:

1. [Architecture](01-architecture.md): how the plugin starts, the packages, and the patterns every feature follows.
2. [Setup and dependencies](02-setup-and-dependencies.md): the plugins it needs, the data folder, building and testing.
3. Then the feature you want to work on.

## Features

| Document | What it covers |
| --- | --- |
| [Currency and money](03-currency-and-money.md) | Architite coins, Gringotts, how money is paid and taken |
| [Prices](04-prices.md) | The price database (`/se ledger`), price expressions, good types (`/se good`), price modifiers (`/se modifier`) |
| [Businesses](05-businesses.md) | Business types, tiers, owners (including `#server`), workers, skill picks, access groups, the vault, shifts, training, Gathering Tokens, the business menu |
| [Economy and Logistics](06-economy-and-logistics.md) | Trade ledgers, imports and deliveries, exports, slots |
| [Nodes](07-nodes.md) | Random nodes, farm nodes, loot groups, primers, harvest costs, `/se node` |

## Reference

| Document | What it covers |
| --- | --- |
| [Commands and permissions](08-commands-and-permissions.md) | Every `/se` command and every permission node |
| [Configuration reference](09-configuration-reference.md) | Every config file and folder, key by key |
| [Database](10-database.md) | Every table, and how data is loaded and saved |
| [Events, API and integrations](11-events-and-api.md) | Bukkit events, what other plugins can call, PlaceholderAPI, CMI, SkysongStats, SkysongItems |
| [Messages and menus](12-messages-and-menus.md) | `messages.yml`, `menus.yml`, the menu framework and its input slots |
| [Testing, known gaps and how-tos](13-testing-and-maintenance.md) | Unit tests, what isn't tested, known follow-ups, recipes for common changes |

## Words used throughout

| Word | Meaning |
| --- | --- |
| Player account | A Minecraft account (a UUID). Money and downtime belong to the account. |
| Character | One SkysongStats profile of a player account (`CharacterRef` = player UUID + profile name). Businesses, workers, Gathering Tokens and skills belong to characters. |
| Active character | The profile the player is playing right now. Commands that take `<player>` without `[profile]` use it. |
| Material id | The id of a Skysong item, stored in the item's `material_id` tag. Prices, ledgers and loot all name items by material id. |
| Staff | Anyone with the admin permission of the feature, for example `skysongeconomy.business.admin`. |
| Downtime | SkysongStats' activity resource. Spending it uses the pool the player picked with `/downtime preference`. |
| Gathering Tokens / Harvest Tokens | The same balance: whole-number tokens per character, earned from Gathering shifts and spent at nodes. Code calls them Gathering Tokens; node texts call them Harvest Tokens. |
| E&L | Economy and Logistics: imports and exports through trade ledgers. |
| Refusal | A player action the rules don't allow. The code throws a `RefusedException` naming the `messages.yml` key that explains why. |
