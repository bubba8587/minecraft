# Gravestone edit screen: docs report (2026-11-23, BACKLOG 22)

## Player-facing change
- Editing a gravestone now shows the gravestone itself (its front face, in its own stone texture, at 16x or 32x) with the text in the same place, size and line spacing as on the placed stone. Before, it showed a sign-shaped board whose lines didn't match.

## Spec facts (specs/graveyards.md)
- Replace the "Edit screen" line (vanilla `SignEditScreen` reading `minecraft:textures/gui/signs/bubbas_gravestone.png`): gravestones open `GravestoneEditScreen` (client, subclass of vanilla `AbstractSignEditScreen`, like `HangingSignEditScreen`), via the client mixin `GravestoneEditScreenMixin` on `LocalPlayer.openTextEdit` (gravestone block entities only; every other sign keeps vanilla's screen).
- The screen draws the stone's front silhouette (plinth, body, shoulder, crown) from the block texture `bubbas_reworked_mobs:textures/block/gravestone.png` (u = x, v = 16 - y, as the model's auto UVs), 9 GUI px per block px, tinted 0.8 like a lit north/south face, text centre at GUI y 115.
- One set of layout constants, `grave/GraveTextLayout` (main): 6 font px per block px (1/96 block), text centre y 7.5, face z 10, line width 60, line height 10, face rectangles. Used by `GravestoneRenderer`, `GravestoneBlockEntity` and the screen.
- The GUI background texture `bubbas_gravestone.png` (16x and 32x) is deleted; `tools/gravestone-textures.py` no longer writes it; its `docs/art-todo.md` row is removed.
- Objective "the gravestone's edit screen opens on placement (vanilla path, not exercised by a test)" is now covered by the `graves` client part (opens the screen via `openTextEdit`; shots graves-edit and graves-edit-stone, plus -32x).

## Tests
- `./gradlew :bubbas_reworked_mobs:build`: green (13 core server tests).
- `-PclientTests=graves`: passes. New shots (in `mods/bubbas_reworked_mobs/build/run/clientGameTest/screenshots/`): `0004_graves-edit-stone.png`, `0005_graves-edit.png`, `0011_graves-edit-stone-32x.png`, `0012_graves-edit-32x.png`. Checked side by side: the three lines sit in the same places on the stone in both.

## Open
- The screen title is still vanilla's "Edit Sign Message".
