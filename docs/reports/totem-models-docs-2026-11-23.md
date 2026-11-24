# Totem models: small-detail pass (2026-11-23)

Owner asked: "audit all totem models to remove overly-small details". Art only; no gameplay change.

## Player-facing (Totems & Artifacts page)

- The totem statues look chunkier and cleaner: no thin slivers or flickering bits. Each keeps its look (Warding's glowing diamond rune and soul flame, the End's purpur crown, Clear Skies' sun crown, North Wind's icicles and gust, Herding's horns).
- Totem of Herding and Totem of Gathering item icons cleaned up (solid horn curls; no stray speck).

## Spec facts (for `specs/` art or textures notes)

- Statue modelling rule used: no element under 1 px thick or wide; glow overlays (eyes, visor) sit 0.25 px in front of the face (0.05 px z-fought at range); emblems stand 1 px proud (Keeping's chest 2 px); topper faces sample a UV the same size as the face (texel = 1 px), not one 4x3 patch squeezed onto every face.
- Statue faces (head front) carry no random grain, so eyes, cheeks and mouths read.
- Generators: `mods/bubbas_totems_artifacts/tools-local/make_statue_textures.py` (statues), `tools/totems-textures.py` (items). Totem items are flat `item/generated` sprites; no 3D held totem models exist.

## Per-totem changes

| Totem | Change |
|---|---|
| Warding | Chest rune: 1.5 x 1.5 x 0.2 px plate with 0.2 px edges -> 2 x 2 x 1 px diamond. Eyes 0.25 px in front of the hood's shadow (was 0.1). |
| Keeping | Chest box 2 px deep (others now 1, so it still stands out). |
| Gathering | Statue: two single-pixel patina specks on the robe removed. Item: stray patina speck on the lower body removed. |
| Fortification | Visor 0.25 px proud (was 0.05). Helm brim side UV sized to its 1.5 px height. Emblem 1 px proud (was 0.4). |
| Growth | Crown sides lost the pink/yellow single-pixel specks (blossom on top unchanged). |
| Seeking | Gold band UV sized to the band (was a 3-row patch squeezed to 2 px). |
| The Beginning | Glowing eyes 0.25 px proud. Grass cap in two bands instead of a checker. |
| The End | Crown band 1 px (was 0.5). Spikes 1 x 2 x 1 on whole pixels with matching UVs (was squeezed checker). Eyes 0.25 px proud. |
| Herding | Statue horns: 1.5 px boxes that overlapped each other (shared faces) -> 2 px horn and curl on whole pixels, ridged texture instead of checker; wool with fewer specks. Item: horn curls filled solid (were 1 px rings with holes). |
| Clear Skies | Crown band 1 px (was 0.5). Rays on whole pixels, none overhanging the band: 2 px broad at front (tallest), back and sides, 1 px at the corners. Gold in bands, not checker. |
| North Wind | Crown band 1 px. Six icicles -> four, lit at the tip. Three 0.5 x 0.5 px gust streaks -> two 1 x 1 px bars. Cheeks 2 px wide (were single pixels lost in grain). |

## Agent choices worth the owner's eye

- North Wind's icicle count cut from six to four, and gust streaks from three to two.
- Clear Skies' rays regrouped (broad side rays, narrow corners) to stay on whole pixels.
