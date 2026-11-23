<!-- dte:C1 -->
# Spiders: docs report, step 2 (2026-11-22, builder: agent 3)

Step 1 (cave nests) was merged into specs/spiders.md. This is step 2, for the orchestrator or docs agent to merge into `specs/spiders.md` and `player-docs/`. Numbers are in `SpiderTuning.java`; **(builder)** marks the builder's choices.

## Step 2: giant spiders and spun webs (built; 11 spider tests and 32 core tests pass; screenshots checked; the owner saw the webs)

### Owner decisions made during the build (2026-11-22, for the spec)

- **Spun webs replace the plain cobweb spans** round the cave nests and between trees. The nests keep their cobweb core.
- **Not an orb web** (no hub, spokes or spiral): "for minecraft fitting-in reasons", a tangle of thick strings stretched diagonally between many random anchor points, criss-crossing through the space. Seeded per site, so it's stable.
- **Big webs thick and clearly visible**, then "probably too thick", so they were trimmed.
- **Pixelated, not smooth:**
  - Smooth strands "look out of place next to vanilla webs" and went "much yellower in shadow".
  - The pixel version: "MUCH better, keep at this angle".
  - This replaces the spec's "fine translucent strands with a faint shine" wording.

### Player-facing behaviour

- **Giant spiders** live in old-growth pine and spruce taiga. About 1.75x a normal spider (vanilla's scale attribute); they still fit between the trees. They:
  - spawn like other monsters, in the dark, and never within 48 blocks of another giant;
  - roam the forest;
  - every 20-40 s while roaming, spin a web between two nearby tree trunks. They stop once 5 webs are within 12 blocks;
  - snatch livestock: any passive animal they reach (pig, sheep, cow, chicken, goat; not tamed pets) is grabbed and hauled up a trunk into a web. It's stuck there in a cobweb while the giant feeds;
  - go for nearby livestock before players, and turn from a player to an animal close by. Not if that player hurt them in the last 5 s;
  - call 2 little spiders when hurt: half size, 3 hearts, a weak bite. At most 4 alive, and a 10 s wait between calls;
  - take no fall damage (they come down on silk);
  - drop more string (3-6) and spider eyes (1-3) than a vanilla spider. This is the default until the owner decides on a unique drop.
- While a giant hauls an animal, hitting it loosens its grip (the drowned rules). At 0 it drops the animal.
- **Spun webs:**
  - **What they are:** a tangle of strands between points on trunks, branches, leaves, the ground, and cave ceilings and walls.
  - **How they look:** drawn in pixels in the cobweb texture's own white and greys, lit like the cobwebs around them. A few strands are heavier guy-lines, two pixels wide.
  - **Tie-offs:** where a giant's strands meet a trunk, it ties them off with a real cobweb block. A cave nest's tangle runs out from its cobweb core to the ceiling and walls.
  - **In play:** walking through strands slows you a little (less than a cobweb); spiders aren't slowed. 3 hits bring a web down, shears cut it cleanly for 2 string, and fire burns it away.
  - **Ageing:** webs tear over time and turn ragged after about 5 in-game days. Torn strands hang loose from where they're tied.

### Numbers (SpiderTuning)

| What | Value |
|---|---|
| Giant | scale 1.75, health 40, attack 5, speed 0.3, step height 1, spawn weight 12 (monster), spacing 48 |
| Snatch and lure | sees animals 16 blocks off; lure radius 10; lure ignored for 100 ticks after a player hurt it |
| Haul and feed | larder search 14; web 4-7 above the trunk base; climb 0.15/tick; gives up hauling after 300 ticks; feeds 300 ticks, a 2-damage bite every 30 |
| Spinning | every 400-800 ticks; trunks 3-7 apart, at least 8 tall, within 8 blocks; cap 5 webs within 12 |
| Little spiders | 2 per call, cap 4, cooldown 200 ticks, scale 0.5, health 6, attack 1 |
| Tree web | 22 rays, reach 7, up to 14 strands, 3 cobweb tie-offs |
| Nest web | 1 per nest; 26 rays (mostly up), reach 11, up to 16 strands, anchored in the cobweb core |
| Strands | at least 2.5 blocks long; slow movement x0.7; 3 hits cut; 2 string from shears; ragged up to 45% over 5 days; +25% torn per hit |

### Builder's choices

- **(builder) Spun webs are an entity (`spun_web`), not blocks (B15).** The client draws each strand as a stepped line of opaque square pixels:
  - Snapped to the world grid at the loaded cobweb texture's texel size: 1/16 with vanilla, 1/32 with a 32x pack such as Faithful, so they always match the cobweb blocks beside them.
  - Coloured with the three colours of vanilla's cobweb texture.
  - Lit per pixel (sky and block light), refreshed every 2 s, with no glow floor. That fixed the yellow.
- **The drop thread** (orchestrator, passing on the owner's rule): drawn the same way as the webs, as a crisp one-texel pixel line in the cobweb palette, lit by world light, taut from the spider's back to where it's tied in the nest. It replaces the smooth thin line. Shared code: `client/spider/PixelStrands`.
- **(builder) How strands are chosen:**
  - Random rays from the site's centre find anchors on blocks.
  - Strands join pairs of anchors at least 2.5 apart, with a clear line between them, passing near the centre, so they cross each other.
  - Each anchor holds at most 3 strands; 30% are heavy two-pixel strands.
  - Strands sag about 6% of their length, less when steep.
- **(builder) Larders:** with no web of its own in reach, a giant makes a cobweb by the nearest trunk, 4-7 blocks up. The catch sits in that cobweb block, stuck in it (vanilla behaviour).
- **(builder) Trunk finding** scans for log columns from the spider's level up, with no heightmaps, so overhangs and canopies don't fool it.
- **(builder) Little spiders** are vanilla spiders with smaller scale, health and attack attributes, tagged so the cap can count them. They despawn like normal mobs.
- **(builder) By day,** giants attack players only in the dark, like vanilla spiders, but take livestock any time. The spec's "what it does by day" stays open.
- **(builder) Webs are never cleared** except by players, fire or explosions. The area cap keeps them from piling up.

### Tests

- **Core:** `giantSpiderIsLargeAndSpinsBetweenTrees`.
- **Milestone (`GiantSpiderGameTest`):**
  - `snatchesLivestockIntoItsWeb`: a pig is hauled up into a cobweb larder and the giant feeds.
  - `livestockLuresItOffAPlayer`.
  - `hurtCallsLittleSpiders`: 2, then 0 during the cooldown; small and weak.
  - `websSlowAndCut`: touching a strand slows; 3 hits cut it; shears give 2 string at the player.
- **Client part `webs` (`-PclientTests=webs`):**
  - Scenes: a five-trunk spruce clearing with three giant webs (close, far, under the canopy, at an angle, torn) and a dark cave nest (torchlight, night vision, close), at 16x and 32x.
  - All screenshots looked at. The owner saw trees close, trees far and the torch-lit cave.
- **Milestone objectives (no test yet):**
  - Giants spawn only in old-growth taiga.
  - Spinning stops at the area cap.
  - Fire burns a web.
  - A broken grip during a haul drops the animal.
  - Webs tear with age.
