# Giant spiders: wander off the perch, own webs (builder report, 2026-11-24)

BACKLOG "Playtest fixes" 5 (full) and 6. Governed by C1 reworkedMobs; spec `specs/spiders.md`.

## Player-facing

- A giant that has cocooned a catch rests on its perch a few seconds, then lets itself down on silk beside the perch (away from the trunk, clear of the cocoon) and **prowls the ground within about 10 blocks of its cocoon**. After the wait (30 s, as before) it walks back to the foot of its perch, climbs up and feeds.
- While prowling it still fights players (in the dark or when hurt) but doesn't snatch more livestock.
- A giant **remembers the webs it spun** (up to 6, kept across save/load). With a catch, it takes it to the **centre of its nearest own web** (within 24 blocks): it walks under the hub, climbs straight up its thread, hangs upside down at the hub with the cocoon slung below it. Webs other spiders spun are ignored. With no own web in reach it uses the old trunk perch.

## Spec facts (for specs/spiders.md)

- New tasks: `DESCEND` (silk down to a landing spot one block off the perch, at `CLIMB_SPEED`), `PROWL` (on the ground, vanilla stroll), `RETURN` (paths to the perch foot, then `CLIMB` without a catch, then `FEED`). `WAIT` is now only the rest on the perch (`WAIT_PERCH_TICKS` 120); `COCOON_WAIT_TICKS` (600) counts across WAIT and PROWL. RETURN gives up pathing after `HAUL_GIVE_UP_TICKS` and climbs from where it is.
- Prowl uses vanilla: `setHomeTo(larder, PROWL_RADIUS=10)` plus a vanilla `RandomStrollGoal(0.8, STROLL_INTERVAL=60, checkNoActionTime=false)` that only runs in PROWL. Why not the existing `WaterAvoidingRandomStrollGoal`: it checks vanilla's monster "no action time", which climbs in daylight, so a giant never strolled by day. `RandomPos` already biases toward the home, so no hand-rolled wander.
- Hand-rolled: the descent and climb (straight-line moves on silk, like the existing haul climb; vanilla has no rope-descent movement) and picking the larder web (a loop over the giant's own remembered webs; no vanilla search covers "entities this mob made").
- Own webs: `EntityReference<SpunWeb>` list saved as `OwnWebs` (vanilla's entity-reference codec, as Fox's trusted list). `OWN_WEBS_MAX` 6, `OWN_WEB_SEARCH` 24. A web hub qualifies if the two blocks under it are air.
- Hits on a giant climbing back up without a catch don't loosen any grip (there's nothing held).
- Tasks still aren't saved: a giant reloaded mid-prowl roams again; its cocoon stays.

## Tests

- Milestone `GiantSpiderGameTest`: `haulsToItsOwnWebCentre` (a nearer foreign web is ignored; larder is the own hub; cocoon within 1 block of the hub), `remembersItsWebsAcrossSaves`, `leavesItsPerchAndComesBackToFeed` (comes down to the ground, strolls 3+ blocks from the perch, walks back, feeds).
- `CocoonGameTest.spiderComesBackToFeed` skips the prowl too.
- Client `webs` part: new shots `giant-cocoon-own-web-hub`, `giant-prowls-below-its-web`.

## Open

- Prowling giants walk 1 block off the perch and the cocoon hangs low: a player can reach under the cocoon while the spider is away (intended: a chance to cut the catch free?). Owner may want it to stay closer or guard.
- `CampsGameTest.buriedSkullCanBeWalkedInto` failed once then passed on rerun (camp code, not touched here): looks seed-flaky.
