# Claims (SkysongClaims)

SkysongClaims groups WorldGuard regions into **Claims** owned by SkysongStats characters. It keeps who owns a
Claim and whether it is for sale, and leaves three things to SkysongEconomy: the **price**, the **money**, and the
**fourth line of every Claim sign**, where the price is shown.

The full agreed design is in the project's design file `claims-integration-design.md` (revision 3, signed off
2026-11-23). It is built in steps; this page describes what is on `master` now.

| Part | State |
| --- | --- |
| Connecting to SkysongClaims | **Done** (this page) |
| Claim prices and server listings (`/se claim price`, `sell`, `unsell`, `list`) | Not yet |
| Claim modifiers (`claim-type:`, `claim:`, `on:claims`, flat amounts) | Not yet |
| The price on sign line 4 | Not yet |
| Buying, and `ClaimPurchaseEvent` | Not yet |
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
