# Claims (SkysongClaims)

SkysongClaims groups WorldGuard regions into **Claims** owned by SkysongStats characters. It keeps who owns a
Claim and whether it is for sale, and leaves three things to SkysongEconomy: the **price**, the **money**, and the
**fourth line of every Claim sign**, where the price is shown.

The full agreed design is in the project's design file `claims-integration-design.md` (revision 3, signed off
2026-11-23). It is built in steps; this page describes what is built so far.

| Part | State |
| --- | --- |
| Connecting to SkysongClaims | **Done** (this page) |
| Claim prices and server listings (`/se claim price`, `sell`, `unsell`, `list`) | **Done** |
| Claim modifiers (`claim-type:`, `claim:`, `on:claims`, flat amounts) | **Done** |
| The price on sign line 4 | **Done** |
| Buying, and `ClaimPurchaseEvent` | **Done** |
| Player resale | Not yet |

## Connecting

SkysongClaims is optional (`softdepend`). `bootstrap/ClaimFeature` starts after every other feature:

- Without SkysongClaims, the console says `SkysongClaims is not installed: Claim sales are off` and nothing else
  changes.
- With it, `integration/skysongclaims/SkysongClaimsAdapter.connect` reads `SkysongClaimsApi.get()` and checks its
  `apiVersion()` against `REQUIRED_API` (1.0.0). An older SkysongClaims is refused with one console error naming
  the API it has, and Claim sales stay off.
- No SkysongClaims class is loaded before the plugin is known to be enabled, so a server without it starts fine.

