// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.GraveTalisman;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The Grave Talisman's proximity check (undead/GraveTalisman) on a real level: the glow-level table, then nothing near,
 * a sleeper at 30 blocks (faint), one at 18 (bright), a woken zombie at 2 (brightest). The client runs the same search
 * on its own level; the client game test looks at the result.
 */
public class GraveTalismanGameTest {
	private static final String LANE = "bubbas_reworked_mobs-test:drift_lane";
	/** The search test runs this high above its spot (see below). */
	private static final int UP = 100;

	@GameTest
	public void glowLevelTable(GameTestHelper helper) {
		check(helper, GraveTalisman.glowLevel(Double.MAX_VALUE), GraveTalisman.OFF, "no undead");
		check(helper, GraveTalisman.glowLevel(40.1 * 40.1), GraveTalisman.OFF, "40.1 blocks");
		check(helper, GraveTalisman.glowLevel(40.0 * 40.0), GraveTalisman.FAINT, "40 blocks");
		check(helper, GraveTalisman.glowLevel(24.1 * 24.1), GraveTalisman.FAINT, "24.1 blocks");
		check(helper, GraveTalisman.glowLevel(24.0 * 24.0), GraveTalisman.BRIGHT, "24 blocks");
		check(helper, GraveTalisman.glowLevel(12.1 * 12.1), GraveTalisman.BRIGHT, "12.1 blocks");
		check(helper, GraveTalisman.glowLevel(12.0 * 12.0), GraveTalisman.BRIGHTEST, "12 blocks");
		check(helper, GraveTalisman.glowLevel(0.0), GraveTalisman.BRIGHTEST, "0 blocks");
		helper.succeed();
	}

	/**
	 * Runs UP blocks above the test, where no other test's undead can be in range (tests sit a few blocks apart, and
	 * blocks from finished tests stay in the world; at ground level this failed now and then). Cleans up after itself.
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:talisman", structure = LANE, maxTicks = 40)
	public void sensesSleepersAndUndead(GameTestHelper helper) {
		Vec3 holder = helper.absoluteVec(new Vec3(1.5, UP + 2.6, 2.5)); // eye height over (1, UP + 1, 2)
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.OFF, "nothing near");
		// A cow is not undead.
		Mob cow = spawn(helper, EntityTypes.COW, new BlockPos(4, UP + 1, 4));
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.OFF, "only a cow near");

		BlockPos far = new BlockPos(31, UP + 1, 2);
		BlockPos near = new BlockPos(19, UP + 1, 2);
		sleeper(helper, far);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.FAINT, "sleeper 30 blocks away");
		sleeper(helper, near);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.BRIGHT, "sleeper 18 blocks away");
		BlockPos zombieAt = new BlockPos(3, UP + 1, 2);
		Mob zombie = spawn(helper, ModEntities.woken(UndeadVariant.ZOMBIE), zombieAt);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.BRIGHTEST, "woken zombie 2 blocks away");

		cow.discard();
		zombie.discard();
		for (BlockPos pos : new BlockPos[] {far, near, far.below(), near.below(), zombieAt.below(), new BlockPos(4, UP, 4)}) {
			helper.setBlock(pos, Blocks.AIR);
		}
		helper.succeed();
	}

	private static Mob spawn(GameTestHelper helper, EntityType<? extends Mob> type, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.STONE);
		return helper.spawnWithNoFreeWill(type, pos);
	}

	private static void sleeper(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, ModBlocks.sleeper(UndeadVariant.ZOMBIE).defaultBlockState());
		helper.getBlockEntity(pos, SleeperBlockEntity.class).setSensitivity(0.0F); // never wakes during the test
	}

	private static void check(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + ": glow " + actual + ", expected " + expected);
	}
}
