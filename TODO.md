# To do

## Research panel
- [ ] Replace the placeholder "I" on the item list toggle button with its icon (the research button's icon is done). Add it to `ResearchPanel.drawButton` the same way as `RESEARCH_ICON`.
- [ ] Remember whether the panel is open between sessions (it resets on restart).
- [ ] Keep the panel on screen at high GUI scales or narrow windows (no clamping yet).
- [ ] Show the panel on the creative inventory screen too (survival inventory only for now).

## Journey Mode
- [ ] Item list: creative-tab style category filters.
- [ ] Test the item list with a few real mods installed (items, search by `@modid`, tooltips).
- [ ] Decide whether items with different data (enchanted books, potions) research separately. Currently one entry per item type.
- [ ] Make research costs configurable (currently one full stack per item, see `ResearchCosts`).
- [ ] Commands.
