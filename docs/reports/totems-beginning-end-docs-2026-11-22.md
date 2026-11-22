<!-- dte:C6,C1 -->
# Totems of the Beginning and the End: docs report (builder, 2026-11-22)

For the orchestrator to merge into `specs/totems-artifacts.md`, `specs/endermen.md` and the player docs, then delete. Branch `totems-beginning-end`.

## Player-facing (for player-docs)

**Totem of the Beginning** (rare, one per stack, never crafted)
- Use it (right-click) and it's used up: the ground around you ripples back, outward, to how the world first generated it. Holes, creeper craters and strip mines fill in with the ground that was there (grass on dirt on stone...). Piles and pillars of dirt, stone or sand go. Sand or gravel left on the top layer where grass was turns back to grass.
- It never touches what you built. It changes only air and natural ground blocks (dirt, stone, sand, gravel, clay, mud, snow block, terracotta). It never changes logs, leaves, ores, flowers, water or lava. Anything built or placed (planks, glass, torches, rails, chests, cobblestone, farmland, paths...) keeps its own column and every column within 2 blocks of it untouched, top to bottom. So a house in a pit keeps its pit, and a mine with torches or rails keeps its tunnel around them.
- It doesn't bury anyone: no block goes where a mob or player is standing.
- Only in the Overworld, and only with Bubba's Reworked Mobs installed. Anywhere else, or without that mod, using it does nothing and it isn't used up.
- Can't bring back: trees, ores, flowers, water and lava (the world's features can't be worked out again from the seed, only its terrain). Burnt forests get their ground back, not their trees.
- In a Reworked Beacons beacon (any pyramid; with Reworked Mobs): endermen come to the beacon's area like a cairn and tidy the ground. Screen line: "Endermen come to tend the ground". Without Reworked Mobs the beacon doesn't take it.
- Found: endermen at some finished cairns leave one at the cairn's foot as an offering, lying on the ground. Take it with an empty hand. Each such cairn gives one once, ever.

**Totem of the End** (rare, one per stack, never crafted)
- Held in either hand: endermen treat you as one of their own. Looking one in the eye doesn't anger it (every enderman, End ones too). With Reworked Mobs, breaking a cairn or fighting near one doesn't turn the cairn's endermen on you either. Hitting an enderman still makes *that* enderman fight back (vanilla).
- Use it (right-click) to teleport to the block you're aiming at, up to 32 blocks away. It isn't used up; a 4-second cooldown sweep shows after each jump. Aiming at nothing, or at a spot with nowhere safe to land, does nothing and starts no cooldown.
  - Aim at a top face: you stand on that block.
  - Aim at a side face: you land on top of that block if there's room. If not, you drop straight down from in front of it to the nearest safe ground below.
  - Aim at an underside: you drop straight down from under it to the nearest safe ground.
  - Safe: you fit there, and there's no lava, fire, campfire, powder snow, magma, cactus, berry bush or wither rose in or under the spot. Never into the void. Water is fine.
  - You hop off anything you're riding, keep facing the same way, take no fall damage from the jump, and there's an enderman's teleport sound and portal particles where you leave and where you land.
