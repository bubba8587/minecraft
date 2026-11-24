# Totem art pass and the Totem of Undying statue: docs report (2026-11-23)

Builder report for the docs merge (Bubba's Totems & Artifacts, C6). Owner asks: "more totem art variety. take a look at the
Totem of Undying and try adjusting your art to something more similar. precious materials", clarified as "something in
between what we have now and the Undying one"; and "give Totem of Undying a statue".

## Player-facing changes (for the player-docs page)

- Every totem has new item art in its own precious material, closer to the Totem of Undying's look: gem eyes, a belt with
  a gem, the Undying's narrow foot. Each keeps its old motif (crown, helm, chest emblem). Statues are recoloured to match.
- New: **a statue for the Totem of Undying.** Sneak and use a vanilla Totem of Undying on a block to set it down as a
  gold illager idol with emerald eyes. Breaking it gives the Totem of Undying back. **It does nothing**: it's
  decoration only, with no area effect. A Totem of Undying in your hand still saves you from death as usual, and using
  it without sneaking does nothing new.

## Material and motif per totem (for specs/totems-artifacts.md, art section)

| Totem | Material (was) | Gem eyes | Kept motif |
|---|---|---|---|
| Warding | silver, cool polished iron (soul stone) | soul-fire slits (now 2 px tall) | hood, soul flame, glowing ward rune |
| Keeping | crying obsidian (purple obsidian) | magenta | gold-bound chest held to the chest |
| Gathering | polished copper (copper with patina) | verdigris | upturned hands, oxidised-copper ring |
| Fortification | netherite (iron) | glowing visor | helm (now gold-banded), shield (now gold with a lapis field) |
| Growth | resin amber (oak) | emerald | leafy crown with blossom, sprout |
| Seeking | lapis (lapis, richer) | gold | gold-rimmed hood, compass dial with needle |
| the Beginning | emerald (weathered stone) | amethyst | grass cap, grass block on the chest |
| the End | amethyst (end stone) | enderman face, magenta eyes | crown (now pale end stone, was purpur), ender pearl |
| Herding | ruby-red redstone (spruce) | gold | woolly ram's head, horns (now ivory), gold bell |
| Clear Skies | quartz (calcite) | sky blue | gold sun crown, sky-and-sun emblem |
| North Wind | diamond (packed ice) | deep blue | icicle crown, gust, snowflake |

Shared changes to every icon (agent's choice, to land "in between"): 2x2 gem eyes in vanilla's pattern (highlight, gem,
gem, dark gem) where the face allows; a dark belt row with a 2 px gem; the lower body tapers like the Undying's
(8 wide belt, 6 wide skirt, 4 wide foot) instead of the old 10 wide pedestal. Heads, arms and emblems are unchanged in
shape. Statues: same shapes, recoloured from the item palettes, gem eyes, and a dark belt with a gem on the robe's
bottom row.

## Spec facts (for specs/totems-artifacts.md, statues)

- `TotemType.UNDYING` (`totem=undying`), item `minecraft:totem_of_undying`. Decorative: `StatueEffects.pulses` is
  false for it (no area effect, no ticking).
- Placement: `statue/UndyingStatue` registers a Fabric `UseBlockCallback`. It acts only when the player is sneaking
  (`isSecondaryUseActive`), isn't a spectator, may build, and holds a Totem of Undying in the hand used. It then runs
  the same `TotemStatueBlock.placeFrom` as our totems (faces the player, uses one totem in survival). Everything else
  passes through, so vanilla's death protection and a plain right-click are untouched.
- Loot: the statue loot table's undying entry drops `minecraft:totem_of_undying` (the generator now accepts a
  namespaced item in `ITEMS`).
- Relic vaults never pick the undying statue for their pedestal (`RelicVaultPiece.primeStatue` drops UNDYING from the
  list first, so existing vault seeds keep their old picks).
- Model: `write_undying` in `tools-local/make_statue_textures.py` makes an 8x6x8 head with a 2x1x3 nose box, a crossed-arms
  band (8x2x2, one pixel proud at the front and sides), a 6x5x4 robe, and the shared plinth. All on whole pixels, with
  textures sized to each face. It uses vanilla's own totem colours.
- Warding's item art still comes from `tools/warding-textures.py` (palette and eyes changed there).

## Milestone test objectives (add to the spec's list)

- `TotemStatueGameTest.undyingStatuePlacesFromVanillaTotemAndDropsIt`: a plain right-click with a Totem of Undying on a
  floor places nothing and keeps the totem. Sneak-using it through `ServerPlayerGameMode.useItemOn` places the undying
  statue facing the player and uses the totem up. The statue drops the vanilla Totem of Undying.
- Client part `totems` now also takes `totems-hotbar2` / `totems-hotbar2-32x`: Warding, Seeking and North Wind beside
  vanilla's Totem of Undying. Part `statues` fits 12 statues (floor widened, row shot pulled back a block).

## Not done / for the owner

- Whether the undying statue should do anything (none asked for; it is decorative).
- The materials are the agent's picks. Swapping any is a palette change in `tools/totems-textures.py`, and the statue
  follows automatically.

## 32x style: Classic Faithful 32x Jappa (owner, 2026-11-23)

The owner changed the 32x target from Faithful 32x to Classic Faithful 32x Jappa (CF32J). That is a B16 rule change, so
`specs/textures.md` needs updating; this builder only restyled the totems. I compared against CF32J-2026r21's
`totem_of_undying`, diamond, emerald, gold and iron ingot textures. CF32J items have a thin 1 px (at 32x) outline in
the material's dark shade, smooth rounded silhouettes, and flat, clean shade areas with a soft lit rim and no noise.
The totem 32x icons now follow that: Scale2x silhouette smoothing, outlines thinned to 1 px, a gentler top-left bevel
(0.10, was 0.12), and **no grain** (was a stone/metal grain). The statue figure textures keep their 2x layout, with the
grain cut to half (0.03). The plinth (vanilla polished deepslate) and every non-totem 32x texture are unchanged; that
restyle is a separate job.
