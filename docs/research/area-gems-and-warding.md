<!-- dte:C4,C1,B9,B15 -->

> **Owner's goal for Warding (2026-11-15):** let players build mob-free zones without spamming torches. Every option below is judged against that.
# Area gems and Warding (design exploration)

**Status:** exploration for the owner, 2026-11-15. Nothing is built or decided. It covers two of the owner's ideas from that day:

1. **Warding** for Bubba's Reworked Mobs: one thing that does all three of *keep sleepers asleep*, *stop monsters spawning nearby* and *make mobs avoid the area*. Gems could carry it, but vanilla has no Warding effect. Light is the natural vanilla hook (light means no spawns).
2. **Area-of-effect gems:** instead of light that follows the player (the owner isn't sure that's good), a gem is **mounted** somewhere and makes an area. Maybe there is one single way of mounting gems, maybe it ties in with beacons. Reuse vanilla mechanics where possible, and look at mounts other than the beacon. **Owner addition: bells** (ringing a bell already "wards" in vanilla).

Vanilla facts below were checked in the 26.3 source (`build/mcsrc/26.3/`). The two mods stay independent: each can react to vanilla state (light, game events, tags, effects) but neither uses the other's code. Questions are numbered **Q1, Q2, ...** and listed again at the end.

## In short

