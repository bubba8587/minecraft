<!-- dte:A4 -->
<!-- covers: mods/packcheck/* -->
# Pack testing (mods/packcheck)

PackTest functions and `tools/packload.py` show that a datapack loads and that its commands behave. They cannot easily ask "does this recipe produce exactly this item, with this name, and does the name show up in game?". `mods/packcheck` is an internal, never-published Fabric mod that answers that with real Java game tests and client screenshots, run against the datapacks in `datapacks/` exactly as they are on disk.

```
export JAVA_HOME=/opt/jdk25
./gradlew :packcheck:build                # server game tests (recipes, pack enabled)
timeout 540 xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew :packcheck:runClientGameTest
                                          # screenshots in mods/packcheck/build/run/clientGameTest/screenshots/
```

## How the packs get into the game

1. `mods/packcheck/build.gradle` has a `bundleDatapacks` (Sync) task. It copies every `datapacks/<name>/` that has a `pack.mcmeta` and no `DISABLED` file to `build/generated/packcheck/datapacks/<name>/`, leaving out `VERSION` and `packtest.commands`. That folder is a resource dir of packcheck's `main` source set, so the files end up at `datapacks/<name>/` in the mod's resources (and jar). The task runs automatically before `processResources`. Nothing is copied into the source tree.
2. `Packcheck.onInitialize` lists `datapacks/` in its own mod container and calls `ResourceLoader.registerBuiltinPack(packcheck:<name>, "datapacks/<name>", container, displayName, PackActivationType.ALWAYS_ENABLED)` for each folder. It throws if a pack can't be registered.
3. Fabric (fabric-resource-loader-v1, `ResourceLoaderImpl.registerBuiltinPack`) builds a `SERVER_DATA` pack from that path when it contains `data/` namespaces, and a `CLIENT_RESOURCES` pack only if it also has `assets/`. The path overload is the one its javadoc recommends for a data-only pack in a mod. The game test server enables every available pack (`ModPackResourcesUtil.createTestServerSettings` moves mod packs after vanilla so they win). New client worlds enable it through `createDefaultDataConfiguration`, because `ALWAYS_ENABLED` counts as enabled by default and also makes the pack required.
4. The pack id is `packcheck:<name>`. `bundledPackIsEnabled` checks that it is in the server's selected packs.

Every pack in `datapacks/` is loaded together, including the internal `refpack`. A test that asks "what else matches this input?" therefore looks only at `minecraft:` and the pack's own namespace. Players won't run refpack alongside a real pack.

## What the tests check (bubbas_crafting_tweaks)

`src/gametest/.../PackcheckGameTest.java` has one table, `CRAFTING_TWEAKS`, with **one row per recipe file**:

- `crafting(id, resultItem, count, itemName|null, lore, grid(key, row1, row2, row3)...)`: each grid is a full 3x3 crafting-table layout. Spaces are empty slots, and `CraftingInput.of` trims the grid the way the table does. Give several grids to prove placement freedom or alternative ingredients.
- `cooking(RecipeType.BLASTING|SMOKING, id, resultItem, itemName|null, lore, cookingTime, experience, inputItem...)`: one `SingleRecipeInput` per accepted ingredient.

Each row has a one-line `@GameTest` method calling `check(helper, id)`. For every input, `check` does five things:

- It looks up `bubbas_crafting_tweaks:<id>` with `serverLevel.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id))` and checks the recipe type.
- It checks `recipe.matches(input, level)`.
- It checks that no other vanilla or pack recipe of that type matches the same input, so it catches a recipe that never wins.
- It runs `recipe.assemble(input)` (26.x: no registry argument) and asserts the item and count. It asserts the `item_name` and the shown name when the row sets a name, and it asserts the lore lines. A row with no name or lore must give a result with an empty component patch.
- For cooking recipes it also checks `cookingTime()` and `experience()`, and that the blast furnace or smoker input slot accepts the ingredient (`RecipePropertySet.BLAST_FURNACE_INPUT` / `SMOKER_INPUT`).

Pack-level tests:

- `everyPackRecipeHasATestCase` fails when the pack's recipe ids and the table's ids differ, so adding or removing a recipe file without updating the table fails the build.
- `onlyVanillaSaddleRecipe` checks that vanilla's saddle recipe exists and is the only one matching its grid, that the pack has no `saddle` recipe, and that the old lead-and-leather grid matches nothing. The owner removed the pack's saddle.
- `smokerCooksRottenFleshLeather` runs the cooking end to end: a real smoker turns rotten flesh into leather that still has its lore.

The client test (`PackcheckClientGameTest`) gives a survival player the assembled result of every row and opens the inventory. It takes one overview screenshot and one screenshot hovering each result that has a custom name or lore (`packcheck_tooltip_<name>`). It checks that the screen really hovers the intended slot. Look at the PNGs after any change to a name, lore or result.

## Changing or adding tests

- **A recipe in an existing pack changed:** edit its row. If it was removed, delete its row and its one-line method. If it is new, add a row and a method. Run `./gradlew :packcheck:build`. If the result's name, lore or item changed, also run the client test and look at the screenshots. For a named result, add it to `HOVERS` in the client test.
- **A new datapack:** nothing to wire up, because it is bundled and enabled automatically. Add a section of tests for it: a new table and `check`-style helper, or a new test class listed under `fabric-gametest` in `src/gametest/resources/fabric.mod.json`. Pass the pack's own namespace to the "what else matches" filter. Recipes reuse the same helpers. Other content (loot tables, tags, advancements, worldgen) is read through `level.registryAccess().lookupOrThrow(Registries.X)` or `recipeAccess()`.
- **Disabled packs** (`DISABLED` file) are not bundled. Their tests must be skipped or removed in the same change.
- Gotcha: a test table that is built when the class loads must not create `ItemStack`s. Item components aren't bound until the world loads, and creating one throws `Components not bound yet`. That's why grids and cooking inputs are suppliers.

PackTest functions in each pack (`data/<ns>/test/`) are still the pack's own behaviour tests for CI (`specs/quality.md`). Packcheck adds the Java-level and visual checks on top.
