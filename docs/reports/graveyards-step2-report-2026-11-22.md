<!-- dte:C1,C6 -->
# Graveyards step 2: report and docs (agent 2, minecraft-b3, 2026-11-22)

For the orchestrator (SendMessage to minecraft-b0 / minecraft-2f and its pipe addresses all fail from this session, so this file is the report) and for merging into `specs/graveyards.md`, `specs/undead.md` (sleepers), `specs/totems-artifacts.md` (loot) and the player docs, then deletion. Branch `graveyards` (develop merged in first).

**Ack:** the owner's "resting dead" addition (via minecraft-2f) is in: yard-grave sleepers are always skeletons and never wake, wear dyed leather more often, keep it when mined and placed again, and there's a test. Crypt sleepers still wake.

## Player-facing

**Sizes:** a lone family plot (7-9 blocks square, 2-3 graves, no path between them, never a crypt), small yards (11-13), medium (15-19) and large (21-27).

**States of breakdown** (each yard is one):
- **Kept:** the whole fence standing, a neat gravel path edged with coarse dirt, stones upright, more flowers on the graves.
- **Overgrown:** (the first build) gaps in the fence, mossy posts, grass and moss through the path, a few stones askew or fallen.
- **Sunken:** the plots have settled into hollows, the path is muddy, half the stones lean, more of the fence is down.
- **Collapsed:** most of the fence down, posts broken off, a scattered gravel and cobble path, many stones fallen, and open graves showing the dead below.
- **Nearly gone:** a few stones and scraps of fence in the grass, the plots grassed over, no path, no lantern. The dead are still there.

**The resting dead** (owner): a yard grave holds a skeleton that never wakes, whatever the noise, bell or robbery. Most are dressed in leather dyed in muted burial colours (undyed brown, greys, charcoal, a dark green, a dull blue, black, linen). Rob them and mine them freely; a sleeper mined with a Silk Touch pickaxe stays one of the resting dead and sleeps wherever it is placed (decoration). The creative tab has one. Crypt sleepers still wake.

**Crypts** (under some yards, beyond the far end): bigger yards have richer crypts.
- **Pauper's ossuary** (small and medium yards): a cramped tunnel of cobblestone, packed mud and dirt with bones in the walls, lined both sides with niches, almost every one holding a sleeper with little or nothing on them, some stacked two high. A candle or two, one chest of poor things (bones, rotten flesh, string, coal, iron nuggets, seeds, leather, rarely an emerald).
- **Family vault** (as before): stone brick, niche sleepers, candles, a chest.
- **Noble crypt** (medium and large yards): polished andesite and chiseled stone. An entry hall, a pillared chamber with a sarcophagus in the middle (an armoured sleeper under a stone lid) and two chests at the far wall, and from the chamber a side hall each way lined with niches of sleepers in full iron, chainmail or gold armour, some with iron swords or axes. Candlelight only. The chests hold gold, emeralds, iron, a golden apple, sometimes diamonds, enchanted books, an enchanted iron sword or golden helmet, a name tag, rarely a music disc; with Bubba's Totems & Artifacts, a Grave Talisman (25 %) and a Totem of Warding (12 %).

**Commands (ops):** `/bubbas_reworked_mobs graveyard build <lone|small|medium|large> <kept|overgrown|sunken|collapsed|nearly_gone> <none|pauper|family|noble>` builds that kind with its gate at your feet (`graveyard place` stays).

## How it's built

