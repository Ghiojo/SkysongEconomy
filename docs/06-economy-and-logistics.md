# Economy and Logistics (E&L)

Economy and Logistics lets a business buy **imports** (delivered after a real-time wait) and sell **exports**, from
the **trade ledgers** it has selected. Prices come from the price database, the ledger's price expressions and the
price modifiers (see [Prices](04-prices.md)).

Code: `trade/` (ledgers, bundles, settings, pricing, `LedgerSelections`, `ExportService`), `trade/delivery/`
(`DeliveryService`, `Delivery`), menus in `gui/business/` (`ImportsMenu`, `ExportsMenu`, `SlotsMenu`,
`LedgersMenu`, `LedgerDetailsMenu`, and their `*Entry` buttons). Config: `economy-and-logistics.yml` and the
`ledgers/` folder.

## Who has it, and how much

A business has E&L when it has picked one of the `skills` in `economy-and-logistics.yml` (by default `economical`,
a Service skill). Each pick of those skills is one **level**, and the level sets its slots:

```yaml
skills:
  - economical
slots:
  1: {imports: 2, ledgers: 1}   # imports: deliveries on the way at once
  2: {imports: 3, ledgers: 1}   # ledgers: ledgers selected at once
  3: {imports: 4, ledgers: 2}
  4: {imports: 5, ledgers: 2}
  5: {imports: 6, ledgers: 3}
```

If the file leaves a level out, the highest level below it is used (`EconomyLogisticsSettings.limits`). Losing
picks or tiers lowers the level: the newest ledger selections beyond the new limit are dropped without a fine, and
deliveries already on the way stay.

## Ledgers

Each file in `plugins/SkysongEconomy/ledgers/` is one ledger, and its file name (lower-case letters, digits, `-`,
`_`) is its id. Every E&L business can select any ledger. The example `smugglers.yml` is only written when the
folder doesn't exist, so deleting it keeps it deleted. Ledgers are read on start-up only.

```yaml
name: "<dark_gray>Smugglers' Ledger"     # MiniMessage
icon: ENDER_CHEST                         # Minecraft material, default PAPER
description:
  - "<gray>Goods nobody asks questions about."

imports:
  steel_weapons:                          # import id, unique in this ledger
    name: "<white>Crate of steel weapons"
    icon: IRON_SWORD
    lore: ["<gray>Straight from the forges, no marks."]
    contents:
      - {item: steel_sword, amount: 2}          # a fixed amount
      - {item: steel_spear, amount: "1-3"}      # a random amount, both ends included
      - rolls: "1-2"                            # roll 1 or 2 times...
        from:                                   # ...each roll picks one by weight
          - {item: iron_dagger, weight: 3, amount: 1}
          - {item: steel_axe, weight: 1, amount: 1}
    prices:                                     # one per delivery time it offers
      express: "{base} * 1.5"
      regular: "{base} * 1.1"
      slow: "{base}"

exports:
  steel_ingots:
    name: "<white>Steel ingots"
    icon: IRON_INGOT
    item: steel_ingot          # one item...
    batch: 16                  # sold 16 at a time, all or nothing
    price: "{base} * 0.8"
  any_weapon:
    name: "<white>Any weapon"
    icon: STONE_SWORD
    good-type: weapon          # ...or any item of a good type
    batch: 1
    price: "{base} * 0.6"
```

Rules the parser (`trade/LedgerParser`) checks:

- every `item` must have a template in `items/` (`<material_id>.yml`);
- an import needs at least one `contents` line and at least one price; price keys must be delivery time ids from
  `economy-and-logistics.yml` (unknown ones are warned about and ignored); leaving a time out means the import
  doesn't offer it;
- an export needs exactly one of `item` or `good-type`, and `batch` of 1 or more;
- prices must be valid expressions.

A broken import or export is skipped with a console warning, so one typo doesn't hide the rest of the ledger. A
file that can't be read at all is skipped.

### What `{base}` means

- **Import**: the base value of the bundle: each fixed item's base value times its amount, plus the **average** of
  each random part (a `"1-3"` amount counts as 2; a weighted roll counts each option by its share of the weight,
  times the average number of rolls). See `trade/Bundle.baseValue`.
- **Export**: the base value of the items actually sold in that batch, added up (for a good-type export, of whichever
  items were taken).

Every item involved must have a base value in the price database, or the price is refused
(`prices.unknown-item`).

## Delivery times

```yaml
delivery-times:
  express:  {name: "<gold>Express (24h)",     hours: 24}
  fast:     {name: "<yellow>Fast (48h)",       hours: 48}
  regular:  {name: "<white>Regular (3 days)",  hours: 72}
  standard: {name: "<gray>Standard (5 days)",  hours: 120}
  slow:     {name: "<dark_gray>Slow (7 days)", hours: 168}
```

The time is **real time**: it passes while the server and the players are offline. Menus list the times in file
order.

## Selecting ledgers

The **Ledgers** button lists every ledger, marking the selected ones. Clicking one opens its details: its imports,
its exports and, for the owner, a Select or Remove button (both ask for confirmation).

- Only the owner selects and removes (staff on a server-owned business). Everyone can browse.
- Selecting is free, and needs a free ledger slot.
- Removing costs `ledger-removal-fine` from the owner's balance (refused if they can't pay; free on a server-owned
  business). Deliveries already ordered from that ledger still arrive.
- A selection whose ledger file was removed is ignored and takes no slot.

Code: `trade/LedgerSelections` (table `business_ledgers`).

## Imports

The **Imports** button opens every import of the selected ledgers, priced for the delivery time chosen in the right
column. Clicking an import shows a confirmation with its contents, delivery time, price and modifiers, then orders
it (`trade/delivery/DeliveryService.order`). Ordering needs:

- E&L, and the ledger selected;
- the owner, or a group with `order-imports`;
- a free delivery slot (`imports` for the level);
- enough money: the **orderer's own balance** pays.

The items are **rolled when the import is ordered** and saved with the delivery, so changing or removing the ledger
afterwards doesn't change what arrives. Orders can't be cancelled.

## Slots: collecting deliveries

The **Slots** button shows each delivery on the way (with its arrival time), each ready one, and the free slots.
Clicking a ready one collects it: whoever ordered it, the owner, or anyone in a group with `order-imports` may. The
items go into the inventory and whatever doesn't fit drops at the player's feet. Collecting frees the slot.

When a business is deleted, every delivery is refunded to whoever paid for it.

Deliveries are saved in `business_deliveries`; their `items` column is written like `steel_sword:2;steel_spear:3`
(`trade/delivery/ItemListCodec`).

## Exports

The **Exports** button lists every export of the selected ledgers, priced for the batch the viewer would sell from
their inventory right now. Each click sells **exactly one batch**, or nothing if the inventory doesn't hold a whole
batch (`business.exports.not-enough`). Selling needs E&L, the ledger selected, and the owner or a group with
`sell-exports` (the `workers` group has it by default). The money goes to the **seller**. Stacks are taken in
inventory order (`trade/ExportBatch`). Selling, like ordering, costs no downtime.

## How a price is worked out (`trade/TradePricing`)

```
import:  value = ledger price expression for that delivery time, with {base} = bundle base value
export:  value = ledger price expression, with {base} = base value of the items taken
both:    price = truncate(value * (1 + sum of applying modifiers' percent / 100)), never below 0
```

The `PriceSubject` that modifiers match on carries the direction (import or export), the business id, the ledger
id, the material ids involved and their good types.
