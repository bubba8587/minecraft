<!-- dte:C4 -->
# Infused Gems: what strength actually does (proposal)

**Status:** proposal for the owner, 2026-11-15. Nothing here is built yet. "Today" numbers come from the code (`GemHits`, `GemActions`, `WornEffects`, `GemMobEffect`, `balance.json`) and the balance model (`tools/gem-balance.py`). Vanilla behavior was checked against the 26.3 source. Each choice ends with a question, numbered **Q1, Q2, ...** and listed again at the end.

## How to read the numbers

Three reference builds, as the balance model defines them:

| | Armor (worn) | Weapon (hit) |
|---|---|---|
| **Weak** | full iron set, 4 Infusion II gems: **10 %** of the perfect set | iron sword, 1 Infusion II gem: **9 %** of the perfect weapon |
| **Mid** | full diamond set, 8 Infusion III splash gems: **37 %** | diamond sword, 2 Infusion III splash gems: **43 %** |
| **Perfect** | 12 perfect gems in netherite: **100 %** | netherite weapon, 2 perfect gems: **100 %** |

A weapon holding two *different* gems gives each one **54 %**, so a weapon combo costs real strength.

How a hit works today: **potency** (how strong the effect is) and **duration** both grow with the square root of strength, so a weak sword gives **31 %** potency, mid **66 %**, perfect **100 %** of the strongest potion. Durations are weak **2.5 s** (the floor), mid **5.3 s**, perfect **8 s**. On/off effects (Levitation, Blindness and the like) are always the full vanilla effect, and their strength goes only into duration: weak **2 s** (floor), mid **3.5 s**, perfect **8 s**.

## 1. The proposed rule, in plain words

**Each effect gets one dial, the one that matters most for that effect, and strength turns that dial. Everything else stays fixed and easy to read.**

The key fact: **every hit refreshes the effect, it never stacks.** So while you keep hitting, the target always has the effect, and only its *strength* matters. Duration only decides how long it lingers after you stop (or between bow shots). So:

- **Damage over time and stat debuffs** (Poison, Wither, Hunger, Slowness, Weakness, Mining Fatigue): strength turns **potency**. Duration is a short linger: **4 s at the weakest, rising to 8 s at perfect** (today it starts at 2 to 2.5 s, which is easy to miss).
- **Control effects** (Levitation, Wind Burst): strength turns **how far the target is moved**, at a fixed, readable time.
- **Screen effects on/off** (Blindness, Darkness, Nausea, Glowing): strength turns **duration**, as today. These can't be partial on a player's screen without looking broken.
- **Proc gems** (Weaving, Ender, Infested): strength turns **chance**. Weak gems get a chance you'll actually see in a fight. A short per-target cooldown keeps fast weapons from spamming them.
- **Worn gems** keep today's rule: strength is potency (or chance), always on, up to each effect's cap. The only fix is the weak end of a few gems that do almost nothing there.

**Q1.** Adopt this rule: one dial per effect, chosen as above? (yes/no)
**Q2.** Potency hits linger 4 s at the weakest (up from 2 to 2.5 s), still 8 s at perfect? (yes/no)

## 2. Effect by effect

### 2a. Damage over time

