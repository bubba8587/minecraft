# Advancements (draft spec)

**Status: draft for the owner to review. Nothing here is built yet.** Every advancement uses a feature that is already built in `develop` (checked against `STATUS.md`, the specs and the player docs on 2026-11-24). No new mechanics.

## How to read this

- **Mostly progression:** each mod gets one or more chains that go *find it, then go deeper, then a challenge at the end*, plus a few encounters and a couple of hidden jokes.
- **Frame:** *task* is the plain square icon, *goal* the rounded one, *challenge* the spiky purple one with the fanfare (as in vanilla).
- **Hidden:** a hidden advancement doesn't show in the tab until it's earned (like vanilla's "The End?").
- **Trigger:** how the game notices it.
  - **V** = a vanilla trigger does the job, so it's only a data file. Names are checked against the 26.3 source (`CriteriaTriggers`). "(check)" means it works as long as our code still goes through vanilla's path at that point; whoever builds it confirms it in a test.
  - **C** = needs a small custom trigger in the mod's code. The plan is one trigger per mod, `<modid>:event`, carrying an event name such as `rob_sleeper`. The code calls it at the moment named in the table. No new mechanics, only a call at a place that already exists.
- **Mods work alone.** Each mod's advancements live in that mod. An advancement that needs two mods is marked **Needs: (the other mod)**. It is loaded only when both mods are installed (Fabric's resource load condition `fabric:all_mods_loaded`), so nobody sees one they can't earn.
- **IDs** are written without their namespace. The namespace is the mod's id (shown in each section heading). Parents written `minecraft:...` are vanilla advancements.

## Tab layout (proposal)

| Mod | Tab | Why |
|---|---|---|
| Reworked Mobs | **Two tabs of its own**: "Restless Dead" (undead, crypts, camps, Fabricators) and "Wild Things" (creepers, drowned, spiders, endermen, trader posts) | It reworks seven mob families. That's about 30 advancements, too many for one readable tab and too many to wedge into Adventure. |
| Totems & Artifacts | **Own tab** | One self-contained hunt (shreds, map, vault) and a collection (every totem). |
| Infused Gems | **Own tab** | A full brew, enchant, socket ladder up to a perfect gem. It would crowd the Nether tab's brewing branch. |
| Reworked Beacons | **Nether tab**, under "Bring Home the Beacon" | Vanilla's beacon advancements already live there; ours carry on from them. |
| Reworked Mining | **Story tab** (tools and stone), with the quartz set in the **Nether tab** | Story is vanilla's mining ladder (Stone Age, Isn't It Iron Pick, Diamonds!), and quartz gear is for the Nether. |
| Extra Recipes | Slots into **Story, Adventure and Husbandry** | Only a handful, each next to the vanilla advancement it relates to. |
| World Tweaks | **Nether tab** | The lava geysers are on the Nether's lava seas. |

Totals: **87 advancements** (80 for single mods and 7 that need two mods). **46 need a custom trigger** and 41 use vanilla triggers.

---

