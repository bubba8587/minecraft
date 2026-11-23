<!-- dte:C5 -->
# Comments to labels: Reworked Beacons (builder: agent 3)

Branch `comments-1`. B38: every comment in `mods/bubbas_reworked_beacons/src` (main, client, gametest) is now a short WHAT label, or gone. `dte:` lines are kept.
- Spec (`specs/reworked-beacons.md`) gains:
  - the `BeaconSlots` API (get and set a beacon's item from another mod, used by Ancient Fabricators), which the spec didn't mention;
  - why `BeaconAreas` is cheap enough for spawn checks;
  - that changing the item drops the area at once;
  - technique notes for the upside-down state: forced default, placement hook, no registry access during `Blocks` init, why the handshake exists.
- Everything else in the old comments was already in the spec.
- Comment-only check: all 26 changed files are identical to before once comments and whitespace are stripped. Core: 5/5 pass.
