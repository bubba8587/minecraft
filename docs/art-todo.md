<!-- dte:B16,B14 -->
# Art for the owner to make or tweak

Owner (2026-11-16): agents may make any art for now (scripted or AI-made), but every agent-made asset is listed here so the owner can redo or tweak it later. Project page images and mod icons must never be AI-made (B14): those stay on this list until a person makes them.

Agents: add a row whenever you add or change art; remove a row only when the owner says it's done.

| Project | Asset | Files | Made by | Notes |
|---|---|---|---|---|
| Infused Gems | Gem (finished gem, octagon) | `mods/bubbas_infused_gems/.../textures/item/gem*.png` (16x + 32x) | `tools/gem-textures.py` | tinted by effect color; owner tuned shape/shine in playtests |
| Infused Gems | Infused Gem (round) | `.../textures/item/infused_gem*.png` (16x + 32x) | `tools/gem-textures.py` | |
| Infused Gems | Mod icon | `mods/bubbas_infused_gems/src/main/resources/assets/bubbas_infused_gems/icon.png` | script | must be person-made before a Modrinth release (B14) |
| Totems & Artifacts | Totem of Warding item | `mods/bubbas_totems_artifacts/src/main/resources/{assets,resourcepacks/bubbas_totems_artifacts_32x/assets}/bubbas_totems_artifacts/textures/item/totem_of_warding.png` (16x + 32x) | `tools/warding-textures.py` (hand-placed 16x grid; 32x twin via `tools/pixelart.py`) | art pass 2026-11-16: hooded soul-stone idol in the Totem of Undying's pose, soul flame on the hood, soul-fire eyes, glowing ward rune on the chest; the statue carves the same figure |
| Totems & Artifacts | Grave Talisman item (dark + 3 glow levels, held-pose models) | `.../bubbas_totems_artifacts/textures/item/grave_talisman*.png` (16x + 32x), `models/item/grave_talisman*.json` | `mods/bubbas_totems_artifacts/tools-local/make_talisman_textures.py` (hand-placed 16x grid; 32x via `tools/pixelart.py`) | art pass 2026-11-16: arched gravestone pendant on a leather cord, moss at its foot, a faceted gem that lights up in 3 steps; the gem must stay in pixels 5..10 x 8..13 (the glow cuboid) |
| Reworked Mobs | Sleeper / loot display models and textures | `mods/bubbas_reworked_mobs/...` | agent | real 16x/32x art listed in BACKLOG |
| Crafting Tweaks | Glider item and worn texture | `mods/bubbas_crafting_tweaks/src/main/resources/{assets,resourcepacks/bubbas_crafting_tweaks_32x/assets}/bubbas_crafting_tweaks/textures/{item,entity/equipment/wings}/glider.png` (16x + 32x) | `mods/bubbas_crafting_tweaks/tools-local/make_placeholder_textures.py` | art pass 2026-11-16: the icon is a hang glider seen from the front (chevron wing, control frame, feather tassels); the wings are phantom-membrane sails on a bamboo spar and battens, diamond spar caps, feathered tips. BACKLOG: real Glider art |
| Crafting Tweaks | Mod icon | (none yet) | | must be person-made (B14) |
| Reworked Beacons | Mod icon | (none yet) | | must be person-made (B14); the beacon screen reuses vanilla art |
| Totems & Artifacts | Totem of Warding statue (block model and textures) | `.../bubbas_totems_artifacts/models/block/totem_statue_warding.json`, `textures/block/totem_statue_{plinth,warding}.png` (16x + 32x), `blockstates/totem_statue.json` | `mods/bubbas_totems_artifacts/tools-local/make_statue_textures.py` (writes the model too) | art pass 2026-11-16: the item's idol carved in 3D on a polished deepslate plinth: robed body, stub arms, open hood with glowing eyes inside, glowing chest rune, soul flame on crossed planes; 10x10 and 16 high (shape raised from 14 for the flame); owner will design the statues |
| Totems & Artifacts | Mod icon | (none yet) | | must be person-made (B14) |
