<!-- dte:C1,C2,C4,C5,B11 -->
# Docs consistency audit (2026-11-16)

Read-only audit of specs, player docs, the decision tree and BACKLOG.md against what is on disk. Other agents were editing at the time (Infused Gems had uncommitted changes to `balance.json`, `GemTotals`, `WornEffects` and others; Reworked Mobs to its sleeper blockstates), so re-check the Infused Gems numbers once that work lands. Numbers for Infused Gems were checked against `tools/gem-balance.py` and `balance.json` (`--check` passes, the export is up to date).

Severity: **wrong** = contradicts the code or data; **missing** = decided or built but not written down; **stale** = old wording that no longer misleads much, or history that should be marked as superseded.

Checks that passed: every `[[link]]` in the vault resolves (29 pages, 0 broken; script in scratch). No `dte:` tokens, code names, "Infusion" or "Flawed Gem" in player docs. Finding Gems' loot table (every place and chance, level contents) matches `balance.json`. Undead tuning numbers (health, speed, noise table, robbing odds, talisman ranges, spawn cut 15) match `UndeadTuning`/`GraveTalisman`/`ModGameRules`. Glider constants (0.03, 0.6) and the datapack recipes (times, XP) match the player pages. `dte validate --as B` is OK, with one warning: C5's title is 138 characters (limit 100).

---

## Bubba's Infused Gems

### Spec vs code (`specs/effect-gems.md`)

