// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import io.github.bubba8587.bubbas_world_tweaks.lava.GeyserLift;
import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeyser;
import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Core smoke tests (test-lite, specs/quality.md): lava geysers and the geyser lift. */
public class BubbasWorldTweaksCoreGameTest {
	/** A 3x3 lava pool, 2 deep, magma under the middle. Returns the surface lava block over the magma (absolute). */
	private static BlockPos pool(GameTestHelper helper, boolean magma) {
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.LAVA);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.LAVA);
			}
		}
		if (magma) {
			helper.setBlock(new BlockPos(2, 0, 2), Blocks.MAGMA_BLOCK);
		}
		return helper.absolutePos(new BlockPos(2, 2, 2));
	}

	private static ServerPlayer player(GameTestHelper helper, Vec3 at, Vec3 velocity) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		player.setDeltaMovement(velocity);
		return player;
	}

	/** Magma under lava is found; the same lava without magma isn't a bubbling spot. */
	@GameTest
	public void magmaUnderLavaIsABubblingSpot(GameTestHelper helper) {
		BlockPos surface = pool(helper, true);
		helper.assertTrue(LavaGeyser.magmaUnder(helper.getLevel(), surface), "magma not found under the lava");
		helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
		helper.assertTrue(!LavaGeyser.magmaUnder(helper.getLevel(), surface), "found magma that isn't there");
		helper.assertTrue(LavaGeysers.lavaSurface(helper.getLevel(), surface.getX(), surface.getY() + 4, surface.getY() - 4, surface.getZ())
				.equals(surface), "lava surface not found");
		helper.succeed();
	}

	/** An erupting lava geyser lifts a player above it and speeds them up; with nothing erupting, nothing happens. */
	@GameTest
	public void lavaGeyserLiftsAndBoosts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos surface = pool(helper, true);
		Vec3 above = Vec3.atBottomCenterOf(surface).add(0.0, 4.0, 0.0);
		ServerPlayer before = player(helper, above, new Vec3(0.3, -0.2, 0.0));
		GeyserLift.apply(before);
		helper.assertTrue(before.getDeltaMovement().y == -0.2 && before.getDeltaMovement().x == 0.3, "moved with no geyser");

		LavaGeyser geyser = LavaGeysers.erupt(level, surface);
		helper.assertTrue(geyser != null, "no eruption");
		ServerPlayer player = player(helper, above, new Vec3(0.3, -0.2, 0.0));
		GeyserLift.apply(player);
		Vec3 v = player.getDeltaMovement();
		helper.assertTrue(Math.abs(v.y - (-0.2 + WorldTweaksTuning.LIFT_PUSH)) < 1.0E-6, "not lifted: vy " + v.y);
		helper.assertTrue(Math.abs(v.x - 0.3 * WorldTweaksTuning.BOOST_FACTOR) < 1.0E-6, "no speed boost: vx " + v.x);
		helper.assertTrue(LavaGeysers.erupt(level, surface) == null, "two geysers on one spot");
		helper.succeed();
	}

	/** Over an erupting sulfur geyser, a player gets the speed boost (vanilla already does the lifting). */
	@GameTest
	public void sulfurGeyserBoosts(GameTestHelper helper) {
		helper.setBlock(new BlockPos(2, 0, 2), Blocks.POTENT_SULFUR.defaultBlockState().setValue(PotentSulfurBlock.STATE, PotentSulfurState.ERUPTING));
		ServerPlayer player = player(helper, helper.absoluteVec(new Vec3(2.5, 4.0, 2.5)), new Vec3(0.0, 0.0, 0.4));
		GeyserLift.apply(player);
		Vec3 v = player.getDeltaMovement();
		helper.assertTrue(Math.abs(v.z - 0.4 * WorldTweaksTuning.BOOST_FACTOR) < 1.0E-6, "no boost: vz " + v.z);
		helper.assertTrue(v.y == 0.0, "lifted by us over sulfur (vanilla does that): vy " + v.y);
		helper.succeed();
	}
}
