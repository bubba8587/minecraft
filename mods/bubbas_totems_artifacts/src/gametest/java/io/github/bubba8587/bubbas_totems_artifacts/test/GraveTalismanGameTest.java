// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.talisman.GraveTalisman;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The Grave Talisman's proximity check (talisman/GraveTalisman) on a real level: the glow-level table; vanilla undead
 * mobs at 30 (faint), 18 (bright) and 2 blocks (brightest), which works without Reworked Mobs; and with Reworked Mobs
 * (-PwithMobs) its sleepers at 30 and 18 blocks. The client runs the same search on its own level; the client game test
 * looks at the result.
 */
public class GraveTalismanGameTest {
	/** The search tests run this high above their spot (see below). */
	private static final int UP = 100;

	/**
	 * Core. Undead mobs only (no other mod needed). Runs UP blocks above the test, where no other test's undead can be in
	 * range (tests sit a few blocks apart, and blocks from finished tests stay in the world; at ground level this failed
	 * now and then). Cleans up after itself.
	 */
	public void sensesUndeadMobs(GameTestHelper helper) {
		// The holder is a point past the lane's east end (eye height over (30, UP + 1, 2)); the mobs stand inside the
		// lane (mobs spawned beyond the test area's loaded chunks aren't found by entity searches).
		Vec3 holder = helper.absoluteVec(new Vec3(30.5, UP + 2.6, 2.5));
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.OFF, "nothing near");
		// A cow is not undead.
		Mob cow = spawn(helper, EntityTypes.COW, new BlockPos(16, UP + 1, 4));
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.OFF, "only a cow near");

		BlockPos far = new BlockPos(1, UP + 1, 2);
		BlockPos near = new BlockPos(13, UP + 1, 2);
		BlockPos close = new BlockPos(19, UP + 1, 2);
		Mob husk = spawn(helper, EntityTypes.HUSK, far);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.FAINT, "husk 29 blocks away");
		Mob skeleton = spawn(helper, EntityTypes.SKELETON, near);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.BRIGHT, "skeleton 17 blocks away");
		Mob zombie = spawn(helper, EntityTypes.ZOMBIE, close);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.BRIGHTEST, "zombie 11 blocks away");

		for (Mob mob : new Mob[] {cow, husk, skeleton, zombie}) {
			mob.discard();
		}
		for (BlockPos pos : new BlockPos[] {far.below(), near.below(), close.below(), new BlockPos(16, UP, 4)}) {
			helper.setBlock(pos, Blocks.AIR);
		}
		helper.succeed();
	}

	/** Core (with Reworked Mobs; passes at once without it). Its sleepers (dormant undead blocks) count too. */
	@GameTest(environment = "bubbas_totems_artifacts-test:talisman", structure = TestSupport.LANE, maxTicks = 40)
	public void sensesSleepers(GameTestHelper helper) {
		if (!TestSupport.withMobs()) {
			helper.succeed();
			return;
		}
		Vec3 holder = helper.absoluteVec(new Vec3(1.5, UP + 2.6, 2.5));
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.OFF, "nothing near");
		BlockPos far = new BlockPos(31, UP + 1, 2);
		BlockPos near = new BlockPos(19, UP + 1, 2);
		sleeper(helper, far);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.FAINT, "sleeper 30 blocks away");
		sleeper(helper, near);
		check(helper, GraveTalisman.glowAt(helper.getLevel(), holder), GraveTalisman.BRIGHT, "sleeper 18 blocks away");
		for (BlockPos pos : new BlockPos[] {far, near, far.below(), near.below()}) {
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
		TestSupport.Mobs.sleeper(helper, pos);
		TestSupport.Mobs.deaf(helper, pos); // never wakes during the test
	}

	private static void check(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + ": glow " + actual + ", expected " + expected);
	}
}
