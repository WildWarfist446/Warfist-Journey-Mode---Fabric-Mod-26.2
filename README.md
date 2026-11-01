# Minecraft Journey Mode - Fabric Mod 26.2

A Fabric mod for Minecraft 26.2 adding Terraria-Style Journey Mode to survival:
sacrifice items to research them, then take as many copies of anything you have researched as you like.

## Features

Open your inventory. Two icon buttons and a trash slot sit on its left edge, so they stay clear of JEI on the right. Each button opens a panel, and only one is open at a time. The panels are centred on the inventory and use an orange and brown colour scheme. Everything hides while the recipe book is open and comes back when it closes.

### Research (first button)
- Pick up a stack with your cursor and click the panel. The items are used up and count toward that item's research cost.
- Or, with the panel open, shift-click an item in your inventory or hotbar to send it straight to research. Only as many items as it still needs are used.
- The panel shows a progress bar for the item on your cursor, and how many items you have researched in total.
- The cost of an item is one full stack by default: 64 for most blocks and materials, 16 for ender pearls, 1 for tools and armor. Items that do not stack always cost 1.
- Costs can be changed in `config/journeymode-costs.json` (created on first launch). Keys are item ids (`"minecraft:ancient_debris": 25`) or item tags starting with `#` (`"#c:ingots": 25`), so items from other mods that use the common tags are included. An item's own entry beats a tag, and the first matching tag in the file wins. The server's costs are sent to players when they join; restart to apply edits.
- Progress is saved per player with the world.
- Sounds: adding items to an item's research plays the item pickup pop. Finishing an item plays a TNT explosion followed by a note block ding.

### Researched items (second button)
- A scrolling grid of everything you have fully researched. A thin scroll bar on the right can be clicked or dragged, and the mouse wheel works too.
- Six tabs above the grid: All, Blocks, Tools & Combat, Materials, Food & Brewing, and Redstone & Misc. They match item tags (including the common `c:` tags other mods use) and what an item is, and an item can show in more than one tab. Search works inside the selected tab.
- A search box at the top. Type words to match item names (`iron pick`), or start with `@` to filter by mod (`@create`).
- Clicks work like the creative menu's item list:
  - Empty cursor: click takes one item, shift-click or middle-click takes a full stack.
  - Same item on the cursor: left-click adds one, shift-click fills the stack, right-click removes one.
  - A click with a different item on your cursor does nothing, so you never lose a real item.
- Hovering an item shows its normal tooltip and the name of the mod that added it.
- Taking an item leaves it on your cursor, ready to place.
- The server checks that an item is fully researched before giving it, so a modified client cannot take items it has not researched.

### Trash slot
- Under the two buttons is a trash slot. It opens no panel: click it with an item on your cursor and the stack goes in.
- The stack stays there, saved with the world, until you put another item in, which throws the old one away. Click it with an empty cursor to take the stack back.

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
- `src/main`: shared and server code. Research data, costs and the trash slot (`research/`), network packets (`network/`).
- `src/client`: the inventory panels (`ResearchPanel`), the item list's tabs (`ItemCategory`), client copy of research progress (`ResearchClientState`), research sounds (`ResearchSounds`), and a small mixin accessor to place the panels beside the inventory.
- `sprites`: the source art (Aseprite files and PNGs) for the button and trash icons. The in-game copies are in `src/main/resources/assets/journeymode/textures/gui`.

## Status

Early work in progress. See [TODO.md](TODO.md) for what is planned, including moving the tab rules into a config file, a reload command, and testing with other mods.
