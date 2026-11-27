<!-- dte:C8 -->
# World Tweaks docs report, 2026-11-27: Amethyst Golem phase 1 (skeleton)

For the docs merge into `specs/world-tweaks.md` ("Amethyst Golem") and `player-docs/`.

## Player-facing (once phase 2 lands; the golem only wanders for now)

- New mob: **Amethyst Golem**. Build it by placing a carved pumpkin or a jack o'lantern on top of one amethyst block (by hand or with a dispenser, like the other golems). Both blocks are used up and the golem appears where the amethyst block was.
- Spawn egg: "Amethyst Golem Spawn Egg", in the creative Spawn Eggs tab right after the Copper Golem's egg.
- It drops nothing (no loot table yet, so never any amethyst).

## Spec facts (how it's built)

- Entity `bubbas_world_tweaks:amethyst_golem`, class `golem.AmethystGolem extends AbstractGolem` (vanilla's golem base: no despawn, no default sounds). `MobCategory.MISC` (never spawns naturally), size 0.49 x 0.98, eye height 0.8125, client tracking range 10: the copper golem's numbers.
- Attributes: the copper golem's (12 health, 0.2 speed, step height 1).
- Goals for now: float, water-avoiding random stroll, look at player (6 blocks), random look around. Vanilla goals, nothing hand-rolled.
- Build pattern: `mixin/CarvedPumpkinAmethystGolemMixin` hooks vanilla `CarvedPumpkinBlock`: `trySpawnGolem` (HEAD, before vanilla's snow, iron and copper checks; the patterns can't overlap since they need different blocks) and `canSpawnGolem` (so a dispenser places a pumpkin onto an amethyst block, as it does for the other golems). Pattern (`AmethystGolems.fullPattern`): `aisle("^", "#")`, `^` carved pumpkin or jack o'lantern (vanilla's own pumpkin predicate), `#` `minecraft:amethyst_block` only (budding amethyst does not count). Spawning reuses vanilla's private `spawnGolemInWorld` through an `@Invoker` (`mixin/CarvedPumpkinBlockInvoker`): clears both blocks with break particles, places the golem at the amethyst block's spot, fires the summoned-entity advancement trigger, updates neighbours.
- Client: `client/golem/AmethystGolemRenderer` (MobRenderer, shadow 0.4), model layer `bubbas_world_tweaks:amethyst_golem#main` registered with Fabric's `ModelLayerRegistry`, texture `assets/bubbas_world_tweaks/textures/entity/amethyst_golem/amethyst_golem.png` (64x64).
- Model contract (art agent owns the geometry): `client/golem/AmethystGolemModel` with `createBodyLayer()`. Parts: root > `body` > (`head` > `crystals`, `left_arm`, `right_arm`); root > `left_leg`, `right_leg`. `setupAnim` only resets the pose and calls `AmethystGolemAnimations.apply(model, state)`.
- Render state `client/golem/AmethystGolemRenderState` (extends `LivingEntityRenderState`) with `dancing`, `singing`, `recording`, filled from `AmethystGolem.isDancing/isSinging/isRecording` (all false until phase 2).
- Spawn egg: `SpawnEggItem` with `Item.Properties().spawnEgg(type)`; item model is a flat `item/generated` with `textures/item/amethyst_golem_spawn_egg.png` (26.3 spawn eggs are single-texture items, no tint layers).
- Placeholder art from `mods/bubbas_world_tweaks/tools-local/make_amethyst_golem_placeholder.py` (row in `docs/art-todo.md`).

## Tests

- Core (`BubbasWorldTweaksCoreGameTest.pumpkinOnAmethystBuildsAGolem`): a carved pumpkin on one amethyst block makes exactly one amethyst golem at the block and leaves both spots air.
- Client part `golem` (`AmethystGolemClientGameTest`; `-PclientTests=golem`): builds the golem from blocks (asserts one was built), freezes it (NoAI) facing the camera on a platform in the sky, then shoots `golem-front`, `golem-side` (3/4), `golem-close`, `golem-back` at 16x and with the 32x pack (`-32x` suffix), then `golem-egg` / `golem-egg-32x` (egg in hand and hotbar). About 2 minutes.
- Milestone test objectives to add to the spec: a jack o'lantern also builds it; a dispenser facing the top of an amethyst block places a carved pumpkin and builds it; budding amethyst does not.

## Builder's choices

- Spawn spot is the amethyst block's position (like the snow golem's bottom block), since both blocks are cleared.
- Crystals are a child of the head so they turn with it.
- No loot table yet (drops nothing); the spec's "never more amethyst than it was built from" holds trivially.
