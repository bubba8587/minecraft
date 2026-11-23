# Nether outposts: docs report (2026-11-24)

Builder report for `specs/monster-camps.md` and the Reworked Mobs player page. Governing leaf: C1 reworkedMobs (no new leaf; everything below is spec).

## Owner's asks (2026-11-24)

- Nether outposts as a kind of monster camp, less common than overworld camps. Piglins and piglin brutes, hoglins kept as livestock (penned and tethered).
- Materials: blackstone, polished blackstone bricks, gilded blackstone, gold, crimson wood, chains, soul and regular lanterns. Bastion as reference.
- Biomes: crimson forest and nether wastes only.
- Addition: a lower room with a Nether portal, ruined-portal style (some lit, some unlit, some broken, crying obsidian). Take layout ideas from bastions and fortresses.

## What was built (spec facts)

- **Kind:** `CampKind.PIGLIN` (`camp locate piglin`, `camp build piglin <size> [seed]`). Same `CampPiece`, footprint plan, roster, refill and banners as overworld camps.
- **Structure:** a new structure type `bubbas_reworked_mobs:nether_outpost` (`NetherOutpostStructure`). It finds its own floor under the ceiling and builds the PIGLIN kind of `CampPiece`.
  - Structure set `nether_outposts`: random spread, spacing 22 and separation 8 chunks, salt 20261124, kept 3 chunks from `nether_complexes` (bastions and fortresses). Overworld camps use 14/5. Terrain rejects many candidates, so outposts are much rarer: seed 20261116 had 5 outposts within 2000 blocks of 5 points.
  - Biome tag `has_structure/nether_outpost`: crimson_forest and nether_wastes.
    - Not basalt deltas: the terrain is too jagged for a level clearing.
    - Not soul sand valley or warped forest: piglins don't spawn there in vanilla, and they are repelled by warped fungus.
- **Placement under a ceiling** (all from noise columns, before any block exists):
  - The centre column is scanned for floors at least 10 above the lava sea (y >= 42). A floor is solid ground with 12 air above it, below the generation depth, so never the bedrock roof. One floor is picked at random.
  - Columns every 3 blocks out to radius + 4 (the gate, rampart and pen stand at the edge) each need a floor within ±5 with 5 air above it.
  - Floor spread across the outpost is at most 5.
  - No lava from 9 below to 2 above any sampled floor.
