# Settings (spec, design 2026-11-22, not built)

Owner (2026-11-22): every mod should have settings. "Whatever is best" on how they work and what they cover. These are the choices made on that basis. Could become a B leaf if the owner wants it kept as a product rule.

## How settings work: vanilla's own systems (specs/vanilla-subsystems.md)

1. **Gameplay switches are game rules.** They're per world, saved with it, and enforced by the server, so they work the same in single player, on LAN and on servers. Players change them in vanilla's own screens:
   - **world creation** → More → Game Rules;
   - **in a world** → the in-game Game Rules screen (`InWorldGameRulesScreen`, from the world options screen; needs gamemaster permission on a server). It syncs with the server through vanilla's own packets.
   - Commands also work: `/gamerule bubbas_reworked_mobs:camps false`.
   - **Each mod gets its own category** (`GameRuleCategory.register(id)`, label `gamerule.category.<modid>...` = the mod's name), so players see a "Bubba's Reworked Mobs" section and not our rules scattered among vanilla's.
2. **Worldgen content can be switched off with built-in datapacks,** the way vanilla offers its own optional features: the world creation **Data Packs** screen. Each structure family is a built-in pack inside the jar, **on by default**: crypt yards, monster camps, Fabricators, spider nests, giant spider haunts. Turning one off means that structure never generates in the new world. A game rule couldn't do this cleanly, because structure placement runs before and outside the rules.
3. **Mod Menu (optional, `suggests` in fabric.mod.json, not required)** gives each mod a settings button. In a world, it opens vanilla's in-game Game Rules screen. Outside a world, it shows a short page listing that mod's rules and how to change them at world creation. Without Mod Menu everything still works through vanilla's screens.
4. **Client-only options (a config file plus a YACL screen) are added only when one is needed.** None are needed today: meters already have a game rule (`show_meters`). If a purely visual option comes up (particle density, say), YACL is the library (`docs/knowledge/libraries.md`, 26.3 build available). Its Maven repo was removed as unused in the 2026-11-22 cleanup, so add it back then.

## What settings cover

**One on/off switch per main feature, plus the few numbers players commonly want to change.** All other tuning stays fixed, so the balance stays ours. Every rule defaults to the mod's normal behavior. Names are `<modid>:<feature>`.

**Bubba's Reworked Mobs**

Existing rules:
- `cut_monster_spawns` (on);
- `monster_cap_percent` (15);
- `watcher_creepers` (on);
- `show_meters` (on).

New switches, all on by default: `sleeping_undead` (undead spawn as sleepers), `back_to_sleep`, `fire_ward`, `bogged_lurk`, `undead_grab_loot`, `caretaker_endermen`, `cairns`, `drowned_rework`, `fabricator_debuffs`, `spider_nests`, `giant_spiders`, `cocoons`, `camp_resets`.

Datapacks:
- crypt yards;
- monster camps (outposts are keyed by banners while the camps pack is on);
- Fabricators;
- spider nests;
- giant spider haunts.

**Bubba's Totems & Artifacts:** one rule per totem (`warding`, `keeping`, `gathering`, `fortification`, `growth`, `seeking`, `beginning_end`, `herding`, `clear_skies`) and `totems_in_loot`.

**Bubba's Infused Gems:**
- `gems_in_loot`;
- `gemmed_mob_gear`;
- `gem_power_cap` (the worn cap, as a percentage of normal, 100).

**Bubba's Reworked Beacons:** `beacon_affects_mobs` (every living thing in range, on).

**Bubba's Reworked Mining:**
- `mining_curve`;
- `rare_finds`;
- `deepslate_needs_iron`.

**Bubba's Extra Recipes:** nothing new. Its recipes are a datapack already, and the Glider is an item.

**Bubba's World Tweaks:**
- `lava_bubbles`;
- `lava_geysers`;
- `geyser_lift`.

When a switch is off, that feature behaves as vanilla. Things already in a world stay in place (sleepers, statues, gems), but stop doing the switched-off behavior where that makes sense. The builder writes down what "off" means for each switch in the mod's spec.

## Player docs

Each project's player docs get a **Settings** page listing its rules and packs with their defaults, and how to change them at world creation, in a world, by command, and from Mod Menu.

## Milestone test objectives

- Each rule exists with its default, in its mod's category, and switching it off stops its feature (one test per mod, looping over its rules).
- With a structure pack disabled, that structure doesn't generate.
- Client: the in-game Game Rules screen shows each mod's category, and the Mod Menu button opens it (screenshot).
