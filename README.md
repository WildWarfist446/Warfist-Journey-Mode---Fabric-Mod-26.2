# Minecraft Journey Mode - Fabric Mod 26.2

A Fabric mod for Minecraft 26.2 adding Terraria-Style Journey Mode to survival:
sacrifice items to research them, then take as many copies of anything you have researched as you like.

## Enabling Journey Mode

Journey Mode is chosen when a world is created and cannot be changed afterwards.

- **Single player and LAN:** on the Create New World screen, open the Game tab and switch on **Journey Mode** (the button with the research icon, under Allow Cheats). It starts off.
- **Dedicated servers:** set `journey-mode=true` in `server.properties` before the world is first loaded with the mod. If the line is missing the mod adds `journey-mode=false`. Only a world that has not been decided yet reads it.
- Worlds that already existed without the mod stay off. Where Journey Mode is off, none of the buttons, panels or features below appear, and the server ignores their requests.

## Features

Open your inventory. A column of six icon buttons and a trash slot sits on its left edge, centred on the inventory and clear of JEI on the right. Most buttons open a panel, and only one is open at a time. It all has a steampunk look, and in a Journey Mode world your inventory and recipe book match it. Everything hides while the recipe book is open and comes back when it closes.

### Research (first button)
- Pick up a stack with your cursor and click the panel. The items are used up and count toward that item's research cost.
- Or, with the panel open, shift-click an item in your inventory or hotbar to send it straight to research. Only as many items as it still needs are used.
- The panel shows a progress bar for the item on your cursor.
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

### Time (third button)
- A slider with four stops: Day, Noon, Night and Midnight. Drag the knob and let go to set the time.
- Operators only. Everyone else sees the slider dimmed with "Operators only". The server runs `/time set` as the player, so the game's own permission check decides.

### Weather (fourth button)
- A slider with three stops: Clear, Rain and Thunder. Works like the Time slider, using `/weather`, and is for operators only.

### Mob Spawns (fifth button)
- A slider from 0x to 10x for hostile mobs around you, set separately for each player and open to everyone. Only the even numbers are labelled, and the knob's own number shows while it is on an odd stop.
- **1x** is normal. **0x** stops hostile mobs spawning naturally in the chunk you stand in and the 8 around it (3 x 3 chunks). Mobs already there stay, and spawners, spawn eggs, phantoms and patrols are not affected.
- **2x to 10x** adds extra tries at the game's own monster spawning in those chunks, so light, terrain, biome and the 24 block distance from players still apply. To avoid lag, hostile mobs in the area are capped at 6 per step (60 at 10x), the extra spawning runs only every few ticks, and it pauses while the server is running slowly.

### God Mode (sixth button)
- The button is the switch: click it to turn God Mode on or off, and it stays pressed in while on. It opens no panel.
- While on, you take no damage except what ignores invulnerability, such as the void and `/kill`. Mobs still act normally. The setting is saved per player with the world.

### Trash slot
- Under the buttons is a trash slot. It opens no panel: click it with an item on your cursor and the stack goes in.
- The stack stays there, saved with the world, until you put another item in, which throws the old one away. Click it with an empty cursor to take the stack back.

### Advancements
A Journey Mode advancement tab is added:

| Advancement | When |
|---|---|
| Welcome to Journey Mode! | You join a Journey Mode world |
| Researcher | Your first item is fully researched |
| Block Head! | Every vanilla block is researched |
| Hardware Heavy | Every vanilla tool, weapon and armor item is researched |
| Hardcore Crafter | Every vanilla material is researched |
| Food O' Plenty | Every vanilla food and brewing item is researched |
| Everything Else... | Every vanilla redstone and misc item is researched |
| Fallen from Glory | God Mode is turned on for the first time |
| Weather Wizard | An operator uses the Weather tab |
| Khonshu Control | An operator uses the Time tab |

"Every vanilla item" means the vanilla items in the creative inventory's tabs, leaving out the Operator Utilities tab, spawn eggs, and a few things survival players cannot get (player heads, frogspawn, chorus plant, spawners, infested blocks, the Mojang banner pattern). The first time they are needed, the mod writes the full list to `logs/journeymode-counted-items.txt`.

### Other mods
Everything is keyed by the item registry, so items added by other mods are researched and listed like vanilla ones, and research cost uses each item's own stack size. Progress for items from a removed mod is kept and returns if the mod is reinstalled. The advancements count vanilla items only.

Tested with a modpack of about 90 mods. **Trinkets** (4.1.x) is supported, and its extra slots fit in with the inventory's look. Its side panels and tabs keep their own look. The support is optional, and other versions of Trinkets just keep their normal slot look.

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

The built jar is in `build/libs`. The dev client is capped at a 3 GB heap (see `build.gradle`).

Layout:
- `src/main`: shared and server code. Research data, costs, the trash slot, God Mode, Mob Spawns, advancements and the world setting (`research/`), network packets (`network/`), and the mixin that lets 0x stop natural spawns (`mixin/`).
- `src/client`: the inventory panels (`ResearchPanel`), the world creation switch (`JourneyToggleButton`), the inventory's steampunk look (`InventoryTheme`), client copy of the server's state (`ResearchClientState`), research sounds (`ResearchSounds`), and mixins for the inventory screen, the recipe book and the Create New World screen. Optional patches for other mods are in `mixin/compat` (see `journeymode.compat.mixins.json`).
- `src/main/resources/data/journeymode/advancement`: the advancement files.
- `sprites`: the source art (Aseprite files and PNGs) for the button icons. The in-game copies are in `src/main/resources/assets/journeymode/textures/gui`.

## Status

Early work in progress. See [TODO.md](TODO.md) for what is planned, including moving the tab rules into a config file, a reload command for the cost config, and remembering which panel was open.
