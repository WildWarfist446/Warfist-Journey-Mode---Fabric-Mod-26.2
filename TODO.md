# To do

## Research panel
- [ ] Remember whether the panel is open between sessions (it resets on restart).
- [ ] Keep the panel on screen at high GUI scales or narrow windows (no clamping yet).
- [ ] Show the panel on the creative inventory screen too (survival inventory only for now).

## Journey Mode
- [x] Item list: category tabs (All, Blocks, Tools & Combat, Materials, Food & Brewing, Redstone & Misc), see `ItemCategory`.
- [ ] Move the tab rules into a config file so the tag lists can be edited.
- [ ] Test the item list with a few real mods installed (items, search by `@modid`, tooltips).
- [ ] Decide whether items with different data (enchanted books, potions) research separately. Currently one entry per item type.
- [x] Make research costs configurable (`config/journeymode-costs.json`, see `ResearchCosts`).
- [ ] Reload the cost config without a restart (a command).
- [ ] Commands.