## Bubba's Totems & Artifacts (`bubbas_totems_artifacts`), own tab

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Totems & Artifacts** | Find an artifact that does what potions can't | task | (tab root) |
| **Dead Reckoning** | Get a Grave Talisman, the gem that lights up when the dead are near | task | Totems & Artifacts |
| **Strike a Pose** | Sneak and use a totem on a block to set it down as a statue | task | Totems & Artifacts |
| **Idol Hands** | Set a Totem of Undying down as a statue. It does nothing. Nothing at all. | task, hidden | Strike a Pose |
| **Scrap Paper** | Find a relic map shred | task | Totems & Artifacts |
| **X Marks the Spot** | Piece together a whole relic map | goal | Scrap Paper |
| **Dig Here** | Find the vault your relic map leads to | goal | X Marks the Spot |
| **Fortune and Glory** | Open a chest in a grand relic vault, the nine-piece kind | challenge | Dig Here |
| **Totem Pole** | Hold every kind of totem, all eleven of them | challenge | Dig Here |
| **Can't Take It With You** | Die with a Totem of Keeping and keep everything anyway | goal | Totems & Artifacts |
| **Seek and Ye Shall Find** | Tell a Totem of Seeking what to look for | task | Totems & Artifacts |
| **Blink and You'll Miss It** | Teleport with a Totem of the End | task | Totems & Artifacts |
| **Should Have Read the Tooltip** | Go for a swim holding a Totem of the End | task, hidden | Blink and You'll Miss It |
| **Second Wind** | Catch a gust from a Totem of the North Wind while gliding | task | Totems & Artifacts |
| **Rain, Rain, Go Away** | End a storm with a Totem of Clear Skies | task | Totems & Artifacts |
| **Back to Square One** | Use a Totem of the Beginning and watch the ground remember | goal, needs Reworked Mobs | Totems & Artifacts |
| **Skeletons in the Closet** | Break through the back wall of a noble crypt and find what's behind it | goal, hidden, needs Reworked Mobs | Dig Here |
| **Gated Community** | Put a Totem of Warding in a beacon. No monsters allowed. | goal, needs Reworked Beacons | Totems & Artifacts |

<details><summary>Triggers (for whoever builds them)</summary>