- **Recommended primary mount: the bell.** A gem goes into a bell. The bell already is vanilla's "ward" block: ringing it makes raiders within 48 blocks glow and sends villagers within 32 blocks home. It works anywhere (underground, in a crypt, at a camp), can be rung by hand, by arrows or by redstone, is found in villages and ruined portals or bought from a smith for 36 emeralds (it can't be crafted), and its ring is a sound, which ties in with the sleepers' hearing. The Reworked Mobs twist: an ordinary bell is loud and wakes the dead; a **Warding bell tolls the dead to rest.**
- **Recommended second mount, later: the beacon**, as the endgame, big-area, always-on version for any gem (pyramid tier as the strength factor, range 20-50 blocks).
- **Warding is a new gem effect** with no potion (like Silence and Ender), brewed from a **soul lantern** (soul fire is vanilla's own "monsters keep away" block). Mounted, it does the three things; worn and on weapons it does smaller versions.
- **Each mod handles its own side.** Infused Gems stops spawns and pushes mobs away. Reworked Mobs decides what calms a sleeper: light, soul fire, and a "lull" game event that a Warding bell sends. Infused Gems adds its event to Reworked Mobs' list through an optional data tag, not code.

## 1. What vanilla already gives us

| Vanilla mechanic | What it does (26.3) | Use for us |
|---|---|---|
| **Monster spawning needs darkness** | In the Overworld and the End, a monster can't spawn where block light is above 0 (`monster_spawn_block_light_limit` 0), plus a random sky-light test. The Nether's limit is 15, so light doesn't stop most Nether spawns. | Real light is a perfect, free spawn ward, but light fades by 1 per block, so it reaches at most ~14 blocks from one source. |
| **Spawners use the same darkness rule** | A spawner checks the mob's normal spawn rules (the same darkness test), unless the spawner has its own light rules. Trial spawners ignore light. | Lighting around a spawner already shuts it down in vanilla. |
| **Piglin repellents** | Piglins walk away from any block in `#piglin_repellents` (soul fire, soul torch, soul lantern, soul campfire) within 8 blocks across, 4 up/down. | The vanilla look and feel of "monsters back away from this". Soul fire = warding is already in players' heads. |
| **Hoglin repellents** | Hoglins avoid `#hoglin_repellents` (warped fungus, potted warped fungus, nether portal, respawn anchor). | Same pattern; respawn anchors already repel. |
| **Animal fear** | Creepers flee cats and ocelots, skeletons flee wolves, phantoms avoid cats (all within 6 blocks). | The "flee" behavior our avoidance can copy. |
| **Bell** | A ring lasts 50 ticks. Villagers within 32 blocks hear it and go home. If raiders are within 48 blocks, the bell resonates and they glow for 3 s. A ring also sends the `block_change` game event, which our sleepers hear (about 82 noise points, as loud as breaking a block). | The primary mount (section 2). |
| **Beacon** | Pyramid of 1-4 levels gives range 20 / 30 / 40 / 50 blocks (full height). Every 4 s it gives players in range its effects. Needs a clear sky above. The payment slot eats one iron/gold/emerald/diamond/netherite ingot. | The endgame mount (section 2). |
| **Conduit** | 16+ prismarine frame blocks make it work: range = frame / 7 x 16 (32 to 96 blocks), players in water or rain only. With the full 42-block frame it hits one hostile mob within 8 blocks (in water or rain) for 4 damage every 2 s. | Vanilla precedent for an *offensive* ward. |
| **Campfire smoke calms bees** | A campfire under a hive lets you harvest without angering bees. | Vanilla precedent for "calming", not just "repelling". |
| **Potent sulfur (26.x)** | A geyser block that gives Nausea to things standing over it. | Precedent for a block with an area effect. |
| **Invisible light block** | `minecraft:light` (levels 0-15) exists in 26.3, no collision, can share a space with an entity. Not obtainable in survival. | A way for a mount to make *real* light at its own spot, so every vanilla light rule (spawns, snow melting, crops) works with no hooks. |

## 2. Mounting options

Effort is relative: **S** about a day of agent work, **M** a few days, **L** a week or more. "B15" says whether a new block would be needed (none of these needs one).

### Comparison table

| Mount | Strength / tier from | Range | Payment / cost | UI | Works underground | B15 | Effort |
|---|---|---|---|---|---|---|---|
| **Bell** (recommended) | the gem's own strength | 8-32 blocks (vanilla bell uses 32 and 48) | the bell (not craftable: villages, ruined portals, 36 emeralds) | none: sneak-use with the gem, ring to activate | yes | none | M |
| **Beacon** (second) | pyramid level 1-4, like armor material | 20-50 (vanilla) | the beacon (nether star) + pyramid | vanilla beacon screen | **no** (needs sky) | none | M-L |
| Conduit | frame size 16-42 | 32-96 (vanilla), water/rain only | heart of the sea + prismarine | none | yes (water only) | none | M |
| Armor stand "mannequin" | the existing worn system (3 slots x 4 pieces, material, matching set) | small aura, 6-16 | a gemmed armor set | the armor stand itself | yes | none | S-M |
| Glow item frame | the gem's strength, one gem | ~8-14 (real light) | a frame | none | yes | none | S |
| Shelf (26.x, 3 slots) | 3 gems, like one armor piece | 8-16 | a shelf (cheap) | vanilla shelf | yes | none | S-M |
| Decorated pot / chiseled bookshelf | one stack / six books | small | cheap | none | yes | none | S |
| Lectern | (a book, not a gem) | small | cheap | lectern screen | yes | none | M |
| Respawn anchor | its 0-4 glowstone charges (vanilla light 0-15) | 8-24 | glowstone as fuel | none | yes | none | M |
| Lodestone | the gem's strength; a linked compass carries it | around the lodestone | lodestone (netherite) | compass | yes | none | M |
| Campfire / soul campfire | the gem "burns" as fuel | 8-16 | the gem is used up over time | campfire slots | yes | none | S-M |
| Spawner | the gem; affects only that spawner | the spawner's own 16-block activation | the gem | use on the spawner like a spawn egg | yes | none | S |
| Vault / enchanting table | poor fit | - | - | - | - | none | - |
| Copper golem statue | the gem; a "guardian statue" | 8-16 | copper golem (oxidized) | none | yes | none | S-M |

### Bell (the owner's suggestion; recommended)

- **How it works:** sneak-use a bell with a finished gem to set it in (the bell keeps it as block data, shown when you look at it); a grindstone-like rule gets it back (breaking the bell drops it). Ringing (hand, arrow, redstone) **activates** the gem's area effect for a while; a warded bell could also stay "on" quietly between tolls (Q4).
- **Reuses:** the bell's ring, its raider glow (48 blocks) and villager call (32 blocks), its redstone input, its loot and trade sources, its `block_change` game event (the noise sleepers hear).
- **Strengths:** works anywhere, including underground crypts and camps, where the mobs mod lives. Not craftable, so a ward is a real find or 36 emeralds. Ringing is a clear, readable action with sound, and redstone clocks can keep it tolling. Every village already has one, so villages can come pre-warded (Q6). The sleeper twist is unique (section 3).
- **Weaknesses:** a bell has no built-in tiers, so its strength must come from the gem alone (or from what it hangs on, which is fiddly). Redstone clocks make "toll" effectively permanent, so the toll needs a cooldown or the duration must be designed for always-on. Bells in villages are easy to grab once you've found a village.
- **B15:** none. Effort **M**: one interaction, block-entity data on the bell, a ring hook, the area logic.

### Beacon (the owner's idea; recommended second)

- **How it works:** the gem goes into the beacon (Q3: as the payment, used up, or set in and kept). Pyramid level 1-4 is the strength factor, as armor material is for worn gems (level 1 like leather ... level 4 like netherite), and sets range 20/30/40/50 as in vanilla. The gem's effect pulses every 4 s like beacon powers.
- **Reuses:** pyramid tiers, range formula, payment slot, beacon screen, the beam (a visible "this is warded" marker).
- **Strengths:** players already know beacons as "area effects"; the tiers map directly onto the gem strength model; big areas; endgame cost (nether star + a pyramid of mineral blocks) fits strong wards.
- **Weaknesses:** **needs a clear sky**, so it can't ward a crypt, a mine or most dungeons (the mobs mod's main places); vanilla only touches players; the beacon screen has no room for a gem slot without a screen change (client work); endgame only, so no early or mid-game wards.
- **B15:** none. Effort **M-L** (screen and menu changes).

