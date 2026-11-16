<!-- dte:C1 -->
# Docs addendum: vanilla-style meter bars, lurk test fix (2026-11-16)

Builder's addendum to `specs/drowned.md` (Visible meters) and `specs/creepers.md`.

## Meter bars look like vanilla's notched bars (owner)

- **Sprites:** vanilla's own boss-bar sprites, no new textures, no `docs/art-todo.md` rows. Each bar is `boss_bar/blue_*` (drowned grip) or `boss_bar/red_*` (creeper startle), with `boss_bar/notched_20_*` on top: the same 182 x 5 notched bar as the experience bar, in vanilla's own blue and red, so no tinting was needed. I picked these over tinting the green XP sprite because the boss-bar colours are vanilla's own and read cleaner. The layers are background, notches, then the filled part with its notches, cropped to the value.
- **HUD** (the held player): the bar sits under the crosshair at vanilla's GUI scale (182 x 5), drawn with `blitSprite` the way `BossHealthOverlay` draws it.
- **In the world:**
  - A camera-facing textured quad, 1 block wide and 0.1 tall, full-bright.
  - It uses the same sprite files as plain textures (`textures/gui/sprites/boss_bar/*.png`) through `RenderTypes.text`, submitted from `EntityRenderer.submit`.
  - Mobs carry the bar from extract to submit in `EntityRenderStateMeterMixin`.
  - Shown within 32 blocks.
- **Fixed overlap:** a drowned holding someone draws its bar over the victim's head (0.35 above the highest passenger), not across their body. Other mobs get theirs 0.35 above their own head.
- The old text bar (bold "|" marks in the scoreboard line) is gone, so the scoreboard below-name line is no longer used.

## `swimsToALurkSpotNearThePlayer` flake: cause and fix

- **Cause:** the test assumed the drowned always picks a spot by the bank (facing -x). But the test pool's side walls are 1-block stone piers level with the surface. A 2-deep column next to one is a valid lurk spot by the rules: land you can stand on within 3 blocks. The search samples random columns, so now and then it picked one by a side wall, at x 10-11 facing -z ("yaw -180") or out past x 10. That's correct behaviour (a pier is a shore), so I fixed the test, not the behaviour.
- **Fix:** the test now accepts any 2-deep column in the shallows (x 7-12). It checks that the drowned faces its own spot's shore (within 30 degrees) and that there's solid ground at surface level within 3 blocks in that direction.
- **Runs after the fix:**
  - 4 full `-PmilestoneTests -PrerunGametests` runs. Every drowned test passed every run.
  - 2 runs each had one failure, in `GrabLootGameTest.idleSkeletonTakesADisplayedSword` and `UndeadCombatGameTest.idleZombiesDriftTogether`. Both are already listed as flaky in BACKLOG.md and don't involve drowned.
  - BACKLOG.md can drop the drowned entry from its flaky list.

## Screenshots

Updated in `docs/previews/drowned/` and checked by eye:
- `drowned-grab-meter.png`: a half-full blue notched bar over the grabbed villager's head.
- `drowned-two-holders.png`: a full blue bar over the villager, with two drowned holding.
- `creeper-startle-bar.png`: a red notched bar over the creeper.
- `drowned-held-player-hud.png`: the blue boss-bar-style bar under the crosshair, plus the world bar over the player's head.
