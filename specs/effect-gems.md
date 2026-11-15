<!-- dte:C4,B9,B15 -->
<!-- covers: docs/research/effect-gems-*, tools/gem-balance.py -->
# Bubba's Infused Gems (spec, early design)

Builds the C4 effectGems direction as **Bubba's Infused Gems** (owner's name), mod id `bubbas_infused_gems`, Modrinth slug `bubbas-infused-gems` (free on 2026-11-14). Independent of the other mods.

## Owner's brief (2026-11-14)

Gems are a class of items, each embodying a **permanent** potion effect, added to and removed from **armor**. Very strong, so **hard to get**; they **can't give full potion strength** with infinite duration, and they need **drawbacks**. Undecided: extra GUI slots / a new GUI tab, or data tagged onto the armor.

## Prior art (checked 2026-11-14 with tools/prior-art.py)

- **Effect Gems** (datapack, 26.3, CC-BY-NC-ND: inspiration only): gems held in the off hand or main hand give effects; dropped by rare "Gem Zombies". Same name, different mechanic: our mod needs another name.
- **TrimEffects** (26.3, MIT): each armor trim grants +1 level of an effect per piece, capped at vanilla level II. **Trim Resonance** / **BetterTrims** (26.3): trim material and pattern give stacking abilities. Closest to ours; also shows that vanilla's smithing table and armor trims already "put something into armor" as data on the item, with no new GUI. Compatibility note: these mods also give armor effects, so stacking with them needs a look.
- Apotheosis/Zenith-style socket systems (gems in sockets) exist only on old versions or other loaders.

## Open design questions

Asked one at a time; answers land here.

