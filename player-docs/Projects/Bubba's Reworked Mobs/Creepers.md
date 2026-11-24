# Creepers

*Part of [[Bubba's Reworked Mobs]].*

> [!infobox] Creeper (reworked)
> | | |
> |---|---|
> | **Behavior** | Watches, explodes when startled |
> | **Spawning** | One watcher per player at night |
> | **Skins** | One per biome, kept for life |
> | **Charged creepers** | Unchanged |

A **creeper** in [[Bubba's Reworked Mobs]] is a nervous watcher rather than a hunter. It follows moving things with its eyes, hides half behind cover to look at them, and explodes only when it is startled too often.

## Spawning

Creepers no longer spawn randomly. At night, each player has one creeper of their own somewhere nearby:

- It appears 15 to 21 blocks away, behind cover and out of the player's view, and watches them.
- A player who turns around quickly can catch it half behind a tree or a wall, looking at them.

### Leaving in the morning

Night watchers leave when day comes. Some time between 10 seconds and about 2.5 minutes after daybreak, each one walks off to a place hidden from its player, or simply walks away when there is nowhere to hide, and vanishes once no player can see it. It leaves sooner when the player has not looked its way for a while. A hissing creeper does not vanish, and hitting or startling a leaving creeper works as usual. Creepers from spawners, structures, spawn eggs and commands never leave this way.

Creepers from spawners and trial spawners are ordinary creepers: they hunt players and explode next to them.

## Behavior

### Watching

A creeper notices things that move where it can see them, players first. It never stops watching a player within 24 blocks.

- **With cover:** it looks for a spot halfway behind a tree, wall or boulder up to 22 blocks from what it watches, walking a long way round to reach one, and peeks out from there. As long as there is any cover around, it always hides behind something.
- **Peekaboo:** when the player looks its way, even out of the corner of the screen, it slides fully behind its cover. When they look away, it eases back out.
- **Creeping closer:** while the player is not looking its way, every few seconds it moves to a closer hiding spot, choosing spots and routes the player cannot see. It can come right up to the wall of a house and peek in round a window frame or doorway, as close as 2 blocks. A player who catches it walking up in the open sends it back to where it came from.
- **Windows:** glass, glass panes and iron bars do not hide it, so it can watch through a window. Leaves still block its view.
- **Running off:** a player who walks or runs toward it, even from inside a house through a window, makes it run (faster than a sprint) to a hiding spot farther away, for example back to the tree it started from. It has to see you coming (glass doesn't stop it, walls and leaves do), so while it's tucked fully behind its cover it won't notice. It waits about 10 seconds before creeping closer again.
- **Sneaking up:** a player who sneaks toward a creeper makes it walk back behind cover farther away and stay hidden.
- **On open ground** with nothing at all to hide behind: it stays about 12 blocks off and stares. It edges back while the player looks at it, and backs away if the player comes closer than about 10 blocks.

### Startling

Being startled fills a meter that empties again over a few seconds. The game rule `bubbas_reworked_mobs:show_meters` shows the meter as a red bar above the creeper. Things that startle it:

- being attacked: one hit calms down in a few seconds; three quick hits set it off;
- anything moving faster than a walk within about 6 blocks: sprinting, riding, gliding, running or panicking mobs and animals, being knocked back toward it;
- projectiles flying close past it, even when they miss.

Falls, fire and cactus do not startle it. Walking calmly near a creeper is safe.

### Hissing

A full meter makes the creeper hiss and swell as normal.

- During the hiss it defuses if the player who startled it crouches or backs away beyond 7 blocks.
- This does not work if something hits it while it hisses.
- A defused creeper stays jumpy for a while.

Flint and steel lights a creeper as normal.

### Blindness

A creeper with Blindness senses nothing: it is not startled by movement or hits, and a hiss in progress defuses.

### Charged creepers

Charged creepers keep the normal behavior: they hunt players and explode next to them.

## Biome skins

Every creeper wears a skin from the biome it first appeared in. In biomes with trees it takes the colours of that tree's bark; in biomes without trees, the colours of the ground. The skin changes only the creeper's look, not its behavior.

| Skin | Biomes |
|---|---|
| Oak | Forest, flower forest, swamp |
| Birch | Birch forest, old growth birch forest |
| Spruce | All taigas, grove, windswept forest |
| Jungle | All jungles |
| Acacia | All savannas |
| Dark oak | Dark forest |
| Mangrove | Mangrove swamp |
| Cherry | Cherry grove |
| Pale oak | Pale garden |
| Poplar | Dappled forest |
| Plains grass | Plains, sunflower plains |
| Meadow grass | Meadow |
| Sand | Desert, beach |
| Red sand | All badlands |
| Snow | Snowy plains, ice spikes, snowy slopes, snowy beach, frozen peaks, jagged peaks, frozen river |
| Stone | Stony peaks, stony shore, windswept hills, windswept gravelly hills |
| Mycelium | Mushroom fields |
| Dripstone | Dripstone caves |
| Sulfur | Sulfur caves |

- Everywhere else (rivers, oceans, lush caves, the deep dark, the Nether, the End and biomes from other mods) creepers keep the normal look.
- A creeper keeps its skin for life, even if it walks into another biome. Creepers in worlds made before the mod was added get a skin from where they stand the first time they load.
- Snow creepers are almost plain white.
- Charged creepers keep the normal blue charge glow on top of their skin.
- The "Bubba's Mods 32x" resource pack has 32x versions of every skin.
- Data packs can add biomes to a skin through the biome tag `bubbas_reworked_mobs:creeper_skin/<skin>`, or make a new skin with a tag `<namespace>:creeper_skin/<name>` and a texture `<namespace>:textures/entity/creeper/biome/<name>.png`.

## Commands

*Main article: [[Reworked Mobs Commands]]*

The game rule `bubbas_reworked_mobs:watcher_creepers` turns watcher spawning off and brings normal creeper spawning back.
