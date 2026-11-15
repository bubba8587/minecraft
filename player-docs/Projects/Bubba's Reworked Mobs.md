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
- **They hear you, and it adds up.** Every noise near a sleeping undead disturbs it a little more, and it slowly settles again when things go quiet (about 5 to 10 seconds). Enough noise and it wakes. Doing something reckless close by wakes it fast: sprinting within about 6 blocks wakes it in about a second, and breaking or placing two blocks, landing two or three jumps, or a fight nearby does too. Walking right past one (about 3 blocks away) usually wakes it within a couple of seconds; walking by 6 to 8 blocks away usually doesn't. An explosion wakes everything near it at once. Noise fades quickly with distance, and solid walls muffle it, so sleepers down another hallway don't hear you.
- **A groan is your warning.** A sleeping undead that's been disturbed but not yet woken lets out a quiet, low groan. Back off or go quiet and it settles.
- **Sneak past them.** Sneaking makes your footsteps and landings silent, and wool walls block sound completely, just like with sculk sensors. Walking in creative mode makes noise too; flying in creative makes none.
- **One wakes, others follow.** A waking undead wakes up to four more within about 6 blocks of it, and those can wake more. Undead woken by their neighbours (or by anything other than you) don't fight each other when they get up; they come looking for players.
- **Rob them.** Use a sleeping undead with an empty hand to take its weapon first, then its armor from the head down, one piece per click. Every theft risks waking it, and armor is riskier than the weapon. If it wakes, it comes for you.
- **Dig them up.** Mining a sleeping undead with a Silk Touch pickaxe gives you its body as an item you can place again (careful: it can still wake up); its weapon and armor drop separately. Anything else only leaves a bone or rotten flesh (and the gear).
- **Animals don't wake them.** Cows and pigs wandering through a graveyard are safe; you, other monsters and arrows landing nearby are not. Hurting an animal is a fight, though, and fights are loud.
- **Place them.** A sleeping-undead item can be placed again (it turns to face you). Placed ones can wake too, so they make good traps, or just decoration if you're careful.
- **Woken undead are tougher and slower.** Much more health, weak fists, slow on their feet: easy to run from, hard to kill. They never despawn and keep the gear they slept with (it always drops when they die). An undead that wakes with empty hands grabs something within a few blocks: a weapon if there is one (sword, axe, spear, bow, crossbow, trident or mace), otherwise whatever else is lying there, on display or dropped on the ground. Later, an empty-handed undead that isn't chasing anyone wanders over to loot on display (or items on the ground) nearby and picks it up. While it's after you, it ignores loot and keeps coming.
- **Don't let them corner you.** A single punch from a woken undead barely hurts, but if one keeps pounding on you, every hit tears harder at your armor. Step away for a few seconds and it has to start over.
- **Smite is your friend.** Every level of Smite gives a small chance (4 % per level, 20 % at Smite V) to destroy a woken undead in one hit, in a burst of soul fire.
- **Bows: aim for the head.** Arrows mostly rattle through a woken skeleton's bones (a quarter of the damage), but an arrow to a woken zombie's head does double damage.
- **They move like the dead.** Woken zombies shamble with their arms hanging: they drag one leg up, then fall forward onto the other, and they really do move that way, creeping, then a quicker step (on average no faster than before); woken skeletons stagger toward you with both arms reaching out. Idle zombies slowly drift together into groups.
- **Loot on display.** Weapons and tools can lie on the ground, hang on a wall, stand stuck point-down in the ground, or be buried with only the handle showing, looking just like the real item (resource packs and modded items too). Use one with an empty hand to take it. Buried ones have to be dug up with a shovel, which is noisy enough to wake sleeping undead nearby; picking up the others is quieter, and silent if you sneak. Breaking one drops the item. Watch out: undead grab whatever is on display near them, not just weapons.
- **Grave Talisman (early test).** A small stone talisman whose gem glows when undead are near: faintly within 16 blocks, brighter within 8, brightest within 4. It counts sleeping undead as well as awake ones, and works in either hand or just sitting in your hotbar or inventory. It only glows; it doesn't do anything else. For now it's only in the creative inventory (no recipe yet), and its look and name may change.
- **Fewer monsters at night.** Natural monster spawning is cut to 15 % of normal. Witch huts, ocean monuments, pillager outposts, Nether fortresses, spawners and trial spawners are unchanged.

## How to use

Install like any Fabric mod (see [[Installing Fabric Mods]]); it's needed on both the server and every player's game. In this test version sleeping undead don't generate in the world yet: find them in the creative inventory under **Bubba's Reworked Mobs** (a lying zombie is its own entry).

## Textures

Comes with vanilla-style 16x textures. For higher resolution, turn on **Bubba's Reworked Mobs 32x (Faithful style)** in Options, Resource Packs (place it above Faithful 32x if you use it).

## Commands and settings

- `/gamerule bubbas_reworked_mobs:monster_cap_percent <0-100>`: how many monsters spawn naturally, as a percentage of normal (default 15).
- `/gamerule bubbas_reworked_mobs:cut_monster_spawns false`: turn the spawn cut off completely, for example if you use another mod that controls spawning.
- `/bubbas_reworked_mobs display <ground|buried|wall|stuck> <item>` (operators, for building and testing): puts an item on display where you're looking, turned toward you; `wall` hangs it on the side of the block you look at. Add a position (and optionally `north`, `south`, `east` or `west`) to place it somewhere exact. There's no survival way to put items on display yet.

## Works with

Requires Fabric API. Only the undead from this mod are changed: normal zombies and skeletons from spawners and other mods behave as usual.