- **Building under a ceiling** (`NetherBuild`, a `ClippedBuild` whose ground is the surface nearest the outpost's height; a lava surface counts as no ground):
  - The clearing is levelled to the outpost's height: fill under, cut over, natural floor blocks kept.
  - Headroom is carved 12 up.
  - Lava within radius + 1.5, from 9 below to 12 above, is sealed to blackstone (lava springs come before our generation step).
  - A level walkway runs out round the stair tunnel to its mouth, and a 5-wide approach leads out of the gate.
- **Materials (PIGLIN):**

  | Part | Block |
  | --- | --- |
  | Posts | crimson stem |
  | Platforms | crimson planks |
  | Roofs | polished blackstone brick slabs |
  | Railings | nether brick fence (fortress) |
  | Ramps | crimson stairs |
  | Tunnel stairs | polished blackstone brick stairs |
  | Ground | blackstone, netherrack, soul soil, polished blackstone |
  | Outcrops | polished/cracked blackstone bricks, blackstone, gilded blackstone, gold blocks jutting out instead of bones |
  | Fire | unlit regular campfire on a gilded blackstone ring |
  | Pikes | piglin heads, or lanterns and soul lanterns |
  | Bridge deck | nether bricks, nether brick slab sag (chains still end on posts) |
  | Banner | black with a yellow piglin snout and an orange triangles-bottom |

- **Pieces taken from bastions:**
  - A gilded gate in a stretch of rampart: thick polished blackstone brick pillars capped with gilded blackstone and gold, a chiselled keystone and gold over a 3-wide arched opening, cracked brick rampart wings with blackstone wall tops, and soul lanterns hung under the arch.
  - The hoglin pen (a nod to bastion hoglin stables).
- **Taken from fortresses:** nether brick fence railings, nether brick bridge decks.
- **Hoglin pen:** a closed 5x5 ring of crimson fence on crimson nylium and soul soil, with a crimson fungus or two. The tether post in the middle is a fence with a lantern on top.
  - Hoglins per pen: 1 small, 2 medium, 3 large. The first is leashed to the post.
  - All are tagged livestock and set "cannot be hunted", as in a bastion stable (mixin `HoglinHuntInvoker`).
  - They are not on the roster and not counted for clearing. They are re-made with the roster only when the pen has none left and its post still stands.
- **Portal room:** the overworld room, deeper (floor 7 down, 5 high, always 7x7 inside), reached by a longer stair tunnel.
  - A 4x5 obsidian frame stands at the back, facing the stairs, its whole frame inside one chunk. Its state comes from the outpost's seed:
    - 40 % lit.
    - 30 % whole but unlit, with 1 to 3 crying obsidian.
    - 30 % broken: 1 or 2 frame blocks gone, crying obsidian, unlit.
  - Netherrack and magma sit at its foot. The magma is only beside the frame, not in the walkway.
  - A lit portal works as vanilla (using it may create an overworld portal).
- **Roster:**
  - Piglins. Ground members are a piglin brute 25 % of the time.
  - Lookouts on towers hold crossbows. Other piglins hold a crossbow or golden sword, and brutes a golden axe.
  - Piglins keep vanilla behaviour: they are neutral to players wearing gold.
  - When built outside the Nether (the command, tests), piglins and hoglins are made immune to zombification.
- **Leader:** a piglin brute, scaled and toughened like overworld leaders. It wears golden helmet, chestplate and leggings with Protection II, and holds a golden axe with Sharpness III. Drop chances are the same as overworld leaders.
- **Light:** outposts are lit (lanterns, a lit portal). Overworld camps stay dark; the "camps are dark" test now skips the piglin kind.
- **Loot:**
  - `chests/nether_outpost` (by the fire): gold nuggets and ingots, crimson fungus, raw and cooked porkchops, leather, quartz, arrows, string, golden carrots, a worn crossbow, and a 1-in-4 chance of the outpost's banner.
  - `chests/nether_outpost_room`: gold, gilded blackstone, enchanted golden sword/helmet/chestplate, obsidian, crying obsidian, flint and steel, fire charges, porkchops, a gold block, the piglin banner pattern, a snout trim template, and rarely netherite scrap.
- **Not in outposts:** the skull rock, winding tower and ribcage (overworld only).

## Numbers for the owner to playtest

These are tuning values in `CampTuning`, `NetherOutpostStructure` and the structure set, and can change freely:

- spacing 22 / separation 8
- floor at least 10 above the lava sea, headroom 12, slope 5
- brute chance 25 %
- hoglins 1/2/3
- portal 40/30/30

## Player-facing (for the Reworked Mobs page)

- Piglin outposts now appear in crimson forests and the Nether wastes. They are rarer than overworld camps.
- An outpost has watchtowers, a gilded blackstone gate, and a hoglin pen with a hoglin tied to a post.
- Under it is a room with a Nether portal: sometimes lit, sometimes broken.
- Its piglins ignore you if you wear gold, as usual. Their leader is a big piglin brute in enchanted gold.
- Like other camps, an outpost's banner is its key, and it can come back after being cleared.

## Tests

- **Server (milestone, `CampsGameTest`):**
  - `netherOutpostBiomes`
  - `outpostRosterAndPennedHoglins` (roster, brute leader, hoglins penned, one leashed, not huntable)
  - `outpostGateCanBeWalkedThrough`, `outpostRampCanBeClimbed`, `outpostPortalRoomCanBeReached`
  - `outpostPortalsMatchTheirState`
  - Existing camp tests now include the piglin kind automatically: `campPiecesNeverOverlap` (footprints; gate and pen are new footprints), `campFencesEndCleanly`, `everyCampBlockIsSupported` (minus the dark check).
- **Client part `outposts` (`OutpostsClientGameTest`, seed 20261116):** natural outposts in the Nether. Each one is checked for:
  - Biome.
  - No lava within radius + 1, 8 below to 3 above.
  - A piglin can path from the fire to the ramp's platform, the gate and down the tunnel.

  Each one is shot from above, close, side, tunnel, gate and portal. The hoglins are checked as penned and leashed, and the roster and pen are shot.
- **Milestone test objective:** add a line for `OutpostsClientGameTest` under the spec's objectives.
- **Natural test world:** `tools/build-naturalworld.py` gets a "Nether outpost (piglins)" sign. The outpost is found with `camp locate piglin` run in the Nether, its chunks are force-loaded there, the sign teleports into the Nether, and Nether chunks are pruned like overworld ones. This was not run (the test world build doesn't run from a worktree); the script is syntax-checked only.

## Open questions for the owner

- Should outposts be rarer or more common? The current setting is spacing 22, plus terrain rejection.
- Should piglins be allowed to hunt the penned hoglins (more vanilla, but the livestock dies)? They currently can't.
- Should the netherite scrap stay in the room loot (weight 1 of about 90)?
