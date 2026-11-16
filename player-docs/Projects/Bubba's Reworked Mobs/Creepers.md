# Creepers

*Part of [[Bubba's Reworked Mobs]].*

> [!infobox] Creeper (reworked)
> | | |
> |---|---|
> | **Behavior** | Watches, explodes when startled |
> | **Spawning** | One watcher per player at night |
> | **Charged creepers** | Unchanged |

A **creeper** in [[Bubba's Reworked Mobs]] is a nervous watcher rather than a hunter. It follows moving things with its eyes, hides half behind cover to look at them, and explodes only when it is startled too often.

## Spawning

Creepers no longer spawn randomly. At night, each player has one creeper of their own somewhere nearby: it appears 10 to 15 blocks away, behind cover and out of the player's view, and watches them. A player who turns around quickly can catch it half behind a tree or a wall, looking at them.

Creepers from spawners and trial spawners are ordinary creepers: they hunt players and explode next to them.

## Behavior

### Watching

A creeper notices things that move where it can see them, with players first. It walks toward what it is watching, finds a spot halfway behind a tree, wall or boulder, and peeks out at it. When the player looks straight at it, it slides fully behind its cover; when they look away, it eases back out. A creeper never stops watching a player within 16 blocks.

On open ground with nothing to hide behind, it comes uncomfortably close (about 6.5 blocks) and stares, and backs away if the player comes at it. A player who sneaks toward a creeper makes it retreat behind cover farther away.

### Startling

Being startled fills a hidden meter that empties again over a few seconds:

- Being attacked (one hit calms down in a few seconds; three quick hits set it off)
- Anything moving faster than a walk within about 6 blocks: sprinting, riding, gliding, running or panicking mobs and animals, being knocked back toward it
- Projectiles flying close past it, even when they miss

Falls, fire and cactus do not startle it. Walking calmly near a creeper is safe.

### Hissing

A full meter makes the creeper hiss and swell as normal. During the hiss it defuses if the player who startled it crouches or backs away beyond 7 blocks, unless something hits it while it hisses. A defused creeper stays jumpy for a while. Flint and steel lights a creeper as normal.

### Blindness

A creeper with Blindness senses nothing: it is not startled by movement or hits, and a hiss in progress defuses.

### Charged creepers

Charged creepers keep the normal behavior: they hunt players and explode next to them.

## Commands

*Main article: [[Reworked Mobs Commands]]*

The game rule `bubbas_reworked_mobs:watcher_creepers` turns watcher spawning off and brings normal creeper spawning back.
