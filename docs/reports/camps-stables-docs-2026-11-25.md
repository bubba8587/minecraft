<!-- dte:C1,B15,B17,B21 -->
# Camps: stables, chest tiers, room stairs fix (builder's report for the docs merge, 2026-11-25)

Spec: `specs/monster-camps.md` sections "Stables", "Chest tiers", "Room stairs fix" (the three sections are updated to as-built in the same change; this report holds what the player docs need).

## Player-facing changes (for `player-docs/`, Reworked Mobs camps page)

- **Stables.** Every large camp and about half of medium camps have a stable: a small roofed pen of the camp's wood (log corner posts, a plank back wall, a fence rail with a gate facing the fire, a slab roof, hay bales at the back). Inside are one or two mounts tied to a post with leads:
  - sometimes the camp's undead mount: a zombie horse (zombie camps), a skeleton horse (skeleton, stray and bogged camps) or a camel husk (husk camps);
  - sometimes a captive living horse or donkey.
  - Click the lead knot on the post to take the leads and walk the animals home (vanilla behaviour). The undead mounts stay in the shade of the roof; a zombie horse led out into the sun burns like any zombie horse.
  - Stable animals aren't part of the camp's monsters: killing or taking them doesn't count toward clearing the camp. When the camp's monsters come back after a reset, an empty stable is restocked, as long as its post still stands (break the post and it stays empty).
- **Chests, in rising order:** the chest by the fire < the chest in the underground room < the winding tower's chest < the skull rock's chest.
  - The chest by the fire has a little less in it than before (fewer stacks, a lower chance of a good item, no diamonds).
  - The underground room's chest is a little better than before (its bonus item is more likely and can be an enchanted sword, bow or golden apple).
  - **Winding towers** (some large camps) hide an archer's stash on the sealed ground floor of the tower: you can see it through the slit windows; break in to reach it. Arrows, tipped arrows (slowness, poison, weakness), a bow or crossbow (sometimes enchanted), a spyglass now and then, some iron and gold, an emerald or two.
  - **Skull rocks** (full and half-buried) have a chest at the back inside: always one enchanted iron weapon or armour piece (or bow), gold, emeralds, often an enchanted book or a golden apple, and rarely a diamond or two.
- **Room stairs:** camp monsters (leaders included) now climb out of the underground room easily. The stair tunnel has more headroom and opens to the sky from its first step; its top meets the ground flush, also where the land rises or falls round the camp.

## Numbers (all in code; `CampTuning` unless named)

| What | Value |
|---|---|
| Stable in a large camp | always (every one of 160 sampled plans) |
| Stable in a medium camp | `MEDIUM_STABLE_CHANCE` 0.5, from the plan seed |
| Stable size (`CampPiece.STABLE_HALF`, `STABLE_DEPTH`) | 7 wide x 5 deep, ring included; inside 5 x 3; roof 4 above the floor |
| Stable placement | after the lean-tos, before the towers, 80 tries, front 4 to 0 blocks inside the clearing's edge (`r - 4` to `r`) |
| Mounts per stable | 1 to `STABLE_MOUNTS_MAX` (2) |
| Undead mount chance (each) | `STABLE_UNDEAD` 0.5; otherwise donkey `STABLE_DONKEY` 0.3, else horse |
| Tunnel headroom (`CampPiece.TUNNEL_HEADROOM`) | 4 above every step (was 3) |
| Way out (`MOUTH_LEVEL`, `MOUTH_SLOPE`) | 3 cells level with the top step, then up to 6 cells rising at most a block each |

## Chest expected values (per chest; `tools`-style EV: rolls x weight share x mean count)

**Fire, `chests/monster_camp`** (2-4 rolls + one 44 % roll): arrows 3.75, bones 2.5, rotten flesh 1.9, string 0.78, leather 0.5, bread 0.5, iron ingot 0.38, gunpowder 0.23, gold ingot 0.19, emerald 0.09; enchanted bow 0.09, enchanted iron sword 0.075, enchanted iron axe 0.056, chain helmet 0.047, chain chestplate 0.037, saddle 0.047, name tag 0.037, enchanted book 0.037, golden apple 0.009. No diamonds (was 0.05).

**Lower room, `chests/camp_room`** (2-4 rolls + one 62 % roll): arrows 2.1, bones 1.8, iron nuggets 1.6, gold nuggets 1.3, rotten flesh 1.1, coal 0.88, string 0.57, iron ingot 0.31, candle 0.18, gold ingot 0.15, emerald 0.15, golden carrot 0.13, enchanted book 0.077, enchanted iron sword 0.051, enchanted bow 0.051, golden apple 0.013.

**Winding tower, `chests/camp_tower`** (3-5 rolls + a weapon roll 85 % + an extra roll 58 %): arrows 12, string 1.75, feathers 1.6, tipped arrows 2.0 (slowness 0.8, poison 0.6, weakness 0.6), iron ingot 0.8, flint 0.75, gold ingot 0.6, bread 0.6, emerald 0.45, golden carrot 0.33; bow 0.3, enchanted bow 0.2, crossbow 0.2, enchanted crossbow 0.15; spyglass 0.25, enchanted book 0.083, name tag 0.083.

**Skull rock, `chests/camp_skull`** (3-5 rolls + one sure gear piece + one 73 % roll): bones 4.5, arrows 3.9, gold nuggets 3.8, rotten flesh 1.7, gold ingot 1.6, iron ingot 1.3, emerald 1.1, golden carrot 0.81; one enchanted piece (levels 5-15): iron sword 0.21, iron axe 0.14, bow 0.14, iron helmet 0.14, iron boots 0.14, iron chestplate 0.11, iron leggings 0.11; enchanted book (levels 5-15) 0.40, golden apple 0.27, **diamond 0.10** (1-2, 1 roll in 15). No gems, no enchanted golden apples, no totems.