The build uses `compileOnly 'com.github.Ghiojo:SkysongClaims:1.0.0'` (JitPack publishes only its `api` module, under
the repository's own name).

## Prices

Every Claim has at most one **asking price**: what its seller asks, before any modifier (`claims/ClaimPrices`,
table `claim_prices`). It is a price expression, usually a plain number such as `2000`, but `{item:<id>}` works too
(`{item:architite_pound} * 600` follows the pound's base value). `{base}` has no meaning here and is refused.

- A price must be worth more than 0 after truncation to the decimals in `prices.yml`.
- Free Claims are never priced.
- Prices are worked out when asked, never cached, so changing an item's base value changes the Claim at once.
- A row whose expression can't be read is skipped at start-up with a warning, and stays in the table for staff to fix.

## Server sales

`claims/ClaimSales` holds every rule about pricing and selling, so every change goes through it:

| Action | Rule |
| --- | --- |
| `sell <claim> [price]` | The Claim must exist, not be free, have no owners, and not be on sale by another plugin. With a price, it is saved first; without, the saved price is used (`claims.no-price` if there is none). Then SkysongClaims marks it for sale in SkysongEconomy's name, and its signs show "For sale" on line 3 |
| `unsell <claim>` | Only Claims SkysongEconomy is selling. The price stays saved |
| `price set` | A Claim on sale stays on sale at the new price |
| `price clear` | A Claim the server was selling goes off sale, since it can't be sold without a price |
| Last owner leaves | `integration/skysongclaims/ClaimChangeListener` hears `ClaimOwnerRemoveEvent`. When the Claim has no owners left, isn't free, isn't on sale and has a price, it goes back on sale at that price (`ClaimSales.abandoned`) |
| Claim deleted | `ClaimDeleteEvent` removes its price |
| Start-up | `ClaimSales.reconcile` removes prices of Claims that are gone or free now, and takes Claims SkysongEconomy is selling without a price off sale. It never puts anything on sale by itself |

SkysongClaims only knows whether a Claim is for sale and which plugin said so (`ClaimSales.SELLER`, the plugin's
name). The price stays here.

## Claim modifiers

Claim modifiers are ordinary price modifiers (`/se modifier`, see [Prices](04-prices.md#price-modifiers-se-modifier))
with `on:claims`, on `global`, `claim-type:<type>` or `claim:<claim>`. They are always positive, and can be a
percentage (`+10%`) or a flat amount (`+50`). Trade modifiers (`on:imports`, `exports`, `both`) never change a
Claim's price.

`claims/ClaimPricing.quote` works out what a Claim costs. Every modifier's share comes from the **asking price
alone**, so each one has an exact amount:

```
share of a percentage = asking price x percent / 100, truncated
share of a flat       = its amount, truncated
total                 = asking price + every share
```

Example: a house asking 2,000, with `district_tax claim-type:house +10%` (share 200) and
`registry_fee claim-type:house +50` (share 50), costs 2,250. The seller's part is always the asking price; the
shares never go to the seller.

`ClaimQuote` holds the asking price, each `Share` (the modifier and its amount), the modifier total and the total.
`/se claim price view` lists them, and `/se claim list` shows the total with the asking price.

When businesses are disabled there are no modifiers, and a Claim costs its asking price.

### The API

`getClaimFeature().pricing()` gives `ClaimPricing`. Another mechanic (a district tax, a festival) uses it with its own
modifier type key, never the table:

| Method | What it does |
| --- | --- |
| `quote(claimId)` | What buying the Claim costs now, with every share |
| `setTypeModifier(key, typeId, amount, duration, name, by)` | Adds or replaces the modifier with that key on every Claim of the type |
| `removeTypeModifier(key, typeId)` | Removes it |
| `modifiersOf(typeId)` | The modifiers on the type itself |
| `applying(claim)` | Every modifier that changes one Claim's price now |

To follow changes (for example to redraw a price), use `PriceModifiers.addListener`.

## The price on sign line 4

SkysongClaims keeps the fourth line of every Claim sign for SkysongEconomy. `claims/SignPriceUpdater` writes there
**what a buyer pays** (the asking price plus every modifier's share) while SkysongEconomy has the Claim on sale, and
blanks it otherwise: a Claim nobody sells, one that is owned, or one another plugin is selling.

```yaml
# messages.yml
claims:
  sign-price: "<gold><price>"                 # e.g. "1,950 Pouches"; signs fit about 15 characters
  sign-price-raised: "<gold><price> <red>↑"   # used when modifiers add to the asking price
```

SkysongClaims saves the line, so it is only written when its text changes; the last text written for each Claim is
kept in memory. What makes it write again:

| What happened | Claims redrawn |
| --- | --- |
| A price set or cleared, a Claim put on sale or taken off, an abandoned Claim back on sale | That Claim |
| A sign linked to a Claim (new signs start blank) | That Claim, always written |
| A Claim changed type, or its sale changed in SkysongClaims | That Claim |
| A modifier with `on:claims` added, replaced or removed | Every Claim on sale |
| Such a modifier expires | Every Claim on sale, within a minute (expired modifiers are looked for once a minute) |
| A base value set or removed with `/se ledger` | Every Claim on sale |
| SkysongEconomy starting, or SkysongClaims reloading | Every Claim with a price or on sale, always written |

If a Claim on sale has a price that can't be worked out (it names an item whose base value was removed), its line
is left blank and the console says why.

## Buying

Players buy a Claim SkysongEconomy has on sale with the **Buy** button in the Claim sign's menu (shown only on Claims
we sell, with what it costs) or `/se claim buy [claim]` (the Claim named, or the one they stand in). Permission
`skysongeconomy.claims.buy`, default everyone. Buying uses the business menus, so it is off while businesses are
disabled (the console says so).

Both open a confirmation listing the Claim's type, the seller, the asking price, each modifier and its share, the
total, the player's balance, and the character that will own it. On **Confirm** (`claims/ClaimPurchases.buy`):

1. The Claim is quoted again. If it is no longer on sale, or the total differs from what the player saw, nothing
   happens; a changed price is shown and the confirmation opens again.
2. SkysongClaims is asked whether the character may own it (`canOwn`: type limits, slots, unknown character).
3. The total is taken from the player (`claims.buy.no-money` otherwise).
4. The character is made an owner. If SkysongClaims refuses at this point, the money is given back; if even that
   fails, the console logs a SEVERE line so staff can pay it back by hand.
5. The Claim comes off sale (line 4 is cleared) but keeps its price, so it goes back on sale at that price if its
   owners ever leave.
6. The sale is recorded in `claim_sales` and `ClaimPurchaseEvent` is fired.

A server sale pays nobody: the money leaves the economy. A plugin that wants to keep part of it listens to the event.

## `ClaimPurchaseEvent`

`org.skysongdev.skysongEconomy.api.claims` is the package other plugins compile against
(`compileOnly 'com.github.Ghiojo:SkysongEconomy:<tag>'`, `softdepend: [SkysongEconomy]`). `ClaimPurchaseEvent` fires on
the main thread after a sale is done and recorded, and can't be cancelled. `getSale()` gives a `ClaimSale`:

| Component | What it is |
| --- | --- |
| `id` | The sale's number in `claim_sales`, counting up from 1 |
| `claimId`, `claimTypeId` | The Claim, and its type when bought |
| `buyer` | The `CharacterRef` that owns it now; `buyer().player()` is the account that paid |
| `askingPrice` | The Claim's own price (in a resale, what the seller receives) |
| `modifiers` | Each `AppliedModifier`: `id`, `key` (such as `district_tax`), `target` (`claim-type:house`), `kind` (`percent` or `flat`), `value`, `share` (money), `displayName` |
| `modifierTotal` | The sum of the shares, never negative |
| `paid` | What the buyer paid: `askingPrice + modifierTotal` |
| `at` | When |

A treasury keeping a district tax:

```java
@EventHandler
public void onClaimPurchase(ClaimPurchaseEvent event) {
    for (AppliedModifier modifier : event.getSale().modifiers()) {
        if (modifier.key().equals("district_tax")) {
            treasury.store(modifier.share());
        }
    }
}
```

## The gateway

The Claim rules live in the pure package `claims/` and only see SkysongClaims through `claims/ClaimGateway`:

| Method | What it does |
| --- | --- |
| `find(id)`, `all()` | Read Claims as `ClaimView`: id, display name, type id, whether it is a Free Claim, owners (as `CharacterRef`), and which plugin put it on sale |
| `typeExists(type)` | Whether SkysongClaims has that type, for checking modifier targets |
| `canOwn(character, id)` | Whether the character may become an owner, as an `OwnershipAnswer`: `OK`, `TYPE_LIMIT`, `FULL`, `ALREADY_OWNER`, `UNKNOWN_CHARACTER` |
| `addOwner(id, character)` | Makes it an owner when `canOwn` allows it, and answers the same way |
| `clearOwners(id)` | Removes every owner (and the Claim's build groups) |
| `setForSale(id, yes/no)` | Puts it on sale in SkysongEconomy's name, or takes it off. SkysongClaims then shows "For sale" on line 3 by itself |
| `setPriceLine(id, miniMessage)`, `clearPriceLine(id)` | Writes or blanks line 4 of every sign of the Claim. SkysongClaims saves it |

`SkysongClaimsAdapter` is the only class that imports SkysongClaims. Tests use `FakeClaims` (in `src/test`, package
`claims`), which answers like SkysongClaims' API documents, checking owners in the same order as SkysongClaims
does; `FakeClaimsTest` pins that down.

**Free Claims are not a type.** Staff make any Claim free with `/sclaims free <id> true [slots]` (a type's
`free-claim:` only sets the default for new Claims). SkysongEconomy will never price, sell or show a price on a
Free Claim.
