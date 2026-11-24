# Trader Posts

*Part of [[Bubba's Reworked Mobs]].*

> [!infobox] Trader post
> | | |
> |---|---|
> | **Type** | Structure |
> | **Location** | Plains, sunflower plains, meadows, savannas, savanna plateaus, and the edges of deserts |
> | **Contains** | A striped wool stall, one or two llama pens, a hitching rail, a signpost, a lit campfire, often a cart; two barrels of supplies |
> | **Visitors** | The wandering trader and its two llamas |
> | **Locatable** | Yes, with `/locate structure bubbas_reworked_mobs:trader_post` |

A **trader post** is a small market square where the wandering trader sets up when it visits. Between visits the post stands empty and intact.

## Generation

- Trader posts appear now and then on open plains, sunflower plains, meadows and savannas, and on the edges of deserts (never deep inside one).
- They stand on level, dry ground, away from villages, monster camps and crypt yards.
- They are rare: in a typical world they are a kilometre or two apart, closer together where open grassland is wide.

## Structure

The post is a levelled square of coarse dirt (sand in a desert) with a dirt path out the front.

- **Stall:** a striped wool stall in blue, red and white (one of three colour schemes) with a gabled roof and an overhang over a counter of logs and barrels. The counter is dressed with a flower pot, cloth, candles and a lantern; inside are a rug and a hanging lantern. The trader's blue-and-red banners, with a gold diamond and a zigzag hem, hang on the stall's posts, its back wall and either side of the entrance.
- **Llama pens:** one or two pens of spruce fence with a gate and lanterns, hay bales and a water trough. A post with one pen has a pile of hay and barrels on the other side.
- **Hitching rail** with a water trough and hay, and a **signpost** reading "Trader Post / All welcome" with a lantern.
- A lit **campfire** with two log benches, and often an **open cart** on dark wheels loaded with cargo.

Two barrels hold a few supplies. Nothing in them undercuts trading.

- The stall's barrel: wheat, dried kelp, bread, string, dyes, paper, and now and then a lead or one emerald.
- The cart's barrel: wheat, kelp, carrots, potatoes, and maybe a hay bale or a lead.

The post is safe ground: no monsters spawn naturally inside it, it is lit by its lanterns, candles and fire, and [[Endermen]] leave it alone.

## The wandering trader

The wandering trader comes to the post. Its timing and odds are unchanged from vanilla: a check every 20 minutes, with a growing chance each time.

- When it would appear near a player and a trader post is within 96 blocks of that player, it arrives **behind the post's counter** instead.
- Its two llamas arrive **tied up in the pens**: one in each, or both in the only pen.
- A village bell within 48 blocks of the player still takes priority, as in vanilla.

The trader stays around the post, within about 12 blocks of it, for its usual two days, then leaves with its llamas. A llama a player leads away or ties up elsewhere stays behind, as trader llamas do in vanilla.

## Commands

Operators can build and find posts (see [[Reworked Mobs Commands]]):

- `/bubbas_reworked_mobs trader_post build [seed]` builds a post here.
- `/bubbas_reworked_mobs trader_post locate` names the nearest post within 3000 blocks.
- `/bubbas_reworked_mobs trader_post arrive` brings a wandering trader to the nearest post within 96 blocks now.

## History

| Version | Changes |
|---|---|
| 0.1.0 | Added. |

## See also

- [[Monster Camps]]
- [[Bubba Test World]]
