<!-- dte:C1 -->
# Spiders: progress reports to the orchestrator (builder: agent 3, session minecraft-73)

SendMessage from this session doesn't reach you, so reports go here.

## Step 2: giant spiders and spun webs (branch `spiders`)

- **Built:** the giant spider (old-growth taiga; roams, spins webs, snatches livestock into them, is lured by livestock, calls little spiders, drops more of the vanilla loot) and spun webs.
- **Spun webs, following the owner's changes you passed on:** a tangle, not an orb; thick, then trimmed; redrawn as pixel art in the cobweb palette, lit per pixel. The owner looked and said "MUCH better, keep at this angle".
- **Tests:** 11 spider tests (with -PmilestoneTests) and 32 RM core tests pass. DTE validate OK.
- **Screenshots:** client parts `spiders` and `webs`, all looked at.
- **Docs:** step 2 is in docs/reports/spiders-docs-2026-11-22.md, including the owner decisions to carry into specs/spiders.md "Spun webs" (pixelated, not translucent; a tangle, not an orb).
- **Messaging:** I can't reach you as minecraft-b0 or minecraft-2f by name, and your pipe refuses connections, so replies come here.
- **Still queued:** the quartz tool textures (Reworked Mining).

## Follow-up: pixel drop thread, develop merged in

- I merged develop into `spiders`. The conflicts were docs only: art-todo, the ClientTests names, and my docs and report files, which now hold step 2 only. All 33 RM core tests pass after the merge.
- The drop thread is redrawn as pixel art in the cobweb palette, lit by world light, as you asked. It shares `PixelStrands` with the spun webs. I looked at spider-drop, spider-dangle and spider-latch, and the webs are unchanged.

## Stormie's Spiders, rear-first reel, fall fix (spiders branch)

- **Fall-damage fix:** done separately on branch `spider-fall-fix` (3798bc0, off develop, REPORT READY), so you can merge it straight away. The same fix is on this branch.
- **Stormie's Spiders is wired in.**
  - Reworked Mobs now declares `"depends": {"stormiespiders": ">=3.3.1"}`, and the build compiles against it.
  - `gradle/stormies-spiders.gradle` is pinned to the Modrinth version ID `jTOAPFV5`. Pinning matters because `3.3.1-fabric` is also the version number of a 1.21.1 build, and Modrinth's maven serves that one.
  - Dev runs need a workaround. The jar carries a root `log4j2.xml`, so on the runtime classpath Fabric Loader treats it as a game library and then reports "needs fabricloader, which is missing". So the script keeps it off the classpath and loads it with `-Dfabric.addMods`.
  - The script is applied by Reworked Mobs, by Totems with `-PwithMobs`, and by `testworldshots`.
  - A normal mods folder doesn't need any of this.
- **Nest sleepers stay ours until they turn vanilla.** NestSpider overrides Stormie's climber methods to report upright with no attachment, so Stormie's doesn't move or rotate them. Checked in screenshots: they hang still and upside down, and the drop, dangle, latch and reel are unchanged.
- **Hitbox note:** with Stormie's loaded, a spider's hitbox is narrower, about 0.95 wide instead of 1.4. So drops are a bit harder to land.
- **Rear-first reel (owner):**
  - The nest spider stays head-down for its whole time off the ceiling. The thread leaves its abdomen, and the victim hangs below its head. Checked in screenshots.
  - A giant hauling a catch up a trunk is posed the same way, with a pixel thread to the web. Its task and larder are now synced to the client.
  - A client mixin into Stormie's `ClientEventHandlers.storeClimberDataForRenderState` skips Stormie's pose during that climb only.
- **Strands slow you like a cobweb block (owner):** exactly vanilla's `WebBlock` multiplier, including the Weaving effect case.
- **Tests:** 12 spider tests pass, including `aMissedDropTakesNoFallDamage`.
- **Not done yet:**
  - **Wall and ceiling screenshot:** a scripted path to a wall or ceiling point is refused by Stormie's navigator (`moveTo` returns false), so I don't have a ceiling or wall shot yet.
  - **Giant hold pose (your latest owner note):** the giant must keep its surface orientation while holding or waiting, upside down under a branch or sideways on the trunk.
  - **Cocoons (owner):** not started.