| Effect | Worn today (weak / mid / perfect) | Worn proposed | Hit today (weak / mid / perfect) | Hit proposed | Why | Question |
|---|---|---|---|---|---|---|
| Poison | potency: 0.15 / 0.55 / 1.5 HP/s, never below 1 HP | no change | 0.51 HP/s for 2.5 s / 1.10 HP/s for 5.3 s / 1.67 HP/s (Poison II) for 8 s | same potency; lingers 4 s / 5.3 s / 8 s | Hits refresh, so the per-second rate is what you feel. A 2.5 s weak linger reads as "did it even work". | Q2 |
| Wither | 0.09 / 0.33 / 0.9 HP/s, can kill | no change | 0.31 HP/s / 0.66 / 1.0 HP/s (Wither II, the Wither's skull) | same potency, 4 s / 5.3 s / 8 s; **perfect at 1.5 HP/s**, past the skull | It's the hardest damage effect to get and can kill. A perfect weapon should feel like more than a wither skeleton's hit. | **Q3** Perfect Wither weapon goes past vanilla, to 1.5 HP/s? (yes/no) |
| Hunger | 1 food point per 150 / 40 / 15 s | no change | 1 food point per 21 s / 10 s / 6.7 s, plus at least half a drumstick per hit (players only) | same; lingers 4 s / 5.3 s / 8 s | Fine on players. **Does nothing to mobs** (they have no food). | **Q4** Give Hunger a meaning on mobs: a hungry mob doesn't heal and attacks a little slower? (yes/no) |

### 2b. Stat buffs and debuffs

Beneficial gems in armor already follow the rule (strength = potency, always on, per-effect caps). No change proposed:

| Effect (worn) | Weak / mid / perfect today |
|---|---|
| Speed | +7 % / +28 % / +75 % (Speed II is +40 %) |
| Strength | +0.6 / +2.2 / +6 attack damage |
| Resistance | -4 % / -15 % / -40 % damage |
| Turtle Master | -8 % / -29 % / -80 % damage, and -9 % / -33 % / -90 % speed |
| Regeneration | 0.08 / 0.29 / 0.8 HP/s |
| Haste | mining +10 % / +37 % / +100 %, attack speed +5 % / +18 % / +50 % |
| Absorption, Health Boost | 1.6 / 5.9 / 16 shield; +0.8 / +2.9 / +8 max health |
| Fire Res., Water Breathing, Night Vision, Conduit Power, Invisibility | partial; 10 % / 37 % / the real effect at perfect |
| Jump Boost, Slow Falling, Dolphin's Grace, Luck, Hero, Saturation | partial; past the potion at perfect (Hero 30 %, Saturation full bar) |

Beneficial gems on a weapon buff the target. That's allowed and pointless by design; no change.

Harmful debuffs on a hit:

| Effect | Worn today | Worn proposed | Hit today (weak / mid / perfect) | Hit proposed | Why | Question |
|---|---|---|---|---|---|---|
| Slowness | -5 % / -20 % / -54 % speed | no change | -18 % for 2.5 s / -40 % for 5.3 s / -60 % (Slowness IV) for 8 s | same potency, 4 s / 5.3 s / 8 s | Clear already; only the weak linger is short. | Q2 |
| Weakness | -0.35 / -1.3 / -3.6 attack | no change | -1.3 / -2.6 / -4.0 attack | same, 4 s linger at weak | Works on mobs and players. | Q2 |
| Mining Fatigue | mining x0.73 / x0.30 / x0.04, attack speed -3 % / -10 % / -27 % | no change | mining x0.33 / x0.09 / x0.027, attack speed -9 % / -20 % / -30 % | add a mob version: the mob's attack cooldown gets 30 % / 65 % / 100 % longer | **Does nothing to mobs today**: mobs don't mine, and their melee timer ignores attack speed (fixed 1 s in vanilla). | **Q5** Mining Fatigue hits slow mobs' attack rate (up to twice as slow at perfect)? (yes/no) |
| Turtle Master | its own pair | no change | -21 % / -59 % / -90 % speed, but takes 19 % / 59 % / 80 % less damage | no change | A real choice: a near-frozen target that's hard to kill. Good for escaping or holding a door. | none |

### 2c. Control and movement on hit

| Effect | Worn today | Worn proposed | Hit today | Hit proposed | Why | Question |
|---|---|---|---|---|---|---|
| **Levitation** | always on, drifts the wearer up 0.19 / 0.27 / 0.45 blocks/s | no change | full vanilla Levitation I (0.9 blocks/s) for 2 s / 3.5 s / 8 s: rises about 1.5 / 3 / 7 blocks; only perfect causes fall damage (about 4 HP) | **(a)** fixed **4 s float**, rise speed scales: **0.3 / 0.6 / 1.8 blocks/s** (perfect = Levitation II speed): about 1.2 / 2.4 / 7 blocks | Answers the owner's question. A weak hit is a slow, long float (the target is helpless and can't walk toward you), not a hop that's over before you see it. The perfect weapon throws them high and fast. The slow drift already exists for armor. | **Q6** Levitation hits: (a) slow float, speed scales (proposed), or (b) keep vanilla speed, duration scales (today)? |
| | | | | Re-hits refresh the 4 s; a bow can keep someone up as long as it keeps hitting (shulkers do this too) | Fun to juggle, but a gemmed skeleton could keep a player up until the fall kills them. | **Q7** Cap juggling: one target can be kept up at most 10 s in a row, then it must land? (yes/no) |
| **Slow Falling** (beneficial, used as a combo) | falls slower, -18 % / -66 % / -100 % fall damage | no change | target falls slower for 2.5 / 5.3 / 8 s **and takes 31 % / 66 % / 100 % less fall damage** | on a hit target, slow falling **only slows the fall**; it no longer protects from fall damage | The owner wants Levitation + Slow Falling to keep enemies in the air. Today the Slow Falling hit cancels the very fall damage Levitation sets up. | **Q8** Slow Falling on a hit target gives hang time only, no fall-damage protection? (yes/no) |
| **Wind Burst** | landing bounce 0.06 / 0.22 / 0.6 blocks/tick, fall damage x0.9 / x0.63 / none | **minimum bounce 0.25** (a visible hop) | thrown up 0.09 / 0.39 / 0.9 blocks/tick (hop of about 0.05 / 1 / 4 blocks), pushed 0.06 / 0.26 / 0.6; Wind Burst mace x1.6 | **minimum launch 0.35 up** (about half a block, like a jump), so 0.35 / 0.45 / 0.9; push and mace bonus unchanged | **A weak Wind Burst hit and a weak worn bounce are invisible today** (a 5 cm hop). | **Q9** Give Wind Burst a visible minimum, armor and weapon? (yes/no) |

