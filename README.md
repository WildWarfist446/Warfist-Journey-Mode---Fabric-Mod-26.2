# Minecraft Journey Mode - Fabric Mod 26.2

A Fabric mod for Minecraft 26.2 that brings a Journey Mode style progression to survival:
sacrifice items to research them, then take as many copies of anything you have researched as you like.

## Features

Open your inventory. Two buttons sit on its left edge, so they stay clear of JEI on the right. Each opens a panel, and only one is open at a time.

### Research (first button)
- Pick up a stack with your cursor and click the panel. The items are used up and count toward that item's research cost.
- The panel shows a progress bar for the item on your cursor, and how many items you have researched in total.
- The cost of an item is one full stack: 64 for most blocks and materials, 16 for ender pearls, 1 for tools and armor.
- Progress is saved per player with the world.

### Researched items (second button)
- A scrolling grid of everything you have fully researched.
- A search box at the top. Type words to match item names (`iron pick`), or start with `@` to filter by mod (`@create`).
- Clicks work like the creative menu's item list:
  - Empty cursor: click takes one item, shift-click or middle-click takes a full stack.
  - Same item on the cursor: left-click adds one, shift-click fills the stack, right-click removes one.
  - A click with a different item on your cursor does nothing, so you never lose a real item.
- Hovering an item shows its normal tooltip and the name of the mod that added it.
- The server checks that an item is fully researched before giving it, so a modified client cannot take items it has not researched.

### Other mods
Everything is keyed by the item registry, so items added by other mods are researched and listed like vanilla ones, and research cost uses each item's own stack size. Progress for items from a removed mod is kept and returns if the mod is reinstalled.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.5 or newer
- Fabric API
- Java 25

## Development

```
./gradlew runClient
./gradlew build
```

The built jar is in `build/libs`.

Layout:
- `src/main`: shared and server code. Research data (`research/`), network packets (`network/`).
- `src/client`: the inventory panels (`ResearchPanel`), client copy of research progress (`ResearchClientState`), and a small mixin accessor to place the panels beside the inventory.

## Status

Early work in progress. See [TODO.md](TODO.md) for what is planned, including logos for the panel buttons, category filters, and configurable research costs.
