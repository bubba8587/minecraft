# Prior art: every project (2026-11-16)

Research only, no code changed. Searched with `tools/prior-art.py`, the Modrinth API (about 100 phrasings, mods and datapacks, with and without the Fabric filter), and GitHub repository search. Every "26.3" below is checked against Modrinth's real file list (a Fabric file tagged 26.3), not the project page. Download counts are Modrinth's on 2026-11-15. Earlier research is not repeated here: `docs/research/undead-prior-art.md` (sleepers in depth), the prior-art notes in `specs/effect-gems.md` and `specs/reworked-beacons.md`, and `docs/knowledge/libraries.md` (library versions).

Overlap words: **full** = does what ours does; **partial** = does some of it; **adjacent** = same theme, different mechanic. **Conflict** = likely to break or fight if both are installed.

## Summary

| Project | Closest existing mods | Verdict |
|---|---|---|
| Bubba's Reworked Mobs: sleepers, hearing meter | Lorespawn (1.16, abandoned), Attract to Sound (26.2), Zombie Awareness (1.21.1), Forsaken Corpses (26.2, look only) | **Unique.** Nothing on any version makes undead real blocks that wake from noise. |
| Reworked Mobs: loot displays, combat, Warding | Forsaken Corpses, Consecration (1.20.1), Torchmaster (1.21.9), Mob Repellent (26.1.2), Brazier (1.20.1) | **Partial overlap**, but nothing in the same space has a 26.3 build. |
| Bubba's Infused Gems | Trims Overhaul / TrimEffects / Trim Resonance (all 26.3), Apotheosis (NeoForge) and an unofficial Fabric port (26.1.2), Socket Sorcery, Effect Gems (datapack) | **Partial overlap.** "Effects from armor" is common; brewed and enchanted gems, partial strength, perfect sets and inversion are not done anywhere. |
| Bubba's Reworked Beacons | Beyond Basic Beacons (26.3), Potion Beacons (26.3), Underground Beacons (26.3), Extended Beacon Range (26.3), Beacon Overhaul (1.20.1) | **Partial overlap, unique core.** Lots of small beacon tweaks; none has an item slot, an API, pairs or a pyramid cap. **Biggest conflict risk of any project.** |
| Bubba's Totems & Artifacts | Artifacts (14.4M, 26.1.2), Relics RPG (26.3), Lost Relics (26.3), More Totems of Undying (26.2), Explorer's Compass, magnet mods, grave mods, No Creeper Grief | **Crowded genre.** Each totem has look-alikes; the "held = you, in a beacon = an area" idea and the sleeper tie-ins are ours alone. |
| Bubba's Crafting Tweaks (datapack) | Crafting Tweaks datapack (900 recipes, 26.3), Flightning99's / Kai's Crafting Tweaks, Craftable Elytra | **Crowded**, and the name is shared by a 10.7M-download mod. Synthetic Diamond Ore is unique. |
| Crafting Tweaks companion mod: Glider | Hang Glider (1.4M, 26.3, MIT), Reliable Gliders (26.3, MIT), Gliders (1.21.1) | **Partial overlap.** Ours is the only one that is an "elytra, but worse at turning" worn in the chest slot. |

## Top things to know

