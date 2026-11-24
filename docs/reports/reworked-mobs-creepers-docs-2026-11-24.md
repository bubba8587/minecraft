# Reworked Mobs creepers: cover to cover, running off, leaving by day (docs report, 2026-11-24)

Builder report for the docs merge (specs/creepers.md, player docs). Owner's design for this change (their words): it walks behind a wall before despawning in the daytime; more likely to despawn if you don't look at it; instead of approaching in an empty field it always hides behind a wall unless there is literally zero cover, creeping to closer and closer walls; if you approach it, it runs to the next farther wall ("behind a tree out your window, look away, it walks up to your wall and looks through the window; run at it, even from inside the house, and it might run away toward the tree again").

## Player-facing changes

- A watcher creeper no longer just sits 10-22 blocks off. While you aren't looking toward it, every 5-8 s it moves to a peek spot behind cover closer to you, preferring spots whose hiding place and route are out of your view. It can come right up to your house wall and peek in round a window frame or doorway (closest about 3 blocks).
- If you catch it walking up in the open (looking toward it while it can be seen) it goes back to the spot it came from.
- Walk or run toward it (not sneaking), even from inside your house through a window, and it runs (faster than you sprint) to a peek spot farther away, e.g. back to the tree. It waits 10 s before creeping closer again. Sneaking up on it still makes it back behind farther cover at a walk and sit hidden (unchanged).
- Glass and glass panes (and iron bars) no longer count as cover: it sees and peeks through them. Leaves still block its view.
- The open-ground stance (C5: hold 12, back off under 10) now only happens when it finds no cover at all in 3 searches in a row. With a tree or wall around it always holds a cover spot. The "backs off when closer than 10" rule now applies only in the open; with cover, coming at it makes it run to farther cover instead.
- Leaving by day: the per-player night watchers (the ones the mod spawned) now leave in the morning. Once it's day each picks a random moment 10 s to 2.5 min later (twice as fast if you haven't looked at it for 30 s), walks to a spot hidden from you (or just walks away from you if there is none) and vanishes only when no player can see it. Spawner, structure, egg and command creepers never do this. A creeper that is hissing doesn't vanish; hitting or startling it while it leaves works as usual.
- Its peek shows a little more of it (its middle sits 0.12 past the cover's edge, was 0.05), so its face shows round a window frame.

## Spec facts (for "How it's built")

- `CreeperWatchGoal` plans: WATCH (hold a spot, peekaboo), APPROACH (walking to a closer spot), FLEE (running to a farther spot), HIDE (sneak rule), LEAVE (heading out of sight by day).
- *Creeping closer:* at a reached spot, not looked at (peekaboo cone) for 3 s, every 100-160 ticks: `CoverFinder.findAll` for up to 4 spots between 3 blocks and (distance - 2), best near (distance - 6); each gets a path (`PathNavigation.createPath`), scored by the share of 6 points along the route the player can see plus 1 if its hidden place is in view; lowest wins.
- *Approach noticed:* every 10 ticks it compares the player's own position now and 20 ticks ago against its current position (its own movement doesn't count). Closing by 2+ blocks in 20 ticks (0.1 a tick; walk 0.216, sneak 0.065) within 20 blocks, not sneaking: FLEE. Flee spot: (distance + 3) to max(22, distance + 14) capped at 23, best near distance + 7, speed x1.4 (0.35 blocks/tick). No spot: it backs away (`DefaultRandomPos.getPosAway`, 10 blocks) at that speed until 3 blocks farther. Sneaking and closing 0.3+ in 20 ticks within 18: the old HIDE rule.
- *Its own running doesn't startle anything:* creepers are never counted by the startle meter (unchanged builder's choice 5). The flee test asserts a walking approach leaves the meter at 0.
- *Line of sight for cover:* `CoverFinder.clear` uses `ClipContext.Block.VISUAL` (was COLLIDER). Vanilla 26.3: `TransparentBlock` (glass, stained, tinted) and `IronBarsBlock` (panes, bars) have an empty visual shape; leaves and doors use their collision shape. The startle and watching senses still use vanilla `Sensing` (collision), so glass still blocks those.
- *Cover sweep (new in the spot search):* 32 level rays from the target's eyes over the creeper's side (±75 degrees of its bearing); where neighbouring rays end more than a block apart there is a cover edge, pinned with 2 more rays; cells 0.7 and 1.5 blocks behind each edge become candidates. Edge cells get their own budget of 24 raycast evaluations, other cells 16 (was 16 shared); `evaluate` now rejects a cell after its 2 outermost rays when both agree (no edge across it), so most rejects cost 2 rays. Reason: random samples missed a lone trunk, and in the test field cells at the foot of a 1-block step used up the whole budget.
- *First spot:* between 3 and 22 blocks, best near min(current distance, 15) (was 10-22, best 15). A held spot is dropped when the target is outside 2-24 blocks (was 8-24).
- *Open ground:* `CreeperMind.openGround()` true only after 3 failed searches in a row in WATCH; any later find clears it.
- *Leaving:* `CreeperMind.tickLeaving` once a second, only for creepers with the `watcher_for` attachment, only while `Level.isBrightOutside()` (dark again resets it). Countdown random 200-3000 ticks, ticking down 2x while the player hasn't looked toward it (peekaboo cone) for 600 ticks; then leaving. While leaving, not hissing, and no non-spectator player within 128 blocks has a clear view (visual clip) of its eyes, middle or top: `discard()` with chance 0.2 + 0.05 per second since its player last looked toward it, per second. The goal's LEAVE plan picks a peek spot (3-23 blocks, best distance + 7) whose hidden place is out of the player's sight and sits hidden there; none found: walks away 16 blocks at a time. `CreeperMind.leaveIn(ticks)` starts the countdown (tests).
- This also fixes BACKLOG creeper audit bug (3) (night watchers never leave by day, blocking the next night's watcher). The other audit items are untouched.
- Cost: the sweep is 32 rays + 2 per edge, only when a search runs (at most once a second while it has no spot; an approach try every 5-8 s; a flee or leave search when those start). Approach tries add up to 4 path searches and 7 rays each. Leaving adds up to 3 rays per nearby player once a second, only while leaving.

## Numbers (CreeperTuning)

| | Value |
|---|---|
| Closest peek spot | 3 blocks (`APPROACH_MIN_DISTANCE`) |
| Creeping closer | not looked at 3 s; tries every 5-8 s; at least 2 closer, prefers 6 closer; 4 candidates; 6 route points |
| Approach noticed | 2 blocks closer in 1 s (not sneaking), within 20; sneaking 0.3 in 1 s within 18 |
| Running off | x1.4 speed (0.35 b/t); farther by 3-14 (best 7), max 23; no new approach for 10 s after |
| Open ground | only after 3 empty searches in a row |
| Cover sweep | 32 rays, ±75 degrees, 2 bisections; 24 edge + 16 other evaluations |
| Half-hide inset | 0.12 (was 0.05) |
| Leaving | day only; starts 10 s-2.5 min after daylight (2x faster if not looked at for 30 s); gone with 20 % + 5 % per second since last looked at, per second, when no player within 128 sees it |
| Removed | `PEEK_MIN_DISTANCE` (10) |

## Builder's choices (owner may change)

1. Approaching is detected without line of sight (it notices you coming even while tucked fully behind its cover, and through walls).
2. Only players trigger running off and creeping closer's peekaboo; a watched animal never makes it flee.
3. A teleport toward it counts as an approach.
4. "Seen by a player" for vanishing is a clear line from any player's eyes, whatever they face; "looked at" for the leave timing is the peekaboo cone.
5. Leaving creepers that lose their player (beyond 24 blocks) still vanish once unseen.
6. The half-hide inset went from 0.05 to 0.12 so it is visible at a window (at 0.05 a pane's frame hid it completely).

## Tests

- New milestone server game tests in `CreeperGameTest` (each its own environment):
  - `creepsToCloserCoverWhenUnseen`: far wall spot first (over 14 blocks), then a spot 3+ blocks closer, never closer than 3.
  - `runsToFartherCoverWhenApproached`: player walks at it for 24 ticks; meter stays 0; it reaches a spot 3+ blocks farther.
  - `neverOpenGroundWithCoverAround`: one 3-tall log 9 blocks out; never open ground for 240 ticks and it reaches a spot.
  - `watcherLeavesOutOfSightByDay`: day, watcher with `leaveIn(0)`; seen while leaving for 20+ ticks without vanishing, then vanishes where no player can see it.
- Existing tests: none changed in purpose. Client test `creepers`: added `creeper-window` (from inside a small plank house, it peeks round the pane's frame) and `creeper-window-outside`; `creeper-caught` and `creeper-window` now wait 10 ticks after it settles, because the client shows mob movement a few ticks late (the old caught shot showed only a sliver for that reason).
- Milestone objectives still untested: it goes back to its old spot when caught walking up in the open; the leave countdown runs 2x when not looked at; a hissing leaving creeper doesn't vanish.
