# Reworked Mobs, creepers: docs report (2026-11-24, builder)

For merging into `specs/creepers.md` and the player docs page. Code is done and tested.

## 1. Noticing an approach needs sight (owner)

- Running off (FLEE) and the sneaking-up back-off (HIDE) now only start when the creeper can see the player at that moment: a clear line from its eyes to the player's eyes or middle (`LivingEntity.hasLineOfSight` with `ClipContext.Block.VISUAL`, so glass, panes and bars don't block it, leaves and solid blocks do). Checked in `CreeperWatchGoal.noticeApproach` every 10 ticks.
- Consequence: while it is tucked fully behind its cover (the peekaboo hidden place, middle 0.27 past the edge, eyes behind the edge) it doesn't notice. A player who walks straight at it while looking at it makes it duck (peekaboo), and once ducked it no longer sees them, so it may not run. The existing `runsToFartherCoverWhenApproached` test still passes (the half-hidden creeper sees the player coming before or between ducks), but the owner may want to playtest this.
- Spec edits: "Running off (FLEE)": replace "This works through walls and windows (no line of sight needed, builder's choice 11)" with "only when it can see the player (eyes to eyes or middle; glass, panes and bars don't block; fully tucked behind cover it doesn't notice)". Same for "Sneaking up (HIDE)". Remove builder's choice 11 (owner decided) and the BACKLOG/Open question "should it need to see you to notice you approaching".
- The "Senses" line about vanilla `Sensing` (collision, glass blocks) still holds for watching and the startle meter; only approach-noticing uses the visual check.
- New `CreeperMind.lastNoticedApproach()` (game time it last started FLEE or HIDE, -1 never), for tests.

## 2. Closest distance 2 blocks (owner)

- `CreeperTuning.APPROACH_MIN_DISTANCE` 3 -> 2. Numbers table: "Closest peek spot | 2 blocks". Also: first peek spot is now 2-22 blocks from the target, a held spot is dropped outside 1-24 (it was 2-24; the code uses min - 1), creeping closer goes down to 2.
- Fast-movement startle at that range: unchanged and fine. It is speed only (faster than 0.24 b/t within 6 blocks); a walk (0.216) or sneak never counts, so a player walking up to a window 2 blocks from it doesn't set it off. Sprinting, jumping-sprinting or riding past it does, as before. Remove the Open question "is 3 blocks the right minimum distance".

## Tests

- New milestone `CreeperGameTest`: `approachThroughSolidWallGoesUnnoticed` (stone wall between, player walks 5 blocks at it: no FLEE/HIDE; verified it fails with the sight check disabled) and `approachThroughGlassIsNoticed` (same with a glass wall: it notices).
- `runsToFartherCoverWhenApproached` now also asserts it noticed the approach.
- Milestone objectives list: add both new tests.

## 3. Pillar mock-up (owner wants to see before building)

- No feature code. Client test part `pillarmock` (`PillarMockClientGameTest`), runs only when named (`-PclientTests=pillarmock`), never in the full suite; new helper `ClientTests.named`.
- Static scenes, six biomes (plains grass, desert sand, badlands red sand, snowy plains snow block, stony shore stone, mushroom fields mycelium; `fillbiome` plus the skin attachment set directly): a 2-high pillar of the ground block, a 1-wide, 2-deep hole diagonally in front of it, a NoAI creeper with the biome skin half behind the pillar's edge (the `HALF_HIDE_INSET` place as seen from the wide camera), facing the wide camera. HUD hidden, toasts cleared, advancement messages off.
- Shots: plains/desert/snowy wide (15 blocks), six close-ups, a plains side view, a plains dusk wide, plains and desert close-ups with the 32x pack (the 32x pack only changes the creeper skins; blocks stay vanilla 16x).
- If the feature is built: spec section for it, and `specs/quality.md` client part list gains `pillarmock` (named-only, like cairnsim).
