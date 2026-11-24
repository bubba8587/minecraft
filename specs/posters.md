<!-- dte:A3 -->
<!-- covers: mods/postershots/*, tools/posters/* -->
# Promo posters (spec)

Shareable images of the mods (social posts, friends), made from real in-game shots. Not for Modrinth or project pages: B14 keeps AI-made images off those.

What the posters may and may not say: `specs/copy-strings.md`.

## Poster camera (`mods/postershots`)

An internal client game test (never published, `internal=true`). It opens a copy of the natural test world (`testworlds/bubba-natural-world`, built by `tools/build-naturalworld.py`) with all seven mods loaded, then waits for scene scripts instead of running a fixed tour.

- **Start:** `tools/posters/camera.sh` (runs `./gradlew :postershots:runClientGameTest -PclientTests=posters` under Xvfb). It never runs in milestone or CI runs: it skips itself unless `posters` is named.
- **Send scenes:** `python3 tools/posters/shoot.py scene.txt [...]` copies each script into `mods/postershots/build/run/posters/queue/`, waits for it and prints its log.
  - `--quit` saves the world and stops the camera.
  - Shots land in `mods/postershots/build/run/posters/shots/<name>.png` (1920x1080).
  - A file named `abort` dropped in the queue stops the running script.
- **World:** the copy in the run's `saves/poster-world` is kept between camera restarts, so scenes built in one session stay. `-Dbubba.posters.fresh=true`, or deleting the folder, starts again from the pristine world.
- **Speed:**
  - Xvfb has no sRGB-capable GLX visual, so the game would fall back to its Vulkan backend on llvmpipe, at about 10 minutes per shot (mostly spent destroying buffer views). `GlBackendMixin` (test source set only) stops the camera asking for an sRGB framebuffer, so it gets the OpenGL backend instead: a shot takes a few seconds, with unchanged colours.
  - The waiting window is 320x180 and frames are uncapped, because the server ticks in step with client frames during a client game test.
- **Render settings:** Fabulous preset, render distance 8 (scripts raise it with `render`), HUD hidden, spectator, time and weather frozen, no mob spawning.

### Scene script steps

One step per line; `#` starts a comment. `$NAME` is replaced by a variable first, and numbers may be sums (`$Px+3`, `118-1`).

| Step | What it does |
|---|---|
| `/command` | runs a server command in the current dimension |
| `run x y z command` | runs a command positioned there (`y` may be `g`, `g+N`: the surface there) |
| `shot name ex ey ez tx ty tz` | camera eye at e, looking at t; waits for chunks; screenshot |
| `orbit name tx ty tz dist up yaw,yaw...` | shots round a target (yaw 0 = camera south of it) |
| `pose ...` / `snap name` | like `shot`, split in two |
| `burst name count every` | quick shots without waiting for chunks (particles, geysers) |
| `place x y z yaw pitch` | puts the player's feet there (loads the area before building) |
| `chest name x y z [slot...]` | opens a container, screenshots it, then once per hovered slot (tooltips) |
| `let N x z [+off]`, `set N v`, `entity N tag` | variables: surface height, a number, a tagged entity's position (`Nx Ny Nz`) |
| `yard N x z seed w d state wealth` | builds a crypt yard (`Ngx..` gate, `Nvx..` vault) |
| `watch creeperTag targetTag` | that creeper watches that entity (Reworked Mobs) |
| `erupt x y z` | a lava geyser erupts there now (World Tweaks) |
| `findlava N x z r` | the most open lava surface near there (Nether) |
| `dim`, `render`, `fov`, `gui`, `window`, `view`, `hud`, `fly`, `wait`, `settle`, `chunkwait` | settings and waits |

Commands only work where chunks are loaded, so a scene starts with `place` near its spot and a `wait`.

Scene scripts used for the current posters are kept in `tools/posters/scenes/` (they ran against `saves/poster-world`, so coordinates are that world's). Lessons:
- **Mobs face the camera (owner):** summon posed mobs with `NoAI:1b` and a `Rotation` yaw pointing at the camera (yaw = atan2(-(camX-x), camZ-z) in degrees); `/tp ... facing` turns the head but not the body of a NoAI mob. Give held items with `/item replace entity ... weapon.mainhand` (summon `equipment` doesn't stick on woken undead).
- **Find real features, don't build fakes (owner):** a natural spider nest is found from its hanging `bubbas_reworked_mobs:nest_spider` (`execute at @p run tag @e[type=...,sort=nearest,limit=1] add x`, then `entity N x`). Never `execute as @e[...] run tp @s ~ ~ ~` without `at @s`: it moves every match to the command's origin.
- **Light dark places with invisible light blocks** (`fill ... minecraft:light[level=11] replace minecraft:air`); night vision alone is too dim in caves. Light blocks aren't air, so add them after anything that measures open space (the spider nest command).
- Sleepers are blocks with gear in `Items` (slot 0 main hand, 1 off hand, 2-5 head to feet); beacon items go in `bubbas_reworked_beacons:item`; `upside_down=true` hangs a beacon.
- `grab drownedTag victimTag` makes a drowned grab now (the AI won't while the camera stares at it); `attack`/`release` hold the attack key, for mid-break shots in survival (switch back to spectator after). In survival, keep away from spider nests: a nest cocoons the camera.
- Large camps only sometimes roll a skull rock (1 in 3, by seed): build a few seeds and look from above. Upright gravestones need a kept yard. Clear stray natural trees out of a hero subject (`fill ... air replace <log/leaves>`).
- The camera's `poster-world` is kept between camera restarts (`gradle/fabric-mod.gradle` skips it when clearing client saves).
- Photos are cropped tight to the subject in `make.py` (`photo_kw={"crop": (...)}` in 1920x1080 pixels).
- **Screens as pictures:** `chest name x y z` opens any block with a menu (brewing stand, beacon, blast furnace) and shoots it; the player must be within reach or the screen closes at once. Fill the block first with `/data merge block` (brewing stand: `BrewTime` counts down from `total_brew_time` 400; a finished gem in a beacon: `{id:"bubbas_infused_gems:gem",components:{"bubbas_infused_gems:gem":{effect,infusion,tier,main,other}}}`). Screens go on posters with `image=` (fitted whole), not `photo=` (which crops to 16:9).
- **Peeking creepers:** a NoAI creeper is placed by hand so the wall edge splits its face; work out the camera ray past the corner first (`creeper-wall.txt`).
- **Kill dropped items** after clearing old posed mobs (`/kill @e[type=minecraft:item,...]`), or their loot floats in the next shot. `/locate structure #minecraft:village` and a top-down `shot` help find a real building fast; `execute if block` probes give exact wall lines.
- The Nether lava sea's fog is strong red: keep the camera within about 8 blocks of a geyser and low over the lava so the smoke and bubbles read (`geyser-wide.txt`).

## Drawing (`tools/posters/`)

- `design.py`:
  - the Minecraft font, straight from the game jar's `font/ascii.png`;
  - logo titles where each font pixel is a bevelled block of a vanilla block texture, extruded and outlined;
  - vanilla tooltips, buttons, hotbar and slots (splash text exists but posters never use it: `specs/copy-strings.md`);
  - `tooltip_crop`: cuts a real tooltip out of a chest screenshot (it finds the purple border lines) and redraws a clean frame;
  - framed photos, fades and block-textured backgrounds.
- `icons.py`: cuts exact 16x16 item icons (gems with their real tints, totems, quartz gear...) out of the camera's chest screenshots into `build/posters/icons/`.
- `copytext.py`: every poster line comes from `specs/copy-strings.md` ("Allowed" bullets), looked up by a short key unique within the mod's section (`line(mod, key)`), so the wording is always the owner's. A `[Requires ...]` note is split off and drawn under the string in aqua; a `[Coming in 26.4]:` tag is drawn in gold above it. No other text goes on a poster (no blurbs, subtitles, headers or footers).
- `make.py`: the posters themselves, `python3 tools/posters/make.py [name...]` writes `build/posters/<name>.png` (1080x1350; the story overview is 1080x1920). Shots are named by the scene scripts that made them.
  - The **card** layout (`cards`): hero shot with the block-letter title on its seam, then one dark card per string, with a framed photo, a fitted image (a cut-out tooltip) or item slots on the left. `layout_cards` gives each card the height its text needs at Minecraft-font scale 3 (side by side, or text under the icons when that's shorter) and shares out the rest; only if the cards can't fit does it drop the costliest ones to scale 2, and it prints a warning. When that happens, split the poster rather than accept small text.
  - Posters: title, three Reworked Mobs (graveyards, world, creatures), two Totems & Artifacts, two Infused Gems, Reworked Beacons, Reworked Mining, World Tweaks, Extra Recipes, and the story overview.
- `layouts.py`: older `hero` and `mosaic` layouts (unused now).
- `sheet.py`: a contact sheet of shots for picking: `python3 tools/posters/sheet.py out.png "glob[,glob]" [cols]`.

Everything is built from vanilla textures, the mods' own textures and in-game screenshots; no 32x textures.
