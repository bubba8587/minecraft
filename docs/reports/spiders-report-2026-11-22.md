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
