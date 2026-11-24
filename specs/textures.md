<!-- dte:B16 -->
<!-- covers: mods/*/src/main/resources/assets/*/textures/*, mods/*/src/main/resources/resourcepacks/*, resourcepacks/* -->
# Textures: 16x and 32x

We make our own textures for everything we add, in both resolutions. Classic Faithful 32x Jappa (the owner's pack, owner 2026-11-23; it replaced Faithful 32x as the reference) is the style reference for the 32x set, not a dependency: whether that pack covers our content is never a design consideration. Every 32x texture was restyled to it on 2026-11-23 (palette-limited `cf=True` rendering in `tools/pixelart.py`; vanilla variants such as quartz gear are CF32J's own sprites recoloured, credited in the mod's `CREDITS.md`).

## The two sets

- Every custom texture exists twice: a **16x** version in vanilla style and a **32x** version in the style of Classic Faithful 32x Jappa.
- Each 32x texture is exactly twice the width and height of its 16x twin, at the same path.
- **Animations and metadata:** a `.png.mcmeta` next to a 16x texture has a matching one next to its 32x twin; frame counts and timing match.

## Where they live

- **Mods:**
  - The 16x set is the mod's normal assets: `src/main/resources/assets/<modid>/textures/...`.
  - The 32x set is a built-in resource pack inside the same jar, at `src/main/resources/resourcepacks/<modid>_32x/`, with its own `pack.mcmeta` and the same `assets/<modid>/textures/...` paths.
  - The mod registers it on the client with `ResourceLoader.registerBuiltinPack(<modid>:<modid>_32x, container, <display name>, PackActivationType.NORMAL)`. The display name ends in "32x (Classic Faithful style)" (e.g. "Bubba's Infused Gems 32x (Classic Faithful style)"). Players switch it on in the Resource Packs screen, above their 32x pack (Classic Faithful 32x Jappa or Faithful 32x) if they use one.
- **Resource pack projects:** `resourcepacks/<name>/` is the 16x pack and `resourcepacks/<name>-32x/` the 32x pack, released together.

## Checking

`python3 tools/texcheck.py` fails when:
- a 16x texture has no 32x twin;
- a twin is not exactly double size;
- the animation metadata doesn't match.

Projects with `internal=true` in `gradle.properties` (refmod) are skipped; their textures are placeholders.

## Credit

A project with Classic Faithful- or Faithful-derived textures has a `CREDITS.md` naming what was used and linking the pack's site (Faithful: https://faithfulpack.net); check that pack's license terms before release. Its player docs page and Modrinth listing carry the same credit.

## Who makes them

- **During development** agents may make the art they need (scripted or AI-made), and give every texture their best shot: vanilla-fitting, shaded, checked in screenshots at 16x and 32x (owner, CLAUDE.md). Every agent-made asset gets a row in `docs/art-todo.md`, so the owner can redo or tweak it.
- **For a Modrinth release** (B14, B16): no texture may be AI-generated where Modrinth's rules forbid it. Person-made textures are the most natural part to carry a project over Modrinth's human-made bar.

## Classic Faithful 32x Jappa restyle (2026-11-23)

- New: `tools/pixelart.py` `render32(..., cf=True)` is the Classic Faithful 32x Jappa mode. Every pixel stays a palette colour. Bevels step one shade up (top/left edges) or down (bottom/right edges) along the material's own ramp (palette chars of that material sorted by luminance). Grain is two-pixel clumps stepped one shade (`grain_p` share each way). `thin={char: char}` keeps a dark inner-edge band one 32x pixel wide. Use `cf=True` for every new 32x twin. `snap=True` (the totem items) still exists.
- New rule (owner, 2026-11-23): **a texture that is a variant or recolour of a vanilla item, block or particle takes its 32x from CF32J's texture for the matching vanilla piece, recoloured to our palette** (CF32J's pixels, outline and shading kept). The generator reads the CF32J zip directly (default `%APPDATA%\ModrinthApp\profiles\porp\resourcepacks\CF32J-2026r21.zip`, or a path as the last argument) and fails on any CF32J shade it has no mapping for. Uses so far: quartz gear (iron), soot plumes (geyser_plume).
- **Credit:** a mod with CF32J-derived art has a `CREDITS.md` (now `mods/bubbas_reworked_mining/CREDITS.md`, `mods/bubbas_world_tweaks/CREDITS.md`) naming the pack, the release, each derived file and its source, with links to https://faithfulpack.net and https://faithfulpack.net/license.
- **Faithful License v4 (Jul 31, 2026), in the zip as `license.txt`:** allows use and modification in mods. Requires (1) clear credit in an obvious place (README/credits file/listing), (2) a visible link to https://faithfulpack.net, (3) naming the specific pack and content used, (4) using only what's needed, (5) shipping a copy of the license or a link to it with every distribution. Forbids paywalls, monetized download links, and taking donations for content mostly based on their work. **Before release:** the Modrinth listing and the player docs page of Reworked Mining and World Tweaks need the same credit and links. Derived textures are not human-made by us, so they don't count toward B14's human-made bar.
- Generators touched: `tools/pixelart.py`, `tools/gravestone-textures.py` (new `stone_texture32`), `mods/bubbas_reworked_mining/tools-local/make_quartz_textures.py` (32x = CF32J recolour; arg 2 = zip), `mods/bubbas_world_tweaks/tools-local/make_lava_bubble_textures.py` (32x bubbles quantised to `LEVELS32`, no grain; plumes from CF32J; arg 2 = zip), `mods/bubbas_totems_artifacts/tools-local/make_{talisman,relic,statue}_textures.py` (cf mode; statues' 32x drawn from `_statues(speckle=False)`), `mods/bubbas_extra_recipes/tools-local/make_placeholder_textures.py` (cf mode).
- The built-in packs' display names still say "32x (Faithful style)". `specs/textures.md` says they keep that until the packs are restyled, which is now done. Renaming them is lang/code work, left for whoever owns those files.

### Colour counts (32x, before -> after)

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

### Builder's choices (owner to review)

- Quartz 32x tools have no veins (CF32J's iron heads are 2-3 px wide; veins would break the outline). Armor icons get 2 veins and worn layers 10.
- CF32J's three near-identical darkest plume greys (193/195/198) all map to the darkest soot.
- The statues' 16x still has its per-pixel speckle. Only the 32x is flattened.
- The gem shapes changed. The finished gem was "a regular octagon seen from above, emerald-like, not an emerald copy (owner)". It is now a side-view cut stone, because the owner asked for a table, crown and pavilion. The iridescence relies on translucent pixels in the untinted layer, which render as translucent in game (checked in the 16x and 32x screenshots).
