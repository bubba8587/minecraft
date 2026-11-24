// dte:C1,B20
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.TerrainOracle;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The terrain oracle on a normal world (client part {@code oracle}): regenerating the spawn chunks gives the same
 * surface as the real, untouched ones. Run with other worldgen mods loaded (-PextraMods) to check compatibility.
 */
@SuppressWarnings("UnstableApiUsage")
public class OracleClientGameTest implements FabricClientGameTest {
	/** The real top ground block: solid, not a fluid, tree or plant (the oracle rebuilds terrain only, not features). */
	private static int groundTop(net.minecraft.server.level.ServerLevel level, int x, int z) {
		var pos = new net.minecraft.core.BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
		while (pos.getY() > level.getMinY()) {
			var state = level.getBlockState(pos);
			if (!state.isAir() && state.getFluidState().isEmpty() && !state.getCollisionShape(level, pos).isEmpty() && !state.is(net.minecraft.tags.BlockTags.LEAVES)
					&& !state.is(net.minecraft.tags.BlockTags.LOGS)) {
				return pos.getY();
			}
			pos.move(net.minecraft.core.Direction.DOWN);
		}
		return level.getMinY();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("oracle")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> {
					s.setWorldType(new WorldCreationUiState.WorldTypeEntry(
							s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
					s.setSeed("20261116");
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				})
				.create()) {
			sp.getConnection().waitForChunksRender();
			String result = sp.getServer().computeOnServer(s -> {
				var level = s.overworld();
				var spawn = level.getRespawnData().pos();
				int cx = spawn.getX() >> 4;
				int cz = spawn.getZ() >> 4;
				int columns = 0;
				int matching = 0;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						TerrainOracle.TerrainChunk chunk = TerrainOracle.getNow(level, cx + dx, cz + dz);
						for (int x = 0; x < 16; x += 5) {
							for (int z = 0; z < 16; z += 5) {
								int bx = ((cx + dx) << 4) + x;
								int bz = ((cz + dz) << 4) + z;
								int real = groundTop(level, bx, bz);
								int regen = chunk.surface(bx, bz);
								columns++;
								if (Math.abs(real - regen) <= 1) {
									matching++;
								}
							}
						}
					}
				}
				return matching + " of " + columns;
			});
			System.out.println("[oracle] surface matches in " + result + " columns");
			String[] parts = result.split(" of ");
			if (Integer.parseInt(parts[0]) * 10 < Integer.parseInt(parts[1]) * 8) {
				throw new AssertionError("the regenerated surface matched the real one in only " + result + " columns");
			}
		}
	}
}
