# Undead follow-ups (undead-2): builder report (2026-11-22)

Branch `undead-2` (worktree `../mc-undead2`), from develop. One section per step; each step is committed with REPORT READY.

## 1. Back to sleep at dawn (specs/undead.md)

### Player-facing

- Woken skeletons, zombies, husks and strays that were awake during the night and are still about at sunrise, with nothing to fight, go back to sleep: they walk to the nearest place out of the sun within **16 blocks** (4 up or down) and become a sleeper block again, keeping everything they carry (gear, picked-up loot, name).
- **Owner's rule (2026-11-22): an undead can always sleep by sitting down in a pocket where it's safe to stand.** So a place is any spot on a solid floor with a roof over it (something that blocks light: glass and barriers don't count) and dim (block light 7 or less; a candle-lit crypt is fine, a torch-lit room isn't):
  - room to stand → it **sits** down there;
  - a one-high niche (a crypt wall niche) → it **lies** down (from beside it: it has no room to step in first);
  - an **open grave** (a pit walled on three sides) works too, roof or not → it lies down.
- Crypts refill themselves, and a cleared graveyard quietly fills up again if you leave survivors. Undead you wake by day (underground, say) don't go straight back to sleep: only those that saw the night. The resting dead are not involved.
- A soft sound (the sleeper's place sound) as it settles.

### Numbers

| What | Value | Where |
|---|---|---|
| Search range | 16 blocks across, 4 up/down | `UndeadTuning.SLEEP_SEARCH_RANGE`, `SLEEP_VERTICAL_RANGE` |
| Searches again every | 3 s (+ up to 1 s) | `UndeadTuning.SLEEP_RESEARCH_TICKS` |
| Dim enough | block light <= 7 | `UndeadTuning.SLEEP_MAX_LIGHT` |
| Walk speed there | 1.2x | `BackToSleep` |

### Spec facts

- `undead/sleeper/BackToSleep` is a vanilla `MoveToBlockGoal` (agent 1 / owner rule: vanilla subsystems over hand-rolled scans): nearest-first search, throttling and walking are vanilla's; `isValidTarget` = the floor under a valid place (`BackToSleep.pose`); it lies down when within 1.6 blocks (`acceptedDistance`). Registered at priority 1 in `WokenZombie` and `WokenSkeleton` (so husks and strays too); runs only by day (`isBrightOutside`, overworld-like dimensions) with no target, not riding or leashed.
- "Saw the night" is the entity tag `bubbas_reworked_mobs.saw_night`, added in `customServerAiStep` while it's dark (`BackToSleep.noteNight`), so it survives a reload.
- Lying down: `BackToSleep.lieDown` places the variant's sleeper block (pose from the place, facing the open side for a niche, waterlogged in water), copies every equipment slot into the block entity's gear and the custom name, then discards the mob.
- An earlier hand-rolled ring search was replaced by the goal before commit.

### Tests (milestone, `DawnGameTest`)

- `survivorsGoBackToSleepAtDawn`: at noon a woken zombie that saw the night lies down in a stone niche, as a sleeper with its helmet and shovel.
- `onlyThoseWhoSawTheNight`: one woken by day stays up.
- `sitsDownUnderAnyCover`: under a plain stone canopy with standing room it sits down.
- Full milestone suite 143/143.
- No screenshots for this step (no new visuals: the sleeper poses are the existing ones).