- `grave/YardKinds`: `Size` (LONE 7-9, SMALL 11-13, MEDIUM 15-19, LARGE 21-27; crypt chance 0 / 0.5 / 0.6 / 0.8; picked 20/30/35/15 %), `State` (the numbers per state: bars gone, one high, mossy and broken posts, posts gone, stones askew, fallen and gone, open graves, flowers; picked kept 15 %, overgrown 30, sunken 20, collapsed 20, nearly gone 15), `Wealth` (PAUPER, FAMILY, NOBLE; by size: lone/small 50/50 pauper or family; medium 20 % pauper, 60 % family, 20 % noble; large 40 % family, 60 % noble).
- `CryptYardStructure` picks size, crypt, wealth and state; `CryptYardPiece` saves `State` and `Wealth` (pieces saved before step 2 read as overgrown and family). The piece's box is sized by wealth (pauper reaches 3 either side of the gate's line and 12 long; noble 12 either side and 14 long). Every roll is still made before reading the world.
- **The resting dead:** `SleeperBlock.RESTING` (block state, default false). `SleeperBlockEntity.disturb` returns 0 and `scheduleWake` does nothing for a resting sleeper, and `SleeperWaking.wake` refuses one: that covers noise, bells and robbing, and anything that schedules a wake. The Silk Touch loot copies `resting` with `pose` (datagen), so the item's `block_state` carries it. Yard graves place resting skeletons via `Build.restingSkeleton` (leather 70 %, `GraveTuning.RESTING_LEATHER_CHANCE`: helmet and chest, legs 60 %, boots 40 %, one of eight colours; a bow 15 %). The old skeleton/zombie roll is kept as an unused roll so later rolls don't shift.
- Loot (26.3 format): `chests/crypt_pauper` (2-5 rolls), `chests/crypt_noble` (4-8 rolls); Totems: `TotemsTuning.TALISMAN_LOOT` adds crypt_noble 25 %, `WardingTuning.LOOT` adds crypt_noble 12 % (keyed by id, no dependency; the old TODO there is resolved).
- Removed now-unused `GraveTuning.YARD_MAX_*`, `CRYPT_CHANCE`, `SKELETON_SHARE`.

## Builder's choices (owner may change)

1. **Shares** of sizes, states and wealths above; lone plots never have a crypt (too small for the stairs).
2. **Sunken** = plots settled one block into a hollow, the sleeper a block lower; stones lean (rotation), not sunk (a gravestone sunk one block would be hidden).
3. **Open graves** (collapsed yards) show the resting dead; buried loot isn't placed in open or sunken plots.
4. **Nearly-gone yards keep their dead** (75 % of graves) but lose their lantern.
5. **Ossuary niches** sometimes stack a second sleeper two above the floor niche.
6. **Noble armour:** 85 % of hall sleepers and the sarcophagus's (100 %) wear a full set: 15 % gold, 40 % chain, 45 % iron; zombies carry iron swords or axes 60 %. No enchanted gear on them (that's in the chests).
7. **The resting dead** are skeletons only and keep the "Sleeping Skeleton" name as items (no separate name or tooltip).
8. **Not built:** standalone crypts with a collapsed hillside entrance (the spec's other way in), and husk/stray sleepers by biome (those sleeper variants don't exist yet).

## Tests

- `:bubbas_reworked_mobs:build` passes (31 core). The core yard test now also checks every yard-grave sleeper is a resting skeleton.
- Milestone `GravesGameTest.restingDeadNeverWake` (new): an explosion three blocks off wakes an ordinary sleeper (control) but not a resting one; neither does a bell rung beside it, nor robbing by a player (at sensitivity 10, where an ordinary robbery wake is certain); mined with Silk Touch the item keeps `resting`, placed again it is still resting and sleeps through another explosion and 40 more ticks. `generatedTextIsGlyphsOrBlank` and `gravestoneIsASign` pass.
- Existing sleeper milestone classes pass: BubbasReworkedMobsGameTest (13), SleeperNoiseGameTest (16), LootDisplayGameTest (8).
- Client part `yards` (new, `YardKindsClientGameTest`): every state, a lone plot, and pauper/family/noble yards on sky platforms: `yard-<kind>-above`, `yard-<kind>-gate`, `crypt-<wealth>` (night vision), `crypt-<wealth>-dark`, `crypt-noble-hall`. Looked at: kept, collapsed, nearly-gone (above), collapsed and noble (gate), pauper, noble and noble-hall crypts.
- Not yet tested (objectives): each wealth's chest rolls its own table in generated worlds (checked by eye); natural generation picks every state and size (shares are code, not measured).
