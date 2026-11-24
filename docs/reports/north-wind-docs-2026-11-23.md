# Totem of the North Wind: docs report (2026-11-23, C6)

For merging into `specs/totems-artifacts.md` and the Totems & Artifacts page in `player-docs/`. Owner's design of
2026-11-23; *(builder's choice)* marks picks the owner didn't make.

## Player-facing

- **Totem of the North Wind**, a found artifact (uncommon, one per stack, like the other totems). Pale packed-ice idol
  with an icicle crown, blowing a gust from puffed cheeks, a snowflake on the chest.
- **Used (right-click) while gliding** (Elytra, or anything that makes you fall-fly, Bubba's Extra Recipes' Glider too):
  a firework rocket boost. It isn't used up; the item then shows vanilla's cooldown sweep for **3 seconds**. Not
  gliding: nothing happens, no cooldown.
  - No explosion and no damage, ever (a plain one-gunpowder rocket's flight with no stars). On the Glider the boost is
    the Glider's weaker rocket boost, as with a real rocket (Extra Recipes' own rocket handling applies; no extra code).
  - Sound and look *(builder's choice)*: the wind charge burst sound (quieter, higher) instead of the rocket's launch
    bang, a puff of snowflakes and a small gust; the rocket's own spark trail stays. A leash you hold breaks, as with a
    rocket.
- **In a Reworked Beacons beacon:** anyone gliding anywhere in the beacon's area (vanilla's beacon box, full height)
  gets the same gust automatically, then a **3 second** cooldown per player before the next one. So gliding through a
  North Wind beacon's area keeps you aloft with a gust every 3 s. Beacon screen line: "Gliders in the area get a gust of
  wind".
- **As a statue** (sneak-use on a block, like every totem): the same as the beacon in the statue's small area (the usual
  statue radius, 5 blocks round it) *(builder's choice: follows the rule that statues give their totem's area effect in a
  tiny radius; it makes a statue a launch pad or a boost ring. Owner may prefer the statue to be decorative only.)*
- **Found:** rarely, in trial chamber vaults: **3 %** per normal vault opening, **6 %** per ominous vault opening
  *(builder's choice of numbers)*. Vaults roll once per player per vault, so a full trial chamber gives roughly a 15 to
  25 % chance overall. Not in the chamber's ordinary chests.
- Tooltip (one line): "Use while gliding: a gust of wind".

## How the cooldown shows (B19 noHiddenCooldowns)

- Hand use: vanilla's item cooldown sweep on the totem.
- Beacon and statue: the per-player cooldown is the **same cooldown group as the item** (`ItemCooldowns` on the totem's
  id), so a North Wind totem anywhere in your hotbar or inventory shows the sweep; and hand use and area gusts share one
  timer (no double gusts). Without a totem on you, the rhythm is the gusts themselves: one fires the moment your
  cooldown ends, so nothing waits invisibly. This matches the existing beacon totems, none of which has another visible
  per-player timer. Only players glide (mobs don't fall-fly), so there's no non-player path.

## Spec facts (for `specs/totems-artifacts.md`)

- Code: `northwind/TotemOfNorthWindItem` (use), `northwind/NorthWind` (the gust; the area tick), `compat/NorthWindBeaconPayload`,
  `TotemAreas.Kind.NORTH_WIND`, `TotemType.NORTH_WIND`, `StatueEffects` (pulses).
- The gust reuses vanilla: an attached `FireworkRocketEntity(level, stack, player)` spawned with
  `Projectile.spawnProjectile`, exactly as `FireworkRocketItem.use`; the stack is a firework rocket with
  `Fireworks(NORTH_WIND_FLIGHT = 1, no explosions)`, so `dealExplosionDamage` does nothing; the entity is set silent.
- Areas: beacon pulses (`TotemAreas.put`, lifetime `AREA_LIFETIME_TICKS`) and statue pulses set a NORTH_WIND area;
  `ServerTickEvents.END_LEVEL_TICK` checks `level.players()` only while a level has a live North Wind area: fall-flying,
  not spectator, off cooldown, inside a box → gust + `NORTH_WIND_AREA_COOLDOWN_TICKS`.
- Numbers (`TotemsTuning`): `NORTH_WIND_COOLDOWN_TICKS` 60, `NORTH_WIND_AREA_COOLDOWN_TICKS` 60, `NORTH_WIND_FLIGHT` 1,
  `NORTH_WIND_LOOT` = `chests/trial_chambers/reward` 0.03, `chests/trial_chambers/reward_ominous` 0.06 (the vault
  tables; added through `TotemsLoot` / Fabric `LootTableEvents.MODIFY` like the others).
- Bubba's Extra Recipes' Glider counts as fall-flying in vanilla terms, and its `FireworkRocketGliderMixin` scales any
  attached rocket's push, so it needs no compat code here.

## Tests

- Core (`BubbasTotemsArtifactsCoreGameTest.northWindGustsGlidersOnly`, body `NorthWindGameTest.usedWhileGlidingBoosts`):
  not gliding → PASS, no cooldown, no rocket; gliding → success, cooldown on, still one totem in hand, one rocket owned by
  the player, the player pushed along their look (> 0.5 blocks/tick).
- Milestone `NorthWindGameTest`: `statueAreaGustsGliders` (glider inside gets a gust and cooldown; outside the radius,
  or not gliding, nothing), `beaconAreaGustsGliders` (with `-PwithBeacons`: the payload accepts only this totem, a
  1-layer beacon's area reaches 15 out and 80 up, a glider in it gets a gust). Both run in their own test environments
  (`north_wind_statue`, `north_wind_beacon`) so they're in batches of their own: an area gusts every glider in the
  level, which otherwise hit other tests' players.
- Existing milestone tests now cover it too: `totemsAreFoundInLoot` (the vault odds), `totemsAreArtifacts`, the
  statue place/drop loop over every `TotemType`.
- Client part `northwind` (`NorthWindClientGameTest`): held + hotbar, cooldown sweep, statue front and 3/4 view, at
  16x and 32x. Part `statues` now lays out 11 statues (floor and close-up pairs extended).

## Art

Item 16x/32x (`tools/totems-textures.py`) and statue (`tools-local/make_statue_textures.py`): rows added to
`docs/art-todo.md`.
