<!-- dte:C1,B18,B19 -->
# Docs addendum: dangerous drowned, the grip meter, others joining in, visible meters (2026-11-16)

Builder's addendum to `specs/drowned.md` (and `specs/creepers.md` for the red bar). Owner changes, all binding: "make the drowned dangerous", the grip meter rework, and visible meters behind a game rule. Nothing here is in the live docs yet.

## The grip meter (replaces the grip-break meter)

- **Grip, 0-100 (`DrownedMind.grip`).** It starts at **50** when a drowned grabs. It **rises 0.25 a tick** while it drags (5 a second: half to full in 10 s if nobody fights). **Hits loosen it** by the same points as before: 6 + 3 per point of damage before armor; axes x1.5 (was x2); +12 per level of Impaling, +10 per level of Knockback. A hit on any holder counts, and so does anyone's hit (B18). **At 0 you're free**: every holder lets go and recoils. Riptide still frees at once. There's no drain any more; the old meter's drain is now the grip's rise.
- **The danger follows the grip (B19: the grip is the visible state; no drag timer).** At 50 or less its hits use the start values; at 100, the full values; in between, a straight line:

| | grip 50 or less | grip 100 |
|---|---|---|
| Hit chance per tick | 1/50 (about every 2.5 s) | 1/10 (about every 0.5 s) |
| Damage per hit | 2 | 4 |
| Air knocked out per hit | 45 (1.5 bubbles) | 90 (3 bubbles) |

  Respiration still divides the air loss by 1 + its oxygen bonus. Water Breathing, Conduit Power and Breath of the Nautilus still stop the air loss.
