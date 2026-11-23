// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.undead.FireWard;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Fire as a weak ward (specs/undead.md). Milestone. */
public class FireWardGameTest {
	private static final String FIELD = CreeperGameTest.FIELD;

	/** An idle zombie standing by a lit campfire walks off to a few blocks away. */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_spawn", structure = FIELD, maxTicks = 200)
	public void idleMobsKeepAwayFromALitCampfire(GameTestHelper helper) {
		BlockPos fire = new BlockPos(12, 1, 8);
		helper.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		Mob zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(10, 1, 8));
		helper.succeedWhen(() -> {
			double d = zombie.position().distanceTo(Vec3.atBottomCenterOf(helper.absolutePos(fire)));
			helper.assertTrue(d >= 4.0, "the zombie should keep off the fire, it's " + d + " away");
			zombie.discard();
		});
	}

	/** Lit campfires, soul campfires and fire repel; an unlit campfire doesn't. */
	@GameTest(structure = FIELD)
	public void onlyLitFireRepels(GameTestHelper helper) {
		helper.assertTrue(FireWard.repels(Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)), "lit campfire");
		helper.assertTrue(FireWard.repels(Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)), "lit soul campfire");
		helper.assertTrue(FireWard.repels(Blocks.FIRE.defaultBlockState()) && FireWard.repels(Blocks.SOUL_FIRE.defaultBlockState()), "fire");
		helper.assertTrue(!FireWard.repels(Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false)), "an unlit campfire doesn't");
		helper.succeed();
	}

	/** Only hostile mobs that fear fire: no fire-immune or Nether mobs, no warden. */
	@GameTest(structure = FIELD)
	public void onlyFireFearingMobs(GameTestHelper helper) {
		for (EntityType<? extends Mob> type : java.util.List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER, EntityTypes.SPIDER)) {
			helper.assertTrue(FireWard.fearsFire(type.create(helper.getLevel(), EntitySpawnReason.COMMAND)), type + " fears fire");
		}
		for (EntityType<? extends Mob> type : java.util.List.of(EntityTypes.BLAZE, EntityTypes.WITHER_SKELETON, EntityTypes.PIGLIN, EntityTypes.HOGLIN,
				EntityTypes.ZOMBIFIED_PIGLIN, EntityTypes.MAGMA_CUBE, EntityTypes.WARDEN, EntityTypes.COW)) {
			helper.assertTrue(!FireWard.fearsFire(type.create(helper.getLevel(), EntitySpawnReason.COMMAND)), type + " doesn't");
		}
		helper.succeed();
	}
}
