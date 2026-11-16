<!-- dte:C4 -->
# Infused Gems: docs to merge (2026-11-16)

Written by the gems builder under the owner's new policy (builders don't edit `player-docs/` or spec prose). For the orchestrator or a docs agent to merge into `specs/effect-gems.md` and the player vault. The balance model (`tools/gem-balance.py`) and its generated report are already updated.

## 1. Gems in beacons (Bubba's Reworked Beacons)

### Player-facing (new "Gems in beacons" section on the Gem Effects page)

- With Bubba's Reworked Beacons installed, a finished Gem goes in a beacon's item slot and stays there (it can be taken out again).
- The beacon gives the gem's effects, worn-style, to **every living thing in its area**: players and mobs, the builder too. A harmful gem makes a trap for everyone; nothing protects you except your own gems.
- Everything on the gem counts: its main effect, its bonus and its drawback.
- The pyramid caps the gem's Gem Power: 1 layer Gem Power I, 2 layers up to III, 3 up to V, 4 layers VI. A stronger gem works at the cap, and the beacon screen says "Only outputting Gem Power N".
- The beacon screen shows the gem's own tooltip lines (e.g. *Speed (100%)*).
- Strength: a perfect gem in a full 4-layer beacon is worth **half a perfect armor set** of that gem. Examples at the perfect gem and 4 layers:

| Gem | In range |
|---|---|
| Speed | +37.5% speed (Speed II is +40%) |
| Strength | +3 attack damage (Strength I) |
| Resistance | 20% less damage (Resistance I) |
| Regeneration | 0.4 HP/s (Regeneration I) |
| Haste | +50% mining, +25% attack speed |
| Fire Resistance | half fire damage, fire goes out twice as fast |
| Breeze, Ender | 50% chance to deflect / blink |
| Saturation | 1 food point every 5.3 s, up to 13 food |
| Poison (harmful) | 0.75 HP/s, never below 1 HP, for everyone in range |
| Slowness (harmful) | 27% slower, for everyone in range |

- A beacon and your own armor (or another beacon, or a potion) of the same effect don't add: the stronger one applies.
- The effect lasts a little longer than the beacon's pulse (vanilla's time: 9 + 2 x layers seconds), so it fades soon after you leave the area.
- With Infused Gems installed, the beacon's standalone payments (an amethyst shard, quartz... buying vanilla powers) are switched off: only finished gems, and other mods' beacon items such as the Totem of Warding, go in the slot (owner, 2026-11-16).
- Without Reworked Beacons, Infused Gems works exactly as before.

### Spec facts

- Owner decisions (2026-11-16, `specs/reworked-beacons.md`): finished gem in the slot broadcasts main, bonus and drawback as worn to every living thing in the area, no protection; Gem Power capped by the pyramid; screen text = the gem's tooltip words plus the beacon's cap line; the gem stays in the beacon.
- **Strength mapping (builder's choice, labelled as such):** `BEACON_SHARE = 0.5` in the model, exported as `beacon_share`. A beacon gives each effect `BEACON_SHARE x the effect's worn cap x the gem's quality`, which is exactly half of what a full netherite set of 12 copies of that gem gives (armor totals are linear in gem quality: a full set of quality q totals `worn_cap x q`). The bonus roll the same; the drawback as one worn drawback (`drawbackSum([size])`). The pyramid cap rescales quality by `factor(capped Gem Power) / factor(gem's Gem Power)`. Why: a perfect gem in a full beacon then lands at about a vanilla beacon's level I to II, below a perfect armor set. Change the constant to retune.
- "Stronger applies": beacon totals join the receiver's worn totals per effect with `max` (`GemTotals.stronger`), against armor and other beacons; potions compare as for worn gems (`GemValues`). Never added past caps.
- Code: `compat/GemBeaconPayload` is the only class touching the beacons API, registered under the `bubbas_reworked_beacons:payload` entrypoint; `suggests` (not `depends`) in `fabric.mod.json`; `compileOnly` / `gametestCompileOnly project(':bubbas_reworked_beacons')`. Each pulse calls `GemBeacons.receive` on every living entity in `context.livingEntities()`; a receipt (beacon pos, gem, capped Gem Power, end time = now + `effectDurationTicks`) is kept in the attachment `bubbas_infused_gems:beacon_gems` (not saved; synced to the player itself so its client-side effects see it). The worn tick drops receipts that ran out. Worn totals are recomputed only when a receipt is new or changed, so the Absorption equip-fill rule is not re-triggered each pulse. The client recomputes its own totals when the attachment changes or a receipt runs out.
- Infused Gems was verified both ways: the core suite without the beacons jar (45 mods loaded, 9/9), and the full suite with it (`-PwithBeacons -PmilestoneTests`, 46 mods, 86/86).
- Not done: a client screenshot of the beacon screen with a gem (the beacon screen is still being built by the Reworked Beacons agent).

### Milestone test objectives

- `GemBeaconGameTest` (passes at once without Reworked Beacons; run with `-PwithBeacons -PmilestoneTests`): a perfect Speed gem pulsed from a 4-layer sky beacon gives a pig and a player in range `0.5 x Speed's worn cap` and makes the pig faster; a 1-layer pyramid caps it to Gem Power I (`x 0.40`).
- `GemBeaconGameTest` also checks the payload switches the standalone payments off (`replacesStandalonePayments()`). Objective without code yet: with both mods installed, an amethyst shard no longer fits the beacon slot.
- Objectives without code yet: leaving the area ends the effect after the effect time; a beacon and armor of the same effect give the stronger, not the sum; a harmful gem beacon hurts its builder; the beacon screen shows the gem's tooltip lines and the cap line.

## 2. Breeze hit: every projectile-launching mob (owner, 2026-11-16)

- Player-facing: a target hit by a Breeze weapon fires off course whatever it shoots or throws: arrows, crossbow bolts and fireworks, tridents, snowballs, witches' potions, llama spit, blaze and ghast fireballs, wither skulls, breeze wind charges; shulker bullets wobble off course while they home in.
- Spec facts: one rule for every projectile an affected entity launches. `Projectile.shoot` (every shot and throw, including `shootFromRotation` and `spawnProjectileUsingShoot`): vanilla's uncertainty x the dial, from at least 1 (`BreezeVeerMixin`). Powered projectiles built with a shooter (fireballs, wither and dragon skulls): the same spread on their direction (`BreezeVeerFireballMixin`). Shulker bullets home, so each time one picks a new heading it is wobbled by the same spread (`BreezeVeerShulkerMixin`). Evokers' fangs and vexes are not projectiles. A fishing hook sets its own velocity and is not covered (players only).
- Note: zombies can hold bows but never fire them in vanilla, so a Breeze hit changes nothing for them.
- Objective without code yet: a Breeze-hit witch's potions and a Breeze-hit shulker's bullets miss a still target more often than unaffected ones.
