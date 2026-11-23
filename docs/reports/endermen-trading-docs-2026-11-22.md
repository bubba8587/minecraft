<!-- dte:C1 -->
# Trading at cairns and sneak + middle-click laying: docs report (builder: agent 3)

Branch `endermen-trading`, off develop 7bf2b51. For `specs/endermen.md` "Trading at cairns" (now built), `specs/undead.md` "Loot on display", and the player docs. **(builder)** marks my choices; the spec left the return table, the pad's design and the delay open.

## Player-facing

- **Every cairn has a trade pad** **(builder: design)**: a purpur block plinth one block above the ground, skirted by four end stone brick slabs, just outside the cairn's offerings.
  - The endermen build and repair it like the rest of the cairn.
  - Existing cairns get one too: their endermen add it, so a finished cairn counts as unfinished until the pad is in.
- **Trading:**
  - Lay any item on the plinth (sneak + middle click). Once no player is within 16 blocks, the endermen take it: it vanishes with portal particles and the teleport sound.
  - 10-30 s later **(builder)**, an enderman teleports in beside the pad, holding its answer, and lays it on the plinth.
  - What you give doesn't matter: the answer is pure random (owner).
  - The answer stays until you pick it up; it is never taken back as a new trade.
  - No trading while the cairn is warded.
- **The answer table (builder, `data/bubbas_reworked_mobs/loot_table/gameplay/enderman_trade.json`, a vanilla barter table, datapack-editable), weights out of 133:**
  - **Good (about 40 %):** ender pearls 1-3 (10), chorus fruit 2-6 (8), emeralds 1-3 (6), bottles o' enchanting 1-4 (6), end rods 1-2 (4), popped chorus fruit 1-4 (4), an enchanted book (3), a diamond (2), a golden apple (2), a name tag (2), a disc fragment (2), a nautilus shell (2), a shulker shell (1), an echo shard (1).
  - **Weird:** a poppy (8), dirt 1-4 (6), end stone 1-4 (5), a poisonous potato (5), a carved pumpkin (4), a dead bush (4), a bone (4), string 1-3 (4), and 3 each of a fermented spider eye, ink sacs, a glass bottle, a sea pickle, snowballs, a lily pad, a cactus, a brown mushroom, clay balls, feathers, rotten flesh and flint. Also a pufferfish (2), TNT (1) and a cake (1).
- **Sneak + middle click** with an item in hand, aiming at a block, lays one of it on the ground as a display, anywhere: on top of the block you aim at, or in front of a side face, wherever there's a floor.
  - Plain middle click is still vanilla's pick block.
  - Works in survival and adventure where the player may use items there (spawn protection and adventure rules respected), within block reach.
  - Picking it back up with an empty hand already worked.
  - In creative it lays a copy and keeps the stack.

## How it's built

- **Pad:** `CairnDesign.buildTradePad`, run last with its own random stream (`seed ^ PAD_SALT`), so existing cairns' forms and offerings don't change.
  - It tries 16 angles at reach + 3 to + 5, on flat ground (the centre and its four neighbours at the same height, within 1 of the origin's), clear of every design block.
  - The position comes out as `CairnDesign.designed(...)` → `Designed(blocks, tradeSpot)`; `CairnSite.tradeSpot()` returns it. `CairnDesign.design(...)` is unchanged for its other callers.
- **Trades:** `enderman/CairnTrades`.
  - Saved level attachment `bubbas_reworked_mobs:cairn_trades`, a map from site origin to the answer's due time, or "answered".
  - Ticked every 20 ticks for sites near players (`CairnSites.nearPlayers`), not on the game test server.
  - "No player near" is `getNearestPlayer(..., EntitySelector.NO_SPECTATORS)` within 16.
  - Taking uses `LootDisplays.takeForMob`.
  - The answer: a courier enderman spawned (`NaturalSpawns.create`) on a spawn cell 2 out from the pad. It's marked as a cairn spawn with the site as home, holds the rolled item in its main hand, and lays it with `HandPlacing.lay`, the same path players use (owner: "the enderman should place it using our place-item mod mechanism"). With no spawn cell free, the item is laid without a courier.
  - The roll is `LootContextParamSets.PIGLIN_BARTER` with the enderman as `THIS_ENTITY`; an empty roll gives a poppy.
- **Laying:** `display/HandPlacing`, shared by players and mobs.
  - `lay(level, placer, target, stack)` splits one item off, places a `GROUND` display facing the placer, plays the item-frame add sound and emits a `BLOCK_PLACE` game event.
  - `canLayAt`: air or still water, over a floor that holds a display.
  - Players: client mixin `LayItemPickMixin` on `Minecraft.pickBlockOrEntity` (sneaking, item in hand, a block aimed at) sends the C2S payload `bubbas_reworked_mobs:lay_item`. The server checks block reach, `mayInteract` and `mayUseItemAt`.
- **Tuning (`EndermanTuning`):** `TRADE_CHECK_TICKS` 20, `TRADE_QUIET_RANGE` 16, `TRADE_RETURN_MIN_TICKS` 200, `TRADE_RETURN_SPREAD_TICKS` 400.

## Tests

- **Core:** `cairnTradePadTakesAndAnswers`. A built cairn has a pad (purpur under the spot, the spot not a design block). An item on it is taken, isn't answered early, is answered once due, and the answer isn't taken back; an empty pad does nothing.
- **Milestone:**
  - `tradeTableAlwaysAnswers`: 400 rolls, never empty, more than 15 kinds.
  - `heldItemLaysOnTheGround`: one at a time, not on top of a display, not in mid-air.
- **Client part `trading`** (`-PclientTests=trading`), real key presses:
  - `trade-pad`;
  - `trade-offered`: a survival player sneaks and middle-clicks rotten flesh onto the plinth, 5 → 4 in hand;
  - `trade-answered`: the courier enderman and its answer on the plinth;
  - `laid-on-grass`: an apple laid on grass, and a plain middle click lays nothing.
  - All looked at.
- **Enderman and cairn milestone tests:** 12/12 with the pad added. Core: 13/13.

## Open for the owner

- The answer table.
- The pad's look.
- The 10-30 s wait.
- The 16-block "nobody near" range.
- Existing finished cairns counting as unfinished until their pad is built.
