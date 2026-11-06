# Currency and money

## Architite coins

The server's currency is Architite (AU), with three coin items:

| Coin | Value | Template file |
| --- | --- | --- |
| Pinch (Pinches) | 0.1 AU | `architite_coins/architite_pinch.yml` |
| Pouch (Pouches) | 1 AU | `architite_coins/architite_pouch.yml` |
| Pound (Pounds) | 10 AU | `architite_coins/architite_pound.yml` |

On start-up, `currency/ArchititeCurrency` builds each coin from its template (through the SkysongItems
template API) and registers it with Gringotts as a denomination: `Configuration.CONF.getCurrency().addDenomination(item, value, singular, plural)`.
The values and names are written in `ArchititeCurrency.DENOMINATIONS`; to add a coin, add a template file to
`src/main/resources/architite_coins/` and a line there. A coin that fails to register is logged and the others
still register.

The template files are copied to the data folder on first start (by `ResourceExtractor`) and can be edited
there: name, lore, item type. The `values` and `tags` lines inside them are SkysongItems' own data.

## Money in the code: `Wallet`

Every feature moves money through `money/Wallet`:

```java
BigDecimal balance(UUID player);
boolean deposit(UUID player, BigDecimal amount);   // false when it couldn't pay (e.g. no room for coins)
boolean withdraw(UUID player, BigDecimal amount);  // false, and nothing taken, when they can't afford it
String format(BigDecimal amount);                  // e.g. "12.5 Pouches"
```

Money belongs to the **player account**, not to a character: every character of a player shares one balance.
On the server the wallet is `integration/gringotts/GringottsWallet` (Gringotts' `Eco` player accounts, which
hold coins and vault balances). Tests use `MapWallet`. Gringotts moves items, so the wallet must be used on the
main thread.

Amounts are `BigDecimal` everywhere and are only turned into `double` inside `GringottsWallet`.

## Where money comes from and goes

| Flow | Paid by | Paid to | Docs |
| --- | --- | --- | --- |
| Shift pay | Created (server money) | The worker's player, on Retrieve Pay | [Businesses](05-businesses.md#work-a-shift-and-retrieve-pay) |
| Vault cut of a worker's shift | Created | The business vault | [Businesses](05-businesses.md#the-vault) |
| Training fee | The trainee | The business vault | [Businesses](05-businesses.md#trainlearn) |
| Vault collection | The vault | The owner's player (or the staff member, on a server-owned business) | [Businesses](05-businesses.md#the-vault) |
| Import order | The character who orders | Leaves the economy | [Economy and Logistics](06-economy-and-logistics.md#imports) |
| Export sale | Created | The seller | [Economy and Logistics](06-economy-and-logistics.md#exports) |
| Ledger removal fine | The owner | Leaves the economy | [Economy and Logistics](06-economy-and-logistics.md#selecting-ledgers) |
| Public node `money` cost | The gatherer | Leaves the economy | [Nodes](07-nodes.md#what-a-harvest-costs) |

When a payment to a player fails (Gringotts returns anything but success), the action is refused and nothing
changes, except where noted in each feature (for example a deleted business that can't refund someone logs a
warning asking staff to pay by hand).
