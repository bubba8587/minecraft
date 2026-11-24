<!-- dte:C4,B9,B15,B17,B18,B19 -->
<!-- covers: docs/research/effect-gems-*, tools/gem-balance.py, tools/gem-textures.py, mods/bubbas_infused_gems/* -->
# Bubba's Infused Gems (spec)

Builds the C4 effectGems direction.

- **Name:** Bubba's Infused Gems (owner's name). Mod id `bubbas_infused_gems`, Modrinth slug `bubbas-infused-gems`, code in `mods/bubbas_infused_gems`, package `io.github.bubba8587.bubbas_infused_gems`.
- **Independent** of the other mods. With Bubba's Reworked Beacons installed, gems also work in beacons (Gems in Reworked Beacons, below).
- **Owner's principles:** B17 vanillaFirst, B18 equalTreatment, B19 noHiddenCooldowns.
- Every gem effect works in armor and on weapons; none is left for a later version.

## Owner's brief

- Gems are a class of items, each embodying a **permanent** potion effect, added to and removed from **armor** (and, decided later, weapons).
- Very strong, so **hard to get**.
- They **can't give full potion strength** with infinite duration, and they need **drawbacks**. (At the top end this changed: below the perfect build gems stay weaker than the potion, but the perfect set reaches or passes it; see Per-effect worn caps.)

## Prior art (checked with tools/prior-art.py)

- **Effect Gems** (datapack, 26.3, CC-BY-NC-ND: inspiration only): gems held in the off hand or main hand give effects; dropped by rare "Gem Zombies". Same name, different mechanic, so our mod needed another name.
- **TrimEffects** (26.3, MIT): each armor trim grants +1 level of an effect per piece, capped at vanilla level II. **Trim Resonance** / **BetterTrims** (26.3): trim material and pattern give stacking abilities. Closest to ours. They show that vanilla's smithing table and armor trims already "put something into armor" as data on the item, with no new GUI. Compatibility: these mods also give armor effects, so stacking with them needs a look.
- Apotheosis/Zenith-style socket systems exist only on old versions or other loaders.

## Core design (owner decisions)

### Where gems go

- **Data on the item via vanilla stations, no new GUI.** A smithing table adds a gem (like a trim); a grindstone removes it and returns the gem (like stripping enchantments). The gem is stored on the item and shown in its tooltip.
- **Slots:** 3 on every armor piece, any material; 2 on every weapon. The turtle helmet takes none (owner).
- **Any gem can go into armor or a weapon; the slot decides who its effects reach.** Armor: the wearer. Weapon: what the weapon hits (bows and crossbows: the arrow). There are no armor-only or weapon-only gems. A Poison gem in one's own armor poisons the wearer (intentionally pointless); a Regeneration gem on a sword heals the target.
- **Weapons:** melee weapons (swords, axes, maces, tridents, spears) apply the gem on hit. **Bows and crossbows turn each fired arrow into its tipped form:** the arrow carries the gem's effect at the gem's strength, flies, trails and hits like a tipped arrow, and is picked back up as a **plain arrow** (no free tipped arrows). Firing a real tipped arrow of the same effect: the stronger applies; they don't add up.
- **Broken gear pops its gems out** (dropped, not lost).
- **The gem itself is enchanted**, never the armor or weapon.

### Making gems (two steps, both vanilla stations)

1. **Brewing stand:** a potion (or an item that carries the effect) + a base gem brews an **Infused Gem**.
2. **Enchanting table:** the Infused Gem is enchanted like gear (XP levels, lapis, bookshelves, the seed and its preview). The three rows are three tiers, each locking in one outcome by chance:
   - **Tier 1:** a weak form of the main effect, or a weak drawback.
   - **Tier 2:** a medium form of the main effect, alone or with a weak other effect (plain medium added by the owner), or with a weak drawback.
   - **Tier 3:** the strongest form, alone or with a weak or medium other effect, or with a weak or medium drawback.
   - Exact outcomes and odds: Enchanting, below.
3. The result is the finished **Gem**, ready for the smithing table.

- **The potion form sets the highest tier (owner):** normal potion tier 1, splash tiers 1-2, lingering all three. An Infused Gem brewed from an item reaches all three (owner: every gem should be able to reach the cap unless it proves too strong; collecting twelve of the item is the cost). So gems start early and the strongest results stay endgame. The enchanting preview and vanilla's reroll trick keep bad rolls from feeling final.
- **Nothing raises an Infused Gem's form afterwards (owner):** the form is the potion's.

### One item, Gem Power levels

- To keep the item count down there is **one Infused Gem item with one model**, not six. The base gem becomes its **Gem Power level**, a second progression line ordered by how hard the base gem is to get (owner):

  | Gem Power | Base gem | Factor | Where from |
  |---|---|---|---|
  | I | Amethyst shard | 0.40 | first trip underground |
  | II | Nether quartz | 0.50 | first Nether trip |
  | III | Ender pearl | 0.62 | farmable |
  | IV | Diamond | 0.75 | common in chest loot |
  | V | Prismarine shard (owner: shard, not crystals) | 0.88 | ocean monuments |
  | VI | Nether star | 1.00 | the Wither |

- **Base gems affect strength only (owner):** no special traits, nether star included; anything more is too complicated. No loot tables change for them.
- The finished, enchanted gem is also **one item**, its rolled effects in the tooltip (confirmed by the owner).
- Names carry the rest: *Infused Gem: Gem Power III, Speed (Splash)*, *Gem: Gem Power VI, Speed*.
- **Color (owner's intent):** each gem is one color, its main effect's potion color, made more saturated and shiny than the potion so it reads as a jewel. A second-effect highlight layer was tried and dropped (owner: didn't look good).
- **Player-facing name "Gem Power" (owner):** used wherever players see the level (item names, `/gems kit ... [power]` and its feedback, docs, this spec, the balance report). Internal names stay "infusion" so saved items and data packs keep working: the `infusion` field of the `infused_gem` and `gem` components, `Balance.infusion(...)`, `INFUSION` in the model, the loot functions' `infusion` field and `balance.json`'s `infusion` table.

### Strength

- A gem's contribution = potion x Gem Power factor x enchant grade x armor or weapon material x matching run (armor only).
- **Potion:** strong (glowstone) and long (redstone) variants make stronger gems; variant factors lie in [0.8, 1].
- **Armor material:** the same gem is stronger in better armor (netherite above leather). Gold counts as diamond (0.93, "magic bonus", owner); netherite is the only material at the cap. Weapon material counts the same way. Bows, crossbows, tridents and maces count as diamond (per-weapon tuning is open).
- **Matching run:** neighbouring pieces of the same material along helmet -> chestplate -> leggings -> boots add strength. A connected run (helmet + chestplate) beats the same number of separated pieces (helmet + boots); a full matching set is best.
- **Stacking:** gems of the same effect stack, strongest first, each extra counting a little less.
- **Top end:** only the perfect build reaches the top: lingering (or item) gem of the strongest variant + Gem Power VI + tier 3 + full contiguous netherite (armor, 12 gems), or netherite + 2 perfect gems (weapons). Every step below it must be a visible upgrade (owner). The balance tables were presented to the owner before building.
- **Mobs and players are treated the same (B18 equalTreatment):** gems affect both the same way, as close to vanilla as possible, with no mob-only or player-only cases.
- **No hidden cooldowns (B19 noHiddenCooldowns):** tune strength, chance and duration instead. Where a wait exists, it is shown (Absorption, below).
- **A gem and a vanilla effect of the same kind never add up: the stronger applies** (Q31; how: Gem and vanilla effect of the same kind, below).

### Rolls: other effects and drawbacks

- **A gem has one set of rolls**, whatever slot it later goes into: its main effect, maybe an "other" (bonus) effect, maybe a drawback.
- **Other effects** are of the same kind as the main effect (never itself). Healing, Harming and Hero of the Village are main-only.
- **Drawbacks are effects of the opposite kind, never the main effect's direct opposite** (owner: a direct inverse just cancels out). A Speed gem can roll Hunger or Weakness, never Slowness; a Poison gem can't roll Regeneration. Excluded direct pairs: Speed/Slowness, Strength/Weakness, Haste/Mining Fatigue, Regeneration/Poison and Wither, Luck/Bad Luck, Healing/Harming, Saturation/Hunger, Night Vision/Blindness and Darkness, Invisibility/Glowing, Slow Falling/Levitation, Conduit Power/Mining Fatigue. The balance model's inverse-pairs table is authoritative.
- **Negative rolls are real drawbacks (owner: "I'd like to have some drawback gems; they'll just need tuning"):** in armor they put a mild harmful effect on the wearer; tuned to sting without ruining the gem.
- **Full inverse on weapons (owner):** a weapon gem's bad roll gives the **target** a mild beneficial effect (the mirror of armor's).
- A beneficial gem's drawback pool: Slowness, Weakness, Mining Fatigue, Hunger, Bad Luck, Poison, Wither (minus direct opposites). A harmful gem's: every beneficial effect that works as a short effect on a target (all but Healing, which is instant), minus its direct opposite (owner: random, real combinations). Other pools are wide too.
- Both pools are weighted by rarity (Rarity-weighted rolls, below).
- **Harmful effects as Thorns-style debuffs on attackers: rejected (owner).**

### Loot and difficulty

- **Gems as loot (owner):** gems of every type, and armor and weapons with gems already in them, are findable loot: in chests and **worn by mobs**. Rare and mostly weak, better in harder places. Gems on mobs give the mobs those effects, including Healing, Harming, Poison and Regeneration.
- **Gems as a difficulty tool (owner):** gems on mobs give designated harder areas a much wider difficulty curve: mobs there wear more and better gems. Ominous trial spawner mobs wear gems even though they never drop their gear (owner: yes).
- Details: Loot, below.

### Ruled out (owner)

- **No gem combining or merging of any kind.** Gems are only made by brewing and enchanting, set by socketing, removed by the grindstone, and inverted at the brewing stand.

## Balance model

The numbers live in `tools/gem-balance.py`. It reads vanilla facts from the 26.3 source, checks every rule and generates `docs/research/effect-gems-balance.md` (and `build/gem-balance.html` for the browser): `python tools/gem-balance.py`. The generated page is the current model and lists the owner's answers to its questions; the decisions in this spec stand.

In short:

- **Scale:** worn strength is measured against the effect's strongest vanilla level (1.0).
- **Cap:** the build cap is **0.90** (`CAP`, owner). Weapons and harmful worn effects stay at it; each beneficial effect's worn total is rescaled to its own `worn_cap` (Per-effect worn caps, below).
- **Hits:** potency is measured against the strongest vanilla potion level; a hit lasts at most 8 s (the best weapon) and never less than the effect's floor (Owner's answers, Q15).
- **Gem strength:** potion x Gem Power factor x enchant grade x material x matching run (Strength, above). Grades: weak 0.50, medium 0.75, strongest 1.00.
- **Drawbacks** are fixed and small: one weak drawback is 10 % of the effect's strongest level (`DRAWBACK_ONE`), a medium one 15 % (the grade ratio); drawbacks of one effect add up saturating under 20 % (`DRAWBACK_CAP`, `drawback_sum`).
- **Healing** returns at most 25 % of each non-lethal hit at 0.90 (26.9 % at 1.0).
- **Every 26.3 status effect is classified** as a craftable gem (from a potion or a vanilla item that carries the effect), a found-only gem, drawback-only (Bad Luck: too little effect to be worth a slot), or not a gem (`NOT_GEMS`, e.g. Bad Omen). Turtle Master is a combined gem of its own.

## Owner's answers to the balance model's questions (2026-11-14)

Numbers are the model page's question numbers; the model holds each answer in one named place.

- **Q1:** build cap **0.90** (`CAP`); beneficial worn totals use per-effect worn caps on top (below).
- **Q2:** gold armor and weapons get a **diamond-level 0.93** magic bonus; netherite stays the only material at the cap.
- **Q3:** the **turtle helmet takes no gems** for now (tag `bubbas_infused_gems:not_socketable`).
- **Q4:** Resistance's 1.0 is **Resistance II** (40 %). No potion makes a plain Resistance gem: it is an "other" roll and a found-only gem.
- **Q5:** a combined **Turtle Master** gem (below).
- **Q6 (open):** the Healing gem's ceiling, 25 % of each survived hit at 0.90; the mod uses the model's value.
- **Q7:** drawback size stays 10 % / under 20 % until playtesting.
- **Q8:** Poison and Wither can roll as a beneficial gem's drawback, at the uncommon rarity weight (Rarity-weighted rolls).
- **Q9:** a harmful gem's drawback rolls from every beneficial effect that works as a short effect on a target (all but Healing), minus its direct opposite.
- **Q10-12, 22-27, 29-30:** as modeled.
- **Q13:** Harming in armor mirrors the Healing gem: a survivable hit hurts up to 25 % more.
- **Q14:** harmful on/off gems in armor are **always on**. Worn Levitation always rises, slower than the potion (Worn effects, below).
- **Q15, 18, 20 (hits):** potency against the strongest potion level; duration at most `MAX_HIT_SECONDS` 8 at the best weapon, x (s / cap)^0.5 below; never under the floor, so every hit does something. Floors: potency hits 4 s (`HIT_FLOOR_POTENCY_SECONDS`), Darkness 3 s, others 2 s (`HIT_FLOOR_SECONDS`); Regeneration long enough to heal 2 HP; Hunger at least 4.0 exhaustion per hit. Nausea and Glowing use their own dials (What strength does on a hit).
- **Q16:** weapon material is a factor.
- **Q17:** bows, crossbows, tridents, maces stay diamond-level (per-weapon tuning backlogged).
- **Q19:** **Healing and Harming on a weapon strike every hit**, no charge: `HIT_STRIKE_HARM` 2.5 / `HIT_STRIKE_HEAL` 2.0 per hit at 1.0 (best weapon 2.25 / 1.8, under Sharpness V's +3), at least `HIT_STRIKE_FLOOR` 1.0.
- **Q21:** Wither's 1.0 is the **Wither boss's skull, Wither II** (1 HP/s).
- **Q28:** Hero of the Village gems stay, at most **30 %** cheaper trades (`HERO_BEST_DISCOUNT`).
- **Q31:** a gem and a vanilla effect of the same kind: **the stronger applies** (rule `gem_vs_potion`).
- **Q32:** undead follow vanilla's inversion (rule `undead_inversion`). Worn Healing makes a survivable hit on an undead wearer hurt up to 25 % more; worn Harming gives it up to 25 % back; no passive damage or healing. On weapons, Harming heals undead targets and Healing hurts them. Poison and Regeneration do nothing to undead.
- **Q33:** **Invisibility** is stealth that scales with strength: hidden on the locator bar from 0.5 (`INVISIBILITY_LOCATOR_HIDE_AT`), and only the perfect complete set (`INVISIBILITY_FULL_AT`, its worn cap) gives real vanilla invisibility (rule `invisibility_stealth`).
- **Q34:** enchant odds and preview (Enchanting, below).

**Turtle Master gem (Q5).**

- One gem effect `turtle_master`: a mod status effect `bubbas_infused_gems:turtle_master` (vanilla has none), Turtle Master's potion color, named "Turtle Master".
- Brewed from Turtle Master potions of every form and variant.
- Both parts scale with the same strength against the strong potion: Resistance IV (80 % less damage) and Slowness VI (90 % slower). The best build: 72 % less damage and 81 % slower.
- The Slowness is part of the effect (full strength, owner), never a drawback, and doesn't make the gem harmful. Its roll table is a beneficial gem's, without Slowness as a drawback since it already has it.
- Against Resistance gems and a vanilla Resistance effect, the strongest damage reduction applies. `/gems` shows one line per part.
- **Worn: shell up (owner; the full set made walking near-impossible).** Both worn parts apply only while the wearer sneaks, and for `shell_linger_ticks` (10, half a second) after, so tapping sneak can't flick it. Code: `WornEffects.shelled`, `shellTick`; the slowness modifier uses the `wet/` id like Conduit Power's and is added and removed per tick; balance flag `sneak_only`. Mobs never sneak, so they never get it.
- On a weapon it puts both parts on the target, unchanged by the sneak rule.
- Test: `GemsEverywhereGameTest.everyGemWorksInArmor` (off walking, on sneaking, lingers 10 ticks, then drops).

## As built (2026-11-14)

Technical choices, not leaves: change them freely when a better way turns up.

### Numbers from one place

- `python tools/gem-balance.py` (or `--export`) writes `src/main/resources/bubbas_infused_gems/balance.json` and the brewing recipe files; `--check` also fails when either is stale.
- `balance.json` holds every factor, curve constant, tier table, roll pool and weight, inverse pair, drawback size, hit duration and floor, rule, and the model's example builds. The mod reads it once at startup (`balance/Balance`).
- In the script:
  - `WORN`: how each effect is realised worn (attributes with amount x s; curves `pow`, `odds` and `step:<threshold>`; hook constants; flags `wet_only`, `always_on`, `full_at`, `death_chance`, `hurt_chance`, `sneak_only`).
  - `HIT_UNIT`: per-hit rates. `HIT_CURVES`: the per-effect hit dials.
  - `RULES`: the answers the mod reads (`gem_vs_potion`, `undead_inversion`, `invisibility_stealth`).
  - `ITEM_SOURCES`: the bottle-slot items of item-sourced gems.
  - `LOOT`, `LOOT_PLACES`, `MOB_GEMS`, `TRIAL_EQUIPMENT`: the one loot table; the script writes the loot data files from them.

### Items and data

Two items, `bubbas_infused_gems:infused_gem` and `bubbas_infused_gems:gem`, one model each, no blocks (B15). Components:

- `bubbas_infused_gems:infused_gem`: `{effect, variant: "normal"|"long"|"strong", form: "normal"|"splash"|"lingering"|"item", infusion: 1-6}`.
- `bubbas_infused_gems:gem`: `{effect, infusion, tier: 1-3, main: 0-1 (0 = drawback only), other?: {effect, strength}, drawback?: {effect, strength}}`. Strengths are gem quality: the main is potion x Gem Power x grade, an "other" is Gem Power x grade, a drawback is its fixed size.
- `bubbas_infused_gems:socketed_gems`: a list of up to 3 `gem` values on armor and weapons.

Details:

- `effect` is a status effect id: vanilla's, or `bubbas_infused_gems:turtle_master` (`balance.json` effect `id`).
- Names come from the data ("Infused Gem: Gem Power III, Speed (Splash)", "Gem: Gem Power VI, Speed"). A drawback-only gem is named like any gem (owner); its socketed summary shows just its drawback.
- Tooltips go through Fabric's `ItemComponentTooltipProviderRegistry`.
- The creative tab holds a Gem Power VI Infused Gem (lingering, or item form for item-sourced gems) and a perfect Gem of each of the 39 gem effects (`balance.json` has 40; Bad Luck is drawback-only).

### Color

- A custom item tint source `bubbas_infused_gems:gem_color` reads the gem's own component and returns the effect's potion color (Turtle Master: its potion's). No color is stored, and one model covers every effect.
- Chosen over potion contents (a finished gem has several effects, and potion contents add potion tooltip lines) and over stored custom_model_data colors.
- The color is pushed toward full saturation (+55 % of the way) and brightness (+35 %), same hue (`GemColorSource.jewel`).
- The gem model has two layers: the tinted body and an untinted shine layer.
- **Tooltip colors:** gem lines (and `/gems`) use the same jewel color (`gem/GemColors`) with brightness at least 0.8, so dark effects (Weakness, Wither, Darkness, Bad Luck, Mining Fatigue, Blindness) read clearly; drawbacks stay red.

### Textures

- Placeholders drawn by `tools/gem-textures.py` from shapes (not AI), at 16x and 32x, plus the icon. `tools/texcheck.py` passes.
- Body: tinted, including a dark rim in the gem's own color. Shine: untinted white glints and a lit rim (file `*_outline.png`).
- The finished gem is a cut stone seen from the side: flat top, five crown facets, a bright girdle line, four darker pavilion facets to a point; the infused gem a round brilliant seen from above (16 facets). The effect colour tints the grey facets; an untinted second layer adds white glints, cool and warm sheens and a short rainbow on the lit edge (owner, 2026-11-23: "they look like flat discs. no iridescence"; replaces the earlier octagon).
- Not done: real textures made by a person.

### Brewing

- Plain `minecraft:brewing` recipes, generated per potion x form x base gem and per source item x base gem: 846 files in `data/bubbas_infused_gems/recipe/infuse/`. Vanilla's own recipe type, so datapacks can override or remove any of them.
- **Potions:** every effect with a potion, Invisibility, Luck, Wind Charged, Weaving, Oozing and Infested included. Luck's potion has no survival recipe, so Luck gems are mostly found. Turtle Master potions brew Turtle Master gems.
- **Source items** (the owner's item-sources table): golden and enchanted golden apple, wither rose, glowstone dust, shulker shell, echo shard, sculk shrieker, chorus fruit, breeze rod, heart of the sea, nautilus shell, rotten flesh, pufferfish, and suspicious stews. Golden and enchanted golden apples are the normal and strong variants.
- **Stews** match by their stew effect, using Fabric API's `fabric:components` ingredient as the recipe's `input.item` (26.3's `PotionIngredient.item` is a plain `Ingredient`, so custom ingredients work there). Every vanilla flower of one effect gives the same component (dandelion, golden dandelion, blue orchid: Saturation 7 ticks; azure bluet, open eyeblossom: Blindness 220 ticks), so one recipe per effect and base gem covers them all. Other stews brew nothing.
- **Item-sourced Infused Gems** carry form `item`: no form in the name, tooltip "Up to tier 3". The model's `ITEM_MAX_TIER_OVERRIDE` (per effect and variant, exported as the variant's `max_tier`) can cap one source later; it is empty now.
- Side effect: side hoppers also push those source items into bottle slots.
- Game tests brew every brewable effect and variant from the model and every stew flower; a real brewing stand brews an echo shard and a stew.

### Enchanting

- Mixins on `EnchantmentMenu.slotsChanged` / `clickMenuButton`, for Infused Gems only.
- **Costs (owner: like gear enchanting):** vanilla's bookshelf formula (copied from `EnchantmentHelper.getEnchantmentCost`, seeded by the enchantment seed), about 10 / 20 / 30 levels at 15 shelves; the full level cost shown is paid, plus 1-3 lapis. The seed is reset after.
- **Gates:** rows above the form's max tier are set to 0. Tier 2 needs 8 bookshelves, tier 3 all 15 (`TIER_MIN_SHELVES`); a row below its gate shows as unavailable, like a form-capped row.
- **Roll:** `GemEnchanting.roll(seed, tier, gem)`, deterministic from the seed and the gem's effect, variant and Gem Power, so the client draws the hint (`EnchantmentScreenMixin`) exactly as the server will roll it.
- **Odds (owner, Q34: the plain result is the most common at every tier, so people who invest a lot keep control):**

  | Tier | Outcome | Chance |
  |---|---|---|
  | 1 | weak | 75 % |
  | 1 | weak + weak drawback | 20 % |
  | 1 | weak drawback only | 5 % |
  | 2 | medium | 55 % |
  | 2 | medium + weak other | 30 % |
  | 2 | medium + weak drawback | 15 % |
  | 3 | strongest | 50 % |
  | 3 | strongest + weak other | 25 % |
  | 3 | strongest + medium other | 10 % |
  | 3 | strongest + weak drawback | 10 % |
  | 3 | strongest + medium drawback | 5 % |

- "Other" effects roll from the full other pool, drawbacks from the full drawback pool, both by rarity weight (`other_weights`, `drawback_weights`). No exceptions (owner).
- **Hover preview, "slightly anonymized" (owner):** the main effect and its strength, then for an extra roll only its kind and size, never which effect: "Speed 100%", "Speed 100% + minor bonus...?", "Speed 100% + drawback...?", "minor drawback only...?". Size words: weak "minor", medium none, strongest "major". `GemEnchanting.outcome` makes the same draw as `roll`, so the preview is exact and the vanilla reroll trick works. No lines beyond vanilla's lapis and levels.

### Rarity-weighted rolls (owner)

Both the bonus and the drawback pool are weighted by how hard the effect is to get (`RARITY_WEIGHT` in the model):

- **Common, 4:** easy to brew.
- **Uncommon, 2:** a rare ingredient, the End, the deep ocean or trial chambers: Regeneration, Turtle Master, Slow Falling, Poison, Wither, Conduit Power, Dolphin's Grace, Silence, Breeze, Ender, Levitation, Darkness, Wind Burst, Weaving, Oozing, Infested. (This also keeps Poison and Wither less common as drawbacks.)
- **Rare, 1:** found-only: Haste, Health Boost, Hero of the Village, Resistance, Luck, Mining Fatigue.

### Gem inversion (owner)

- A finished gem with a drawback in a brewing stand's bottle slot + a fermented spider eye inverts it. Code: `station/InvertGemRecipe` (a vanilla `BrewingRecipe` subclass, data `recipe/invert_gem.json`, so the stand's slots, timing and hoppers work), `station/GemInversion`.
- **Swap:** the old drawback's effect becomes the main; the old main becomes the drawback, at the old drawback's size.
- **Nerf every time:** the new main is one grade below the old (strongest -> medium -> weak; read back from the main's strength and Gem Power level, unambiguous because potion variant factors lie in [0.8, 1]), and the gem loses a Gem Power level.
- **The bonus ("other") is kept unchanged both ways.** After inverting it is the opposite kind to the new main and acts as a second drawback; inverted back, it is a bonus again.
- Inverting back is allowed, with the nerfs again.
- **A drawback-only gem inverts too (owner):** its drawback becomes a weak-grade main (Gem Power factor x weak grade), Gem Power one lower, no drawback after, bonus kept.
- **Refused** (the bottle slot doesn't take the gem: `PotionIngredient.isPotionInput`): no drawback, a weak main, Gem Power I, or a drawback that isn't a gem effect (Bad Luck).
- The model checks an inverted gem is never perfect (at most 0.66: medium 0.75 x Gem Power V 0.88).
- The tooltip shows the result like any gem.

### Smithing table

- A `bubbas_infused_gems:socket` smithing recipe with no template (26.3's `SmithingRecipe` allows an empty template). A special recipe, not in the recipe book.
- Base `#bubbas_infused_gems:socketable`: `socketable_armor` = the four vanilla armor tags; `socketable_weapons` = swords, axes, spears, mace, trident, bow, crossbow. Addition: the gem item.
- Slot limits 3 / 2 come from the JSON. `#bubbas_infused_gems:not_socketable` (the turtle helmet, owner) has 0 slots, so the table gives no result.

### Grindstone and breaking

- `GrindstoneMenu.computeResult` strips `socketed_gems`; the result slot (`GrindstoneMenu$4`) gives back every gem from both inputs when taken.
- `LivingEntity.onEquippedItemBroken` drops the broken item's gems at its owner (players and mobs).

### Per-effect worn caps (owner: the perfect set "should feel much more impactful")

What the perfect armor set (12 perfect gems in netherite) reaches, as a share of the strongest vanilla level.

- Model: `WORN_CAP`, grouped as `WORN_COMBAT_CAP`, `WORN_FULL_CAP`, `WORN_DEFAULT_CAP` and the mobility constants; exported as each effect's `worn_cap`.
- `GemTotals.ofArmor` scales each armor total by worn_cap / 0.90, so every build keeps its share. It snaps a total within 1e-6 of its cap onto it, so "full" effects never miss by rounding.
- **Harmful effects in armor stay at 0.90** (they hurt the wearer). An undead wearer's swapped Healing reads at Harming's scale.
- **Effects measured as a share of their own best keep 0.90**, where the perfect set is already 100 %: Wind Burst, Weaving, Oozing, Silence, Saturation, Ender and Breeze, Hero of the Village (30 % cheaper at the cap).

| Effect | Worn cap | Perfect set | Strongest vanilla |
|---|---|---|---|
| Strength, Resistance, Turtle Master, Regeneration, Health Boost, Absorption | 1.00 | the full potion, permanently: +6 attack damage; damage taken -40 %; -80 % damage taken and -90 % speed; 0.8 HP/s; +8 max health; 16 HP shield | Strength II, Resistance II (beacon), strong Turtle Master, Regeneration II, Health Boost II, Absorption IV |
| Healing | 1.00 | 26.9 % of each survived hit back (its own curve: 25 % at 0.90) | (no worn vanilla equivalent) |
| Fire Resistance, Water Breathing | 1.00 | complete: the real vanilla effect, kept on (ambient, no particles) like Invisibility's | Fire Resistance, Water Breathing |
| Night Vision, Conduit Power, Invisibility | 1.00 | complete: full night-vision brightness; air never runs out in water, full underwater mining and sight; mobs notice the wearer at 7 % range plus real invisibility | the potion / the conduit |
| Luck | 1.20 | +1.2 luck | Luck I (+1) |
| Haste | 2.5 | mining x2.0, attack speed +50 % | Haste II (beacon): x1.4, +20 % |
| Speed | 1.875 | +75 % speed | Speed II: +40 % |
| Jump Boost | 2.19 (`WORN_JUMP_HEIGHT` 1.79) | jump about 4.5 blocks, safe fall +4.4 | Jump Boost II: about 2.5 blocks |
| Dolphin's Grace | 1.8 | water drag 0.976 (1.8x the potion's swim-speed gain) | drag 0.96 |
| Slow Falling | 1.8 | falls 1.8x slower than the potion (gravity 0.0056), no fall damage | gravity 0.01 |

With a vanilla effect of the same kind, the stronger-applies top-up still holds (a perfect Speed set adds only its part above Speed II).

**Gem Power ladder in practice.** Reference full sets of 12 tier-3 gems from the strongest potion:

| Set | Total | Share of the cap |
|---|---|---|
| amethyst in iron | 0.31 | 35 % |
| quartz in gold | 0.42 | 47 % |
| diamond-base in diamond | 0.63 | 70 % |
| Gem Power V in netherite | | 88 % of the perfect set |
| the perfect set | 0.90 | 100 % |

The model's early-game floors: weakest single gem 0.015, early kit band 0.08-0.30, early Healing kit 4.5 %.

### Worn effects

**Totals and caching.**

- Totals (`effect/GemTotals`) are recomputed only on `ServerEntityEvents.EQUIPMENT_CHANGE` for armor slots and cached on the entity (mixin field).
- Every hook reads `effect/GemValues`: the worn total after the undead swap (Healing and Harming trade places on undead), immunity (`canBeAffected` with the vanilla effect: undead and poison, wither skeletons and wither), and "the stronger applies" against a vanilla effect of the same kind, maxed with a gem effect a weapon put on the entity.
- Gear factor: an unknown (modded) armor piece with gems counts as iron in a run of 1 and breaks any matching run; an unknown weapon counts as iron.
- **Client totals:** the client keeps its own totals for the local player, recomputed only when an armor stack changes or when its Reworked Beacons receipts change or run out. Used for gravity, levitation, water drag, the Night Vision lightmap and Conduit Power's underwater sight (`LightmapMixin`), and the worn screen effects.

**Attribute effects.** Transient modifiers (`worn/<effect>/<i>`) set by `WornEffects.updateAttributes` on equipment change and on every effect change (see Gem and vanilla effect of the same kind).

**Per tick, only for entities with gems:**

- Regeneration heal; Hunger exhaustion; Poison and Wither every 10 ticks.
- Absorption refill (below).
- Conduit Power's wet-only modifiers (`wet/conduit_power/<i>`, on only in water or rain).
- Kept-on vanilla effects: Glowing always (owner); Invisibility only at the perfect complete set (owner, Q33); Fire Resistance and Water Breathing at their perfect sets. Re-applied as ambient, particle-free effects for 100 ticks, checked every 20. A gear change that ends one removes it at once (told apart from a real potion's by being ambient, particle-free and at most 100 ticks long).

**Hooks, by effect:**

- **Resistance and Turtle Master:** at `getDamageAfterMagicAbsorb`, the strongest reduction of the two and a vanilla Resistance.
- **Fire Resistance:** same hook, on damage tagged `is_fire`.
- **Harming (worn):** same hook, on attacks and falls.
- **Healing and Infested:** after damage (`ServerLivingEntityEvents.AFTER_DAMAGE`). Infested calls vanilla's `onMobHurt` with chance s.
- **Wind Charged, Weaving and Oozing at death:** `AFTER_DEATH`, vanilla's `onMobRemoved` with chance s, not on top of the real effect.
- **Slow Falling and Levitation:** at `getEffectiveGravity`.
- **Levitation (owner, after playtest):** always rising, 0.16 blocks/s for any gem up to about 0.46 at the cap (`LEVITATION_MIN_RISE` 0.008, `LEVITATION_BEST_RISE` 0.0232 blocks/tick), about half of vanilla Levitation I's 0.9.
  - Below the rise speed the velocity goes straight to it: vanilla's 0.2-a-tick easing at these speeds stays under the 0.003 blocks/tick that `aiStep` rounds to 0, so it would barely move.
  - Above it (a jump) it eases down as vanilla does.
  - Worn Levitation doesn't negate falls: while falling, gravity is only x (1 - s/cap); the rise applies on the ground and once the fall has slowed.
  - Not in fluids or while gliding. Computed on the client for players, who move client-side. Tested on a server mob and a real client player in survival and creative.
- **Dolphin's Grace:** at `travelInWater`'s drag.
- **Saturation (owner):** restores food and saturation like the vanilla effect.
  - Food fills only up to round(6 + 14 r), with r = s / Saturation's cap (`SATURATION_FOOD_BASE`, `SATURATION_FOOD_SPAN`; `WornEffects.saturationFoodTarget`): a weak gem about 7, a mid build 13-14, only the perfect set 20. So only the perfect set feeds vanilla's fast regeneration and gives the vanilla effect's free food.
  - Saturation added scales by r too and stays at most the food level, as vanilla clamps it (`FoodData.eat(n, r)`, whole points as a per-player carry builds up).
  - Rate: 0.035 food points/s for the weakest gem up to 1.0 at the cap, geometric in between (`SATURATION_MIN_RATE`, `SATURATION_BEST_RATE`): a single weak gem about a point every 22 s, the best build about a point a second (vanilla: 20 a second).
  - The same on a hit target for the hit's few seconds.
- **Invisibility:** at `getVisibilityPercent` (stealth x (1 - 0.93 s)), plus `waypoint_transmit_range` -100 % from s 0.5.
- **Hero of the Village:** at `Villager.updateSpecialPrices`, the stronger of vanilla's and the gem's cut, at most 30 % at the cap. When the gem's cut is stronger, vanilla's is switched off and the gem's applied as floor(cut x cost), without vanilla's 1-emerald floor. Trade prices only; villager gifts stay vanilla's.

**Absorption (owner, playtest).**

- A new or bigger gem shield fills at once on equip, joining or respawning, but at most once per 30 s. Within that, re-equipping starts the normal 30 s wait instead, so swapping gear can't refill it.
- Then it refills 30 s (600 ticks) after the last damage.
- The refill (after damage and on equip) fills only up to the gems' own share, never a golden apple's part.
- The equip-fill throttle is game time saved on the wearer (attachment `last_equip_fill`, kept on death), so rejoining or respawning doesn't refill inside it.
- **The wait is shown (owner, B19):** on players, every armor piece carrying an Absorption gem (main or bonus) shares one item cooldown group (`effect/AbsorptionCooldown`, `mixin/ItemCooldownsMixin`). It is put on cooldown for the refill time on each damage and on a throttled equip, so vanilla's cooldown sweep (as on wind charges and ender pearls) runs over those pieces in the inventory and hotbar exactly while the shield waits. While it runs, those pieces can't be right-click equipped (fits the throttle). Mobs are unchanged. Test: `GemsEverywhereGameTest.absorptionRefillWaitShowsAsItemCooldown`.
- A hit's Absorption fills the shield only when the effect is new.

**Worn screen effects (owner, after playtest).** Worn Darkness, Blindness and Nausea put no vanilla effect on the wearer. Instead the wearer's own client draws s of the effect's strongest look, steady, from its own gem totals. (A re-applied vanilla effect made the screen fully dark and blink as vanilla's pulse and fade restarted.)

- Darkness dims the lightmap (`LightmapMixin`: vanilla's darkness scale 0.45 x s, without its cosine pulse).
- Darkness and Blindness pull the fog in s of the way to vanilla's (15 and 5 blocks) and darken its color by (1 - s)^2 (`FogRendererMixin`).
- Darkness and Nausea raise `LivingEntity.getEffectBlendFactor` to s on the client, which drives Nausea's wobble and Darkness's brightness drop.
- One weak gem is a slight dim (7 %); the best build 90 % of vanilla's darkest. Nothing is re-applied, so nothing blinks. A real potion's effect, when present, draws itself as in vanilla.
- Blindness's other vanilla rules (no sprinting, no critical hits) are not applied by gems.
- On weapons the real effects are used; a refreshing hit updates the existing instance in place (vanilla's `MobEffectInstance.update`), so it doesn't restart the fade (vanilla Darkness still pulses as in vanilla).

### Gem and vanilla effect of the same kind (Q31)

Never added; the stronger applies, with no state of our own to go stale.

- **Worn attribute gems:** `updateAttributes` runs on equipment change and at the end of `onEffectAdded`, `onEffectUpdated` and `onEffectsRemoved`, which every source goes through (potions, splash and lingering clouds, tipped arrows, beacons, conduits, golden apples and totems, milk, expiry, death). It sets the gem modifier to exactly what makes the attribute equal the stronger of the two:
  - `add_value`: gem(s) - vanilla(f);
  - multiplied operations: (1 + gem(s)) / (1 + vanilla(f)) - 1;
  - removed when vanilla is at least as strong.
  - f is the vanilla level as a fraction of the 1.0 level (the model's `level_one` and `worn_amplifier`). The vanilla effect keeps its own modifier, timer and icon. Modifiers are transient, so nothing survives a save or a respawn; the next refresh restores the right value.
  - They also top up above a gem effect from a hit, never adding to it.
- **Worn hook gems** read the vanilla effect at call time (s - f, or for Resistance the combined reduction), so they need no updates.
- **Gem effects on a target (from hits) and vanilla effects:** `LivingEntity.addEffect` checks the other kind first. If the one already there is at least as strong, the new one is refused; otherwise the weaker one is removed.
- A beneficial hit's potency never exceeds that effect's worn cap.
- Tested (`GemVsPotionGameTest`): a stronger potion running out (the gem takes over the same tick), a weaker potion (its timer runs, the total is the gem's), clearing every effect as milk does, beacon and conduit ambient effects, golden and enchanted apple Absorption, totem Fire Resistance, a tipped arrow (splash potions and clouds end in the same `addEffect` call), save and reload, gear taken off mid-potion (death is simulated as gear off plus clearing effects), and no gem modifier left behind.

### Gem effects look like vanilla's (owner: vanilla mechanics, never hand-drawn visuals; B17)

- Where vanilla decides a visual from "does the player have effect X", that one check also counts a gem of X (worn, from the client's own totals, or a gem effect from a hit; `client/GemLooks`):
  - the heart style (`Hud.HeartType.forPlayer`: poisoned and withered hearts);
  - Regeneration's heart wave and Hunger's green food bar (`Hud.extractPlayerHealth`, `Hud.extractFood`).
  - Vanilla draws them with its own textures.
- Elsewhere the gem already is the vanilla mechanic:
  - Absorption and Health Boost are the real attributes (vanilla's golden and extra hearts).
  - Effect particles and colors come from the gem effects carrying the vanilla effect's color and category; their icons are vanilla's sprites (`HudMixin`).
  - Glowing, Invisibility, Fire Resistance and Water Breathing (perfect sets) and the on/off hit effects are the real vanilla instance.
- A scaled hit stays a gem copy: its potency is in 1 % steps and rarely equals a vanilla amplifier.

### Perfect Invisibility hides the armor (owner)

The perfect set's wearer is fully invisible, armor included. Done by editing the worn items the vanilla way (the owner's choice over a render hook, to keep vanilla mechanics):

- While the set is complete on a living wearer (`HiddenArmor.active`: Invisibility's worn total at `full_at`), each worn piece's `minecraft:equippable` points at the mod's empty equipment asset `bubbas_infused_gems:empty`. Every other equippable field is kept, and the real asset id is kept in the item component `bubbas_infused_gems:hidden_asset`.
- `assets/bubbas_infused_gems/equipment/empty.json`: 26.3's codec needs a non-empty layer map, so it has a single pig-saddle layer, a layer type humanoid armor never draws.
- The armor, trim and elytra layers all draw nothing for it, on every client, from the item data alone. Held items still show (vanilla). The wearer's own inventory preview shows the armor hidden too (same item data).
- **Restored the moment the set stops hiding it:**
  - `WornEffects.refresh` (every armor change, including a gem removed or a piece off) restores all worn pieces.
  - A marked stack restores itself on its tick unless it is still worn by an entity with the set active (`ItemStack.inventoryTick`: inventory, hotbar, a piece taken off, a piece picked up from a chest).
  - On the ground, on its first tick (`ItemEntity.tick`; death drops).
  - Crafting, smithing, anvil and grindstone outputs are cleaned (`ResultContainer.setItem`).
- Tests: `TopEndGameTest` (set completes: all four hidden, other equippable fields kept; one piece off: all restore, and the removed piece restores in the inventory; back on: hidden again; a marked piece from a chest after pickup, on the ground and in an output container; a mob's guaranteed drops on death all visible). Client part `invisible` (a perfect-set zombie shows only its sword; a near-perfect set under a vanilla Invisibility potion shows its armor).

### Gems beyond potions (owner)

These do what the thing does, not just apply its potion (`effect/GemActions`; numbers in the model's `WIND_*`, `WEAVING_*`, `OOZING_*`, `ENDER_*`, `BREEZE_*` constants). Hit kind `action` for Wind Burst, Weaving and Oozing.

- **Wind Burst** (key `wind_charged`, brewed from Wind Charged potions, named "Wind Burst"):
  - Hit: throws the target up (0.9 x s/cap blocks/tick, at least `WIND_HIT_UP_MIN` 0.35) and away from the attacker (0.6 x s/cap), syncing the velocity to players. On a mace with the Wind Burst enchantment the strength is x1.6, past the cap (`WIND_MACE_BONUS`, the owner's deliberate exception).
  - Worn: fall damage x (1 - s/cap) (none only at the perfect set), and a small burst (gust particles, wind charge sound) bounces the wearer up (0.6 x s/cap, at least `WIND_LAND_BOUNCE_MIN` 0.25).
- **Weaving:**
  - Worn: `Entity.makeStuckInBlock` in a cobweb moves s/cap of the way to no slowdown. At the cap the multiplier becomes zero, which vanilla reads as not stuck, so walking, sprinting, jumping and falling are exactly as in open air, client-side for players too. (Any non-zero multiplier, even (1, 1, 1), makes `Entity.move` zero the velocity every tick, so speed would never build up.) Tested with a real client player sprinting and falling through webs.
  - Hit: webs the target by the Weaving dial (What strength does on a hit), only into air (owner: the webs stay).
- **Oozing:**
  - Worn: fall damage x (1 - s/cap) (none only at the perfect set), and a slime-block bounce (0.9 x s/cap of the impact speed, none while sneaking).
  - Kill: splits off 1 (2 from half strength) size-1 slimes owned by the attacker (Fabric attachment `slime_owner`). They are allied through `Entity.considersEntityAsAlly`, neutral (`Mob.setTarget` refused), allowed in peaceful, and drop no loot or XP. At most 8 living near the owner (within 128 blocks; `OOZING_SLIME_CAP`), counted from an in-memory set of each owner's slimes (a few lookups, no entity search).
  - Owner: the slimes are never saved (`Entity.shouldBeSaved`), so slimes left behind dissolve when their chunk unloads or the server stops; while loaded they never despawn (`checkDespawn` skipped).
  - Oozing slimes stay plain slimes (Q16).
- **Silence** (echo shard; mod status effect `bubbas_infused_gems:silence`): the wearer's movement game events (step, swim, splash, hit ground, flap, elytra glide) are dropped at `ServerLevel.gameEvent` before any listener, each with chance s/cap (all at the cap). Sculk sensors, wardens and Reworked Mobs' hearing (vanilla game events) all miss them. Darkness's source is the sculk shrieker (owner).
- **Ender** (chorus fruit; `bubbas_infused_gems:ender`), worn: when a projectile (not the wearer's own) would actually hurt the wearer (damage above 0; never explosions), with chance s/cap the wearer takes chorus fruit's random teleport (`randomTeleport`, 16 tries) with diameter 4 to 16 by strength, and no damage.
  - **At the perfect set (owner: "imagine a boss with perfect blink, you'd wanna be able to trap them"):** the blink always triggers, with chorus fruit's normal 16 tries for a safe spot; boxed in (no spot found), the projectile hits.
- **Breeze** (breeze rod; `bubbas_infused_gems:breeze`), worn: `Entity.deflection` for the wearer returns a breeze's deflection (reverse, breeze deflect sound) with chance s/cap. Wind charges get through, as `Breeze.deflection` does (Q17).
- **Chance gems at the top end (owner: "projectile immunity isn't damage immunity"):** worn Ender and Breeze chances are s / 0.90, so the perfect set is certain. Measured: 300 of 300 dispenser arrows and every skeleton arrow deflected; 200 of 200 arrows blinked away from; other damage still lands. Worn Ender and Breeze read only the worn total.
- On a weapon, Silence gives the target the effect for the hit's seconds (pointless, but allowed); Ender and Breeze on a weapon have their own dials (below).
- Frost waits for the next Minecraft drop's freezing effect.

### Weapons and arrows

- **Hook:** `EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak`, which every vanilla post-attack path reaches with the weapon that hit: a player's melee and stab attacks (they call the item-source variant directly, so a hook on `doPostAttackEffects` would miss them), mobs' melee, sweeps, and thrown tridents. Bows and crossbows are skipped there (their arrows carry the gems).
- **Scaled effects** apply gem mob effects `bubbas_infused_gems:gem_<effect>` (Turtle Master: its own effect). Amplifier = potency in 1 % steps; vanilla icon via `HudMixin` (Turtle Master shows Resistance's); "Poison 46%" in the inventory.
  - Potency: (s / 0.90)^0.5 of the strongest potion's level, so the perfect weapon matches it on the target (Poison II, Slowness IV...).
  - Duration: `GemHits.duration`, at most 8 s, floors from the JSON (Q15).
- **On/off harmful effects** apply the vanilla effect for s/cap of 8 s, floored (Nausea and Glowing: their dials below).
- **Healing and Harming** strike every hit (`GemHits.strike`), resetting the target's damage cooldown first so the hit's own cooldown doesn't swallow the strike. Harming's strike uses its own damage type `bubbas_infused_gems:gem_strike` (indirect magic, in `#bypasses_cooldown` and `#bypasses_armor`) and restores the target's hit cooldown and `lastHurt` after, so clicks during the cooldown land nothing and trigger no gems.
- A target immune to the vanilla effect gets nothing.
- **Drawbacks** go on the target for 3 s (`DRAWBACK_HIT_S`), refreshed by each hit.
- **Hunger hit (owner):** drains twice Hunger III's rate for at least 4 s, and a player target loses at least one food point's worth (4.0 exhaustion) per hit: what the effect won't drain in time is added at the hit.
- **Arrows:** `ArrowItem.createArrow` (all bows, crossbows and skeletons) gives the `Arrow` the weapon's hit plan beside its pickup stack (so the pickup stays plain) and the trail color. The plan is saved with the arrow (`bubbas_infused_gems:gem_hit`). On hit, a vanilla tipped effect of the same effect competes with the gem's (the stronger level wins); different effects both apply.

### What strength does on a hit: one dial per effect (owner's answers to `docs/research/effect-gems-strength-proposal.md`)

Dials set per build are `HIT_CURVES` in the model: (weak, mid, perfect) at the reference weapons' strength shares (the model's Early and Mid weapons, 9 % and 43 %, and the perfect weapon), straight lines between, flat below weak. Exported as each effect's `hit_curve` and read by `Balance.hitCurve`.

- **Potency hits** (Poison, Wither, Slowness, Weakness, Mining Fatigue, Hunger; Q1/Q2): strength is potency; they last at least 4 s, up to 8 s at perfect. Wither stays at the skull's Wither II (Q3). Hunger and Mining Fatigue do nothing to mobs, as in vanilla (Q4/Q5).
- **Screen effects** keep strength in duration: Blindness and Darkness by the general rule; **Nausea 4 / 6 / 10 s** (Q11); **Glowing 10 / 30 / 60 s** (Q12); Infested vanilla (Q13).
- **Levitation** (Q6, hit kind `control`): both the rise speed (**0.3 / 0.6 / 1.8 blocks/s**) and the time (**3 / 4 / 6 s**) scale. The hit is the gem effect `gem_levitation` at the strength share; `WornEffects.gravity` runs vanilla's own levitation step (move 0.2 of the way to the rise speed each tick) at that speed, mobs and players alike, with no air-time limit (Q7). Slow Falling on a target is vanilla's (Q8).
- **Wind Burst** (Q9): any hit throws at least 0.35 up; any worn landing bounce is at least 0.25.
- **Weaving** (Q14): chance **10 / 25 / 50 %** per hit, no cooldown. Below perfect: one web at the feet (only into air). The perfect weapon: 2-3 webs around the target by vanilla Weaving's placement rule (air spots on a sturdy floor). Webs stay (Q15).
- **Ender** (Q18/Q19, an action): chance **10 / 25 / 40 %** per hit to blink the target with vanilla's `TeleportRandomlyConsumeEffect` (diameter 16, chorus fruit's own landing rules, sound and game event), players included.
  - A blinked **mob** is briefly confused (owner): it drops its target (`Mob.setTarget(null)` and the brain's attack-target memory) and takes no new one for `ENDER_CONFUSED_TICKS` 20 (1 s), then behaves normally. `EnderConfusionMixin` holds off `Mob.setTarget`, which every target goal goes through, and for brain-driven mobs (piglins, hoglins, wardens) erases the brain's attack-target memory before and after each AI step. Players only teleport.
  - No boss exemptions: vanilla has no teleport immunity to match, so the Wither, Ender Dragon and Warden can be moved; the owner decides whether to exempt them (Open).
- **Breeze** (Q22, hit kind `control`): the target carries the Breeze gem effect (lasts 4-8 s like a potency hit) and **every projectile it launches veers** (owner: one rule for every projectile-launching mob).
  - The uncertainty vanilla passes to `Projectile.shoot` is multiplied **x3 / x6 / x12** (from at least 1) (`BreezeVeerMixin`). That covers every shot and throw, `shootFromRotation` and `spawnProjectileUsingShoot` included: arrows, bolts, fireworks, tridents, snowballs, witch potions, llama spit, wind charges. Base spread: vanilla's 0.0172275 per unit of uncertainty.
  - Powered projectiles built with a shooter (fireballs, wither and dragon skulls) get the same spread on their direction (`BreezeVeerFireballMixin`).
  - Shulker bullets home, so each new heading they pick is wobbled by the same spread (`BreezeVeerShulkerMixin`).
  - Not covered: evoker fangs and vexes (not projectiles) and fishing hooks (they set their own velocity; players only). Zombies hold bows but never fire them in vanilla, so a Breeze hit changes nothing for them.
- Silence on a weapon is unchanged (Q20/Q21 backlogged).

### Loot (owner: rare and mostly weak, better in harder places; gems on mobs widen the difficulty curve of harder areas)

**One table in the model:**

- `LOOT`: four levels (common, rare, hard, ominous), each with weighted Gem Power levels and tiers, the potion variant (weakest or any), the armor materials and weapons of gemmed gear, and gems per piece.
- `ARMOR_SHARE` 20 % and `WEAPON_SHARE` 15 % of a level's rolls are gemmed gear; the rest are loose gems. Weapon gems are harmful `WEAPON_HARMFUL_SHARE` 75 % of the time, any gem otherwise (owner: they hit the player).
- `LOOT_PLACES`: 40 vanilla tables, chance per opening 4-20 %, found-only gems listed per place.
- `MOB_GEMS`, first matching row wins, per armor piece or weapon (owner: farms of ordinary mobs give little, harder areas keep more; no other farm limits, since there are too many gems to farm a chosen one):
  - mobs spawning inside `HARD_AREA_STRUCTURES` (ancient city, bastion, end city, mansion, monument, fortress, pillager outpost; a `location_check` on the spawn position): 7 %, hard;
  - raid mobs: 7 %, hard;
  - piglins elsewhere: 1.5 %, rare;
  - every other mob: 1 %, common.
- `TRIAL_EQUIPMENT`: ominous trial spawner mobs, 10 %, armor and weapons, hard.
- Loot gem strengths by level: common 0.20-0.25, rare 0.20-0.46, hard 0.25-0.56, ominous vaults 0.46-1.00.

**Perfect loot (owner).** Only ominous vaults (`PERFECT_LOOT`) use Gem Power VI. There a Gem Power VI pick always makes a perfect gem of any effect (strongest variant, tier 3, full strength) carrying a tier 3 drawback (weak or medium, in tier 3's proportions), except `PERFECT_CLEAN_CHANCE` 1 in 20, which are clean. Below ominous vaults, craftable gems from loot never use Gem Power VI.

**Model checks:** only `PERFECT_LOOT` levels reach a perfect craftable gem; each level's best gem beats the level below; every table and item exists in 26.3.

**Making a loot gem:** as if brewed and enchanted (`GemRollSpec`: effect, variant, Gem Power and tier picked, then `GemEnchanting.roll`).

**Data files** (written by the script, all overridable by a datapack):

- `loot_table/gems/<level>`: a gem of any craftable effect, a gemmed armor piece or a gemmed weapon.
- `loot_table/inject/<vanilla path>`: one pool: the place's chance, then its level table or one of its found-only gems.
- `item_modifier/mob_gear` and `item_modifier/trial_chamber_gear`.

**Code:**

- Loot functions `bubbas_infused_gems:random_gem` and `socket_random_gems` (up to the free slots; `gear` armor / weapon / any); roll fields `prefer_harmful` and `perfect` (`GemRollSpec`).
- `GemLoot` adds one pool referencing the inject table to each listed vanilla table, and the gear modifier to `equipment/trial_chamber_melee` and `_ranged` (they nest the armor table, so a table-level modifier reaches every piece), through Fabric's `LootTableEvents.MODIFY`. Additive; tables loaded from vanilla's feature packs (such as the trade rebalance's chest tables) arrive as data-pack sources, so only Fabric-REPLACED tables are skipped.
- One mixin on `populateDefaultEquipmentSlots` of every class that chooses spawn equipment (Mob, Zombie, Drowned, ZombifiedPiglin, AbstractSkeleton, WitherSkeleton, Piglin, PiglinBrute, Pillager, Vindicator, Vex; overrides set weapons after or instead of Mob's armor) counts nesting depth and, when the outermost call returns, applies `mob_gear` to each armor and hand slot in an equipment loot context. Piglins get their weapon in `finalizeSpawn` just before, so it is covered.
- Raids hand out weapons later, in `applyRaidBuffs` (a vindicator's axe; a pillager's enchanted crossbow), so `RaiderGearMixin` gems a weapon that call puts in hand.
- Mob gear drops with vanilla's equipment drop chances and keeps its gems (ominous trial mobs never drop theirs, as in vanilla). The worn system covers mobs as it does players.

### Command

**`/gems [player]`** (`registry/ModCommands`): everyone for themselves; `[player]` for gamemasters.

- Header `Gems on <player>:`, then one short line per effect (owner): the effect's name, its in-game values at their current precision, and its strength in brackets as a share of its best (worn: the effect's perfect set; hits: the cap; drawbacks: the drawback cap). E.g. `Speed +26.2% (69%)`, `Strength +1.97 damage (34%)`, `Turtle Master -72.0% damage, -81.0% speed (100%)`.
- Drawbacks: the same line in red, no extra words.
- Then `Target, on hit:` and the held weapon's per-hit plan (`Poison 1.15 HP/s for 5.5 s (48%)`).
- Sent as system messages, so it shows with `send_command_feedback` off.
- Gem tooltips show the strength in brackets too, `Speed (69%)`, and a drawback as just its name in red.

**`/gems kit <effect> [material] [power]`** (owner, for testing; permission level 2):

- A full armor set (netherite unless named: leather, chainmail, copper, iron, gold, diamond, netherite) with 3 gems of the effect per piece, and a matching sword with 2 (leather: wooden, chainmail: stone).
- Gems are tier 3 at full strength at the Gem Power level (1-6, default 6 = perfect), no extras.
- Worn armor goes back to the inventory or drops. Effect keys tab-complete.
- One feedback line, e.g. "Gem Power V Invisibility set (netherite)".

### Tests

Server: 75 `@GameTest` methods; classes and what they prove are in Milestone test objectives. The tuning values themselves are checked by `tools/gem-balance.py --check`, so tests compare the Java against the model's exported numbers instead of pinning them.

- `BalanceGameTest`: balance.json loads and the Java math gives the model's check numbers; the model's builds from real items.
- `BrewingGameTest`, `EnchantingGameTest` (incl. full pools: kind and no direct opposite), `SocketingGameTest`, `GemEffectsGameTest`, `GemsCommandGameTest`, `GemVsPotionGameTest`.
- `GemsEverywhereGameTest`: every gem worn (with its worn description) and on hit through real paths, both at the cap and as the weakest legal gem, which still does something; a player's own sword swing poisons and the poison ticks; the weakest Poison and Wither tick past the cooldown and the weakest Regeneration heals a heart; thrown trident; arrow gems survive a save; undead in armor and as targets; Invisibility's threshold and full set; Turtle Master's two parts; the turtle helmet takes no gems; Conduit Power only in water.
- `BreezeMeasureGameTest`: a survival player in the perfect `/gems kit breeze` netherite set is shot by 30 real dispenser arrows and 30 skeleton arrows through vanilla's arrow tick; every one that reaches the wearer is deflected.
- `LootGameTest`:
  - core: every place is hooked into its table, and 8 fixed-seed rolls each keep vanilla loot, with gems turning up;
  - milestone: every listed vanilla table rolled 400 times with fixed seeds (gems at about the model's chance, vanilla loot still there); each level table's Gem Power, tier, gear and effect pool, never a perfect craftable gem; every found-only gem from its places, and Mining Fatigue only from player-killed elder guardians; spawn gear rates through the real spawn-equipment method (zombie and piglin armor; skeleton bows, drowned tridents, piglin and vindicator weapons); a raid vindicator's axe at the hard-area 7 % (a test can't register a structure start, so the structure row is checked through the generated data); the trial chamber equipment tables (armor and weapons); gemmed weapons in each level, mostly harmful; ominous perfect gems always tier 3 at full strength, about 1 in 20 clean; a zombie in gemmed armor gets Speed and Glowing, a piglin regenerates and an undead skeleton doesn't; dropped mob armor keeps its gems.

Client: `GemsClientGameTest` parts `beyond`, `colors`, `dials`, `enchanting`, `hits`, `inventory`, `invisible`, `jump`, `levitation`, `looks`, `mobs` (a zombie in gemmed iron armor glowing, two angles; a hard-place loot tooltip), `screen`, `smithing`, `weaving`, `worn` (`-PclientTests=`).

## Gems in Reworked Beacons (owner, 2026-11-16)

With Bubba's Reworked Beacons installed (`specs/reworked-beacons.md`), a finished Gem goes in a beacon's item slot and stays there (removable). Without it, nothing here loads and the mod works as before.

- **Who and what (owner):** every living thing in the beacon's area gets the gem's effects as if worn: main effect, bonus and drawback; players and mobs, the builder included. No protection from a harmful gem but one's own gems (B18 equalTreatment).
- **Pyramid caps Gem Power (owner):** 1 layer I, 2 up to III, 3 up to V, 4 VI. The beacon screen shows the gem's tooltip lines plus the beacon's "Only outputting Gem Power N".
- **Strength:** `BEACON_SHARE = 0.5` in the model (exported `beacon_share`). Builder's proposal, **approved by the owner for now; revisit after playtesting.**
  - Each effect gets `BEACON_SHARE x the effect's worn cap x the gem's quality`: half of what a full netherite set of 12 copies of that gem gives (armor totals are linear in quality).
  - The bonus works the same way; the drawback counts as one worn drawback (`drawbackSum([size])`).
  - The pyramid cap rescales quality by `factor(capped Gem Power) / factor(gem's Gem Power)`.
  - A perfect gem in a full beacon lands at about a vanilla beacon's level I to II, below a perfect armor set.
  - Examples (perfect gem, 4 layers): Speed +37.5 %, Strength +3 damage, Resistance -20 % damage, Regeneration 0.4 HP/s, Haste +50 % mining and +25 % attack speed, Breeze and Ender 50 % chance, Saturation up to 13 food; harmful Poison 0.75 HP/s (never below 1 HP), Slowness 27 %.
- **Stronger applies:** beacon totals join the receiver's worn totals per effect with `max` (`GemTotals.stronger`), against armor and other beacons; potions compare as for worn gems (`GemValues`). Never added.
- **Timing:** effects last the beacon's effect time (vanilla's 9 + 2 x layers seconds), so they fade soon after leaving the area.
- **Standalone payments off (owner):** `GemBeaconPayload.replacesStandalonePayments()` is true, so with gems installed the beacon's gem-base payments (amethyst shard = Speed, ...) are switched off. Only finished gems and other mods' beacon items (the Totem of Warding) fit the slot.
- **Code:**
  - `compat/GemBeaconPayload` is the only class touching the beacons API, registered under the `bubbas_reworked_beacons:payload` entrypoint; `suggests` (not `depends`) in `fabric.mod.json`.
  - Build: `compileOnly` / `gametestCompileOnly project(':bubbas_reworked_beacons')`; `-PwithBeacons` puts the beacons jar on the game test classpath.
  - Each pulse calls `GemBeacons.receive` on every entity in `context.livingEntities()`. A receipt (beacon pos, gem, capped Gem Power, end time = now + `effectDurationTicks`) is kept in the attachment `bubbas_infused_gems:beacon_gems` (not saved; synced to the player itself for its client-side effects). The worn tick drops receipts that ran out.
  - Worn totals are recomputed only when a receipt is new or changed, so the Absorption equip-fill rule isn't re-triggered each pulse. The client recomputes its own totals when the attachment changes or a receipt runs out.
- Verified both ways: core suite without the beacons jar, full suite with it (`-PwithBeacons -PmilestoneTests`).
- Not done: a client screenshot of the beacon screen holding a gem.

## Open

Owner decisions still pending (built as described above until decided; parked items also in BACKLOG.md):

- How gems are lost, beyond popping out of broken gear.
- Healing gem's ceiling: 25 % of each survived hit at 0.90 (model question 6; the mod uses the model's value).
- Per-weapon tuning for gems in bows, crossbows, tridents and maces (all count as diamond level now).
- Drawback sizes (10 % each, under 20 % stacked): playtest.
- Tipped arrow vs gem arrow of the same effect: the higher level wins now; the alternative is total effect (level x time).
- Turtle Master worn: whether its Slowness part should follow "stronger applies" instead of adding to other Slowness.
- Perfect Invisibility set: mobs notice the wearer at 7 % range on top of real invisibility (fine for now, revisit).
- Ender on a weapon: whether to exempt bosses (the Wither, Ender Dragon, Warden) from the blink.
- Silence on a weapon ("quiet kills", deafening players) and Breeze on a weapon: revisit after playtesting.
- Gem strength in a beacon (`BEACON_SHARE` 0.5): approved for now, revisit after playtesting.
- Frost gem: waits for the Minecraft drop that adds a real freezing effect.
- Compatibility with trim-effect mods (TrimEffects, Trim Resonance, BetterTrims): how their effects stack with gems.
- Minor review notes: inverting can craft found-only effects as mains; worn Infested is permanent.
- Not done: person-made textures; a client screenshot of the beacon screen holding a gem.

## Milestone test objectives

Test-lite policy (`specs/quality.md`, Test suites): `InfusedGemsCoreGameTest` runs on every build and push; the rest is the milestone suite (release tags, manual CI dispatch, `-PmilestoneTests`). What each class proves:

- Core (`InfusedGemsCoreGameTest`, 8): balance.json loads and the Java math matches the model's check numbers; a potion and a base gem brew an Infused Gem; the enchanting table finishes a gem; the smithing table sockets up to the limit (armor and one weapon); worn effects match the JSON; melee weapon gems hit the target; `/gems` lists combined effects; every listed loot table keeps its loot and gems turn up.
- `BalanceGameTest` (milestone part): the model's example builds (best build included) come out the same from real items.
- `BrewingGameTest` (milestone part): every brewable source and variant brews, suspicious stews brew by their effect, nothing raises an Infused Gem's form, a real brewing stand accepts and brews.
- `EnchantingGameTest` (milestone part): the form and the bookshelves cap the tiers (item-sourced gems reach tier 3), fixed seeds follow the model and its rarity weights (common effects about 4 to 1 over rare ones), rolls keep their kind and never the direct opposite.
- `SocketingGameTest` (milestone part): every weapon kind takes two gems, tools and elytras none; the grindstone returns all gems; broken armor drops its gems.
- `GemEffectsGameTest` (milestone part): bows tip arrows while pickup stays plain; gem Poison never kills but Wither can.
- `GemsCommandGameTest` (milestone part): `/gems kit` gives a perfect set.
- `GemsEverywhereGameTest`: every gem does something worn and on hit through real paths (player swings, mob hits, thrown tridents, arrows across a save), the perfect Fire Resistance and Water Breathing sets keep the real effect on, undead inversion, the Absorption refill wait shows as an item cooldown, Levitation lifts, mobility sets beat the potion, Invisibility's threshold and full set, Turtle Master's parts and the turtle helmet, the weakest gems still act, Saturation and Hunger, Conduit Power only in water. (Ender and Breeze have their own tests below.)
- `GemVsPotionGameTest`: a gem and a vanilla effect of the same kind: the stronger applies (potion running out, weaker potion, beacons and conduits, golden apples and totems, a tipped arrow, clearing effects, reload), no modifier left behind.
- `BeyondPotionsGameTest`: Wind Burst goes past the mace cap, Weaving webs only into air, Oozing kills split off owned neutral slimes up to the cap (only slimes near the owner count, and they are never saved), Silence keeps a sculk sensor quiet.
- `GemInversionGameTest`: a brewing stand inverts and reverts gems; a drawback-only gem inverts to a weak main.
- `BalanceReviewGameTest`: each bug found in the balance review stays fixed (falls, Harming cooldown, hit stacking, the Absorption equip-fill throttle and refill share, Hero floor, Ender and harmless projectiles, weak and mid Saturation stops and fast regeneration).
- `LootGameTest` (milestone part): level tables make weak rare gems, found-only gems have sources, spawn gear and raid rates, mobs in gemmed armor get its effects, dropped mob armor keeps its gems.
- `BreezeMeasureGameTest`: the perfect Breeze set deflects every dispenser and skeleton arrow (30 of each).
- `TopEndGameTest`: perfect Ender always blinks, perfect Invisibility hides armor and every exit restores it, hidden armor drops visible. (The caps and the 88 % netherite Gem Power V build are checked by `tools/gem-balance.py`.)
- Client (`GemsClientGameTest` parts via `ClientTests`; screenshots): the gems in the inventory, colors, enchanting, smithing, worn and hit views, gemmed mobs, invisible perfect sets (and the wearer's own inventory), gem effects looking like vanilla (`looks`: poisoned hearts, green food bar).
- `StrengthAnswersGameTest`: a perfect Levitation hit lifts a pig 2-4.5 blocks in 2 s, a perfect Ender weapon blinks about 40 % of targets, worn Breeze lets wind charges through and a Breeze hit veers shots x12, a perfect Weaving hit drops 2-3 webs.
- Client part `dials`: a perfect Levitation hit lifting a pig, a perfect Weaving hit's webs around a pig.
- `GemBeaconGameTest` (passes trivially without Reworked Beacons; real with `-PwithBeacons -PmilestoneTests`): a perfect Speed gem pulsed from a 4-layer sky beacon gives a pig and a player in range 0.5 x Speed's worn cap and makes the pig faster; a 1-layer pyramid caps it to Gem Power I (x 0.40); the payload switches the standalone payments off.
- Objectives without code yet:
  - Ender hits: a piglin (brain-driven) blinked by an Ender hit has no attack-target memory for 20 ticks with an enemy in reach, then attacks again; a zombie targeting a player and blinked has no target for 20 ticks, even with the player in reach, then targets again; a blinked player keeps control.
  - Beacons: with both mods installed an amethyst shard no longer fits the beacon slot; leaving the area ends the effect after the effect time; a beacon and armor of the same effect give the stronger, not the sum; a harmful gem beacon hurts its builder; the beacon screen shows the gem's tooltip lines and the cap line.
  - Breeze hits: a Breeze-hit witch's potions and a Breeze-hit shulker's bullets miss a still target more often than unaffected ones; a Breeze-hit skeleton's arrows mostly miss a target 10 blocks away; a Breeze-hit blaze's fireballs spread.
  - A Nausea hit on a player lasts 4 / 6 / 10 s by build; a weak worn Wind Burst landing bounces at least 0.25.
  - (Confident changes, test-lite) `/gems kit <effect> <material> <power>` gives tier 3 gems at that Gem Power's factor and names it; a boxed-in perfect Ender wearer (no safe spot in 16 tries) is hit; `/gems` and tooltip lines carry no "of max" or "drawback" words; gem Wither shows withered hearts and gem Regeneration the heart wave, like Poison and Hunger in the `looks` part.