- Found: rarely in End city chests (5 % per chest). With Reworked Mobs, also given once per cairn, ever, to the player who places the last missing block of that cairn. Breaking and rebuilding the cairn never gives another. If the endermen finished it themselves, the first player to break a block and put it back gets it (and has angered the endermen, unless they're holding a Totem of the End already).

## Numbers

| | Value | Where |
|---|---|---|
| Beginning ripple radius | 24 blocks, a cylinder 24 up and 24 down from your feet | `TotemsTuning.BEGINNING_RADIUS` |
| Beginning ripple speed | 1 block every 2 ticks (about 2.4 s to the edge) | `TotemsTuning.BEGINNING_TICKS_PER_BLOCK` |
| Build protection | a column is skipped if it or any column within 2 holds a built/placed block | `EndermanTuning.RIPPLE_PROTECT_RADIUS` |
| Waiting for generated terrain | a column not worked out yet is retried for up to 30 s | `EndermanTuning.RIPPLE_PENDING_TICKS` |
| End teleport range | 32 blocks | `TotemsTuning.END_RANGE` |
| End cooldown | 80 ticks (4 s) | `TotemsTuning.END_COOLDOWN_TICKS` |
| End in End city treasure | 5 % per chest | `TotemsTuning.END_LOOT` |
| Cairns that leave an offering | 1 in 2 (picked from each site's seed) | `EndermanTuning.OFFERING_SHARE` |
| Offering timing | checked every 5 s while a player is within 96; 1 in 12 per check (about a minute) once complete with one of its endermen within 16 of the centre | `OFFERING_CHECK_TICKS`, `OFFERING_ONE_IN`, `OFFERING_WITNESS_RANGE` |
| Offering spot | 2 blocks outside the cairn's footprint, side picked by the seed | `OFFERING_GAP` |
| Beacon area draw | 35 % per 5 s, up to 6 caretakers in the area (+8 round it), never within 16 of a player | `NEXUS_SPAWN_CHANCE`, `NEXUS_CAP`, `NEXUS_MARGIN`, `NEXUS_PLAYER_DISTANCE` |

## How it's built (spec facts)

**Totems & Artifacts** (`mods/bubbas_totems_artifacts`)
- Items `bubbas_totems_artifacts:totem_of_the_beginning` (`beginning/TotemOfTheBeginningItem`) and `:totem_of_the_end` (`end/TotemOfTheEndItem`), `stacksTo(1)`, `Rarity.RARE`, in the creative tab after Seeking. No new blocks (B15). No statue (only Warding has one so far; spec says statues come with the owner's designs).
- `end/EndTeleport`: aim = `level.clip` from the eyes, `ClipContext.Block.COLLIDER` (aims through grass and flowers) and `Fluid.NONE` (through water). Landing uses vanilla's `noCollision` for the entity's box (as `randomTeleport` does). Teleport like the ender pearl: `stopRiding`, `ServerPlayer.teleport(TeleportTransition(..., Vec3.ZERO, Relative.ROTATION, DO_NOTHING))`, `resetFallDistance`, `resetCurrentImpulseContext`, `GameEvent.TELEPORT`; sound `ENDERMAN_TELEPORT` and `PORTAL` particles at both ends; cooldown via `ItemCooldowns.addCooldown` (B19).
- `mixin/EndermanKinMixin`: HEAD of `Enderman.isBeingStaredBy` returns false when the player holds the totem in either hand (vanilla's own disguise check is the carved pumpkin on the head; this is the same check for the hand).
- `compat/BeginningBeaconPayload` (entrypoint `bubbas_reworked_beacons:payload`): accepts the totem only when Reworked Mobs is installed; power 1, 1 layer; refreshes the area each pulse for `AREA_LIFETIME_TICKS` (100).
- `compat/SleeperHook` gained `restoreAround`, `drawEndermen`, `setUpEndermen(offering, completionGift, kin)`; `ReworkedMobsSleepers` calls the new api methods and ignores a `LinkageError` from an older Reworked Mobs. `BubbasTotemsArtifacts.onInitialize` calls `setUpEndermen` once.
- Loot: `TotemsTuning.END_LOOT` joins `TotemsLoot` (same additive-pool pattern).
- Textures: `tools/totems-textures.py` (two new grids), rows in `docs/art-todo.md`.

**Reworked Mobs** (`mods/bubbas_reworked_mobs`), new in `api/Endermen` (add, don't change):
- `boolean restoreAround(ServerLevel, BlockPos centre, int radius, int ticksPerBlock)` → `enderman/Ripples`. False outside the overworld or with a generator the target world can't reproduce.
- `void drawIn(ServerLevel, Object source, AABB, int lifetimeTicks)` / `stopDrawing(ServerLevel, Object source)` → `enderman/EndermanNexus` (lapses unless refreshed, like `EndermanWards`).
- `void setKin(Predicate<Player>)` → `enderman/Endermen.anger` skips kin (creative and spectator were already skipped).
- `void setCairnGifts(Supplier<ItemStack> offering, Supplier<ItemStack> completionGift)` → `enderman/CairnGifts`.

`enderman/Ripples`: per column, nearest first as the ring passes: (1) top down, terrain-family blocks above the generated surface where the target is air are removed; (2) bottom up, air where the target is a family block is filled with the target (falling blocks only on a solid floor; never into a cell an entity's box touches, items and XP orbs excepted); a family block of the wrong family at or one below the generated surface, not touching a fluid, becomes the target. Protective blocks (any of them anywhere in the cylinder's height protects its column and every column within 2): anything not `TerrainKinds.isNatural`, any block entity, lava, cobblestone, mossy cobblestone, cobbled deepslate, smooth stone, dirt path, and geode blocks (amethyst block, budding amethyst, amethyst buds and clusters, smooth basalt). Structures and wards: the target world has no opinion, so they're skipped. Columns not worked out yet wait. Effects: reverse-portal particles on changed blocks (at most 48 a tick) and the block's place sound now and then. Updates with `Block.UPDATE_ALL`.

`enderman/EndermanNexus`: per area, every `SPAWN_INTERVAL` (5 s) in the overworld, when cairn spawning is allowed (game rule, spawn_monsters, not Peaceful): below the cap, 35 % to spawn a caretaker (marked as a cairn spawn, no home) at a random column inside the area, on a spawn cell (vanilla spawn placement and darkness rules, not warded, not within 16 of a player). It arrives carrying the original block of a hole it finds in the area (16 random columns, 12 deep, like a cairn filler), else empty-handed.

`enderman/CairnGifts`: saved level attachment `bubbas_reworked_mobs:cairn_gifts` (`offered` and `granted` lists of site origins). Offering: ticked every 100 ticks for sites within 96 of a player (not on the game test server); needs a supplier, the site picked by its seed, not offered, its centre loaded and not warded, the cairn complete, and one of its own endermen within 16. The site is marked before the item is placed, as a Reworked Mobs loot display (`GROUND`, facing the cairn), with portal particles and the enderman teleport sound. Spot: 16 angles round the cairn starting at a seed-picked one, `radius() + 2` out, y from origin+3 down to origin-3, air on a floor that holds a display up and not on or above a design block. Completion gift: `mixin/BlockItemPlaceMixin` (RETURN of `BlockItem.placeBlock`, success only, server player) calls `CairnGifts.placedBy`: the placed block is a design block standing as designed, the site is complete and not marked; then mark, `placeItemBackInInventory`, portal particles and `END_PORTAL_FRAME_FILL`. Nothing is marked while no supplier is set.

## Builder's choices (owner may change)

1. **Ripple radius 24 and speed 2 ticks a block** (about 2.4 s). A cylinder 24 up and down, not a sphere, so each column is done in one go (fills bottom up, so sand and gravel always land on a floor).
2. **Build protection is per column, radius 2, the whole cylinder's height.** Safe rather than thorough: a torch at the bottom of a mine keeps the crater above it too. A mine with rails or torches is only partly filled (pockets round each torch).
3. **Cobblestone, mossy cobblestone, cobbled deepslate, smooth stone and dirt paths count as built** (terrain generation never makes them on its own), so cobblestone houses are safe. Dirt and plain stone builds are *not* recognised: a pure dirt hut standing on the ground can be taken away like a dirt pillar. Say if that should change.
4. **Geodes' insides are left alone** (their shell protects the column), and so are lava pools.
5. **Water and lava are never filled over or put back**; a crater that flooded stays a pond.
6. **A strip mine you're standing in fills round you but not where you stand**, so you can end up in a 1x2 pocket of stone. No block goes where a mob or player is.
7. **The top-layer swap only happens away from water** (like the endermen), so natural sand and gravel by rivers and lakes stay.
8. **The Beginning is used up only when the ripple starts**; the client shows the use animation only in the overworld with Reworked Mobs installed. No message when it does nothing.
9. **Beacon: the totem isn't accepted without Reworked Mobs** (there are no caretakers to draw), rather than taking it and doing nothing.
10. **Beacon area spawns** follow the cairn rules (night, game rule, Peaceful...) but keep only 16 blocks from players, not 24 (beacon areas are often small).
11. **Offerings: 1 in 2 cairns, about a minute after completion with one of its endermen present.** The offering appears with an enderman's teleport sound and portal particles; no enderman walks over and sets it down.
12. **Completion gift when endermen finished the cairn**: the first player to put back a broken block gets it (it's per site, once).
13. **Totem of the End landing:** top face with no room or unsafe → nothing (no fallback downward). Aims through grass, flowers and water (lands on a lake bed, as an ender pearl can). Fire, campfire, powder snow, magma, cactus, berry bush, wither rose and lava count as unsafe. Range 32, cooldown 4 s.
14. **Kin covers eye contact for every enderman** (vanilla, Nether and End too), and Reworked Mobs' cairn anger. Hitting an enderman still makes that one fight you (vanilla); the others near a cairn stay calm.
15. **Rarity RARE** (aqua name), one step above the other totems' UNCOMMON, since one is a once-ever cairn gift and the other is single-use terrain repair.
16. **End city chance 5 %** (the Totem of Keeping is 8 % in the same table).
17. **No totem statue** for either (only Warding has one; the spec waits for the owner's statue designs).

## Tests

- Reworked Mobs core `ReworkedMobsCoreGameTest.endermanRippleRestoresGroundButNotBuilds` (body in `EndermanTotemsGameTest`): a hole fills with grass on stone, a dirt pillar goes, sand on the top layer turns to grass, a 3x3 pit with a plank floor keeps its air.
- Reworked Mobs milestone `EndermanTotemsGameTest`: `completionGiftOncePerCairn` (placing through `ItemStack.useOn`, the real path: an unfinished cairn gives nothing; completing gives one; breaking and rebuilding gives no second; the site is marked), `offeringLaidOncePerCairn` (laid once on display; a player takes it with an empty hand; never laid again, also after it's taken).
- Totems core `BubbasTotemsArtifactsCoreGameTest.endLandingRules` (body in `BeginningEndGameTest`): top face, side with room, side without room → floor in front, slab top, fire on top → nowhere, bottom face → floor under it, miss → nowhere.
- Totems milestone `BeginningEndGameTest`: `endTeleportsWhereAimedWithCooldown` (real use: unsafe aim → FAIL, no cooldown, no move; aim at a block 10 away → there, cooldown on, not used up), `endHeldMakesEyeContactHarmless` (vanilla `isBeingStaredBy`: true without, false with the totem in the off hand), `beginningUsedOnlyWithReworkedMobs` (used up with `-PwithMobs`, PASS and kept without), `endFoundInEndCitiesAndSingleStack`.
- Reworked Mobs client part `endermen` (real default-preset world, seed 20261116): a ripple dry run (`Ripples.dryRun`, changes nothing) at spawn and 40 blocks east of each of the three nearest cairns, radius 24: **0 changes on untouched ground at all four**. Control: after digging a 3x3x3 hole and stacking a 3-high dirt pillar at spawn it finds exactly 30 (9 air→grass, 18 air→dirt, 3 dirt→air). Printed as `[endermen] ripple dry run ...`.
- Client part `totems`: both in the hotbar and held in first person, 16x and 32x (`totems-hotbar`, `totems-held-beginning`, `totems-held-end`, and their `-32x`).

Milestone test objectives not yet written as tests:
- A beacon with the Totem of the Beginning draws caretakers into its area at night and they repair a hole there (needs `-PwithMobs -PwithBeacons`).
- With Reworked Mobs, a player holding the Totem of the End breaks a cairn block and no enderman turns on them.