### 2d. On/off screen effects (and Glowing, Infested)

Worn, all of these are always on (owner's rule). Blindness, Darkness and Nausea are partial and steady on the wearer's screen (9 % / 33 % / 90 % of the full look). Glowing is just on. No change to worn.

| Effect | Hit today | Hit proposed | Why | Question |
|---|---|---|---|---|
| Blindness | full vanilla effect for 2 s / 3.5 s / 8 s | players: unchanged. **Mobs (new):** a blinded mob sees only 75 % / 47 % / 20 % of its usual range, so it loses you if you back off | **Does nothing to mobs today**: vanilla Blindness and Darkness only change a player's screen. | **Q10** Give Blindness and Darkness a mob version (shorter sight)? (yes/no) |
| Darkness | full vanilla effect for 3 s (floor) / 3.5 s / 8 s | as Blindness (players unchanged; mobs: shorter sight) | Same problem. | Q10 |
| Nausea | full vanilla for 5 s (floor) / 5 s / 8 s | players: **4 s / 6 s / 10 s**, so strength shows. Mobs (new): a dizzy mob walks unsteadily and loses its path now and then | Today weak and mid are the same, and it does nothing to mobs. | **Q11** Nausea hits 4 / 6 / 10 s and a mob version? (yes/no) |
| Glowing | full vanilla for 2 s / 3.5 s / 8 s | **5 s / 10 s / 20 s** (a spectral arrow gives 10 s) | Glowing is for tracking; 2 s is gone before you look. It's harmless, so the perfect weapon can go past vanilla. | **Q12** Glowing hits last 5 / 10 / 20 s? (yes/no) |
| Infested | vanilla Infested (10 % per hurt, 1 to 2 silverfish) for 2 s / 3.5 s / 8 s | **fixed 6 s; chance per hurt 10 % / 20 % / 35 %** | Strength should be how often silverfish burst out. The silverfish attack players, often the wielder. | **Q13** Silverfish from an Infested *hit* attack (a) anyone, vanilla style, or (b) the creature they burst from? |

Worn Infested: chance 0.9 % / 3.3 % / 9 % per hurt. It's a harmful gem, so no change.

### 2e. Proc and chance gems

| Effect | Worn today (weak / mid / perfect) | Worn proposed | Hit today | Hit proposed | Why | Question |
|---|---|---|---|---|---|---|
| **Weaving** | moves 10 % / 37 % / 100 % of the way to full speed in cobwebs; perfect: webs don't hold you at all | no change | chance per hit to drop one real cobweb at the target's feet: **3 % / 15 % / 35 %**; never into a non-air block; the web stays | chance **10 % / 25 % / 50 %**; the perfect weapon drops vanilla Weaving's **2 to 3 webs** around the target; at most once per 3 s per target | Answers the owner: today strength changes only the chance, and 3 % at weak is "never happens". A webbed target is held to 25 % speed sideways and 5 % up/down, so a web is a big deal; the cooldown stops a fast sword from cocooning. | **Q14** Weaving hits 10 / 25 / 50 %, multi-web at perfect, 3 s cooldown? (yes/no) |
| | | | | | Hit webs stay forever, so a long fight litters the area with webs. | **Q15** Should webs from *hits* fade after 30 s (worn and placed webs stay vanilla)? (yes/no) |
| **Oozing** | landing bounce 9 % / 33 % / 90 % of the fall speed, fall damage x0.9 / x0.63 / none | **minimum bounce 25 %** | on a kill only: 1 / 1 / 2 small slimes that belong to the wielder; neutral, drop nothing, at most 8 | perfect: **3** slimes; and the slimes **draw hostile mobs' attention** (decoys) | **The slimes do nothing useful today**: tiny slimes deal no damage, and nothing targets them. | **Q16** Oozing slimes act as decoys that nearby monsters attack? (yes/no) |
| Ender | 10 % / 37 % / 100 % chance a projectile hit becomes a chorus blink instead (5 / 8 / 16-block range); boxed in, the hit lands | no change (perfect boss stays trappable) | see section 3 | see section 3 | | |
| Breeze | 10 % / 37 % / 100 % chance to bounce a projectile back | **wind charges always pass**, as with a real breeze | see section 3 | see section 3 | Vanilla breezes can be hit by wind charges. Today a perfect Breeze boss is immune to every projectile. | **Q17** Worn Breeze lets wind charges through, as vanilla does? (yes/no) |
| Silence | 10 % / 37 % / 100 % of the wearer's movement vibrations dropped | no change | see section 3 | see section 3 | | |

## 3. Ender, Silence and Breeze on a weapon (and what's broken today)

Today these three just give the target the gem's own worn effect (34 % / 73 % / 100 % of it, for 2.5 / 5.3 / 8 s). That is worse than nothing:

- **Ender:** inert on almost everything. The blink only checks targets that *also wear gems*, and on those it helps them dodge your arrows.
- **Breeze:** the target bounces your next arrows (and your friends' arrows) back, **34 % / 73 % / 100 %** of the time. A perfect Breeze bow makes its own second arrow bounce.
- **Silence:** the target's footsteps go quiet: it helps the target sneak past sculk.

Proposals:

**Ender hit: blink the target away.** Chance per hit **10 % / 25 % / 40 %** to send the target on a chorus-fruit teleport, range **6 / 10 / 16 blocks**, at most once per 2 s per target. It uses chorus fruit's own safe-landing rules (never into lava, water or a wall). The Wither, the Ender Dragon and the Warden are never moved. Use: peel a creeper or a crowd off you, break up a fight, PvP disruption.
**Q18** Ender hit: (a) random chorus blink, as above, or (b) a blink straight *away from you* by a set distance (more predictable, and harder to abuse near cliffs)?
**Q19** Can gemmed mobs blink *players* with Ender weapons? A random blink can drop a player into a cave below. (yes/no)

**Silence hit: quiet kills.** For the duration (2.5 / 5.3 / 8 s, by strength), the target makes no sound and no vibration (being hurt, dying), and **it can't call for help**: no zombie reinforcements, and zombified piglins, piglins, wolves, bees and iron golems don't rally to it. Use: fight in an Ancient City without waking sculk, kill one zombified piglin without the whole crowd.
**Q20** Silence hit = no sounds, no vibrations, no calling allies? (yes/no)
**Q21** Also deafen a hit *player* (the game's sounds get 30 % / 60 % / 90 % quieter for them)? (yes/no)

**Breeze hit: blow the target's own shots off course.** For the duration (2.5 / 5.3 / 8 s), arrows, tridents and fireballs the target fires veer off: a small spread at weak, most shots miss at perfect (spread x3 / x6 / x12). Use: it counters skeletons, pillagers, blazes and archers, and it's the weapon partner to Breeze armor.
**Q22** Breeze hit = the target's projectiles veer off? (yes/no)

## 4. Synergy notes (for boss and player kits)

Strong combos the proposal makes, and what to watch:

- **Levitation + Slow Falling (hits):** up slowly, hang in the air, then a full-damage landing (needs Q8). On one weapon each gets 54 %; split across a bow and a sword it's at full strength. Watch: mobs using this on players. Q7's 10 s limit is the safety valve.
- **Levitation + Wind Burst:** a wind launch on a rising target adds height. A perfect pair can kill from fall damage alone. Fine for players; for a boss it should be a telegraphed move, not every hit.
- **Weaving armor + Weaving weapon:** web the target, then fight inside the webs while it's held to 25 % speed and you move freely (perfect armor). This is the strongest melee lockdown here. The 3 s cooldown (Q14) keeps it from being permanent. In PvP it's brutal at 50 %. A Weaving boss should wear Weaving armor so its own webs don't trap it.
- **Weaving or Slowness + Poison / Wither:** a held target can't walk out of the damage. Expected and fine.
- **Ender armor + Breeze armor:** projectiles either bounce or blink the wearer. A perfect boss with both is melee-only. That's OK if it's trappable (Ender) and wind charges still hit (Q17). Don't give one boss *both* perfect plus a melee answer like Levitation.
- **Breeze hit + Breeze armor:** the anti-archer kit. Strong against skeleton-heavy places and ranged bosses.
- **Silence hit + Invisibility armor:** the stealth kit (quiet kills, and mobs notice you late). Strong against zombified piglin crowds and in Ancient Cities; harmless to play against.
- **Blindness or Darkness from mob weapons on players:** a perfect weapon refreshes 8 s of real Blindness on every hit, nearly permanent in melee. For bosses, prefer a weak or mid gem (2 to 3.5 s), or Darkness's pulse.
- **Ender hit vs melee builds:** an anti-combo. Blinking the target away ends your own combo, so it's an escape or crowd tool, not a damage tool. Pair it with a bow or with Glowing (find them after the blink).
- **Mob-side Blindness (Q10) + Invisibility armor:** mobs lose you very quickly. Strong for escaping; fine.

## Questions for the owner (all in one place)

1. **Q1** One dial per effect (potency for damage and debuffs, distance for control, duration for screen effects, chance for procs)? yes/no
2. **Q2** Potency hits linger 4 s at the weakest (up from 2 to 2.5 s), still 8 s at perfect? yes/no
3. **Q3** Perfect Wither weapon goes past vanilla, to 1.5 HP/s? yes/no
4. **Q4** Hunger hits on mobs: no healing and slightly slower attacks? yes/no
5. **Q5** Mining Fatigue hits slow mobs' attack rate (up to twice as slow)? yes/no
6. **Q6** Levitation hits: (a) slow float, rise speed scales, fixed 4 s, or (b) vanilla speed, duration scales (today)?
7. **Q7** Juggling cap: at most 10 s in the air in a row from Levitation hits? yes/no
8. **Q8** Slow Falling on a hit target = hang time only, no fall-damage protection? yes/no
9. **Q9** Visible minimum for Wind Burst (worn bounce and hit launch)? yes/no
10. **Q10** Blindness and Darkness hits shrink a mob's sight range? yes/no
11. **Q11** Nausea hits 4 / 6 / 10 s, plus unsteady walking on mobs? yes/no
12. **Q12** Glowing hits 5 / 10 / 20 s? yes/no
13. **Q13** Infested-hit silverfish attack (a) anyone, or (b) the creature they burst from?
14. **Q14** Weaving hits 10 / 25 / 50 %, 2 to 3 webs at perfect, 3 s cooldown per target? yes/no
15. **Q15** Webs from hits fade after 30 s? yes/no
16. **Q16** Oozing slimes are decoys that monsters attack, 3 slimes at perfect? yes/no
17. **Q17** Worn Breeze lets wind charges through, as vanilla does? yes/no
18. **Q18** Ender hit: (a) random chorus blink, or (b) blink straight away from the attacker?
19. **Q19** Gemmed mobs can Ender-blink players? yes/no
20. **Q20** Silence hit: no sounds, no vibrations, no calling allies? yes/no
21. **Q21** Silence hit also deafens players? yes/no
22. **Q22** Breeze hit: the target's own projectiles veer off? yes/no