1. ~~How gems attach~~ **Decided (owner): data on the armor via vanilla stations.** A smithing table adds a gem to a piece of armor (like a trim); a grindstone removes it and returns the gem (like stripping enchantments). The gem is stored on the armor item and shown in its tooltip. No new GUI.
2. ~~Strength~~ **Decided (owner):**
   - A gem gives a **partial** version of its effect, never a full potion (where an effect can't be partial, e.g. night vision, its drawback balances it instead).
   - Its strength comes from **the potion used to make it** and **the base gem it's crafted from**. Base gems: diamond, quartz, ender pearl, nether star, prismarine, amethyst. (Order revised below: by difficulty of getting it, nether star strongest.)
   - **More than one gem** can go on armor, and the same effect **stacks**: the best possible stack (diamond gems from the strongest potions) reaches **just under the strongest vanilla potion level** of that effect, never equal to it.
   - **Broken armor pops its gems out** (dropped, not lost). If that's tricky, gems fall out at very low durability instead.
   - **3 gem slots on every armor piece**, any material (owner).
   - **Armor material is a strength factor**: the same gem is stronger in better armor (e.g. netherite above leather).
   - **Contiguous set factor**: neighbouring pieces of the same material along helmet -> chestplate -> leggings -> boots add strength. A connected run (helmet + chestplate) beats the same number of separated pieces (helmet + boots); a full matching set is best.
   - So a gem's contribution = f(potion) x f(base gem) x f(armor material) x f(matching contiguous run), and the total per effect stays just under that effect's strongest vanilla potion level.
   - Still open: the strength ladder of the six base gems (and what's special about nether star, which outranks diamond in vanilla).
3. Drawbacks: **shelved for now (owner, 2026-11-14).** Candidates raised: gem-specific side effects, a general gem load, fragile gems that crack, undead drawn to gem-wearers. Balance meanwhile comes from partial strength, the stacking cap and how hard gems are to get.
4. **Making gems (owner, 2026-11-14), two steps, both extending vanilla mechanics:**
   - **Brewing stand:** a potion + a base gem brews an **Infused Gem**. Six infused items in all, one per base gem (Infused Diamond, Infused Quartz, Infused Ender Pearl, Infused Nether Star, Infused Prismarine, Infused Amethyst): a glowing version of the base gem that carries the potion's effect and strength as data. 26.3 brewing recipes are data-driven (`minecraft:brewing`: potion-matching input, item reagent, any output item), so the recipes can be generated per potion x base gem.
   - **Enchanting table:** the infused gem is "enchanted" like gear, with vanilla's mechanics (XP levels, lapis, bookshelves, the seed and its preview). The three option rows are three tiers, each locking in one outcome by chance:
     - **Tier 1 (top row):** a weak negative effect, or a weak form of the intended effect.
     - **Tier 2:** a medium form of the intended effect alone, or medium + a weak form of another effect (plain medium added by the owner).
     - **Tier 3:** one of: the strongest form alone; strongest + a weak other effect; strongest + a medium other effect; strongest + a weak negative effect.
     The result is the finished effect gem, ready for the smithing table.
   - **The potion form sets the highest tier the infused gem can reach (owner):** a normal potion allows tier 1 only; splash, tiers 1-2; lingering, all three. So gems start early and the strongest results stay endgame. The base gem is a second progression line (amethyst/quartz early ... diamond; nether star's place to settle in the tables). The enchanting table's preview and vanilla's reroll trick keep bad rolls from feeling final.
6. **Balance tables (owner):** with this many factors (potion type and level/duration, potion form, base gem, enchant tier and result, armor material, contiguous matching set, 3 slots x 4 pieces, stacking), run the numbers so every step is a visible upgrade and the very best is reached only by **lingering potion + diamond base + tier 3 enchant + full netherite set**, landing just under the strongest vanilla potion level. Present them as tables before building.
7. **Gems as loot (owner):** gems of every type, and armor with gems already in it, are findable loot: chests, and **worn by mobs** (mob armor with gems socketed). Gems on mobs give the mobs those effects, including healing, harming, poison and regeneration. Instant effects (healing, harming) need a permanent form (e.g. a periodic pulse, or applied on hit): to settle in the balance work.
5. How gems are lost (beyond popping out of broken armor): open.

## Balance model
The numbers live in `tools/gem-balance.py`, which reads vanilla facts from the 26.3 source, checks every rule and generates `docs/research/effect-gems-balance.md` (and `build/gem-balance.html` for the browser): `python tools/gem-balance.py`. The generated page is the current proposal and lists its open questions; the decisions in this spec stand. In short: every effect is measured against its strongest vanilla level (1.0) and capped at 0.95; a gem's strength is potion x base gem (amethyst 0.80 ... nether star 1.00) x enchant grade x armor or weapon material x matching run (armor); gems of one effect stack strongest first; the only builds reaching the cap are lingering + nether star + tier 3 + full contiguous netherite (armor, 12 gems) and netherite + 2 perfect gems (weapons). Healing returns at most 25 % of each non-lethal hit; drawbacks are fixed and small (under 20 %, never poison or wither on armor); every 26.3 status effect is classified as a craftable gem (from a potion or a vanilla item that carries the effect), a found-only gem, drawback-only, or not a gem.

## Decisions on the balance model's questions (owner, 2026-11-14)

- **Base gems affect strength only.** No special traits (nether star included): anything more is too complicated. Nether star stays on the strength ladder.
- ~~Harmful-effect gems work like Thorns~~ **Rejected by the owner** (2026-11-14): no debuffs on attackers.
- **Healing gem:** when the wearer takes damage from an attack or a fall that doesn't kill them, they instantly regain a portion of that damage instance (a killing blow still kills). Its own, much flatter curve so the best possible version stays modest (starting idea: at most ~25 % of each hit returned; to settle in the tables).
- **Negative enchant outcomes stay as drawbacks on the wearer** (owner: "I'd like to have some drawback gems; they'll just need tuning"): tier 1's weak negative and tier 3's "strongest + weak negative" put a mild harmful effect on the wearer. Tune them to sting without ruining the gem.
- **Weapon gems (owner):** Infused Gems made from harmful potions (poison, slowness, weakness, harming, wither, ...) go into **weapons** and apply a partial version of the effect to what the weapon hits. Beneficial gems stay armor-only.
- **Full inverse for drawbacks (owner):** a weapon gem's bad enchant rolls give the **target** a mild, weak beneficial effect (the mirror of an armor gem's bad roll giving the wearer a mild harmful effect). Same brewing, enchanting tiers, smithing table and grindstone.
- **2 gem slots per weapon** (owner).
- **Which weapons:** melee weapons (swords, axes, maces, tridents, spears) apply the gem's effect on hit. **Bows and crossbows turn each fired arrow into its tipped form** (owner): the arrow gets the gem's effect, at the gem's partial strength, as potion contents when it's shot, so it flies, trails and hits like a tipped arrow. Picked back up, it returns as a **plain arrow** (no free tipped arrows). Firing a real tipped arrow: the stronger effect applies; they don't add up.
- Open: the weapon strength curve (balance tables).
- **Base gem order by difficulty of getting it (owner), weakest to strongest:** amethyst, quartz, ender pearl (farmable), diamond (common in chest loot), prismarine shard (owner: shard, not crystals; ocean monuments), nether star (the Wither). The very best build is therefore **lingering potion + nether star + tier 3 + full netherite**. No loot tables are changed for this.

## Gems are independent of where they go (owner, 2026-11-14)

- **The gem itself is enchanted** (brew the Infused Gem, enchant the gem, then socket the finished gem); armor and weapons are never enchanted.
- **Any gem can go into armor or a weapon; the slot decides who its effects reach.** In armor: the wearer. In a weapon: what the weapon hits (bows and crossbows: the arrow). No armor-only or weapon-only gems: a poison gem in your own armor poisons you, intentionally pointless; a regeneration gem on a sword heals the target. (Replaces the earlier "beneficial gems armor-only, harmful gems weapon-only" split, which was a misunderstanding.)
- **A gem has one set of rolls**, whatever slot it later goes into: its main effect, maybe another effect (tier 2/3 "other"), maybe a drawback.
- **Drawbacks are effects of the opposite kind to the main effect, but never its direct opposite** (owner: a direct inverse just cancels out). A beneficial gem's drawback is a harmful effect that isn't its counterpart (a Speed gem can roll Hunger or Weakness, never Slowness); a harmful gem's drawback is a beneficial effect that isn't its counterpart (a Poison gem can't roll Regeneration). Excluded direct pairs: Speed/Slowness, Strength/Weakness, Haste/Mining Fatigue, Regeneration/Poison and Wither, Luck/Bad Luck, Healing/Harming, Saturation/Hunger, Night Vision/Blindness and Darkness, Invisibility/Glowing, Slow Falling/Levitation (the balance model keeps the authoritative list).

## One item, Infusion levels (owner, 2026-11-14)

- To keep the item count down, there is **one Infused Gem item with one model**, not six. The base gem becomes its **Infusion level**: I amethyst, II quartz, III ender pearl, IV diamond, V prismarine shard, VI nether star. The name carries the rest, e.g. *Infused Gem: Infusion III, Speed (Splash)*, *Infused Gem: Infusion I, Jump Boost (Lingering)*.
- **Confirmed (owner):** the finished, enchanted gem is also one item, its rolled effects in the tooltip. Gems are **colored by potion color, multi-colored**: the main effect tints the body of the gem and a second effect (bonus or drawback) tints a second part (e.g. a highlight or facet layer), using item-carried tint colors like Bamboo Paper, so no extra art per effect.
