// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.BackToSleep;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Back to sleep at dawn (specs/undead.md). Milestone. */
public class DawnGameTest {
	private static final String FIELD = CreeperGameTest.FIELD;
	/** A niche: stone either side and behind, a stone roof over it. */
	private static final BlockPos NICHE = new BlockPos(14, 1, 8);

	private static void niche(GameTestHelper helper) {
		helper.setBlock(NICHE.east(), Blocks.STONE);
		helper.setBlock(NICHE.north(), Blocks.STONE);
		helper.setBlock(NICHE.south(), Blocks.STONE);
		helper.setBlock(NICHE.above(), Blocks.STONE);
		helper.setBlock(NICHE.above().east(), Blocks.STONE);
		helper.setBlock(NICHE.above().north(), Blocks.STONE);
		helper.setBlock(NICHE.above().south(), Blocks.STONE);
	}

	private static Monster zombie(GameTestHelper helper, boolean sawNight) {
		Monster zombie = helper.spawn(ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(8, 1, 8));
		zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)); // keeps the sun off meanwhile
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
		if (sawNight) {
			zombie.addTag(BackToSleep.SAW_NIGHT_TAG);
		}
		return zombie;
	}

	/** A woken zombie that was up in the night lies down in the nearest niche at sunrise, a sleeper again with its gear. */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_noon", structure = FIELD, maxTicks = 400)
	public void survivorsGoBackToSleepAtDawn(GameTestHelper helper) {
		niche(helper);
		Monster zombie = zombie(helper, true);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.ZOMBIE), NICHE);
			helper.assertTrue(zombie.isRemoved(), "the zombie became the sleeper");
			SleeperBlockEntity sleeper = helper.getBlockEntity(NICHE, SleeperBlockEntity.class);
			helper.assertTrue(sleeper.getGear(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && sleeper.getGear(EquipmentSlot.MAINHAND).is(Items.IRON_SHOVEL),
					"the sleeper keeps its gear");
		});
	}

	/** One woken by day (it never saw the night) stays up. */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_noon", structure = FIELD, maxTicks = 120)
	public void onlyThoseWhoSawTheNight(GameTestHelper helper) {
		niche(helper);
		Monster zombie = zombie(helper, false);
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(!zombie.isRemoved(), "woken by day, it stays up");
			helper.assertBlockNotPresent(ModBlocks.sleeper(UndeadVariant.ZOMBIE), NICHE);
			zombie.discard();
			helper.succeed();
		});
	}

	/** Owner: anywhere safe to stand out of the sun will do; it sits down there (a stone canopy, open on every side). */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_noon", structure = FIELD, maxTicks = 400)
	public void sitsDownUnderAnyCover(GameTestHelper helper) {
		BlockPos spot = new BlockPos(14, 1, 8);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(spot.offset(dx, 2, dz), Blocks.STONE); // a roof two up: standing room under it
			}
		}
		Monster zombie = zombie(helper, true);
		helper.succeedWhen(() -> {
			helper.assertTrue(zombie.isRemoved(), "the zombie should have sat down to sleep");
			boolean sitting = false;
			for (BlockPos at : BlockPos.betweenClosed(spot.offset(-1, 0, -1), spot.offset(1, 0, 1))) {
				var state = helper.getBlockState(at);
				if (state.is(ModBlocks.sleeper(UndeadVariant.ZOMBIE))
						&& state.getValue(io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock.POSE) == io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose.SITTING) {
					sitting = true;
				}
			}
			helper.assertTrue(sitting, "a sitting sleeper under the canopy");
		});
	}
}
