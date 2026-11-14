---
type: mod
minecraft: "26.3"
status: active
modrinth:
requires: [Fabric API]
side: client and server
---
# Bubba's Reworked Mobs

Reworks Minecraft's mobs, a family at a time. Part 1 is the undead: skeletons and zombies now lie dormant in the world as bodies that wake up when you make noise near them, and far fewer monsters spawn at night. Early test version: the sleeping bodies still use the normal skeleton and zombie look, and their item icons are plain coloured squares until the real art is drawn.

## Features

- **Sleeping undead.** Skeletons sit slumped, zombies sit or lie on their backs, often holding a weapon and wearing armor. They don't move or attack while asleep.
- **They hear you.** Walking, breaking or placing blocks, opening chests, fighting and explosions nearby can wake them. Louder and closer noises are more likely to. Sneaking makes your footsteps silent, and wool walls block sound completely, just like with sculk sensors.
- **One wakes, others follow.** A waking undead can wake up to four more around it, and those can wake more.
- **Rob them.** Use a sleeping undead with an empty hand to take its weapon first, then its armor from the head down, one piece per click. Every theft risks waking it, and armor is riskier than the weapon. If it wakes, it comes for you.
- **Dig them up.** Mining a sleeping undead with a pickaxe, axe, shovel or hoe gives you the body as an item; its weapon and armor drop separately. Punching it apart only leaves a bone or rotten flesh (and the gear).
- **Place them.** A sleeping-undead item can be placed again (it turns to face you). Placed ones can wake too, so they make good traps, or just decoration if you're careful.
- **Woken undead are tougher and slower.** Much more health, weak fists, slow on their feet: easy to run from, hard to kill. They never despawn and keep the gear they slept with (it always drops when they die). A zombie that wakes with empty hands grabs a sword, axe or spear lying right next to it.
- **Fewer monsters at night.** Natural monster spawning is cut to 15 % of normal. Witch huts, ocean monuments, pillager outposts, Nether fortresses, spawners and trial spawners are unchanged.

## How to use

Install like any Fabric mod (see [[Installing Fabric Mods]]); it's needed on both the server and every player's game. In this test version sleeping undead don't generate in the world yet: find them in the creative inventory under **Bubba's Reworked Mobs** (a lying zombie is its own entry).

## Textures

Comes with vanilla-style 16x textures. For higher resolution, turn on **Bubba's Reworked Mobs 32x (Faithful style)** in Options, Resource Packs (place it above Faithful 32x if you use it).

## Commands and settings

- `/gamerule bubbas_reworked_mobs:monster_cap_percent <0-100>`: how many monsters spawn naturally, as a percentage of normal (default 15).
- `/gamerule bubbas_reworked_mobs:cut_monster_spawns false`: turn the spawn cut off completely, for example if you use another mod that controls spawning.

## Works with

Requires Fabric API. Only the undead from this mod are changed: normal zombies and skeletons from spawners and other mods behave as usual.
