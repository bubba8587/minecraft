# Crypt warding statues: docs report (2026-11-24)

Builder report for the owner's 2026-11-24 design: crypts place Totem of Warding statues, and a warding statue's own ward is
bigger and keeps sleepers asleep. Merge into `specs/graveyards.md`, `specs/totems-artifacts.md`, `specs/undead.md`
("Warding") and the player docs for both mods.

## Player-facing changes

- **Crypts (Reworked Mobs, with Totems & Artifacts installed):** a Totem of Warding statue stands out in the crypt, among
  the sleepers, facing the way in. One chance per wing:
  - noble crypt: 85 % for the chamber, and 85 % for each of the two side halls (so up to three);
  - family vault: 65 %;
  - pauper's ossuary: 35 %.
  Chest loot rates are unchanged. Without Totems & Artifacts nothing is placed (no stand-in decoration).
- **Warding statue (Totems & Artifacts):** its ward now reaches 5 blocks (was 3), and inside it sleepers never wake
  (noise, bells, robbing: same as a full-pyramid beacon ward). Other statues keep radius 3.
- Breaking a crypt's statue drops the Totem of Warding (the statue's normal drop), so the sleepers round it can wake again.

## Spec facts

- `GraveTuning.CRYPT_STATUE_NOBLE / _FAMILY / _PAUPER` (0.85 / 0.65 / 0.35), rolled per wing from the piece's own seed
  (`mixStafford13(seed) ^ 0x3A2D5`), separate from the build random, so existing layouts are unchanged.
- `CryptYardPiece.wardSpots()` (public, record `WardSpot(pos, facing)`): spots, all on the floor:
  - family: the vault's middle (gate x, `start + VAULT_LENGTH/2`), facing north (toward the stairs);
  - pauper: one block west of the tunnel's middle (`gate - 1`, `start + PAUPER_LENGTH/2`), facing north; the cell east of it
    is cleared (a random bone block or candle could otherwise close the tunnel next to the candle at `gate + 1`);
  - noble: chamber at (gate, `chamber + 1`), just inside the entry, between the corner pillars, facing north (3 blocks from the
    sarcophagus); each side hall at its middle (`gate ± 8`, `mid`), facing back toward the chamber (west hall east, east hall
    west), leaving both side lanes of the 3-wide hall free.
  Every crypt sleeper lies within 5 blocks of its wing's spot.
- Placement: `CryptYardPiece.wardStatues` looks the block up by id (`bubbas_totems_artifacts:totem_statue`) through
  `BuiltInRegistries.BLOCK.getOptional`, sets `totem=warding` and `facing` by property name (no compile dependency), and
  schedules a block tick so the statue starts pulsing at once (worldgen `setBlock` doesn't call `onPlace`).
- Totems & Artifacts: `TotemsTuning.WARD_STATUE_RADIUS = 5`; `StatueEffects.area(pos, type)` uses it for WARDING;
  a warding statue now calls `Wards.put(..., keepsSleepersAsleep = true, ...)` (was false; the spec's "Statues: radius 3,
  and a Warding statue doesn't keep sleepers asleep (builder)" line is superseded by the owner).

## Tests

- Reworked Mobs milestone `GravesGameTest.cryptWardStatueOdds`: over 4000 seeds per kind, the per-wing rate is within 3
  points of the tuning; without Totems & Artifacts the statue lookup is null and a family crypt whose seed rolls a statue has
  air on the spot.
- Totems & Artifacts milestone `CryptWardsGameTest` (needs `-PwithMobs`; passes at once without):
  `wardStatueKeepsNearSleepersAsleep` (sleeper 5 blocks off sleeps through an explosion 3 blocks from it, one 7 blocks off
  wakes); `cryptStatuesStandInEveryWing` (pauper, family, noble built from seeds rolling every wing: warding statues on the
  spots with the right facing, the statues never split the crypt floor into more walkable areas than without them, and every
  noble sleeper within a statue's ward is kept asleep). New structure `crypt_field` (a copy of Reworked Mobs' `camp_field`).
- Client part `crypts` in Totems & Artifacts (`CryptWardsClientGameTest`, needs `-PwithMobs`): crypt-wards-family,
  crypt-wards-noble, crypt-wards-noble-west, crypt-wards-noble-east.
- Milestone test objective lines for the specs: "a crypt with T&A gets statues per wing at the stated odds"; "a warding
  statue keeps sleepers within 5 asleep, not farther"; "statues never cut off part of a crypt".

## Builder's choices

- No plain decoration without Totems & Artifacts (keeps the crypt as it was).
- Only the warding statue's radius changed; the other statues stay at 3.