Per camp this adds little: winding towers are in 1 large camp in 4 (5 % of camps), skull rocks in 1 large camp in 3 (about 7 % of camps), so the diamond EV per average camp drops from about 0.05 to about 0.007.

Relic map shreds: `bubbas_totems_artifacts` `RelicTuning` adds shreds to `chests/monster_camp` and `chests/camp_room` only. The new `camp_tower` and `camp_skull` get none; whether they should is for the orchestrator or owner (an entry there, another mod).

## Root cause of the stair problem (for the spec's as-built note)

Reproduced with real walks and path checks over every kind and size: a leader-sized zombie (1.25x) found no path out of the room in 55 of 108 camps, and normal zombies stalled on the steps when they hopped.
1. **Headroom:** the tunnel cleared only 3 blocks above each step, and the first steps sat under the room wall's top and the untouched ground block. A mob climbs stairs by hopping, and while stepping up its back half is still under the previous cell's lower ceiling, so it bonks; a 2.44-tall leader can't step up at all. Now 4 above every step (and above the doorway cell), which opens the tunnel to the sky from its first step.
2. **Falling ground:** with the doorway cell cleared higher, trodden gravel or sand above it could fall into the doorway. Any sand or gravel left over a tunnel cell is now turned to the kind's rock.
3. **Bones by the door:** the room's random bone blocks could land in the row by the stairs and wall off the doorway for a leader. No bones in that row now.
4. **Mouth on uneven land:** the tunnel's top step is at the camp's centre height; where the land at the mouth was higher, a block of ground stood in front of the top step (not visible on flat test ground, only on natural terrain). The mouth is now cut or filled flush for 3 cells, then may rise a block per cell up to the land (only earth is cut, never under something built).
Also: a narrow room (one wide toward the stairs) no longer shifts away from the stairs, so the tunnel's first open step stays clear of the fire's seats; the reserved stair footprint starts at the first step.

## Builder's choices

- Stables are placed from their own random stream (`seed ^ 0x57AB1E`), after the lean-tos and before the towers, so the main layout stream is unchanged; pieces placed after it fit round it. Every large camp got one in 160 sampled plans; a medium stable that can't fit is dropped.
- All mounts are leashed to the one post (the hoglin pen leashes only the first) so none wander and every undead mount stays under the roof; a player takes all the leads at once from the knot (vanilla).
- Undead mounts don't burn under the roof because sky light under it is below 15 (vanilla's rule). The client shot waits for the light to settle after building, as a natural camp's has long before its roster appears.
- Mount mix is rolled per animal when the stable is stocked (level random), not from the plan seed.
- The tower chest is on the core's sealed ground floor (no doorway reaches it; the fence window slits show it), facing into the core from a corner picked by the tower's seed. "Hidden" read as "you have to break in".
- The skull chest stands on the furthest-back hollow floor cell beside the middle path (never on the path the walk test uses), facing the mouth; it isn't a shade spot.
- Fire chest: fewer rolls (2-4) and a lower good-roll chance (44 %), diamonds moved out to the skull chest. The room chest had to move up to sit above it: its bonus roll is now 62 % (was 42 %), adds enchanted sword/bow and a rare golden apple, emeralds 1-2.
- Small camps could end up with no banner once the stair footprint grew (seed 7 lost its only one): a banner that finds no spot in 16 tries now tries 48 more a block further out. `campPiecesNeverOverlap` now also checks every plan has a banner and a lean-to.
- The test field's surface heightmap (WORLD_SURFACE_WG) is kept as first read in a live chunk, so the room-climb test re-primes it after reshaping the field.
- The spec's `CampSite.hasStable()` is saved as `stable` in the camp's data (old saves read false).

## Tests

- Milestone `CampsGameTest.zombieCanClimbOutOfTheRoom`: every kind and size, 3 seeds each plus once on land rising round the camp (non-Nether kinds); a zombie and a leader-sized zombie in the room path to the clearing beside the fire, and the two cells past the top step are ground with 2 open above. Failed on the old code (55 leader-sized faults in 108; 15 not-flush faults on the slopes); passes now.
- Milestone `CampsGameTest.stablesHoldTetheredMounts`: stables in every large plan, 30-70 % of medium, none small or Nether; built large camps of each overworld kind: 1-2 mounts of the right types, leashed to the post's knot, persistent, under the roof, off the roster; not re-made while some remain; restocked when empty; none once the post is broken.
- Milestone `CampsGameTest.towerAndSkullChestsHoldTheirLoot`: the winding tower, skull rock and buried skull chests exist where expected with their loot tables, two kinds each, and can open.
- `campPiecesNeverOverlap` also checks a banner and a lean-to in every plan.
- Client part `camps` (also alone as `campextras`): `camp-stable-<zombie|husk|skeleton>-close/-clearing`, `camp-skull-chest`, `camp-buried-chest`, `camp-tower-chest`, `camp-stairs-up-<skeleton|husk>`.

## Milestone test objectives (for the spec)

- A zombie and a leader-sized zombie in a camp's room can path up the tunnel to the clearing by the fire, every kind and size, also on rising land; the way out is flush with the top step (`zombieCanClimbOutOfTheRoom`).
- Stables: every large camp, about half of medium; 1-2 tethered mounts of the kind's undead mount or a horse or donkey, under the roof, off the roster, restocked only when empty with the post standing (`stablesHoldTetheredMounts`).
- Tower and skull chests stand where expected with their loot tables (`towerAndSkullChestsHoldTheirLoot`).
