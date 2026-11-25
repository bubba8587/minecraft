// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.test;

import io.github.bubba8587.bubbas_reworked_beacons.beacon.BeaconLogic;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.HangingBeacon;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Milestone: corner particles show when a beacon's effect changes, not on its first pulse or an unchanged one. */
public class BeaconEdgeGameTest {
	@GameTest
	public void cornersShowOnlyWhenTheEffectChanges(GameTestHelper helper) {
		BlockPos floor = new BlockPos(4, 2, 4);
		BlockPos ceiling = floor.above(4);
		BeaconGameTest.pyramid(helper, floor, 1, -1, Blocks.IRON_BLOCK);
		helper.setBlock(floor, Blocks.BEACON);
		helper.setBlock(ceiling, Blocks.BEACON.defaultBlockState().setValue(HangingBeacon.UPSIDE_DOWN, true));
		BeaconGameTest.pyramid(helper, ceiling, 1, 1, Blocks.GOLD_BLOCK);
		ReworkedBeacon rb = BeaconGameTest.beaconAt(helper, floor);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(floor);
		BeaconLogic.updatePair(level, abs, rb);
		rb.bubba$setItem(new ItemStack(Items.AMETHYST_SHARD));

		BeaconLogic.pulse(level, abs, rb);
		helper.assertValueEqual(rb.bubba$edgeFlash(), 0, "first pulse after loading shows nothing");
		BeaconLogic.pulse(level, abs, rb);
		helper.assertValueEqual(rb.bubba$edgeFlash(), 0, "an unchanged pulse shows nothing");

		rb.bubba$setItem(new ItemStack(Items.QUARTZ));
		BeaconLogic.pulse(level, abs, rb);
		helper.assertValueEqual(rb.bubba$edgeFlash(), BeaconLogic.EDGE_FLASH_TICKS, "a new item shows the corners");

		rb.bubba$setEdgeFlash(0);
		BeaconGameTest.pyramid(helper, floor, 2, -1, Blocks.IRON_BLOCK);
		BeaconGameTest.pyramid(helper, ceiling, 2, 1, Blocks.GOLD_BLOCK);
		BeaconLogic.updatePair(level, abs, rb);
		BeaconLogic.pulse(level, abs, rb);
		helper.assertValueEqual(rb.bubba$edgeFlash(), BeaconLogic.EDGE_FLASH_TICKS, "a bigger area shows the corners");

		rb.bubba$setEdgeFlash(0);
		rb.bubba$setItem(ItemStack.EMPTY);
		BeaconLogic.pulse(level, abs, rb);
		helper.assertValueEqual(rb.bubba$edgeFlash(), 0, "switching off shows nothing");
		helper.succeed();
	}
}