### Bell vs beacon (coordinator's request)

| | Bell | Beacon |
|---|---|---|
| Fits the mobs mod (crypts, caves, camps) | yes, works underground | no, needs sky |
| Vanilla "warding" already | yes (raiders glow, villagers go home) | no (buffs players) |
| Strength ladder | gem only | gem x pyramid tier (closest to the existing model) |
| When in the game | mid (find a village or 36 emeralds) | late (nether star) |
| Area | 8-32 | 20-50 |
| Feel | an action: ring it, hear it, it lasts a while | set and forget |
| Sleeper twist | natural (a bell is a sound) | none |
| Work | M | M-L |

Verdict: the bell is the better *Warding* mount and the better single, general mount; the beacon is the better *endgame amplifier* for big, permanent areas above ground. If the owner wants one way only, the bell (Q1).

### Conduit

- Frame size 16-42 sets strength and range; already attacks hostile mobs when complete, so it is the natural home for **offensive** gems (a Poison gem in a conduit poisons drowned instead of hitting for 4). Water and rain only, which is its identity and its limit. Prismarine shard is already Infusion V, so it is on theme. Prior art: *Conduits Prevent Drowned* already stops drowned spawns around conduit-powered players.

### Armor stand as a "mannequin" emitter

- An armor stand wearing gemmed armor radiates that armor's worn effects in a small aura. The gem system already computes totals for any living thing wearing armor, and armor stands are living entities, so the whole strength model (3 slots x 4 pieces, material, matching set) is reused as-is. 26.3 also has a real **Mannequin** entity, but it has no survival item (commands only), so the armor stand is the player's version.
- **Weakness:** cheap and easy to stack many (needs a "strongest stand applies, no stacking" rule); entities can be knocked over or blown up. Good for "share my gear's buff with my friends", weak as a fortress ward.

### Item frame / glow item frame

- One gem in a frame; a **glow item frame** holding a Warding gem places an invisible vanilla light block in its own space, so it makes real light and every vanilla light rule just works. Cheap, early, decorative. Range is limited by light falloff (~14 blocks) and one gem means low strength. A good **early** ward and a nice answer to "light, but not following the player".

### Shelf, decorated pot, chiseled bookshelf

- The **shelf** (26.x) holds 3 visible items, like one armor piece's 3 slots: three gems on a shelf = one piece's worth of strength. Cheap and early, displays the gems nicely. Pots and chiseled bookshelves fit less (a pot hides its contents, bookshelves hold books).

### Lectern

- Holds a book and has a page screen. A gem doesn't fit, but a lectern could be the **settings** for a nearby ward (whitelist who may pass, toggle) if one is ever needed. Not a mount.

### Respawn anchor or lodestone