- **What players see and hear as it tightens:** more and harder hits, with a squeeze sound (the drowned's water ambient) that gets louder and lower. Bubbles stream off a victim under water every 2 ticks: 1, rising to 7 at full grip. With the game rule on, the grip itself shows as a blue bar (below).
- Numbers are in `DrownedTuning`: `GRIP_START`, `GRIP_RISE_PER_TICK`, `GRIP_RISE_PER_HELPER`, `HIT_CHANCE_PER_TICK`/`_FULL`, `HIT_DAMAGE`/`_FULL`, `HIT_AIR_LOSS`/`_FULL`, `ESCALATION_BUBBLES`. `GRIP_DECAY_PER_TICK` and `ESCALATE_TICKS` are gone.

**Death times if the victim panics** (nobody hits back), from `tools/drowned-danger.py`, a Monte Carlo over vanilla's air, drowning, armor and hurt-time rules with the numbers above. Medians, with the middle 80 % in brackets:

| Victim | 1 drowned | 2 drowned | 3 drowned |
|---|---|---|---|
| Unarmored | 8.5 s (7.2-9.8) | 6.0 s (5.2-6.9) | 4.9 s (4.2-5.5) |
| Full iron | 12.7 s (11.6-14.1) | | |
| Full diamond | 17.6 s (15.9-19.6) | | |
| Unarmored, Respiration III | 8.9 s (7.5-11.0) | | |
| Diamond, Respiration III | 24.3 s (21.4-27.5) | | |

Before this change it was about 17 s unarmored. The model assumes the victim is under water from the start; in 2-deep shallows their head is above water for the first second or two.

**Getting out (fresh grab, one drowned):** two iron axe hits (49.5 each) about a second apart; three or four iron sword hits (24 each, 1.6 a second) in about 2 s; fists (9 a hit, at most 2 a second through vanilla's hurt time) in about 4 s. All of these are well inside the 8.5 s.
- With two holding: an axe takes about 4 hits (3.5 s), a sword about 3 s, and fists can't win alone.
- With three holding: an axe takes about 5 s, against about 5 s to death. You need a friend.

## Others join in

- A dragging drowned looks once a second (one box query, 16 blocks) for drowned that are already there and free: not vanilla, not riding, and idle, travelling or lurking. The nearest swim over (state JOIN: lying, swimming at 0.15 blocks a tick) and grab the other leg (state HELP). At most 2 extras, so 3 holders in all. No drowned are spawned for this. It works on any victim the same way (passive mobs too, B18).
- **How it fits the riding hold (one vehicle):** the first drowned stays the vehicle and the only one that moves the victim. The extras aren't vehicles. Each one holds a grip point at the victim's feet from the side (90 degrees round from the first one's grip, one on each side), level with the first. A spring keeps it there, facing the victim, upright in water. It follows as the first one drags. If it's pulled more than 3 blocks off its point, it swims back.
- **Effects of each extra holder:**
  - The grip rises 50 % faster (+0.125 a tick per extra).
  - Hits loosen it less: points are divided by 1 + 0.6 per extra (two holders: /1.6, three: /2.2).
  - The drag is 35 % faster per extra.
  - Each extra also rolls its own hits on the same grip value, so more hits land.
- **How extras let go:** one meter, shared and held by the first drowned. When it reaches 0 (or Riptide fires, or the first one dies) everyone lets go and the extras recoil too. An extra lets go on its own if the first one stops dragging, the victim is gone, or it gives up swimming over (24 blocks away). Killing an extra removes it.
- The `DRAG_SPEED_PER_HELPER`, `GRIP_EXTRA_HOLDER_DIVISOR` and `JOIN_*` numbers are in `DrownedTuning`.

## Visible meters (game rule `bubbas_reworked_mobs:show_meters`, default false)

- **Rule:** "Show mob meters (drowned grip, creeper startle)", category Mobs, in the lang file and datagen.
- **Drowned grip:** blue. A bar of 20 marks shows above the holding drowned, in the line under where its name tag would go, for everyone within 32 blocks. On the held player's screen there's an 81 x 5 bar under the crosshair: black edge, dark blue well, blue fill with a lighter top line, drawn with filled rectangles.
- **Creeper startle:** red. The same 20-mark bar above the creeper, rising with hits and fast movement and falling as it calms (`CreeperMind.meter()` scaled from `METER_FULL`). It shows only while the meter is above 0.
- **How values get to clients (`meter/Meters`):** one S2C payload `bubbas_reworked_mobs:meter`: entity id (VarInt), kind, and a value 0-100 (1 byte each). It goes to the players tracking the mob, only while the rule is on, and only when the whole-number value changes. A 0 is sent once when the meter empties or the rule goes off; the client then drops it.
- **Cost:**
  - A dragging drowned sends one 3-6 byte packet to each tracking player about every 4 ticks.
  - A creeper that is calming sends one every 2 ticks; a calm creeper sends nothing.
  - With the rule off, nothing is sent at all. The server does a game-rule read per mob per tick.
  - On the client, the bar costs a map lookup per rendered entity.
- **Limits:**
  - A player who starts tracking a mob mid-meter sees its bar from the next change, at most a few ticks later while it's moving.
  - The above-head bar uses the scoreboard below-name line, so a mob with a scoreboard below-name display shows the bar instead while it has a meter.
  - The bar marks are bold "|" characters, not a texture, so `docs/art-todo.md` needs no rows.
- **Code:** `meter/Meters` (common: payload, rule check, sending); `client/meter/MeterBars` (client map, bar component, HUD element after the crosshair); `client/mixin/MeterBarMixin` (puts the bar into `EntityRenderState.scoreText` at the end of `EntityRenderer.extractRenderState`); the hooks are in `DrownedMind.tick` and `CreeperMixin`'s tick.

## Builder's choices

1. **The danger starts counting at 50**, not 0, so a fresh grab has the old start values. Hitting it below 50 makes its hits no weaker than the start values, but brings it closer to letting go.
2. **Extra holders make it harder both ways:** the grip rises faster and your hits count for less. The extras also add their own hits. Fists can't beat two holders alone.
3. **Axes went from x2 to x1.5**, so a fresh grab still needs two axe hits (with x2, one axe hit from 50 would free you at once).
4. **The extras hold the sides of the feet, not the vehicle slot:** the riding hold has room for one vehicle, so only the first drowned moves the victim; the extras follow it.
5. **Meters show for creative players and spectators too** (it's a debug/playtest view behind a rule that's off by default).

## Tests and screenshots

- `./gradlew :bubbas_reworked_mobs:build -PmilestoneTests -PrerunGametests`: all 95 pass.
- **Core:**
  - `drownedGripLoosensWithHitsAndTightensWhileDragging` (replaces `drownedGripBreakMeterBreaksTheHold`): starts at 50; a fist hit loosens it by about 9; 40 ticks of dragging raise it by 10; two fast axe hits free a villager.
  - `drownedSecondDrownedJoinsAndHoldsHarder`: a drowned in the pit swims over and holds at the villager's feet; with two holding the grip rises 1.5x as fast; a hit on the helper loosens the shared grip by 1/1.6; at 0 both let go.
  - The drag test's player now has Resistance V (the faster hits could kill the test player mid-drag).
- **Milestone:** `gripTightensToFullAndHitsHarder`: after 10 s of dragging the grip is full, and a full-grip hit does 4 damage and takes 90 air.
- **Client part `drowned`** (rule on) adds `drowned-two-holders` and `creeper-startle-bar`. `drowned-grab` now has the blue bar above the drowned, and `drowned-held-player-hud` has the blue HUD bar under the crosshair plus the bar above the drowned. The villager in the deep-drag shots has Resistance and Water Breathing so it lives through the shots.
- **Previews** in `docs/previews/drowned/`: `drowned-grab-meter.png`, `drowned-two-holders.png`, `drowned-drag-deep.png`, `creeper-startle-bar.png`, `drowned-held-player-hud.png`.
- **Milestone objectives not yet tested:**
  - A third drowned joins and the holders split one on each side.
  - The meters stop sending when the rule goes off.
  - A late tracker sees the bar on the next change.
  - Riptide frees from several holders at once.

## Player-facing text changes

- Its grip **tightens the longer it holds you**: more and harder blows, more of your breath knocked out, more bubbles streaming off you. Fight back fast: every hit loosens its grip, and when the grip is gone it lets you go.
- **Other drowned nearby swim over and grab your other leg.** Each one holding on makes the grip tighten faster and your blows count for less. Bring a friend.
- New game rule **Show mob meters** (off by default): a blue bar shows a drowned's grip (above it, and under your crosshair while it holds you), and a red bar shows how startled a creeper is.
