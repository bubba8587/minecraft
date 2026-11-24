<!-- dte:A2,A4 -->
<!-- covers: mods/refmod/*, datapacks/refpack/* -->
# Reference projects

`mods/refmod` and `datapacks/refpack` exist to prove the toolchain and the knowledge base against the real game. They are never published.

- **refmod** uses each major system the knowledge base describes, the way the notes say to. It has server game tests for each and a client game test with a screenshot.
  - Test-lite: `RefModCoreGameTest` (3 tests: the tally stick item, the counter block saving and syncing, datagen recipe/loot/tag loading) runs on every build; the other tests are milestone (`-PmilestoneTests`).
  - When it stops compiling or a test fails after a version bump, the notes for that system are stale.
- **refpack** covers pack.mcmeta, function tags, plain and macro functions, command storage, recipes, predicates and advancements, with PackTest test functions for each, and must load clean on the vanilla server (`tools/packload.py`).
  - It overrides `minecraft:blocks/barrel` to prove item-keyed drops on a block entity (`docs/knowledge/item-keyed-block-drops.md`, approach A): a barrel placed from an item named "Marked Barrel" drops a diamond, one with `custom_data {refpack_marker:1b}` drops an emerald, and any other barrel drops itself as in vanilla.
  - packcheck's `ItemKeyedDropsGameTest` tests it through the real place-and-break path.

## refmod requirements

Mod id `refmod`, package `io.github.bubba8587.refmod`. One small feature per system, each named after the system so a reader can find it. Content IDs are under the `refmod` namespace. Display names come from `assets/refmod/lang/en_us.json`.

1. **Registration:** registry helper classes (`ModItems`, `ModBlocks`, `ModBlockEntities`, `ModEntities`, `ModComponents`, ...) registering with `Identifier`-based keys and `.setId(key)` on properties.
2. **Item + data component:** a `refmod:tally_stick` item whose custom data component (Codec + StreamCodec) counts uses. Using it increments the count; the tooltip shows it.
3. **Tool via properties:** a `refmod:flint_hatchet` axe made the 26.3 way (`Item.Properties#axe(ToolMaterial.STONE, ...)`), so it strips logs through block transformers.
4. **Item component on an existing item:** vanilla `minecraft:stick` is a furnace fuel worth 50 ticks, set through `DefaultItemComponentEvents.MODIFY` (`DataComponents.COOKING_FUEL`).
5. **Block + block state:** a `refmod:signal_lamp` block with a boolean `lit` state toggled by right-click, with blockstate, model and item model definition files.
6. **Block entity:** a `refmod:counter_block` whose block entity counts ticks, saves and loads through `ValueOutput`/`ValueInput`, and syncs to the client.
7. **Creative tab:** a `refmod` tab listing every refmod item.
8. **Entity:** a `refmod:pebble` thrown projectile, thrown by a `refmod:pebble` item, that deals 1 damage, with a client renderer (a thrown-item renderer).
9. **Networking:** an S2C payload sent when the signal lamp toggles, which the client logs and shows in the action bar or a HUD element.
10. **Data attachment:** a persistent per-player attachment `refmod:lamp_toggles` counting how many lamps the player has toggled; it survives death.
11. **Command:** `/refmod stats [player]` prints that player's attachment value; the optional argument is permission-gated.
12. **Event:** a server event that logs and counts block breaks of `refmod:signal_lamp`.
13. **Mixin:** one small MixinExtras mixin with a visible effect a game test can check: snowballs deal at least 2 damage to anything, not just blazes (`SnowballMixin`).
14. **Recipes, loot, tags, lang, models via datagen:** the Fabric data generation API (`fabric-datagen` entrypoint, `runDatagen`) generates recipes for the new items, block loot tables, an item tag and English lang entries. Generated files are committed.
15. **Client:** a key mapping (default K) that prints the client player's lamp toggle count from a synced copy, and a HUD element drawing a small text line while the tally stick is held.
16. **Game tests:** a server game test per behavior above that can run on a server (2, 3, 4, 5, 6, 8, 10, 11, 12, 13, and 14's loot and recipe outputs), plus a client game test that places a signal lamp, holds the tally stick and takes a screenshot, which the builder looks at.
17. **No-new-blocks patterns (B15 fewNewBlocks):**
    - (a) Proof that the display and interaction entity APIs work. This is not a pattern for blocks: the owner rejected display-entity fake blocks. A `refmod:lantern_charm` item, used on any block, spawns an `item_display` entity showing a custom item model on top of that block, plus an `interaction` entity as its hitbox. Right-clicking the hitbox removes both and drops the item.
    - (b) Extra data on an existing vanilla block without a new block or block entity: right-clicking a vanilla `minecraft:barrel` with the tally stick stores a per-position counter in a chunk-level data attachment, which survives a chunk save and load.
    - Game tests for both.
18. **Item-keyed drops on a block without a block entity, still no new block (B15 fewNewBlocks):**
    - A mixin adds a boolean `synthetic` property to vanilla `minecraft:deepslate_diamond_ore`. It defaults to `false`, so worldgen, commands and old saves give natural ore.
    - Everything else is data in refmod's resources:
      - a blasting recipe `refmod:synthetic_diamond_ore`: coal block in, the ore out with `item_name` "Synthetic Diamond Ore" and `block_state {synthetic:"true"}`;
      - an override of `minecraft:blocks/deepslate_diamond_ore` that sends `synthetic=true` to `refmod:blocks/synthetic_diamond_ore` (Silk Touch gives the synthetic item back, anything else drops nothing) and keeps vanilla drops otherwise.
    - A second mixin stops synthetic ore dropping XP.
    - Clients without refmod are turned away during configuration, because the extra state shifts block state ids.
    - Game tests (`SyntheticOreGameTest`): natural ore keeps vanilla drops, Fortune, Silk Touch and XP; synthetic ore placed from the recipe item uses its own table and drops no XP; pick-block gives the plain ore; saves without the property load as natural, and an unknown property is ignored.
    - The client game test screenshot `refmod-synthetic-ore` shows both states side by side with the vanilla model.
    - Details: `docs/knowledge/item-keyed-block-drops.md`.

The builder may add a class tweaker or extra mixins only when a requirement needs them. Textures: plain solid-color 16x16 PNGs generated by a script are fine for this internal mod (it is never published).
