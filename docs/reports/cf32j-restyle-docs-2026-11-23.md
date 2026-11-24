# CF32J restyle of every 32x texture (2026-11-23), docs report

For the orchestrator to merge into `specs/textures.md`, the project specs and the player docs vault.

## What changed (player-facing)

With a mod's built-in 32x pack on, every 32x texture now matches Classic Faithful 32x Jappa (the owner's pack): few colours, a crisp 1-px dark outline, flat shade bands, no blur or soft gradients.

- **Reworked Mining:** the 32x quartz sword, shovel, pickaxe, axe, hoe, the four armor icons and both worn armor layers are CF32J's own iron textures recoloured to quartz white (owner: "retexture items from Faithful where relevant"). Thin veins on the armor.
- **Reworked Mobs:** the 32x gravestone is weathered stone in four flat shades, blotched like CF32J's stone, with moss clumps on the plinth.
- **Totems & Artifacts:** 32x Grave Talisman (all glow levels), Relic Map Shreds (/4, /9, whole) and every totem statue (plinth and figures) are restyled. Statues have flat faces lit on the top and left edges with small grain patches, instead of the 16x speckle doubled up.
- **Extra Recipes:** the 32x glider icon has stepped shading instead of blended shading.
- **World Tweaks:** 32x lava bubbles are flat lava bands; the 32x geyser soot plumes are CF32J's geyser plume recoloured to soot.
- **Infused Gems (redesign, owner: "not very gemmy, flat discs, no iridescence"):** both gems redrawn at 16x and 32x. The finished gem is now a cut gemstone seen from the side, like vanilla's diamond: a flat table, five crown facets catching the light, a bright girdle line, and four darker pavilion facets narrowing to a point. The infused gem is a round brilliant seen from above: an octagonal table ringed by sixteen facets alternating light and dark. Both shimmer: translucent cool and warm sheens on a couple of facets, a short rainbow along the lit edge, and bright white glints. Each effect still colours its gem. The body texture is flat grey facets tinted by the effect; the glints and iridescence are the untinted second layer, so they show over every colour. The mod icon (a placeholder) was regenerated from the new gem. Test: the `colors` client test now also shoots the inventory with the 32x pack on (`gems-colors-finished-hotbar-infused-above-32x`).
- 16x art unchanged everywhere except the two Infused Gems.

## Spec facts (for `specs/textures.md`)

- New: `tools/pixelart.py` `render32(..., cf=True)` is the Classic Faithful 32x Jappa mode. Every pixel stays a palette colour. Bevels step one shade up (top/left edges) or down (bottom/right edges) along the material's own ramp (palette chars of that material sorted by luminance). Grain is two-pixel clumps stepped one shade (`grain_p` share each way). `thin={char: char}` keeps a dark inner-edge band one 32x pixel wide. Use `cf=True` for every new 32x twin. `snap=True` (the totem items) still exists.
- New rule (owner, 2026-11-23): **a texture that is a variant or recolour of a vanilla item, block or particle takes its 32x from CF32J's texture for the matching vanilla piece, recoloured to our palette** (CF32J's pixels, outline and shading kept). The generator reads the CF32J zip directly (default `%APPDATA%\ModrinthApp\profiles\porp\resourcepacks\CF32J-2026r21.zip`, or a path as the last argument) and fails on any CF32J shade it has no mapping for. Uses so far: quartz gear (iron), soot plumes (geyser_plume).
- **Credit:** a mod with CF32J-derived art has a `CREDITS.md` (now `mods/bubbas_reworked_mining/CREDITS.md`, `mods/bubbas_world_tweaks/CREDITS.md`) naming the pack, the release, each derived file and its source, with links to https://faithfulpack.net and https://faithfulpack.net/license.
- **Faithful License v4 (Jul 31, 2026), in the zip as `license.txt`:** allows use and modification in mods. Requires (1) clear credit in an obvious place (README/credits file/listing), (2) a visible link to https://faithfulpack.net, (3) naming the specific pack and content used, (4) using only what's needed, (5) shipping a copy of the license or a link to it with every distribution. Forbids paywalls, monetized download links, and taking donations for content mostly based on their work. **Before release:** the Modrinth listing and the player docs page of Reworked Mining and World Tweaks need the same credit and links. Derived textures are not human-made by us, so they don't count toward B14's human-made bar.
- Generators touched: `tools/pixelart.py`, `tools/gravestone-textures.py` (new `stone_texture32`), `mods/bubbas_reworked_mining/tools-local/make_quartz_textures.py` (32x = CF32J recolour; arg 2 = zip), `mods/bubbas_world_tweaks/tools-local/make_lava_bubble_textures.py` (32x bubbles quantised to `LEVELS32`, no grain; plumes from CF32J; arg 2 = zip), `mods/bubbas_totems_artifacts/tools-local/make_{talisman,relic,statue}_textures.py` (cf mode; statues' 32x drawn from `_statues(speckle=False)`), `mods/bubbas_extra_recipes/tools-local/make_placeholder_textures.py` (cf mode).
- The built-in packs' display names still say "32x (Faithful style)". `specs/textures.md` says they keep that until the packs are restyled, which is now done. Renaming them is lang/code work, left for whoever owns those files.

## Colour counts (32x, before -> after)

| Mod | Texture | Before | After |
|---|---|---|---|
| reworked_mining | quartz_sword / shovel / pickaxe / axe / hoe | 96 / 61 / 77 / 78 / 62 | 12 / 9 / 11 / 10 / 10 |
| reworked_mining | quartz_helmet / chestplate / leggings / boots | 73 / 100 / 87 / 89 | 7 / 7 / 7 / 8 |
| reworked_mining | humanoid / humanoid_leggings layers | 176 / 122 | 9 / 8 |
| reworked_mobs | gravestone | 86 | 12 |
| extra_recipes | glider icon (wings) | 31 (15) | 15 (15) |
| totems_artifacts | grave_talisman (+ glow 1-3) | 134-135 | 17 |
| totems_artifacts | relic_map_shred_4 / 9 / whole | 162 / 153 / 175 | 8 / 8 / 9 |
| totems_artifacts | statue plinth / warding / keeping / gathering / fortification / growth | 103 / 98 / 101 / 108 / 83 / 42 | 7 / 11 / 11 / 10 / 11 / 12 |
| totems_artifacts | statue seeking / beginning / end / herding / clear_skies / north_wind / undying | 106 / 124 / 106 / 26 / 11 / 89 / 105 | 11 / 13 / 15 / 15 / 10 / 14 / 8 |
| world_tweaks | lava_bubble_0..4 | 22-33 | 6 |
| world_tweaks | lava_geyser_plume_01..08 | 5-6 | 3 |
| infused_gems | gem / infused_gem body (grey, tinted) | 6 / 7 | 6 / 6 flat greys; the shine layers add white, two translucent sheens and a 6-step rainbow |

## Agent choices to surface to the owner

- Quartz 32x tools have no veins (CF32J's iron heads are 2-3 px wide; veins would break the outline). Armor icons get 2 veins and worn layers 10.
- CF32J's three near-identical darkest plume greys (193/195/198) all map to the darkest soot.
- The statues' 16x still has its per-pixel speckle. Only the 32x is flattened.
- The gem shapes changed. The finished gem was "a regular octagon seen from above, emerald-like, not an emerald copy (owner)". It is now a side-view cut stone, because the owner asked for a table, crown and pavilion. The iridescence relies on translucent pixels in the untinted layer, which render as translucent in game (checked in the 16x and 32x screenshots).