1. **wrong** `:46` Gem Power V is "0.87". `balance.json` has 0.88. Fix: `V prismarine shard 0.88`.
2. **wrong** `:46` Worn strength is "capped at **0.90**". Since the top-end pass, worn caps are per effect (1.0 to 2.5, `worn_cap`); only harmful worn gems and the "share of their own best" gems stay at 0.90. Fix: "...worn strength is measured against the effect's strongest vanilla level (1.0); the build cap is 0.90 (`CAP`) and each effect's worn total is rescaled to its own `worn_cap` (table below, Per-effect worn caps)".
3. **wrong** `:46` "Poison and Wither (the last two at half weight)". This was replaced by rarity weights (`:103`). Fix: drop "(the last two at half weight)" and add "weighted by rarity (below)".
4. **wrong** `:152` Hero of the Village "at most 20 % at the cap". The code and `:136` say 30 % (`HERO_BEST_DISCOUNT 0.30`). Fix: "at most 30 % at the cap".
5. **wrong** `:101` "The model checks an inverted gem is never perfect (at most 0.72)". The model now reports 0.66, which is 0.75 x Gem Power V 0.88. Fix: "(at most 0.66)".
6. **wrong** `:91` "a perfect Gem of every one of the 36 gem effects" and "a lingering Gem Power VI Infused Gem". The code (`ModCreativeTabs`) adds an Infused Gem of each effect (lingering, or item form for item-sourced gems) and a perfect Gem of each, and there are 39 gem effects (40 in `balance.json`, Bad Luck being drawback-only). Fix: "The creative tab holds a Gem Power VI Infused Gem (lingering, or item form) and a perfect Gem of each of the 39 gem effects."
7. **wrong** `:164` Weaving hit "with chance 0.35 x s/cap". This was replaced by the owner's dial at `:127` (10 / 25 / 50 %, `HIT_CURVES`). `WEAVING_WEB_CHANCE_BEST = 0.35` in the model looks dead. Fix: "a hit webs the target by the Weaving dial (above)".
8. **wrong** `:171` "On/off harmful effects apply the vanilla effect for s/cap of 8 s, floored". Nausea and Glowing now follow their own dials (`:124`). Fix: add "(Nausea and Glowing: their dials above)".
9. **stale** `:64` Several owner answers here have since been superseded but aren't marked: Q8 (half weight, now rarity weights), Q14 (Levitation "slow drift 0.02", now 0.16 to 0.46 blocks/s at `:152`), Q28 (20 %, now 30 %), Q1 "cap 0.90" (now per-effect worn caps). Fix: after each one, add "(superseded: see <section>)".
10. **stale** `:25`, `:30`, `:41` "just under the strongest vanilla potion level, never equal to it", "lingering + diamond base". The top end now reaches or passes the potion, and nether star is the strongest base. Fix: strike them through like `:21`, with "superseded by Per-effect worn caps / base gem order".
11. **stale** `:31` "Still open: the strength ladder" (decided at `:58` and `:144`), `:34` "Six infused items in all" (one item now, `:77`), `:40` "nether star's place to settle", `:42` "Instant effects ... to settle". Fix: strike them through or mark "(decided: see ...)".
12. **stale** `:3` "(spec, early design)". Fix: "(spec)".
13. **stale** `:97` The item-sources list leaves out sculk shrieker (Darkness), chorus fruit (Ender) and breeze rod (Breeze), which are all in `ITEM_SOURCES`. Fix: add them to the list.
14. **stale** `:152` The field is named `lastEquipFillTick`; it is now the attachment `last_equip_fill` (as `:142` says). Levitation's "up to 0.45" is 0.464 in the model (fine as written; could say "about 0.46").
15. **stale** `:177` "Server (51)": the mod has about 86 `@GameTest` methods. The client parts listed are 7, but `ClientTests` has 15 (`beyond, colors, dials, enchanting, hits, inventory, invisible, jump, levitation, looks, mobs, screen, smithing, weaving, worn`). Fix: drop the count and list all parts.
16. **missing** Gems in beacons are not in the spec. `balance.json` exports `beacon_share 0.5`, and `tools/gem-balance.py:45` records the decision (a gem in a beacon broadcasts its effects to every living thing in range, Gem Power capped by the pyramid, strength = 0.5 x a full netherite set's; marked "agent's choice"). `compat/GemBeaconPayload` and `GemBeaconGameTest` exist. Fix: add a "Gems in Reworked Beacons" section, and flag `BEACON_SHARE` to the owner as an agent-invented number.
17. **missing** Milestone objectives have no line for `GemBeaconGameTest`. Fix: add one.

### Player docs (`player-docs/Projects/Bubba's Infused Gems/`)

18. **wrong** `Bubba's Infused Gems.md:23` "Gems are never as strong as a real potion". The perfect set gives the full potion for combat effects and goes past it for movement effects. `:21` says "permanent but partial version" for the same reason. Fix `:23`: "Gems below the very best are weaker than a real potion; only a perfect set matches the strongest potion (and goes past it for movement effects). Gems of the same effect stack, and some gems carry a drawback." Fix `:21`: replace "a permanent but partial version of the potion's effect" with "a permanent version of the potion's effect".
19. **wrong** `Enchanting Gems.md:65` The inversion example reads "Hunger (72%) ... Gem Power V". It comes out at 66 % (medium 0.75 x Gem Power V 0.88). Fix: "Hunger (66%)".
20. **wrong** `Infused Gems Commands.md:27` "Poison 1.24 HP/s for 6.3 s (48%)". By the current formula 48 % gives 1.16 HP/s for 5.5 s (the spec's `:175` example). Fix: "*Poison 1.15 HP/s for 5.5 s (48%)*".
21. **stale** `Gem Effects.md:33` "Values are for the best build (90%)". Since the top-end pass the best build is the perfect set at 100 % of each effect's own scale. Fix: "Values are for the best build (the perfect set); weaker builds give less." `:5` "permanent, partial potion effects": change it to "permanent potion effects" (same reason).
22. **stale** `Home.md:13` "for permanent, partial effects". Fix: "for permanent effects".
23. **missing** No page says that gems work in beacons with Reworked Beacons. That's fine while Beacons is unreleased, but it is needed before that release (Compatibility section on the overview page).

---

## Bubba's Reworked Mobs

### Spec vs code (`specs/undead.md`)

24. **missing** Warding is built in this mod but has no spec: `warding/` (`TotemOfWardingItem`, `Wards`, `WardFleeGoal`, `WardingLoot`, `WardingTuning`), `SleeperBells`, `BellBlockMixin`, `PhantomSpawnerMixin`, the node evaluator mixins, `WardingGameTest`, `WardingClientGameTest`, and 4 Warding tests in core. The owner's decisions exist only in `docs/research/area-gems-and-warding.md:261-277` and in commits b071305, 190eca5, 9b245c3, 34bab06 and c288f61. These cover: Totem of Warding (a found item, more common than the Totem of Undying); held pulse every 10 s, monsters within 8 blocks flee about 5 s, sleepers calmed a little, vanilla cooldown sweep; in a beacon, 1-3 layers stop spawns and keep monsters out, 4 layers also keep sleepers asleep; wards stop natural spawns only; strong light (level 12+) calms sleepers (x0.7); bells are 250 points out to 32 blocks. Fix: add a "Warding" section to `undead.md` (or `specs/warding.md`), with the numbers from `WardingTuning` and milestone objectives for `WardingGameTest`. Under the new CLAUDE.md rule this arrives via `docs/reports/`, but no report exists yet.
25. **wrong** `:94` "Every humanoid undead variant sleeps, for now: skeleton, stray, bogged, parched, wither skeleton; zombie, husk, drowned, zombie villager". `UndeadVariant` has only SKELETON and ZOMBIE. Fix: "Built: skeleton and zombie (sitting and lying). Planned: stray, bogged, parched, wither skeleton; husk, drowned, zombie villager."
26. **stale** `:161` "Core (`ReworkedMobsCoreGameTest`, 8)". The class has 12 (8 listed, plus 4 Warding: held totem pulse, strong light, bells, ward). `:169` client names leave out `warding`. Fix: update the count and list, and add the part name (CLAUDE.md's Commands line "Reworked Mobs: sleepers, displays, gait, talisman" also lacks `warding`).
27. **stale** `:124` and `:131` "Grave Talisman (v0)", "dark (v0)". Fix: drop "(v0)".
28. **stale** `:47`, `:89` "the hook for the planned anti-waking magic" / "Planned later: magic that prevents waking". The Totem of Warding now does this. Fix: point to the Warding section.

### Player docs (`player-docs/Projects/Bubba's Reworked Mobs/`)

29. **wrong** `Sleeping Undead.md:9`, `:15` "Skeleton: sitting slumped". The code gives skeletons sitting and lying poses too (`UndeadVariant.SKELETON`: SITTING, LYING; the testworld spec's graveyard also has lying skeletons). Fix: "Poses: sitting or lying on its back (both)". `:49`: "Sleeping Skeleton and Sleeping Zombie (sitting and lying)" is already right. (The lying skeleton pose is committed, in 9cb5880.)
30. **missing** Warding is player-visible but has no page: the Totem of Warding (held pulse, where it's found), bells waking sleepers from far away, strong light calming sleepers, and wards stopping natural spawns. Fix: a `Totem of Warding.md` topic page, a "Warding" section on the overview, and lines in `Noise and Waking.md` (bells are loud; bright light makes sleepers a little harder to wake) and `Monster Spawning.md` (warded areas get no natural spawns). The overview's History needs a line too.
31. **stale** `Bubba's Reworked Mobs.md:21` "skeletons and zombies lie dormant". That's correct for what is built; recheck it when more variants land.

---

## Bubba's Crafting Tweaks (datapack + mod)

32. **wrong** `specs/crafting-tweaks.md:13` The player page path is "`player-docs/Projects/Bubba's Crafting Tweaks.md`". The vault now uses a folder. Fix: "Player pages: `player-docs/Projects/Bubba's Crafting Tweaks/` (overview, Crafting Tweaks Recipes, The Glider, Synthetic Diamond Ore, Companion Mod), updated whenever a recipe changes."
33. **wrong** `specs/crafting-tweaks-mod.md:85` The client objective says "the 32x pack on by default". `:52` makes the 32x pack opt-in (`NORMAL`); what is on by default is the bundled resource pack (`:76`). Fix: "the bundled resource pack on by default".
34. **stale** `specs/crafting-tweaks.md:19` "Stone → quartz (blasting)". The recipe (`blast_quartz.json`) takes andesite, diorite or granite; stone makes iron nuggets. Fix: "Andesite/diorite/granite → quartz (blasting)".
35. Player docs: no contradictions found. Recipes, times and XP match the data; the Glider numbers match `GliderHandling`.

---

## Bubba's Reworked Beacons (`specs/reworked-beacons.md`)

36. **wrong** `:43` The table row says "nothing to put in it until another mod supplies items (to decide: should vanilla payments still work alone?)". That was decided at `:66-79` (standalone gem-base payments, built as `StandalonePayload`, tested by core `standalonePaymentBuysItsPower`). Fix: "gem-base payments buy vanilla's six powers (Standalone, below)".
37. **wrong** `:89-91` Milestone objectives say "None yet (not built)". The payload API, standalone payments, pyramid cap, pairing and hanging placement are built, and there is a core suite of 4 plus `BeaconGameTest` and a client test. Fix: list the core suite (standalone payment, pyramid cap, paired box, hanging beacon) and a line per milestone class.
38. **stale** `:64` "Details are open in `docs/research/...`". They are answered (`:84`) and built. Fix: point to the Warding spec once it exists (item 24).
39. **missing** "As built" notes for the beacon side: `BeaconLogic`, the menu and screen, the `HangingBeacon` added property (the B15 technique, like synthetic ore, with its handshake), `BeaconRendererMixin` for the paired beam. Expected via a docs report.
40. **missing** No player docs yet. That's correct while unreleased, but add the project folder before the first release (B11), and a Home row.
41. **stale** `docs/research/area-gems-and-warding.md:6` "Nothing is built or decided". Fix: "Status: owner decisions at the end; built in Reworked Mobs and Reworked Beacons."

---

## Test world (`specs/testworld.md`, `player-docs/Getting Started/Bubba Test World.md`)

42. **wrong** `specs/testworld.md:61`, `:63`, `:67` These still say "Infusion" (`gems kit <effect> [material] [infusion]`, "Infusion 6 is perfect", "Perfect vs Infusion V", "Infusion V twin", "Infusion V 88%"). Signs now read "Gem Power V 88%" (commit df2532e). Fix: replace with "Gem Power" / `[power]` / "Gem Power 6".
43. **wrong** (in the world itself, player-visible) `testworlds/builder/.../grounds_wardrobe.mcfunction`, `grounds_pool.mcfunction` and `grounds_lava.mcfunction` still have 7 signs reading "Inf. V 88% +". The rename missed them. Fix: "GP V 88% +" (or "Gem Power V" if it fits in 15 characters), then rebuild the world.
44. **stale** `specs/testworld.md:55` "a Flawed Gem" in the ready-made gems chest. That item is the drawback-only gem. Fix: "a drawback-only gem".
45. **stale** `specs/testworld.md:86` says the tour takes "124 screenshots"; `specs/quality.md:35` says "73 shots". Fix: re-measure and make them agree.
46. **missing** There's no Warding or Beacons station yet (Totem of Warding, a bell near sleepers, lit vs dark sleepers, sky vs paired beacons). Add to the spec and world when the owner wants to playtest them.

---

## Repo-wide specs

47. **wrong** `specs/releasing.md:3-6` "CI builds, tests and uploads it to Modrinth". B14 says GitHub Releases until a project meets Modrinth's AI rules, and CLAUDE.md points to this file for "how". Fix: "A release is published by pushing a tag `<project>/v<version>`. CI runs the full suite (core, milestone and client tests) and publishes to Modrinth if the project meets the AI rules in `docs/knowledge/publishing.md` (B14: disclosure on, no AI images, a substantial human-made part), otherwise to GitHub Releases. Nothing is uploaded by hand."
48. **wrong** `specs/player-docs.md:60` "updates the project's pages in the same change". This conflicts with the owner's new rule (CLAUDE.md: builders write `docs/reports/<project>-docs-<date>.md`; the docs are merged separately). Fix: "...ends up on the project's pages: builders note it in `docs/reports/`, and the orchestrator or a docs agent merges it (CLAUDE.md)."
49. **missing** `specs/build.md` or `specs/quality.md`: the docs-report workflow (where reports go, who merges, delete after merging) lives only in CLAUDE.md. Fix: one paragraph in `specs/player-docs.md`.
50. **stale** `specs/quality.md:31` mentions only the old Reworked Mobs client parts, and the core counts in `:36-38` ("core (5)", "(9)", "(9)") predate Warding (Crafting Tweaks 4, Infused Gems 8, Reworked Mobs 12, Beacons 4). Fix: update the counts and add a Beacons row when measured.
51. **missing** Two owner principles appear only inside the Infused Gems spec (`:122`), but they now govern Beacons too (`reworked-beacons.md:55` cites "owner's equality principle"). They are (a) mobs and players are affected the same way, with no mob-only or player-only cases, and (b) no hidden cooldowns. A third, "vanilla mechanics, never hand-drawn visuals" (`effect-gems.md:120`), is the same kind of rule. Fix: state them once in a shared design-principles spec (or ask the owner whether they want B leaves, since CLAUDE.md lists "design rules the owner sets" as B ring). Default: spec, offered as a possible leaf.

---

## Decision tree

52. **wrong** C4 effectGems (human-held, needs the owner's OK). The title says "put weakened, permanent potion effects on armor" and the body says gems "never give full potion strength, and carry drawbacks". That no longer matches the project: gems go in weapons too, include effects that aren't potions (Wind Burst, Ender, Breeze, Silence and others), a perfect set reaches or passes the strongest potion, and drawbacks are a chance roll, not a rule. Proposed title (89 characters): **"Bubba's Infused Gems: hard-to-get gems giving permanent effects through armor and weapons"**. Proposed body: "A mod adds gems, each embodying an effect (from a potion or an item that carries one), set into armor (affecting the wearer) or weapons (affecting what they hit) and removed again. They are very strong, so they are hard to get: below the very top they are weaker than the potion, only a perfect build reaches the strongest vanilla level, and enchanting can roll drawbacks."
53. **stale** C5 reworkedBeacons (ai, unratified). The title is 138 characters (validate warns at 100) and leaves out the standalone gem-base payments. Proposed title (99 characters): **"Bubba's Reworked Beacons: one item slot replaces fixed powers; sky column or paired underground box"**. The leaf date is 2026-11-15, but its Why and the spec say "owner, 2026-11-16" (the commits are 2026-11-15 local time, so pick one). The owner should ratify it.
54. C1, C2, C3: the titles are still accurate. C1's body could say that Warding (Totem of Warding) now belongs to this mod, but that is spec detail, so no change is needed.

---

## BACKLOG.md

55. **stale** `:13` "Part 1 next pass: ... armor-shredding fists, Smite insta-kill, bow and headshot rules, stagger animation, zombie grouping". All of those are built (`undead.md` Tuning). Fix: "Part 1 next pass: remaining undead variants, graves and crypts in worldgen, hybrid rendering, real 16x/32x art."
56. **stale** `:11` "Warding and sleepers: light, soul fire and a 'lull' tag ... plus the Warding bell's toll from Infused Gems". Light calming and loud bells are built, and Warding moved to Reworked Mobs plus Beacons (not a gem or bell mount). Fix: keep only what's still open ("soul fire and a 'lull' game event tag that calm sleepers").
57. **stale/duplicate** `:21` "Area gems and Warding ... (recommended: bells first, beacons later) and a Warding gem". This was superseded by the Reworked Beacons project and the Totem of Warding (not a gem), and it duplicates `:35-36`. Fix: delete it.
58. **stale** `:22` "Drawbacks (shelved by the owner)". Drawbacks are built (enchanting rolls). Fix: delete, or "Drawback sizes" (already at `:24`).
59. **stale** `:33` "Gem inversion ... Not designed yet". It's built (`effect-gems.md`, Gem inversion). Fix: delete.
60. **stale** `:20` The heading says "Effect gems". Fix: "Bubba's Infused Gems".
61. **stale** `:36` Reworked Beacons "Not built yet". The payload API, standalone payments and pairing are built. Fix: "Being built; open questions in specs/reworked-beacons.md."
62. **check** `:32` "Hidden timers to review: Absorption's 30 s refill and 30 s equip-fill throttle". This is still true in code (`refill_ticks 600`, `last_equip_fill`), so keep it. It conflicts with the owner's no-hidden-cooldowns principle, so it is worth raising with the owner.