- **Respawn anchor:** charges (0-4 glowstone) are a ready-made fuel gauge and already give light 0-15, and it is already a hoglin repellent. A ward that burns one charge per span of time is very vanilla. Downside: it explodes outside the Nether when used, so it would need its ward use split from its respawn use.
- **Lodestone:** a compass linked to it could carry the ward with the player ("you are warded while your compass points home"), which is the "follows the player" idea in a limited, opt-in form.

### Campfire / soul campfire

- A gem placed on a campfire "burns" slowly as fuel (its ward lasts while it burns; Q9 on whether mounted gems are ever used up). Soul campfires are already piglin repellents, and campfire smoke already calms bees, so this is the most vanilla-flavoured "calming" block. Douse it (water or shovel) to switch it off. Prior art: *No Hostiles Around Campfire* (795k downloads, 26.3) already makes campfires spawn-proof and burn nearby hostiles, so this mount is the least original.

### Spawner

- Use a gem on a spawner the way vanilla uses a spawn egg on it. A **Warding gem shuts that spawner down** (a local, close-range, dangerous-to-reach ward: you have to stand at the spawner to do it). Any **other gem** makes the mobs it spawns wear that gem's effect, a ready difficulty tool for dungeons and the "gems as difficulty" plan in `specs/effect-gems.md`. (Vanilla note: lighting the area already stops ordinary spawners; trial spawners ignore light and should stay untouched, Q8.)

### Vault, enchanting table

- The vault is a per-player reward box and the enchanting table is already where gems are made; neither fits as a mount.

### Other good fits found in 26.3

- **Copper golem statue:** the same block the sleepers are modeled on. A gem in a statue makes a "guardian statue" that wards the dead: thematically perfect for crypts, but statues come only from oxidized copper golems, so it's an odd thing to ask of players.
- **Bell + beacon together** is possible later (a bell in a beacon's range borrows the pyramid tier), but it adds a rule for little gain.

## 3. Warding as a gem

### Brewing it

There's no Warding potion, so it is an **item-sourced gem** (like Silence from an echo shard). Glowstone dust is taken by Glowing. Options:

| Source item | Why it fits | Cost |
|---|---|---|
| **Soul lantern** (recommended) | soul fire is vanilla's "monsters back off" block (`#piglin_repellents`); a lantern is a light | moderate: iron nuggets + soul torch (soul soil/sand from the Nether) |
| Soul torch | same theme, cheaper | cheap; twelve for a perfect set is easy |
| Warped fungus | vanilla's hoglin repellent | cheap, Nether |
| Totem of undying | "protection" in vanilla | rare-ish, but raid farms make it plentiful |
| Bell | the mount itself | not craftable; turns the bell into an ingredient, confusing |

A new mod status effect `bubbas_infused_gems:warding` (like Silence), soul-blue colour. Item-sourced gems reach tier 3 under the current rules, so a cheap source makes perfect wards easy: the soul lantern is the middle ground.

### What it does where

| Slot | Proposed effect |
|---|---|
| **Mounted** (bell, beacon, frame...) | the full three: no monster spawns, monsters steer clear, sleepers calmed (below) |
| **Worn** | a personal, small version: monsters without a target don't come within a few blocks of you (up to ~6 at the perfect set), and your noise counts a little less to sleepers. No light following you, no spawn ward around you (the owner's doubt about "light follows the player" stands). |
| **Weapon** | a hit makes the target flee from you for a short time (like a creeper fleeing a cat): 1-4 s by strength; bosses and the warden immune |

### The three effects and their vanilla hooks

**1. No spawning.** Two ways, both kept inside Infused Gems:

- *Real light:* small mounts (glow item frame) place an invisible light block in their own space. Fully vanilla; limited to ~14 blocks; only at the mount, never a trail of light blocks.
- *Spawn rule:* bigger mounts (bell, beacon) refuse monster spawns in their radius at the same natural-spawning check where Reworked Mobs' monster cap sits. Both only ever say "no", so they combine safely. This is how the big spawn-proofing mods (Magnum Torch, TorchMaster) do it. Raids, structures' own spawns, trial spawners and Reworked Mobs camps stay untouched by default (Q8).

Note: Reworked Mobs already cuts monster spawns to 15 % of vanilla everywhere, so in a world with both mods this part matters less than the other two.

