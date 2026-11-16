<!-- dte:C6,B17,B18,B19 -->
# Bubba's Totems & Artifacts (spec, planned: not built as its own mod yet)

A separate mod for **artifacts**: found items that do things potions and enchantments don't. Planned mod id `bubbas_totems_artifacts`. Owner's call (2026-11-16): "this could be its own mod, Bubba's Totems & Artifacts. Grave Talisman and all totems go in here."

## What moves here

| Artifact | Today | Status |
|---|---|---|
| Grave Talisman | built in Reworked Mobs (`specs/undead.md`) | moves here |
| Totem of Warding | built in Reworked Mobs (Warding, see the Warding section of `specs/undead.md` and `docs/research/area-gems-and-warding.md`) | moves here |
| Totem of Keeping, Gathering, Fortification, Growth (name TBD), Seeking | ideas, `BACKLOG.md` "Totems" | new, later |

What stays in Reworked Mobs: sleepers, the hearing/disturbance meter, light calming sleepers, bells being loud to sleepers, loot displays, combat.

## Independence (same pattern as Reworked Beacons)

- **Reworked Mobs offers a small hook** for other mods: "calm sleepers in this area" and "is this a sleeping undead" (for the Grave Talisman's sensing). Totems & Artifacts uses it only when Reworked Mobs is installed (optional dependency, `suggests`). Without it: the Talisman senses vanilla undead only; the Totem of Warding still scares monsters and wards spawns, it just has no sleepers to calm.
- **Beacons:** totems plug into Bubba's Reworked Beacons through its payload API (`specs/reworked-beacons.md`), optional.
- Totems & Artifacts never requires either mod; neither requires it.

## Design rules for totems (owner)

- Many totems work both **held** (personal, small) and **in a beacon** (area).
- Any rhythm (e.g. a held pulse) shows with vanilla's item cooldown sweep (B19 noHiddenCooldowns); vanilla mechanics first (B17); mobs and players treated the same (B18).
- Fortification must stop explosion block damage only, never via the mobGriefing rule (that would also stop villager farming).

## Open questions (owner)

- Moving the Talisman and Totem of Warding out of Reworked Mobs: when (it's real work: new mod, the sleeper hook, loot tables move, saved items need their ids migrated)?
- Saved worlds: old `bubbas_reworked_mobs:totem_of_warding` / `grave_talisman` items need converting to the new ids (item id remapping).

## Milestone test objectives

None yet (not built).