- **Totems & Artifacts** (`root`): V `inventory_changed`: any item in a new tag `#bubbas_totems_artifacts:artifacts` (the 11 totems and the Grave Talisman)
- **Dead Reckoning** (`grave_talisman`): V `inventory_changed`: `grave_talisman`
- **Strike a Pose** (`statue`): C `statue_placed`: in the statue placing code, when a statue goes down
- **Idol Hands** (`undying_idol`): C `statue_placed` with the Totem of Undying idol
- **Scrap Paper** (`relic_shred`): V `inventory_changed`: `relic_map_shred`
- **X Marks the Spot** (`relic_map`): V `inventory_changed`: `filled_map` carrying the mod's relic shred component (only finished relic maps have it on a filled map)
- **Dig Here** (`relic_vault`): V `location`: inside structure `bubbas_totems_artifacts:relic_vault`
- **Fortune and Glory** (`grand_vault`): V `player_generates_container_loot`: loot table `bubbas_totems_artifacts:chests/relic_vault_grand`
- **Totem Pole** (`all_totems`): V `inventory_changed`: one criterion per totem, all required (like vanilla's "Monsters Hunted")
- **Can't Take It With You** (`keeping`): C `keeping_saved`: in the Totem of Keeping death mixin, when a totem is used up
- **Seek and Ye Shall Find** (`seeking`): V `item_used_on_block` with `totem_of_seeking` (check; else C in the Seeking choose code)
- **Blink and You'll Miss It** (`end_teleport`): C `end_teleport`: in the Totem of the End teleport, after a landing
- **Should Have Read the Tooltip** (`end_water`): C `end_water`: where water hurts a holder of the totem
- **Second Wind** (`north_wind`): C `north_wind_gust`: in the North Wind boost, held or from a statue
- **Rain, Rain, Go Away** (`clear_skies`): C `clear_skies_used`: when the totem actually ends the weather
- **Back to Square One** (`beginning`): **Needs: Reworked Mobs** (the totem only works with it). C `beginning_ripple`: when a held totem sends a ripple
- **Skeletons in the Closet** (`crypt_vault`): **Needs: Reworked Mobs** (crypt vaults only exist with it). V `player_generates_container_loot`: `bubbas_totems_artifacts:chests/relic_vault_crypt`
- **Gated Community** (`warding_beacon`): **Needs: Reworked Beacons**. C `beacon_payload`: Reworked Beacons' payload API, when a Totem of Warding takes effect in a beacon (award players in range)

</details>

15 for the mod alone, 3 cross-mod.

---

## Bubba's Reworked Mobs (`bubbas_reworked_mobs`), tab 1: Restless Dead

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Restless Dead** | The dead are sleeping. Keep it down. | task | (tab root) |
| **Here Lies Nobody** | Walk into a crypt yard | task | Restless Dead |
| **Grave Robber** | Take something from a sleeping undead without asking | task | Here Lies Nobody |
| **Light Fingers** | Take a sleeper's weapon and all four pieces of its armor without waking it | challenge | Grave Robber |
| **Bodysnatcher** | Dig up a sleeping undead with Silk Touch | task | Grave Robber |
| **Rude Awakening** | Kill an undead you woke up | task | Here Lies Nobody |
| **Smite Makes Right** | Destroy a woken undead in one hit with Smite | goal | Rude Awakening |
| **Old Money** | Open a chest in a noble crypt | goal | Here Lies Nobody |
| **Unhappy Campers** | Find a monster camp | task | Restless Dead |
| **Take Me to Your Leader** | Defeat a camp's leader | task | Unhappy Campers |
| **Strike the Colours** | Break a camp's last banner and clear it for good | goal | Take Me to Your Leader |
| **Brute Force** | Defeat the leader of a piglin outpost in the Nether | goal | Take Me to Your Leader |
| **Some Assembly Required** | Find an Ancient Fabricator beside an ancient city | task | Restless Dead |
| **Express Elevator** | Ride a Fabricator's teleporter up to the surface | task | Some Assembly Required |
| **Factory Reset** | Break the hidden spawner and shut a Fabricator down for good | goal | Some Assembly Required |
| **Hostile Takeover** | Loot the Warden's office | challenge | Factory Reset |
| **Quality Control** | Find where the failed products go | task, hidden | Some Assembly Required |
| **Change of Heart** | Take the Fabricator Heart out of its beacon | goal, needs Reworked Beacons | Some Assembly Required |
| **Under New Management** | Put a Fabricator Heart in a beacon of your own | task, hidden, needs Reworked Beacons | Change of Heart |

<details><summary>Triggers (for whoever builds them)</summary>

- **Restless Dead** (`dead/root`): V `tick` (given on joining)
- **Here Lies Nobody** (`dead/crypt_yard`): V `location`: inside structure `bubbas_reworked_mobs:crypt_yard`
- **Grave Robber** (`dead/rob_sleeper`): C `rob_sleeper`: in the sleeper's empty-hand use, when an item is taken
- **Light Fingers** (`dead/light_fingers`): C `rob_sleeper` with a running count per sleeper; fires when the last armor piece comes off and it's still asleep
- **Bodysnatcher** (`dead/bodysnatcher`): V `inventory_changed`: any of the five sleeper items (a new item tag `#bubbas_reworked_mobs:sleepers`)
- **Rude Awakening** (`dead/rude_awakening`): V `player_killed_entity`: type `#bubbas_reworked_mobs:woken_undead` (tag exists)
- **Smite Makes Right** (`dead/smite`): C `smite_proc`: in the undead combat code, at the soul-fire one-hit kill
- **Old Money** (`dead/noble_crypt`): V `player_generates_container_loot`: `bubbas_reworked_mobs:chests/crypt_noble`
- **Unhappy Campers** (`dead/camp`): V `location`: inside structure `bubbas_reworked_mobs:monster_camp`
- **Take Me to Your Leader** (`dead/camp_leader`): V `player_killed_entity`: entity `nbt` with the tag `bubbas_reworked_mobs.camp.leader` (the leader already carries it)
- **Strike the Colours** (`dead/clear_camp`): C `camp_cleared`: in the camp banner code, when a camp loses its last banner (award players in the camp)
- **Brute Force** (`dead/nether_outpost`): V `player_killed_entity`: `piglin_brute` with the leader tag (check the outpost leader uses the same tag)
- **Some Assembly Required** (`dead/fabricator`): C `fabricator_found`: Fabricators are a feature, not a structure, so a nearness check on the known Fabricator sites (could become V `location` once Fabricators are real structures, which is designed)
- **Express Elevator** (`dead/teleporter`): C `fabricator_teleport`: in the teleporter send, for players
- **Factory Reset** (`dead/factory_reset`): C `fabricator_stopped`: when the hidden spawner is broken
- **Hostile Takeover** (`dead/hostile_takeover`): V `player_generates_container_loot`: `chests/fabricator_office` or `chests/fabricator_office_with_gems`
- **Quality Control** (`dead/quality_control`): V `player_generates_container_loot`: `bubbas_reworked_mobs:chests/fabricator_rejects`
- **Change of Heart** (`dead/change_of_heart`): **Needs: Reworked Beacons** (the heart only exists with it). V `inventory_changed`: `echo_shard` with the heart's custom data marker
- **Under New Management** (`dead/new_management`): **Needs: Reworked Beacons**. C `beacon_payload` with the heart (Reworked Beacons' payload API)

</details>

17 for the mod alone, 2 cross-mod.

## Bubba's Reworked Mobs (`bubbas_reworked_mobs`), tab 2: Wild Things

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Wild Things** | Something is watching you. Several somethings. | task | (tab root) |
| **Peekaboo** | Catch a creeper peeking at you from behind cover | task | Wild Things |
| **Deep Breaths** | Calm a hissing creeper by crouching | goal | Peekaboo |
| **Red Light, Green Light** | Stare down a creeper while it builds its own cover | task, hidden | Peekaboo |
| **Ankle Biter** | Get grabbed by a drowned | task | Wild Things |
| **Not Today, Davy Jones** | Break a drowned's grip | task | Ankle Biter |
| **Three's a Crowd** | Break free while three drowned are holding on | challenge | Not Today, Davy Jones |
| **Drop In Anytime** | Get snatched by a spider dropping from its nest | task | Wild Things |
| **That's a Wrap** | Get wrapped in a spider's cocoon | task | Drop In Anytime |
| **Itsy Bitsy** | Kill a giant spider | goal | Wild Things |
| **Purple Patch** | Find a finished enderman cairn | task | Wild Things |
| **No Refunds** | Trade with the endermen at a cairn's trade pad | task | Purple Patch |
| **Dirt Cheap** | Get dirt back from a cairn trade | task, hidden | No Refunds |
| **Open for Business** | Trade with the wandering trader at a trader post | task | Wild Things |

<details><summary>Triggers (for whoever builds them)</summary>

- **Wild Things** (`wild/root`): V `tick` (given on joining)
- **Peekaboo** (`wild/peekaboo`): C `creeper_hid`: in the watch goal, when a watcher slides behind cover because this player looked its way
- **Deep Breaths** (`wild/deep_breaths`): C `creeper_defused`: in the fuse goal, when a hiss stops because its player crouched
- **Red Light, Green Light** (`wild/red_light`): C `creeper_froze`: in the pillar building, when it freezes because it's being watched
- **Ankle Biter** (`wild/ankle_biter`): V `started_riding`: player whose vehicle is `minecraft:drowned` (the grab mounts the player on the drowned)
- **Not Today, Davy Jones** (`wild/kick_and_scream`): C `grip_broken`: in the drowned grip, when it reaches zero and lets go of a player
- **Three's a Crowd** (`wild/three_crowd`): C `grip_broken` with three holders at the time
- **Drop In Anytime** (`wild/drop_in`): V `started_riding`: vehicle `bubbas_reworked_mobs:nest_spider`
- **That's a Wrap** (`wild/wrapped_up`): V `started_riding`: vehicle `bubbas_reworked_mobs:silk_cocoon`
- **Itsy Bitsy** (`wild/giant_spider`): V `player_killed_entity`: `bubbas_reworked_mobs:giant_spider`
- **Purple Patch** (`wild/cairn`): C `cairn_found`: cairns are a feature, so a nearness check on finished cairn sites
- **No Refunds** (`wild/cairn_trade`): C `cairn_trade`: in the cairn trades, when the answer is laid (award the player who laid the offering)
- **Dirt Cheap** (`wild/dirt_cheap`): C `cairn_trade` with dirt as the answer
- **Open for Business** (`wild/trader_post`): V `villager_trade`: player `location` inside structure `bubbas_reworked_mobs:trader_post`

</details>

14 for the mod alone. Reworked Mobs in all: 31 alone, 2 cross-mod.

---

## Bubba's Infused Gems (`bubbas_infused_gems`), own tab

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Infused Gems** | Brew a potion into a gem | task | (tab root) |
| **Cut Above the Rest** | Enchant an Infused Gem into a Gem | task | Infused Gems |
| **Set in Stone** | Set a Gem into armor or a weapon | task | Cut Above the Rest |
| **Bedazzled** | Fill every gem slot on one piece of armor | goal | Set in Stone |
| **Crown Jewels** | Wear twelve gems at once | challenge | Bedazzled |
| **Second Thoughts** | Get your gems back out of your gear at a grindstone | task | Set in Stone |
| **Silver Lining** | Turn a gem's drawback into its main effect at a brewing stand | task | Cut Above the Rest |
| **All Downside** | Enchant a gem that comes out with only a drawback | task, hidden | Cut Above the Rest |
| **Star Power** | Brew a gem with a nether star | goal | Infused Gems |
| **Flawless** | Make a perfect gem: no drawback, nothing held back | challenge | Star Power |
| **You Can't Brew That** | Find a gem nobody can brew: Haste, Luck, Health Boost, Hero of the Village, Mining Fatigue or Resistance | goal | Infused Gems |
| **Share the Wealth** | Put a Gem in a beacon so everything nearby gets it. Everything. | goal, needs Reworked Beacons | Set in Stone |

<details><summary>Triggers (for whoever builds them)</summary>

- **Infused Gems** (`root`): V `inventory_changed`: `infused_gem`
- **Cut Above the Rest** (`enchant`): V `enchanted_item`: item `gem` (the gem enchanting screen already calls this trigger)
- **Set in Stone** (`socket`): V `recipe_crafted`: recipe `bubbas_infused_gems:socket` (the smithing table awards it on taking the result)
- **Bedazzled** (`full_piece`): C: `inventory_changed` with a small custom item test (armor with 3 socketed gems)
- **Crown Jewels** (`crown_jewels`): C `gems_worn`: when worn gear changes, count socketed gems in the armor slots
- **Second Thoughts** (`grind`): C `gems_ground`: in the grindstone take hook that returns gems
- **Silver Lining** (`invert`): C `gem_inverted`: in the inversion brew
- **All Downside** (`all_downside`): C `gem_enchanted` with a drawback-only result
- **Star Power** (`star_power`): C `gem_brewed` with Gem Power VI (or a custom item test on the infused gem)
- **Flawless** (`flawless`): C `gem_enchanted` with a 100 % result and no drawback
- **You Can't Brew That** (`found_only`): C: `inventory_changed` with a custom item test on the gem's main effect
- **Share the Wealth** (`beacon_gem`): **Needs: Reworked Beacons**. C `beacon_payload` with a Gem

</details>

11 for the mod alone, 1 cross-mod.

---

## Bubba's Reworked Beacons (`bubbas_reworked_beacons`), Nether tab

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Slot Machine** | Put an item in a beacon's one slot and turn it on | task | `minecraft:nether/create_beacon` |
| **Hang in There** | Hang a beacon upside down | task | Slot Machine |
| **Beam Me Down** | Join a beacon and an upside-down beacon into a working pair | goal | Hang in There |
| **Underground Empire** | Power a pair of beacons with two full pyramids | challenge | Beam Me Down |
| **Everyone's Invited** | Get hit by a monster your beacon made stronger | task, hidden | Slot Machine |

<details><summary>Triggers (for whoever builds them)</summary>

- **Slot Machine** (`nether/one_slot`): C `beacon_payload`: when the slot item takes effect (award players in range, as vanilla's beacon trigger does)
- **Hang in There** (`nether/hang_in_there`): V `placed_block`: `beacon` with state `upside_down=true`
- **Beam Me Down** (`nether/beam_me_down`): C `beacon_paired`: in the beacon tick, when a pair starts working (award players in range)
- **Underground Empire** (`nether/underground_empire`): C `beacon_paired` with both pyramids at 4 layers
- **Everyone's Invited** (`nether/everyones_invited`): V `entity_hurt_player`: attacker has Strength marked as a beacon effect (`ambient`) (check our beacon keeps that flag)

</details>

5 for the mod alone. Its cross-mod ones sit with the other mods: Gated Community (Totems), Change of Heart and Under New Management (Mobs), Share the Wealth (Gems).

---

## Bubba's Reworked Mining (`bubbas_reworked_mining`), Story and Nether tabs

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Wrong Tool for the Job** | Try mining deepslate with a wooden or golden pickaxe | task | `minecraft:story/mine_stone` |
| **Bare Knuckle** | Break stone with your bare hands. It only takes 47 seconds. | task, hidden | `minecraft:story/mine_stone` |
| **Lucky Strike** | Find something extra in plain stone | task | `minecraft:story/mine_stone` |
| **Diamond in the Rough** | Find a diamond in plain deepslate. No ore required. | challenge | Lucky Strike |
| **Efficiency Expert** | Get an iron pickaxe with Efficiency V | goal | `minecraft:story/iron_tools` |
| **Built to Last** | Find a book of True Unbreaking | goal | `minecraft:story/enchant_item` |
| **Forever Tool** | Put True Unbreaking on a netherite pickaxe | challenge | Built to Last |
| **Quartz Is the New Iron** | Craft a quartz pickaxe | task | `minecraft:nether/root` |
| **Cover Me in Quartz** | Get a full set of quartz armor | goal | Quartz Is the New Iron |

<details><summary>Triggers (for whoever builds them)</summary>

- **Wrong Tool for the Job** (`story/wrong_tool`): C `block_broken`: Fabric's after-break event, deepslate with a wood or gold pickaxe
- **Bare Knuckle** (`story/bare_knuckle`): C `block_broken`: stone with an empty hand
- **Lucky Strike** (`story/lucky_strike`): C `rare_find`: in the rare finds drop
- **Diamond in the Rough** (`story/diamond_rough`): C `rare_find` with a diamond
- **Efficiency Expert** (`story/efficiency_expert`): V `inventory_changed`: `iron_pickaxe` with enchantment Efficiency level 5
- **Built to Last** (`story/built_to_last`): V `inventory_changed`: `enchanted_book` with stored enchantment `bubbas_reworked_mining:true_unbreaking`
- **Forever Tool** (`story/forever_tool`): V `inventory_changed`: `netherite_pickaxe` with enchantment `true_unbreaking`
- **Quartz Is the New Iron** (`nether/quartz_pickaxe`): V `inventory_changed`: `quartz_pickaxe`
- **Cover Me in Quartz** (`nether/quartz_armor`): V `inventory_changed`: all four quartz armor pieces

</details>

9.

---

## Bubba's Extra Recipes (`bubbas_extra_recipes`), Story, Adventure and Husbandry tabs

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Gross...** | Cure rotten flesh into leather | task | `minecraft:husbandry/root` |
| **Pitch Fork** | Craft a trident from prismarine | task | `minecraft:adventure/root` |
| **Lab-Grown** | Bake a block of coal into Synthetic Diamond Ore | task | `minecraft:story/mine_diamond` |
| **Lab-Grown Disappointment** | Mine Synthetic Diamond Ore and get nothing | task, hidden | Lab-Grown |
| **Homemade Wings** | Craft a Glider | task | `minecraft:adventure/root` |
| **Don't Look Down** | Glide on a Glider | task | Homemade Wings |

<details><summary>Triggers (for whoever builds them)</summary>

- **Gross...** (`husbandry/gross`): V `inventory_changed`: `leather` with the "Cured zombie flesh. Gross..." lore
- **Pitch Fork** (`adventure/pitch_fork`): V `recipe_crafted`: `bubbas_extra_recipes:trident`
- **Lab-Grown** (`story/lab_grown`): V `inventory_changed`: `deepslate_diamond_ore` with custom data `synthetic: "true"`
- **Lab-Grown Disappointment** (`story/lab_dud`): C `synthetic_dud`: in the companion mod's synthetic ore drop, when it drops nothing (mod only)
- **Homemade Wings** (`adventure/homemade_wings`): V `inventory_changed`: `bubbas_extra_recipes:glider` (mod only)
- **Don't Look Down** (`adventure/dont_look_down`): V `tick`: player flag `is_fall_flying` with a Glider in the chest slot (mod only)

</details>

6. The first three only use vanilla triggers, so they could also ship in the plain datapack (see questions).

---

## Bubba's World Tweaks (`bubbas_world_tweaks`), Nether tab

| Title | Description | Type | Comes after |
|---|---|---|---|
| **Old Faithful** | Watch a lava geyser erupt | task | `minecraft:nether/root` |
| **Hot Air** | Get lifted by a geyser | task | Old Faithful |
| **Riding the Thermals** | Climb 50 blocks circling one geyser | challenge | Hot Air |
| **Bamboo Balloon** | Ride a geyser's updraft on a Glider | task, needs Extra Recipes mod | Hot Air |

<details><summary>Triggers (for whoever builds them)</summary>

- **Old Faithful** (`nether/old_faithful`): C `geyser_erupted`: when a vent erupts, award players close by
- **Hot Air** (`nether/hot_air`): C `geyser_lift`: in the updraft code, the first lift
- **Riding the Thermals** (`nether/thermals`): C `geyser_lift` with height gained over one ride
- **Bamboo Balloon** (`nether/bamboo_balloon`): **Needs: Extra Recipes mod**. C `geyser_lift` while wearing a Glider

</details>

3 for the mod alone, 1 cross-mod.

---

## Counts

| Mod | Alone | Cross-mod | Custom triggers |
|---|---|---|---|
| Totems & Artifacts | 15 | 3 | 9 |
| Reworked Mobs (two tabs) | 31 | 2 | 16 |
| Infused Gems | 11 | 1 | 9 |
| Reworked Beacons | 5 | 0 | 3 |
| Reworked Mining | 9 | 0 | 4 |
| Extra Recipes | 6 | 0 | 1 |
| World Tweaks | 3 | 1 | 4 |
| **All** | **80** | **7** | **46** |

## Open questions for the owner

1. **Reworked Mobs: two tabs or one?** It has 31, well over the 8 to 15 per mod in the brief, because it's really seven mob families. Options: two tabs (as drafted), one big tab, or trim to about 15 (say which families matter most).
2. **Small mods: their own tab or vanilla tabs?** Beacons, Mining, Extra Recipes and World Tweaks slot into vanilla tabs as drafted. The other way is one shared **"Bubba's Mods"** tab that holds every small mod's advancements together.
3. **Infused Gems: own tab or the Nether tab** (after "A Local Brewery")? An own tab is drafted, because 12 more in the Nether tab would crowd it.
4. **Hidden jokes:** 10 are drafted (Idol Hands, Should Have Read the Tooltip, Quality Control, Under New Management, Red Light Green Light, Dirt Cheap, All Downside, Everyone's Invited, Bare Knuckle, Lab-Grown Disappointment). Keep all, or only the best couple?
5. **Should challenges give XP?** Vanilla challenges usually give experience (e.g. 100 for "Monsters Hunted"). The draft gives nothing; suggest 50 to 100 XP on challenges only.
6. **Extra Recipes datapack:** should the three vanilla-trigger advancements (Gross..., Pitch Fork, Lab-Grown) also ship in the plain datapack, or only in the companion mod?
7. **Totem Pole asks for all eleven totems.** Two (Beginning and End) are rare. Fine as an end-game challenge, or lower it to, say, any eight?
8. **Missing ideas you'd want?** Some built features have no advancement yet: will-o'-wisps, explorer maps, gravestones, bells waking crypts, creeper biome skins, bogged, totem statues for each effect, the Totem of Gathering ring. Say if any should have one.
9. **Icons:** each advancement needs an icon item. The plan is to use the obvious item (the totem, the gem, a sleeper head and so on), with no new art. Any you want different?
10. **Once Fabricators become real structures** (designed, not built), "Some Assembly Required" can switch to a vanilla location trigger. No action needed, just noting it.
