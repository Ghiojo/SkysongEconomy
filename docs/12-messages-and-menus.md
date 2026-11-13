# Messages and menus

## `messages.yml`

Every player-facing text lives in `messages.yml`, written in [MiniMessage](https://docs.advntr.dev/minimessage/format.html)
with `<name>` placeholders. The top-level sections follow the features: `command`, `menu`, `prompt`, `business`
(with `hire`, `fire`, `access`, `vault`, `ledgers`, `imports`, `slots`, `exports`, `activity`, `shift`, `training`,
`picks`, `menu`, `block`, ...), `node`, `tokens`, `ledger`, `prices` (with `modifiers`). `prefix` is put before every
chat message.

How it is loaded (`messaging/MessageCatalog.of`):

- The file bundled in the jar is the default for every key. The server's file overrides it **key by key**, so a file
  from an older version still gets new messages without being replaced.
- A key in the server's file that the plugin doesn't use is ignored with a console warning (usually a typo).
- A key missing from both shows `Missing message: <key>` in red, so it is easy to spot.
- A list value is joined with new lines in chat, or used line by line for item lore.

How it is sent (`messaging/Messenger`):

- `send(sender, key, placeholders...)`: prefix + text.
- `send(sender, refusal)`: the refusal's key, with its placeholder values as **unparsed** text.
- `render(key, ...)` / `plainText(key)`: the text as a component, or as plain words (for example the chat cancel word
  `prompt.cancel-word`).

Anything a player typed (names, material ids, shift messages) must be passed with `Placeholder.unparsed`, so it can
never inject MiniMessage tags. Text staff write in config files (ledger names, table names, node names) is passed
with `Placeholder.parsed`, so its colours show.

The older `Messenger.info/success/error(sender, text)` methods take the text itself; only `/se good` and the command
group usage lines still use them.

To add a text: add the key to `src/main/resources/messages.yml` and send it by key. Servers get it automatically.

## The menu framework (`gui/`)

| Class | Role |
| --- | --- |
| `Menu` | A chest inventory made of `MenuButton`s. Subclasses implement `draw()` and call `set(slot, button)`. Each opening is a new object, so it can keep state (its page, the chosen delivery time). `redraw()` draws it again. |
| `MenuButton` | An icon plus what clicking does (`MenuButton.of(icon, click -> ...)`), or `MenuButton.display(icon)` for information only. |
| `MenuClick` | Who clicked and how (left, right, shift). |
| `MenuListener` | Cancels every click and drag while a plugin menu is open, so the server never moves an item by itself; runs the clicked button; handles input slots; gives back held items when a menu closes. `MenuListener.closeAll(server)` runs on shutdown. |
| `BorderedMenu` | A 6-row menu with a border, a header slot, 28 content slots and 4 side buttons (see `gui/layout/BorderedLayout`). |
| `PagedMenu<T>` | A bordered menu that lists items across pages, with previous, back and next on the bottom row. |
| `ConfirmMenu` | A summary in the middle, confirm on the left, cancel on the right. Closing counts as cancelling. Used before anything that can't be undone: skill picks, ordering imports, selecting and removing ledgers. |
| `ChatPrompts` | Asks a player to type an answer in chat (renaming a business). The answer isn't shown to anyone; typing the cancel word, logging out or waiting a minute drops the question. |
| `MenuTheme` | Builds items: the shared buttons from `menus.yml`, and any item whose name and lore are `messages.yml` keys. Names and lore are never italic. |

The bordered layout:

```
row 0:  B B B B H B B B B      H = header
row 1:  B c c c c c c c S      c = content (28 slots)
row 2:  B c c c c c c c S      S = side buttons (right column)
row 3:  B c c c c c c c S
row 4:  B c c c c c c c S
row 5:  B B B P K N B B B      P = previous page, K = back, N = next page
```

### Input slots

A menu can have slots where players put items in, such as the farm planting slot and the booster
slot. The `Menu` methods:

| Method | Meaning |
| --- | --- |
| `isInputSlot(slot)` | This slot takes items. |
| `accepts(player, slot, offered, held)` | How many of the offered stack the slot takes (0 refuses). |
| `received(player, slot)` | Called after items arrived; for example plant once the whole primer is there. |
| `held(slot)` | What the slot holds now. |
| `useHeld(slot)` | The menu used the items (planted them), so they are not given back. |
| `giveEverythingBack(player)` | Returns what input slots still hold. |

`MenuListener` moves items itself: a click with an item on the cursor offers that stack (a right-click offers one),
a click with an empty cursor takes back what the slot holds, and a shift-click in the player's inventory offers that
stack to the first input slot that takes it. Whatever an input slot still holds goes back when the menu closes,
when the player quits, and on shutdown. Nothing is ever moved by the server, so odd clicks can't lose or copy items.

### Adding a business menu button

Implement `gui/business/MainMenuEntry`:

```java
int slot();                                              // pick a free one in MainMenuSlots
Optional<MenuButton> button(BusinessMenuContext context); // empty hides it for this viewer
```

`BusinessMenuContext` tells you the viewer, their active character (if any), the business, and whether they are
staff, with helpers `isOwner()`, `actsAsOwner()` (owner, or staff on a server-owned business), `isMember()` and
`canManage()` (owner or staff). Register it with `BusinessSystem.menus().addEntry(...)` in the bootstrap. The main
menu itself never changes.

### Business menu screens

| Screen | Class |
| --- | --- |
| Main menu | `BusinessMainMenu` |
| Skill picks, choose a skill | `SkillPicksMenu`, `SkillChoiceMenu` |
| Imports (with delivery time column) | `ImportsMenu`, `ImportScreens` |
| Exports | `ExportsMenu` |
| Delivery slots | `SlotsMenu` |
| Ledgers, one ledger | `LedgersMenu`, `LedgerDetailsMenu`, `LedgerScreens` |
| NPCs, one NPC | `NpcsMenu`, `NpcDetailsMenu`, `NpcScreens` |
| Who may use the NPCs: groups, one group's actions | `NpcAccessGroupsMenu`, `NpcAccessActionsMenu` |
| Modules, a skill's pool, choose NPCs for a module | `ModulesMenu`, `ModulePoolMenu`, `ModuleAssignMenu`, `ModuleScreens` |

Work a Shift, Retrieve Pay, Train/Learn and Vault are single buttons (`*Entry`) that act when clicked.

### Node menu screens

`gui/node/NodeMenus` opens and runs them. `NodeGatherMenu` (random nodes) shows the node and one button per tier;
`FarmMenu` adds the planting slot and the take-out button. Tier buttons the player can't use are greyed out
(`locked-tier` icon) and say why (`NodeMenuText`). Every click checks everything again, since the menu may have
stayed open while things changed.
