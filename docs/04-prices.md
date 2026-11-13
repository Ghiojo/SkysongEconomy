# Prices

Three things decide what an import costs or an export pays:

1. the **price database**: each Skysong item's base value, set with `/se ledger`;
2. the **price expression** written in the ledger for that import or export, which may use `{base}`;
3. the **price modifiers** that apply, set with `/se modifier`.

The result is truncated to the decimals in `prices.yml`. This page explains each part. How ledgers use them is in
[Economy and Logistics](06-economy-and-logistics.md).

Code: `prices/` (database and expressions), `prices/modifier/` (modifiers), `trade/TradePricing` (puts them
together), `data/database/PriceLedgerRepository` and `SqlModifierRepository` (storage).

## Price expressions

An expression is arithmetic written as text (`prices/PriceExpression`, which adds the price placeholders to the
shared parser `expression/Formula`). It allows:

- numbers, such as `4` or `0.25`
- `+ - * /`, parentheses and a leading minus
- `{base}`: the base value of whatever is being priced (only in ledger prices, never in the database)
- `{item:<material_id>}`: another item's base value from the price database

Examples: `{base} * 1.5`, `{item:steel_ingot} * 2 + 1`, `({base} + 10) / 2`.

The grammar is a small recursive-descent parser:

```
expression = term (("+" | "-") term)*
term       = factor (("*" | "/") factor)*
factor     = ("-" | "+") factor | number | "(" expression ")" | "{base}" | "{item:" id "}"
```

Expressions are parsed once (a typo is a `prices.invalid-expression` refusal naming the problem) and evaluated
exactly with `BigDecimal`. Dividing by zero is refused (`prices.division-by-zero`).

## Decimals: `prices.yml`

```yaml
decimals: 1    # 0 to 4
```

Prices are **truncated, never rounded**, wherever players see or pay them: with 1 decimal, 4.09 becomes 4.0
(`PriceSettings.truncate`). Values inside a calculation stay exact until the end.

## The price database: `/se ledger`

Each item's base value is itself an expression, so items can follow each other: a sword can be
`{item:steel_ingot} * 2 + 1`, and changing steel changes the sword at once (values are worked out when asked,
never cached between uses). `prices/PriceDatabase` enforces:

- a base value can't use `{base}` (`prices.base-in-database`);
- it can only name items that already have a base value (`prices.unknown-item`);
- it can't loop back to itself through other items (`prices.loop`, showing the path);
- an item that another item's value uses can't be removed (`prices.still-used`, listing the users).

The database is loaded into memory on start-up. Each change is written to the `price_ledger` table straight away
(the expression, plus its value at that moment in the old `cost` column for tools that read the table). Rows from
before expressions existed only have `cost`, which is read as a plain number.

| Command | Permission | What it does |
| --- | --- | --- |
| `/se ledger view [materialId]` | `skysongeconomy.ledger` | Shows an item's value and the expression behind it. Without an id, the held item. |
| `/se ledger list` | `skysongeconomy.ledger` | Every base value, alphabetically; broken entries are marked. |
| `/se ledger add [materialId] <expression>` | `skysongeconomy.ledger.add` | Sets a base value. The first word is taken as the id when there is more than one word and it looks like an id (starts with a letter, no expression characters); otherwise the held item's id is used. |
| `/se ledger remove [materialId]` | `skysongeconomy.ledger.remove` | Removes a base value, unless another item uses it. |

Example: `/se ledger add steel_ingot 4`, then `/se ledger add steel_sword {item:steel_ingot} * 2 + 1`.

## Good types: `/se good`

A good type is a tag such as `weapon` or `food` on a material id (`good_types` table, cached in memory by
`data/database/CachedGoodTypes`). Good types are used by category exports (sell "any weapon") and by
`good-type:` modifiers.

| Command | Permission | What it does |
| --- | --- | --- |
| `/se good add [materialId] <goodType>` | `skysongeconomy.good.manage` | Tags an item. Without an id, the held item. |
| `/se good remove [materialId] <goodType>` | `skysongeconomy.good.manage` | Removes the tag. |

The group needs `skysongeconomy.good` (default: everyone). There is no list command yet; these two commands
still use hard-coded texts instead of `messages.yml`.

## Price modifiers: `/se modifier`

A modifier is a signed percentage on a **target**, such as a port tax of +10% on one ledger. Code:
`prices/modifier/PriceModifiers` (the rules), `PriceModifier` (one modifier), `ModifierTarget`, `ModifierArguments`
(parses the command words).

### Targets

| Written as | Changes the price of |
| --- | --- |
| `global` | every import (or export) |
| `business:<id>` | everything one business trades |
| `ledger:<id>` | everything from one ledger |
| `good-type:<type>` | trades involving an item of that good type |
| `item:<material_id>` | trades involving that item |

For an import, "involving" means any item its bundle can deliver; for an export, the items actually sold.

### Rules

- Each modifier has a **type**, a short key like `port_tax` (`[a-z0-9_]`, up to 32 characters). A target holds
  at most one modifier of each type, so nothing stacks by accident: `add` refuses a second one
  (`prices.modifiers.exists`) and `set` replaces it, keeping its id. Modifiers of different types add up.
- The percent is from -100 (free) to +1000, never 0. It may be written `+10`, `-5` or `7.5%`.
- `on:imports` (the default), `on:exports` or `on:both` says which direction it changes.
- `for:<time>` makes it expire, such as `30m`, `12h`, `7d` or `1d12h`. Without it, it never expires. Expired
  modifiers stop applying at once and are deleted the next time the list is read or changed.
- The display name (the words after the options, up to 64 characters, MiniMessage) is shown when players hover
  a price. Without one it is `<type> <percent>%`, such as `port_tax +10%`.
- Ids are generated: `m1`, `m2`, ...
- When a business is deleted, every modifier on `business:<its id>` is removed.

### How a modified price is worked out

```
price = truncate( value * (1 + sum of every applying modifier's percent / 100) ), never below 0
```

`value` is the ledger expression's result. `ModifiedPrice` keeps the base, the modifiers applied, their total
percent and the final price, so menus can show how the price was reached.

### Commands

| Command | Permission |
| --- | --- |
| `/se modifier list [target]` | `skysongeconomy.modifiers` |
| `/se modifier add <type> <target> <percent> [for:<time>] [on:imports\|exports\|both] [name...]` | `skysongeconomy.modifiers.edit` |
| `/se modifier set <type> <target> <percent> [for:<time>] [on:...] [name...]` | `skysongeconomy.modifiers.edit` |
| `/se modifier remove <id>` | `skysongeconomy.modifiers.edit` |

Examples:

```
/se modifier add port_tax ledger:smugglers +10 for:7d Port tax
/se modifier set festival global -15 on:both Harvest festival
/se modifier add embargo good-type:weapon +50
/se modifier remove m3
```

Other features (a future tax mechanic, for example) should call `PriceModifiers.add/set/remove` with their own
type key instead of writing to the table, so the same rules apply. `BusinessSystem.modifiers()` exposes it.
