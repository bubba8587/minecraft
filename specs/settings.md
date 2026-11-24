# Settings (spec; designed, not built; low priority, owner)

- **Priority:** build after the content and cleanup work in flight; don't take a builder off features for it.
- **Owner:** every mod should have settings; "whatever is best" on how they work and what they cover. The choices below are made on that basis. This could become a B leaf if the owner wants it kept as a product rule.

## How settings work: vanilla's own systems (`specs/vanilla-subsystems.md`)

1. **Gameplay switches are game rules.** They're per world, saved with it and enforced by the server, so they work the same in singleplayer, on LAN and on servers. Players change them in vanilla's own screens:
   - **at world creation:** More → Game Rules;
   - **in a world:** the in-game Game Rules screen (`InWorldGameRulesScreen`, from the world options screen; needs gamemaster permission on a server), which syncs with the server through vanilla's own packets;
   - **by command:** `/gamerule bubbas_reworked_mobs:camps false`.
   - **Each mod gets its own category** (`GameRuleCategory.register(id)`, label `gamerule.category.<modid>...` = the mod's name), so players see a "Bubba's Reworked Mobs" section rather than our rules scattered among vanilla's. (Today's rules sit in vanilla's Spawning and Mobs categories.)
2. **Worldgen content is switched off with built-in datapacks,** the way vanilla offers its own optional features: the world creation **Data Packs** screen.
   - Each structure family is a built-in pack inside the jar, **on by default**: crypt yards, monster camps, Fabricators, spider nests, giant spider haunts.
   - Turning one off means that structure never generates in the new world.
   - A game rule couldn't do this cleanly, because structure placement runs before and outside the rules.
3. **Mod Menu** (optional: `suggests` in fabric.mod.json, not required) gives each mod a settings button.
   - In a world, it opens vanilla's in-game Game Rules screen.
   - Outside a world, it shows a short page listing that mod's rules and how to change them at world creation.
   - Without Mod Menu, everything still works through vanilla's screens.
4. **Client-only options (a config file plus a YACL screen) are added only when one is needed.** None are needed today: the meters already have a game rule (`show_meters`). If a purely visual option comes up (particle density, say), YACL is the library (`docs/knowledge/libraries.md`; a 26.3 build is available). Its Maven repository is not in the build now, so add it back then.

## What settings cover

**One on/off switch per main feature, plus the few numbers players commonly want to change.** All other tuning stays fixed, so the balance stays ours.
- Every rule defaults to the mod's normal behavior.
- Names are `<modid>:<feature>`.
- When a switch is off, that feature behaves as vanilla. Things already in a world (sleepers, statues, gems) stay in place, but stop doing the switched-off behavior where that makes sense. The builder writes down what "off" means for each switch in the mod's spec.

**Bubba's Reworked Mobs**
- Existing rules (`ModGameRules`):
  - `cut_monster_spawns` (on);
  - `monster_cap_percent` (15);
  - `watcher_creepers` (on);
  - `cairn_endermen` (on: overworld endermen spawn at cairn sites);
  - `show_meters` (off).
- New switches, all on by default: `sleeping_undead` (undead spawn as sleepers), `back_to_sleep`, `fire_ward`, `bogged_lurk`, `undead_grab_loot`, `caretaker_endermen`, `cairns`, `drowned_rework`, `fabricator_debuffs`, `spider_nests`, `giant_spiders`, `cocoons`, `camp_resets`.
- Datapacks:
  - crypt yards;
  - monster camps (outposts are keyed by banners while the camps pack is on);
  - Fabricators;
  - spider nests;
  - giant spider haunts.

**Bubba's Totems & Artifacts:** one rule per totem (`warding`, `keeping`, `gathering`, `fortification`, `growth`, `seeking`, `beginning_end`, `herding`, `clear_skies`), and `totems_in_loot`.

**Bubba's Infused Gems:**
- `gems_in_loot`;
- `gemmed_mob_gear`;
- `gem_power_cap` (the worn cap, as a percentage of normal; 100).

**Bubba's Reworked Beacons:** `beacon_affects_mobs` (every living thing in range; on).

**Bubba's Reworked Mining:**
- `mining_curve`;
- `rare_finds`;
- `deepslate_needs_iron`.

**Bubba's Extra Recipes:** nothing new. Its recipes are a datapack already, and the Glider is an item.

**Bubba's World Tweaks:**
- `lava_bubbles`;
- `lava_geysers`;
- `geyser_lift`.

## Player docs

Each project's player docs get a **Settings** page listing its rules and packs with their defaults, and how to change them: at world creation, in a world, by command, and from Mod Menu.

## Open

- The totem list above predates the Totem of the North Wind; it needs a rule too (`north_wind`?) when this is built.
- `cairns` (new switch) and the existing `cairn_endermen` overlap; decide whether one rule covers both when this is built.

## Milestone test objectives

- Each rule exists with its default, in its mod's category, and switching it off stops its feature (one test per mod, looping over its rules).
- With a structure pack disabled, that structure doesn't generate.
- Client: the in-game Game Rules screen shows each mod's category, and the Mod Menu button opens it (screenshot).