## Cocoons, giant perch pose, two cocoon shapes (spiders branch)

- **Cocoons (owner).** New entity `silk_cocoon`. A nest spider that reels its victim up, and a giant that hauls a catch to its perch, wraps it instead of biting it to death.
  - The victim rides the cocoon and can't move or sneak off. It shows the hint "Cocooned!" (trimmed per the owner).
  - Wrapping poisons the victim. Vanilla Poison never takes the last half heart.
  - The spider backs off and waits 600 ticks, then comes back and bites: 3 damage every 40 ticks. Those bites can kill.
    - The nest spider waits upside down on the ceiling, then hangs head down to feed.
    - The giant backs off along the underside, or up the trunk, then returns.
  - Cutting: the victim hits from inside, or anyone hits from outside. Silk is 100.
    - Points per hit: hand 13 (8 hits), other items 18, axe 28, sword 35 (3 hits), shears 40. Shears used from outside cut 50.
    - At 0 the victim drops out alive with the poison cleared.
    - Fire and explosions burn or blow the cocoon open.
  - Cocooned livestock cry their ambient sound at volume 2.5 every 30-70 ticks.
  - A nest spider whose victim gets away turns vanilla if a player is near, else goes back to sleep. A giant whose victim gets away goes back to roaming. Hitting a waiting or feeding nest spider makes it drop and fight.
- **Two cocoon shapes (owner).** Animals, and anything wider than it is tall, are slung lengthwise in a horizontal cocoon from two threads. Upright victims (players, villagers, zombies) hang head down in a vertical cocoon tied at the feet.
  - Both are drawn in the same pixel silk as the webs, and the silk thins as it's cut.
- **Giant pose (owner).**
  - The perch prefers an underside: the highest spot beside the trunk with leaves or wood above and room below for the cocoon. There the giant hangs upside down with the cocoon below. Without one, it clings head down on the trunk, belly to the bark.
  - It never flips upright while climbing, waiting or feeding. We pose it ourselves then, with Stormie's render pose skipped.
  - Busy giants no longer despawn (`requiresCustomPersistence` while it has a task), so they never leave a cocoon behind.
- **Stormie's Spiders and the Fabricator.**
  - With Stormie's loaded, a Fabricator's deactivated spider stuck to the walls instead of dropping onto its pad.
  - New mixin `StormieDeactivatedSpiderMixin` (Spider, priority 1500) makes Stormie's merged `onTravel` step aside for deactivated mobs.
  - That fixed `fabricatorDropsDeactivatedMob`.
- **Shear string** now falls where the web was cut, not at the player.
- **Tests.**
  - New milestone class `CocoonGameTest`:
    - `swordCutsFreeFasterThanAHand`
    - `anotherPlayerCutsAPigFree`
    - `poisonNeverKills`
    - `spiderComesBackToFeed`
  - The latch and snatch tests now expect a cocoon.
  - Spider tests run in their own test environment `bubbas_reworked_mobs-test:spiders`, so they don't shift the big Fabricator tests' layout.
  - All 17 spider and cocoon tests pass, three runs in a row.
- **Flaky on develop too (not from this branch).** `mob_factory_game_test_warden_wakes_when_it_hears_aplayer_at_the_machine` ("one Warden per Fabricator, found 2") and `mob_factory_game_test_secrets_are_built` fail in about 2 of 3 full `-PmilestoneTests` runs on plain develop (checked in ../mc-spiderfix). Neighbouring Fabricator test structures overlap.
- **Screenshots.** Client parts `spiders` and `webs`, all looked at:
  - cocoon-player-outside and cocoon-player-outside-back: a player head down in a vertical cocoon;
  - spider-held: a pig slung horizontally under its nest;
  - giant-haul-rear-first: on the trunk, head down, sheep below;
  - giant-cocoon-sheep-under-canopy and giant-cocoon-sheep-from-below: upside down under the canopy, sheep slung horizontally below.
- **Still open: a Stormie's wall and ceiling shot.** Its navigator refuses scripted climbing paths, so I'll need to bait the shot with prey at night.
