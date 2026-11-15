<!-- dte:C4,B9,B15 -->
# Effect gems (spec, early design)

Builds the C4 effectGems direction. Mod name and id not decided. Independent of the other mods.

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
   - Its strength comes from **the potion used to make it** and **the base gem it's crafted from**. Base gems: diamond, quartz, ender pearl, nether star, prismarine, amethyst. **Diamond is the best.**
   - **More than one gem** can go on armor, and the same effect **stacks**: the best possible stack (diamond gems from the strongest potions) reaches **just under the strongest vanilla potion level** of that effect, never equal to it.
   - **Broken armor pops its gems out** (dropped, not lost). If that's tricky, gems fall out at very low durability instead.
   - **3 gem slots on every armor piece**, any material (owner).
   - **Armor material is a strength factor**: the same gem is stronger in better armor (e.g. netherite above leather).
   - **Contiguous set factor**: neighbouring pieces of the same material along helmet -> chestplate -> leggings -> boots add strength. A connected run (helmet + chestplate) beats the same number of separated pieces (helmet + boots); a full matching set is best.
   - So a gem's contribution = f(potion) x f(base gem) x f(armor material) x f(matching contiguous run), and the total per effect stays just under that effect's strongest vanilla potion level.
   - Still open: the strength ladder of the six base gems (and what's special about nether star, which outranks diamond in vanilla); prismarine shard or crystals.
3. Drawbacks: **shelved for now (owner, 2026-11-14).** Candidates raised: gem-specific side effects, a general gem load, fragile gems that crack, undead drawn to gem-wearers. Balance meanwhile comes from partial strength, the stacking cap and how hard gems are to get.
4. **Making gems (owner, 2026-11-14), two steps, both extending vanilla mechanics:**
   - **Brewing stand:** a potion + a base gem brews an **Infused Gem**. Six infused items in all, one per base gem (Infused Diamond, Infused Quartz, Infused Ender Pearl, Infused Nether Star, Infused Prismarine, Infused Amethyst): a glowing version of the base gem that carries the potion's effect and strength as data. 26.3 brewing recipes are data-driven (`minecraft:brewing`: potion-matching input, item reagent, any output item), so the recipes can be generated per potion x base gem.
   - **Enchanting table:** the infused gem is "enchanted" like gear, with vanilla's mechanics (XP levels, lapis, bookshelves, the seed and its preview). The three option rows are three tiers, each locking in one outcome by chance:
     - **Tier 1 (top row):** a weak negative effect, or a weak form of the intended effect.
     - **Tier 2:** a medium form of the intended effect + a weak form of another effect.
     - **Tier 3:** one of: the strongest form alone; strongest + a weak other effect; strongest + a medium other effect; strongest + a weak negative effect.
     The result is the finished effect gem, ready for the smithing table.
   - **Owner's concern:** with a lingering-potion requirement (dragon's breath, so post-dragon only) this may feel unreachable or unsatisfying. Balance for satisfaction. Gating options being discussed.
5. How gems are lost (beyond popping out of broken armor): open.