1. **Reworked Beacons will fight other beacon mods.** At least six beacon mods have 26.3 builds and touch the same beacon code (details under Compatibility risks). Decide which ones we declare incompatible (`breaks` in `fabric.mod.json`) and which ones we test with.
2. **Warding has an empty field on 26.3.** Every area spawn-blocker (Torchmaster, Mob Repellent, Brazier, Spawn-Proofed Villagers) is stuck on an older version. That's a gap the Totem of Warding fills right now.
3. **Seeking, Gathering, Keeping and Fortification each have popular single-purpose mods** (Explorer's Compass 12.6M; magnets; grave and soulbound mods; No Creeper Grief). Their held versions add little on their own. The beacon versions (an area, or into a container) are where ours is new.
4. **Name clashes:** "Crafting Tweaks" is a 10.7M-download mod (crafting-grid buttons) and also a 26.3 datapack; "Effect Gems" is a datapack (already handled by naming ours Infused Gems). Our "Bubba's" prefix keeps the Modrinth slugs free (all four of ours are unused), but players searching will see the others first.
5. **Libraries:** Trinkets Updated (26.3, MIT) is the one accessory-slot API we could use if artifacts ever get worn slots; Accessories and owo-lib have no 26.3 build. Cardinal Components and Lithostitched are available. We don't need any of them yet.

---

## 1. Bubba's Reworked Mobs

### Sleepers and the hearing meter

| Mod | Link | Loader / latest MC | License | Downloads | What it does | Overlap | Conflict? |
|---|---|---|---|---|---|---|---|
| Lorespawn (SpawnerMod) | github.com/lilypuree/SpawnerMod | Forge 1.16.3, abandoned | Unlicense | small | Spawner blocks that wake from player noise | partial (the concept) | no (not on 26.3) |
| Attract to Sound | modrinth.com/mod/attract-to-sound | Fabric/NeoForge, **26.2** (updated 2026-11-06, so 26.3 is likely soon) | GPL-3.0 | 81k | Awake mobs hear, see and smell you; stealth overhaul | partial (hearing, for awake mobs) | **possible** once it reaches 26.3: it rewrites how every mob senses players. Ours only touches our own woken undead, so the risk is a double-up (their senses on our zombies), not a crash |
| Zombie Awareness | modrinth.com/mod/zombie-awareness | 1.21.1, last update 2024 | ARR | 9.2M | Zombies track sound, blood and light | partial | no (old) |
| MobAwareness | modrinth.com/mod/mobawareness | 26.1.2 | ARR | 2.6k | Mobs react to light and sound | partial | no (not on 26.3) |
| Forsaken Corpses | modrinth.com/mod/forsaken-corpses | Fabric 26.2 | ARR | 59k | Lying skeleton corpse blocks in caves (they never wake) | adjacent (look) | no |
| The Graveyard | modrinth.com/mod/the-graveyard-fabric | Fabric **26.3** | GPL-3.0 | 5.7M | Graveyard structures, ghouls rising from the ground, sarcophagus that may spawn a wraith | adjacent | low: separate mobs and structures. Its structures may sit near our graves later |
| Undead Nights | modrinth.com/mod/undead-nights | Fabric **26.3** | ARR | 319k | Zombie horde nights | adjacent | low: it spawns vanilla zombies. Check whether its hordes count as "natural spawns" for wards |

**What ours adds:** undead that are real blocks (free to keep, outside mob caps, minable), a warden-style meter that sneaking and wool beat, and robbing them. Nothing else does this.

### Loot displays and combat

- **Loot display block:** nothing close. Item frames, armor stands and "display case" mods are adjacent at most.
- **Smite kills, arrows weak on skeletons, armor shredding:** Consecration (1.20.1, LGPL, 67k) makes undead resist everything but holy damage. Not on 26.3; adjacent. Mob Champions (26.3, MIT, 56k) and Mobs Equipment (26.3, LGPL) make mobs tougher or better geared. They're adjacent, but they would apply to *vanilla* mobs, not our woken undead, so there's no conflict.
- **Grabby Mobs** (26.3, ARR, 409k): every mob picks up floor items. Adjacent to our woken undead picking up loot; low risk.
- **Mobs Always Drop / Mob LootBags:** change death drops. Low risk; they would also apply to our undead's drops.

### Warding (Totem of Warding, wards)

| Mod | Latest MC | License | Downloads | What it does | Overlap | Conflict? |
|---|---|---|---|---|---|---|
| TorchMaster | 1.21.9 | MIT | 3.4M | Mega Torch blocks spawns in a big radius; Dread Lamp attracts them | partial (the area spawn-stop) | no (not on 26.x). MIT, so its code is fine to read |
| Mob Repellent (modrinth.com/mod/mob-repellent) | 26.1.2 | ARR | 9k | A block that stops spawns around your base | partial | not yet |
| Brazier (github.com/TeamGalena/Brazier) | 1.20.1 on Modrinth | custom | 43k | A beacon-like multiblock that stops spawns | **partial: the closest to wards** | no (old) |
| Spawn-Proofed Villagers | 1.21.9 | MIT | 40k | No monster spawns near villagers | adjacent | no |
| Conduits Prevent Drowned | **26.3** | ARR | 139k | No drowned spawns near conduit users | adjacent | low: both stop spawns; they add up |

**What ours adds:** the held pulse that scares monsters, monsters refusing to walk in (not only spawn-stopping), the 4-layer "sleepers stay asleep" ward, and it being a beacon payload. On 26.3 nobody else does area spawn-proofing at all.

## 2. Bubba's Infused Gems

| Mod | Link | Loader / latest MC | License | Downloads | What it does | Overlap | Conflict? |
|---|---|---|---|---|---|---|---|
| Trims Overhaul | modrinth.com/mod/trims-overhaul | Fabric **26.3** (also datapack) | MIT | 84k | Each armor trim gives abilities, optionally locked behind quests | partial ("armor gives powers") | balance only: effects stack with gems. No crash expected (different data) |
| TrimEffects | modrinth.com/mod/trimeffects | **26.3** | MIT | 4.7k | Each trim gives +1 effect level per piece, capped at II | partial | balance only |
| Trim Resonance | modrinth.com/mod/trim-resonance | **26.3** | ARR | 5.4k | Trim material and pattern give effects | partial | balance only |
| NTRDeal's Items | modrinth.com/mod/ntrdeals-items | **26.3** | MIT | 0.9k | An "infusion" system using armor trims | partial | balance only |
| Effect Gems | modrinth.com/mod/effect-gems | **26.3** datapack | CC-BY-NC-ND (look only) | 11k | Hold a gem in a hand for an effect | adjacent, same name | no |
| Apotheosis | modrinth.com/mod/apotheosis | NeoForge only | MIT | 399k | Sockets, gems, affixes, enchanting overhaul | partial (sockets) | n/a on Fabric |
| apotheosis-fabric (unofficial port) | github.com/WasabiIceCream/apotheosis-fabric | Fabric 26.1.2, 0 stars, one server's port | MIT | n/a | Apotheosis's sockets, gems and affixes on Fabric | partial | **yes if it reaches 26.3**: a second socket system on the same armor, and it changes loot |
| Socket Sorcery | github.com/TinyTaru/socket-sorcery | Fabric (uses Trinkets) | CC0 | n/a | Engrave gems, socket them into worn accessories | adjacent | low (accessories, not armor) |
| Artisan's Sockets | github.com/X1miyaStudio/Artisans-Sockets | datapack 1.21.11 | ARR | n/a | Drop a gem by an anvil for one permanent bonus | partial (simple) | no |
| Zander's Reinforced Tools/Armors, Runes of Power | Modrinth | 1.21.1 | ARR | 5-8k | Sockets and runes on gear | partial | no (old) |
| Easy Magic | modrinth.com/mod/easy-magic | Fabric **26.3** | MPL-2.0 | 14.6M | Enchanting table keeps items, rerolls, shows enchantments | adjacent | **likely**: it reworks the enchanting table screen, and Infused Gems mixes into the same `EnchantmentMenu` methods. Test together |
| Enchanting Infuser | modrinth.com/mod/enchanting-infuser | Fabric **26.3** | MPL-2.0 | 8.4M | A separate block for choosing enchantments | adjacent | low (its own block). Gems won't enchant in it |

**What ours adds:** brewing a potion into a gem, enchanting it to set its strength, permanent but partial effects with drawbacks, perfect sets, inversion, weapon gems, and gems working in beacons. No other mod brews gems.

## 3. Bubba's Reworked Beacons

| Mod | Link | Loader / latest MC | License | Downloads | What it does | Overlap | Conflict? |
|---|---|---|---|---|---|---|---|
| Beyond Basic Beacons | modrinth.com/mod/beyond-basic-beacons | Fabric **26.3** | AGPL-3.0 | 273 | Several effects per beacon, adjustable range and power | partial (most similar idea) | **yes**: reworks the beacon menu and effects |
| Potion Beacons | modrinth.com/mod/potion-beacons | Fabric **26.3** | CC-BY-SA | 1k | Potions as beacon effects | partial (item → effect) | **yes**: changes payments and effects |
| Underground Beacons (Serilum) | modrinth.com/mod/underground-beacons | Fabric **26.3** | ARR | 118k | Beacons work with blocks above them | partial (we have underground pairs) | **yes**: changes the sky check our pairs and beam rule rely on |
| Underground Beacon | modrinth.com/mod/underground-beacon | Fabric 26.2 | MIT | 86 | Same, smaller | partial | yes when updated |
| Extended Beacon Range | modrinth.com/mod/extended-beacon-range | Fabric **26.3** | MIT | 3.7k | Bigger ranges | partial | **likely**: our ranges and pyramid cap come from the same code |
| Better Beacon Placement | modrinth.com/mod/better-beacon-placement | **26.3** | ARR | 302k | Right-click a beacon with blocks to build its base | adjacent | low |
| Discontinuous Beacon Beams | modrinth.com/mod/discontinuous-beacon-beams | **26.3** | MIT | 128k | Hides the beam through clear glass | adjacent | low-medium: could hide our pair beam (client only) |
| PurpurPack beacon bases (copper, amethyst, raw ores) | modrinth.com/mod/purpurpacks-copper-block-beacon-base | **26.3** datapacks | MIT | 8-33k | More base blocks and payment items | adjacent | low for bases (a tag). Their payment items won't fit our one-slot rules |
| Beacon Overhaul (+ Revived fork) | modrinth.com/mod/beaconoverhaul | 1.20.1 / 26.1.2 | Apache-2.0 | 449k | Beacon tiers, better effect scaling | partial | not on 26.3 yet. Apache, fine to read |
| Beacons For All | modrinth.com/mod/beacons-for-all | 1.20.2 | LGPL | 22k | Beacons affect animals and mobs too | partial (we already do this) | no (old) |
| CERBON's Better Beacons | modrinth.com/mod/cerbons-better-beacons | 1.21.1 | CC-BY-NC-SA | 176k | New beacon screen and features | partial | no (old) |

**What ours adds:** one item slot that any mod can fill through our API, the pyramid capping power, upside-down paired beacons underground, and mobs in range. No beacon API exists for 26.3 to build on (B9: hand-rolled because no library does it).

## 4. Bubba's Totems & Artifacts

| Feature | Closest mods (26.3 unless noted) | Overlap | Conflict? |
|---|---|---|---|
| **Genre: found artifacts** | Artifacts (modrinth.com/mod/artifacts, MIT, 14.4M, **26.1.2**), Relics RPG Series (ARR, 481k, 26.3, uses Trinkets), Lost Relics (ARR, 1.6M, 26.3), Relics & Artifacts (Apache, 14k, 26.3), Charged Charms (LGPL, 36k, 26.3), Mythic Charms (GPL, 2.7M, 1.21.1), Ancient Artifacts 2 (CC-BY-NC-ND, 26.2) | adjacent (same genre, different items) | low: separate items. Loot table additions all add up, which can crowd chests |
| Grave Talisman (glows near undead) | none (see `specs/undead.md`) | unique | no |
| Totem of Warding | see Warding above | partial | see above |
| **Keeping** (keep inventory once) | Universal Graves (LGPL, 5.2M), Gravestones (MIT, 1.3M), Nice Keep Inventory (GPL), Keep Inventory Orbs (MIT), Soulbound Enchantment datapack (AGPL), Inventory Totem (ARR, 3.3M: Totem of Undying works from anywhere) | partial: same goal, done as graves or an enchantment | **yes, with grave mods**: both act on death drops. With a grave mod the Keeping totem must win (fire first and leave nothing for the grave), or the items get duplicated or lost. Test with Universal Graves |
| **Gathering** (pull items/XP) | Sticky Fingers (MIT, 26.3), davdeo's Item Magnet (MIT, 26.1.2), EXP Vacuum (MIT, 26.2: a chest that sucks XP), IG Magnet (GPL, 26.3) | partial (held), adjacent (beacon into a container) | low: two magnets just both pull |
| **Fortification** (no explosion block damage in an area) | No Creeper Grief (MIT, 103k, 26.3: global, creepers only), Anti Creeper Grief, Creeper Healing (Apache, 1.21.8), Blast Plaster (NeoForge) | partial: theirs are global or creeper-only; ours is an area and covers every explosion | low: both cancel block damage |
| **Growth** (crops grow faster) | Crop Growth Modifier (MIT, 1.1M, 1.20.1: global), Twerk Crop Growth (GPL, 1.21.1), Sprinklerz (MIT, 1.20.1), Snad | partial; **nothing area-based on 26.3** | low: random-tick boosts add up |
| **Seeking** (points to a structure) | Explorer's Compass (CC-BY-NC-SA, 12.6M, 26.3), Nature's Compass (biomes, 27.7M, 26.3), OmniCompass (MIT, 26.1.2), Luna's Structure Compass (datapack, MIT) | **full** for the held version | no conflict, but a big, well-known overlap. Ours needs a reason to exist (found not crafted, a beacon version, or tied to our graves and camps) |
| Other totems on 26.x | More Totems of Undying (MIT, 1.4M, 26.2: seven mob-themed Undying variants), Void Totem (GPL, 1.21.1; another one is 26.3 ARR), Custom Totems (MIT, 26.3) | adjacent | low |

## 5. Bubba's Crafting Tweaks (datapack and companion mod)

| Mod | Link | Loader / latest MC | License | Downloads | What it does | Overlap | Conflict? |
|---|---|---|---|---|---|---|---|
| Crafting Tweaks (datapack) | modrinth.com/datapack/craftingtweaks | **26.3** | ARR | 5.7k | 900+ quality-of-life recipes | partial (same kind of pack) | low: different namespace. Two recipes with the same inputs would show both results in the book |
| Flightning99's / Kai's Crafting Tweaks | Modrinth datapacks | **26.3** | MIT | under 1k | Small recipe tweak packs | partial | low (same as above) |
| Crafting Tweaks (BlayTheNinth, mod) | modrinth.com/mod/crafting-tweaks | Fabric **26.3** | ARR | 10.7M | Rotate, balance and clear buttons on crafting grids | **name only** | no conflict; name confusion only |
| Craftable Elytra | modrinth.com/datapack/craft-elytra | **26.3** | custom | 72k | An elytra recipe | adjacent (our old elytra recipe now makes the Glider) | no |
| Hang Glider (Fuzss) | modrinth.com/mod/hang-glider | Fabric **26.3** | MIT | 1.4M | A held hang glider for slow, controlled descents | partial | low: separate item and flight code |
| Reliable Gliders | modrinth.com/mod/reliable-gliders | Fabric **26.3** | MIT | 49k | A simple balanced glider | partial | low |
| Gliders (Suff99) | modrinth.com/mod/gliders | 1.21.1 | GPL | 1.6M | Gliders with upgrades | partial | no (old) |
| Do a Barrel Roll | modrinth.com/mod/do-a-barrel-roll | Fabric **26.3** | GPL | 11.8M | Full rotation elytra controls | adjacent | **possible**: it rewrites how elytra flight turns, so our slower turning may be overridden. Test with it |
| Elytra Nerf - Dimensional | modrinth.com/mod/elytra-nerf-dimensional | **26.3** | MIT | 0.9k | Extra elytra durability loss | adjacent | no (the Glider has no durability) |

**What ours adds:** a worn glider that flies like the elytra but turns slowly and gets weaker rockets, from the owner's recipe, plus Synthetic Diamond Ore (nothing similar found) and tinted Bamboo Paper.

---

## Compatibility risks

From most to least likely to cause trouble. "Test" means put both in the "mod testing" profile and try it.

1. **Beacon mods vs Reworked Beacons.** Beyond Basic Beacons, Potion Beacons, Underground Beacons and Extended Beacon Range all change the same beacon code (menu, effects, sky check or range). Two mods changing one method usually means one silently loses, or the game crashes on start. Suggest: declare the first two as `breaks` (they replace the menu and effects outright); test Underground Beacons and Extended Beacon Range, and break them if they wreck pairs or the cap.
2. **Lithium (144M downloads, 26.3) vs Warding.** Wards keep monsters out by changing how pathfinding marks blocks as walkable. Lithium speeds up the same pathfinding code by caching which blocks are walkable. Our check runs before the block lookup, so it's probably fine, but almost every player runs Lithium, so this matters most. Test with Lithium before release.
3. **Easy Magic (14.6M, 26.3) vs Infused Gems.** Both change the enchanting table's menu. Test.
4. **Grave and keep-inventory mods vs the Totem of Keeping** (when built). Both act on death drops: risk of duplicated or lost items. Test with Universal Graves.
5. **Do a Barrel Roll vs the Glider:** may override our slower turning. Test.
6. **Attract to Sound (when it reaches 26.3), Undead Nights, Mob Champions:** would also act on our woken undead or interact with wards. Probably fine, worth one look.
7. **Balance, not bugs:** trim-effect mods (Trims Overhaul and others) stack effects with gems; extra loot from artifact mods dilutes the chance of finding our totems in chests.

## Libraries to consider (B9)

| Library | 26.3 build | Use for us | Recommendation |
|---|---|---|---|
| Trinkets Updated (Patbox fork, MIT) | yes, 4.2.1+26.3 | Worn accessory slots (ring, necklace) for artifacts | Only if the owner wants worn artifacts. Today's design (held or in a beacon) needs no slots. Use it as an optional extra, never required |
| Accessories (wisp-forest) | **no** (newest 1.21.10) | Same as Trinkets | Not usable now |
| Curios | NeoForge only | - | Not usable |
| Cardinal Components API (MIT) | yes, 8.1.0 | Saving extra data on players, chunks or worlds | Fabric's own data attachments already cover what we do; keep it in reserve |
| owo-lib | **no** (26.2) | Config screens, UI | Not usable now |
| Lithostitched (MIT) | yes, 2.0.6 | Worldgen helpers for graves and crypts | Good fit when Reworked Mobs worldgen starts |
| GeckoLib (MIT) | yes, 5.5.7 | Animated mobs | Not needed (vanilla models do our poses) |
| Polymer (LGPL) | yes | Server-side-only items | Not needed |
| A beacon API, a socket/gem library | none exist for 26.3 | - | Hand-rolled because no library does it (already the case) |

Don't use the near-empty GitHub repos that came up for "accessories api" (`forwardfrostview/...`, `ordershogunsnatch/...`, created 2026-09-21 with no history). They look like spam and possibly malware.

## Artifact ideas seen elsewhere (inspiration only)

Read for ideas, never copied. Most of these are ARR or non-commercial. Descriptions of Artifacts and MC Dungeons items are from memory of those mods, not re-checked today.

- **Artifacts (MIT):** Universal Attractor (a magnet, like Gathering); Chorus Totem (teleports you away instead of dying); Kitty Slippers (creepers avoid you, which suits Warding); Antidote Vessel (bad effects wear off faster); Cross Necklace (longer safety after a hit); Panic Necklace (a speed burst when hurt); Obsidian Skull (fire resistance after you first burn); Superstitious Hat / Lucky Scarf (more looting and fortune); Villager Hat (cheaper trades); Umbrella (slow fall and blocks rain); Everlasting food; a Mimic chest that bites.
- **MC Dungeons Artifacts (restrictive license):** *placed* totems are the closest idea to ours. Totem of Regeneration (a healing zone), Totem of Shielding (a projectile shield zone), Totem of Soul Protection; also Corrupted Beacon, Wind Horn (knocks mobs back), Iron Hide Amulet.
- **More Totems of Undying (MIT):** Totem of Undying variants that each add a mob-themed bonus when they pop.
- **Charged Charms (LGPL, 26.3):** charms with limited charges that refill: a non-hidden way to limit power (fits B19 if the charge shows on the item).
- **Void Totem:** saves you from falling into the void, an easy "Totem of Undying, but for one other kind of death" pattern.
- **Quark's Totem of Holding (CC-BY-NC-SA, from memory):** your items are kept in a floating totem where you died, a grave-style take on Keeping.
- **Vanilla:** the recovery compass (points to your death spot) and explorer maps already cover parts of Seeking. A totem that points to the nearest *grave or crypt* (ours) would be new.

## Side note on the tool

`tools/prior-art.py` crashes on Windows when a result's description contains an emoji (Python prints with the Windows code page). Running it with `PYTHONIOENCODING=utf-8` or adding `sys.stdout.reconfigure(encoding="utf-8")` fixes it. Not fixed here (research only).
