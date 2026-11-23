<!-- dte:A3 -->
<!-- covers: mods/postershots/*, tools/posters/* -->
# Promo posters (spec)

Shareable images of the mods (social posts, friends), made from real in-game shots. Not for Modrinth or project pages: B14 keeps AI-made images off those.

## Poster camera (`mods/postershots`)

An internal client game test (never published, `internal=true`) that opens a copy of the natural test world (`testworlds/bubba-natural-world`, built by `tools/build-naturalworld.py`) with all seven mods loaded, and then waits for scene scripts instead of running a fixed tour.

- **Start:** `tools/posters/camera.sh` (runs `./gradlew :postershots:runClientGameTest -PclientTests=posters` under Xvfb). It never runs in milestone or CI runs: it skips itself unless `posters` is named.
- **Send scenes:** `python3 tools/posters/shoot.py scene.txt [...]` copies each script into `mods/postershots/build/run/posters/queue/`, waits for it and prints its log. `--quit` saves the world and stops the camera. Shots land in `mods/postershots/build/run/posters/shots/<name>.png` (1920x1080). Dropping a file named `abort` in the queue stops the running script.
- **World:** the copy in the run's `saves/poster-world` is kept between camera restarts (scenes built in one session stay); `-Dbubba.posters.fresh=true` or deleting the folder starts from the pristine world again.
- **Speed:** Xvfb has no sRGB-capable GLX visual, so the game normally falls back to its Vulkan backend on llvmpipe, which spent ~10 minutes per shot here (most of it destroying buffer views). `GlBackendMixin` (test source set only) stops the camera asking for an sRGB framebuffer, so it gets the OpenGL backend: a shot takes a few seconds. Colours are unchanged. The waiting window is 320x180 and frames are uncapped, because the server ticks in step with client frames during a client game test.
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

## Drawing (`tools/posters/`)

- `design.py`: the Minecraft font straight from the game jar's `font/ascii.png`; logo titles where each font pixel is a bevelled block of a vanilla block texture, extruded and outlined; splash text; vanilla tooltips, buttons, hotbar and slots; `tooltip_crop` cuts a real tooltip out of a chest screenshot (finds its purple border lines) and redraws a clean frame; framed photos, fades and block-textured backgrounds.
- `icons.py`: cuts exact 16x16 item icons (gems with their real tints, totems, quartz gear...) out of the camera's chest screenshots into `build/posters/icons/`.
- `layouts.py`: `hero` (full-bleed shot fading into a textured background, logo on the seam, framed insets, hotbar) and `mosaic`.
- `make.py`: the posters themselves, `python3 tools/posters/make.py [name...]` writes `build/posters/<name>.png` (1080x1350; the story overview is 1080x1920). Shots are named by the scene scripts that made them.
- `sheet.py`: a contact sheet of shots for picking: `python3 tools/posters/sheet.py out.png "glob[,glob]" [cols]`.

Everything is built from vanilla textures, the mods' own textures and in-game screenshots; no 32x textures.