**2. Mobs avoid the area.** Copy vanilla's own avoid behaviors rather than inventing new ones:

- Piglins and hoglins: feed the ward's position into their existing "nearest repellent" memory, so they back off exactly as from soul fire.
- Other monsters: a flee-from-a-spot behavior like the creeper's flee-from-cat, only while they have **no target**, so a monster idles and paths around the ward instead of wandering in.
- Strength decides how hard: weak wards only steer idle mobs; strong ones also make a chasing mob hesitate at the edge for a moment; only the perfect ward keeps chasing mobs out entirely (Q5). Ranged mobs can still shoot in from outside.
- Woken undead from Reworked Mobs are ordinary monsters to this rule, so they avoid wards too, with no link between the mods.

**3. Sleepers stay asleep.** Lives entirely inside Reworked Mobs (which already has the hook: every sleeper has a `sensitivity` that multiplies all noise). Three sources, all based on vanilla state so torches, glowstone and wards all work without a dependency:

| Source (in Reworked Mobs) | Rule | Why it's fair |
|---|---|---|
| **Light at the sleeper** | strong block light at the sleeper's spot lowers its sensitivity, e.g. light 12+ x0.5 (never 0) | placing a torch next to a sleeper is itself a loud block-place (about 88 points), so you have to risk waking it to light it |
| **Soul fire nearby** | a block tag `#bubbas_reworked_mobs:calms_sleepers` (default: soul fire, soul torch, soul lantern, soul campfire), within a few blocks: sensitivity lowered further | mirrors the piglin rule; works without Infused Gems; datapacks can change it |
| **A "lull" game event** | a game event tag `#bubbas_reworked_mobs:lulls_sleepers`: sleepers that hear an event in it have their meter reset and their sensitivity lowered for a while | Infused Gems registers its own event (the Warding bell's toll) and adds it to that tag as an **optional** entry in its data. Neither mod's code mentions the other; without Reworked Mobs the tag file does nothing. |

**The bell twist.** Today a bell's ring is a `block_change` vibration (about 82 points), like breaking a block. Reworked Mobs could make an ordinary bell *louder* for the dead (ringing a bell in a crypt wakes the whole crypt: a fun trap and a reason to fear bells), while a Warding bell's toll is a lull event instead: meters drop to zero and sleepers in range stay calmed for the toll's duration. Optional extra (Q7): idle woken undead (no target) near a Warding toll walk back to a spot and **fall asleep again**, turning back into sleeper blocks with their gear ("tolling the dead to rest"). Undead chasing a player never do.

### Strength scaling (starting idea)

Strength r runs from 0 to 1 (1 = the perfect gem, the gem system's own scale):

| Mounted on a bell | Weak gem (r 0.1) | Mid (r 0.5) | Perfect (r 1) |
|---|---|---|---|
| Spawn-free radius | 10 blocks | 20 | 32 |
| Idle mobs steer around | within 8 | 16 | 32 |
| Chasing mobs | unaffected | hesitate 1 s at the edge | kept out |
| Sleeper calm after a toll | sensitivity x0.8 for 1 min | x0.5 for 3 min | x0.2 for 5 min |
| Toll cooldown | 30 s | 30 s | 30 s |

On a beacon the pyramid level multiplies r like armor material does (level 4 = full), and the radius follows the beacon's own 20-50.

### Balance risks

- **Trivialising the mobs mod.** If a ward can put a whole crypt to sleep, the crypt has no teeth. Guard rails: calming never reaches 0 (only sensitivity 0 sleepers, decor, are fully silent); robbing keeps its own wake chance; the lull reaches only 16 blocks (the sleepers' hearing range); a bell must be carried in and placed, which is itself noisy.
- **Perfectly safe bases.** That's the point of a ward, but a perfect ward plus Reworked Mobs' 15 % spawns makes nights empty. Fine for a base, bad if it covers a whole region: keep radii small (bells at 32 max) and don't let wards stack (strongest applies).
- **Redstone tolling** makes "a toll lasts a while" permanent. Either accept it (wards are meant to be permanent) or limit it (the bell needs a player to ring it for the sleeper part).
- **Mob farms.** A ward near a farm breaks it; players' choice, but avoidance must not break *vanilla* farms that rely on mob pathing unless a ward is actually nearby.
- **Servers:** a ward next to someone else's base. Same as torches today; not our problem unless the owner wants claims.
- **Raids and bosses:** wards shouldn't stop raids (bells already alert to them) or keep out bosses (Q8).

### Offensive wards (open question)

Harmful gems mounted could hit **monsters in the area instead of players**, the conduit's model: every 2-4 s one or all monsters in range get a short dose of the gem's effect (Poison, Slowness, Weakness, Wither...). Beneficial gems mounted would buff **players** in range, the beacon's model. Questions this raises:

- Who counts as a target: anything in `Enemy` (the conduit's rule), or also players not on a list? (PvP and griefing on servers.)
- AFK experience farms: a Wither bell over a mob drop is a free grinder. Conduit-style "one target at a time" limits it.
- Pets, villagers, iron golems must never be hit.
- Should beneficial mounted gems be capped at the beacon's own levels (Haste II, Speed II...) so a gem beacon never beats a vanilla one? (Q10.)

## 4. Prior art (checked 2026-11-15 with `tools/prior-art.py` and GitHub search)

| Mod | Supports 26.3 | License | What it does | What ours adds |
|---|---|---|---|---|
| **Magnum Torch** (Fuzss) | yes | MPL-2.0 | new torch blocks stop spawns in a large area | no new blocks; strength from gems; avoidance and sleepers |
| **TorchMaster** (Xalcon) | no (1.21.9) | MIT (readable) | Mega Torch stops hostile spawns (big radius), Dread Lamp stops passive spawns; a flare lantern places invisible lights | same; their spawn-check approach is a good reference |
| **No Hostiles Around Campfire** (Serilum) | yes | All rights reserved | campfires stop hostile spawns and burn hostiles in a radius | gem strength, other mounts, sleepers (overlaps the campfire mount) |
| **Brazier** (TeamGalena) | no (1.20.1) | custom | beacon-like multiblock pyramid stops spawns, range by size | closest to "beacon tier as range"; ours uses the real beacon |
| **Conduits Prevent Drowned** (Serilum) | yes | All rights reserved | no drowned spawn around players with Conduit Power | precedent for the conduit mount |
| **Mob Repellent** | no (26.1.2) | All rights reserved | a block stops spawns around a base | - |
| **Celestial Obstructor** | no (2023) | CC0 | a block stops spawns in a set area | - |
| **Light Up!** | yes | MIT | server tool that lights up an area to stop spawns | ours is in-game, no command |
| **Peaceful Aura** | yes | MIT | a potion effect: mobs don't target you | precedent for a Warding status effect |
| **Beacon Aura**, **Better Beacon Effects**, **Beacon Overhaul** | yes / yes / no | LGPL-3.0 / CC-BY-SA / Apache | beacon reworks (effect stacking; blocks under the beacon add effects) | none take gems or ward |
| **LambDynamicLights** and server-side **Dynamic Lights** mods | yes | custom / MIT | light that follows the player (client-only light doesn't stop spawns; server versions place light blocks) | we deliberately don't follow the player |

Nothing found calms or re-sleeps dormant mobs, uses a bell as a ward, or mounts gems from a strength system into vanilla blocks. The spawn-proofing part alone is well covered (Magnum Torch is on 26.3 with 8.6 million downloads), so ours earns its place through the gems, the bell, avoidance and the sleepers, not through spawn-proofing.

## 5. Recommendation

1. **One mount first: the bell**, for Warding and for any mounted gem. It fits the mobs mod (works underground), has vanilla warding behaviour already, is a mid-game find, and gives the sleeper twist.
2. **Second, later: the beacon**, as the big, always-on, endgame version, with the pyramid level as the strength factor.
3. **Warding gem from a soul lantern**, mounted for the full three effects; small personal versions worn and on weapons; no light following the player.
4. **Reworked Mobs gets its own anti-waking rules** (light at the sleeper, soul fire nearby, the lull game event tag), so it works without Infused Gems, and Infused Gems plugs in only through data.
5. Small extras worth having because they're cheap: **glow item frame** (an early, real-light ward) and **spawner** (a Warding gem shuts one spawner; other gems buff its mobs).
6. Park: conduit offensive wards, armor stand auras, respawn anchor fuel, campfire, lodestone carry.

If yes, the details go into `specs/effect-gems.md` and `specs/undead.md`. The owner could also make "mounted gems live in bells" a direction leaf if it should be preserved.

## 6. Questions for the owner

1. Bell as the main (or only) mount for area gems? (yes/no)
2. Beacon as a second, endgame mount later? (yes/no)
3. In a beacon, is the gem (a) used up as payment, or (b) set in and kept?
4. A Warding bell: (a) always on while the gem is in it, ringing adds the sleeper calm, or (b) only on for a while after each ring?
5. Should a perfect ward keep chasing mobs out completely? (yes/no; no = they only hesitate at the edge)
6. Should some village bells come with a weak Warding gem already in them? (yes/no)
7. Can a Warding toll put idle woken undead back to sleep (turn them back into sleepers)? (yes/no)
8. Leave raids, trial spawners, bosses and camp respawns untouched by wards? (yes/no)
9. Are mounted gems ever used up (a campfire burning them, an anchor using charges)? (yes/no)
10. Mounted beneficial gems capped at the beacon's own levels? (yes/no)
11. Harmful gems in a mount hit monsters in range (offensive ward)? (yes/no/later)
12. Soul lantern as the Warding gem's source item? (yes, or name another)
13. Should an ordinary bell be extra loud to sleepers (ringing one in a crypt wakes it)? (yes/no)
14. Strong light at a sleeper calms it a little, in Reworked Mobs, even without gems? (yes/no)

## Owner decisions (2026-11-16)

- Bells: a bell feels activation-only (it matters while it rings), so not the always-on Warding mount.
- Armor stands: fully out; inert with respect to gems.
- **Beacons are reworked around gems** (owner): the beacon menu loses primary/secondary powers and the iron/gold/emerald/diamond payment; it gets one gem slot, and the screen shows what the beacon will do in the gem's tooltip words. Pyramid size caps the gem's level: 1 layer Gem Power I, 2 layers up to III, 3 layers up to V, 4 layers VI; a stronger gem is capped with a message (e.g. 'Only outputting Gem Power III'). Range grows with the pyramid as in vanilla. Big product call: could be a decision-tree leaf if the owner wants it preserved.
- **'Infusion' level renamed 'Gem Power' everywhere** (owner).
- **The gem stays in the beacon** (socketed, removable), revisit if too strong; the beacon itself already costs a nether star.
- **A gem beacon affects every living thing in range, mobs included** (equality principle): a Regeneration beacon heals nearby mobs, a harmful gem makes a trap for everyone.
- **Sky beacons keep vanilla's area exactly**: a square box out to the range (20/30/40/50) sideways and downward, extended up to the build limit (BeaconBlockEntity: AABB(pos).inflate(range).expandTowards(0, height, 0)).
- **Underground: paired beacons.** Beacons become placeable upside down (a new placement of the existing block, not a new block). A gemmed beacon on the floor pairs with an upside-down beacon above it (its pyramid built upside down above it; the space between clear or glass, like a beam); the upside-down beacon needs no gem. The smaller pyramid sets the range: a square box 3x vanilla's range sideways (60/90/120/150 blocks out), vertically from 10 blocks below the floor pyramid to 10 blocks above the ceiling pyramid.
- **Warding is not a gem** (owner): it's an artifact, the **Totem of Warding**, a found item like the Grave Talisman, owned by Bubba's Reworked Mobs; never on armor or weapons. In a Reworked Beacons beacon it wards the beacon's area. The three mods are split: Reworked Beacons (C5, specs/reworked-beacons.md), Infused Gems, Reworked Mobs.
- **Where it's found: a mix** of crypts/graveyards (once worldgen has them), evokers, and rare chests in harder places; **noticeably more common than the Totem of Undying** (players want several).
- **Held** (either hand): pulses every 10 s; monsters within 8 blocks flee for about 5 s (vanilla flee behavior, like creepers from cats); nearby sleepers are calmed a little. No spawn-proofing when held. The pulse rhythm is shown with **vanilla's item cooldown sweep on the hotbar slot** (as after an ender pearl or wind charge), plus soul-fire particles and a chime; no custom HUD (owner: no hidden cooldowns).
