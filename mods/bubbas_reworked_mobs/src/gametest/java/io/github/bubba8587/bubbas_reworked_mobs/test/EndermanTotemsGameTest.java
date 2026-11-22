// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.api.Endermen;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnGifts;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanAccess;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.Ripples;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TargetWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * What Reworked Mobs does for Bubba's Totems & Artifacts' Totems of the Beginning and the End, through api/Endermen
 * (specs/endermen.md): the ripple back to generated ground and the once-ever offering (no reward for completing a cairn, owner 2026-11-22).
 * Gifts are stood in by vanilla items here, so this runs without that mod.
 */
public class EndermanTotemsGameTest {
	private static final String FIELD = EndermanGameTest.FIELD;

	/**
	 * Core. A ripple fills a hole with the ground that generated there (grass on stone), takes away a dirt pillar and
	 * puts grass back where sand lies on the top layer; a pit with a plank floor (a build) keeps its air.
	 */
	public void rippleRestoresGroundButNotBuilds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int floor = helper.absolutePos(BlockPos.ZERO).getY();
		// The generated ground here, as the target world sees it: grass at the floor, stone under it, air above.
		BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(4, -8, 0)), helper.absolutePos(new BlockPos(20, 9, 16)));
		Runnable undo = TargetWorld.overrideTerrain(level, box,
				pos -> pos.getY() > floor ? Blocks.AIR.defaultBlockState() : pos.getY() == floor ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState(),
				(x, z) -> floor);
		// And the real ground made to match it, inside the ripple's reach.
		for (int x = 5; x <= 19; x++) {
			for (int z = 1; z <= 15; z++) {
				for (int y = -7; y <= 8; y++) {
					helper.setBlock(new BlockPos(x, y, z), y > 0 ? Blocks.AIR : y == 0 ? Blocks.GRASS_BLOCK : Blocks.STONE);
				}
			}
		}
		// A hole, a dirt pillar, sand on the top layer, and a "build": a 3x3 pit with a plank floor.
		helper.setBlock(new BlockPos(14, 0, 8), Blocks.AIR);
		helper.setBlock(new BlockPos(14, -1, 8), Blocks.AIR);
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(10, y, 8), Blocks.DIRT);
		}
		helper.setBlock(new BlockPos(12, 0, 6), Blocks.SAND);
		for (int x = 11; x <= 13; x++) {
			for (int z = 11; z <= 13; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.AIR);
				helper.setBlock(new BlockPos(x, -1, z), Blocks.AIR);
			}
		}
		helper.setBlock(new BlockPos(12, -2, 12), Blocks.OAK_PLANKS);

		helper.assertTrue(Endermen.restoreAround(level, helper.absolutePos(new BlockPos(12, 1, 8)), 6, 1), "the ripple didn't start");
		helper.runAtTickTime(30, () -> {
			try {
				helper.assertFalse(Ripples.running(level), "the ripple is still running");
				helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(14, 0, 8));
				helper.assertBlockPresent(Blocks.STONE, new BlockPos(14, -1, 8));
				for (int y = 1; y <= 3; y++) {
					helper.assertBlockPresent(Blocks.AIR, new BlockPos(10, y, 8));
				}
				helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(12, 0, 6));
				for (int x = 11; x <= 13; x++) {
					for (int z = 11; z <= 13; z++) {
						helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 0, z));
						helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, -1, z));
					}
				}
				helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(12, -2, 12));
			} finally {
				undo.run();
			}
			helper.succeed();
		});
	}

	/**
	 * A complete cairn picked for an offering, with one of its endermen at it, lays the offering once; a player takes it
	 * with an empty hand; it is never laid again.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void offeringLaidOncePerCairn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CairnGifts.set(() -> new ItemStack(Items.HEART_OF_THE_SEA));
		BlockPos origin = helper.absolutePos(new BlockPos(16, 1, 8));
		CairnSite site = null;
		for (long seed = 1; site == null; seed++) {
			CairnSite candidate = CairnSites.addSite(level, origin, seed);
			if (CairnGifts.offers(candidate)) {
				site = candidate;
			} else {
				CairnSites.removeSite(level, candidate);
			}
		}
		site.buildAll(level);
		Enderman witness = EndermanGameTest.enderman(helper, 13.5, 8.5, false);
		((EndermanAccess) witness).brm$setHome(site.origin());
		ServerPlayer player = CreeperGameTest.player(helper, 4.5, 4.5);
		BlockPos spot = null;
		try {
			spot = CairnGifts.tryOffering(level, site, true);
			helper.assertTrue(spot != null, "a complete chosen cairn with a witness laid no offering");
			helper.assertTrue(LootDisplays.hasItemAt(level, spot), "no offering on display at " + spot);
			helper.assertTrue(CairnGifts.tryOffering(level, site, true) == null, "a second offering was laid");
			helper.assertTrue(LootDisplays.take(level, spot, player), "the player couldn't take the offering");
			helper.assertTrue(count(player, Items.HEART_OF_THE_SEA) == 1, "the player didn't get the offering");
			helper.assertTrue(CairnGifts.tryOffering(level, site, true) == null, "the offering was laid again after it was taken");
		} finally {
			CairnGifts.set(null);
			CreeperGameTest.remove(helper, player);
			witness.discard();
			if (spot != null) {
				level.setBlock(spot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			EndermanGameTest.removeBuilt(level, site);
		}
		helper.succeed();
	}

	/** Places {@code state}'s block at {@code pos} as the player would: using the item on the top of the block below. */
	private static void place(GameTestHelper helper, ServerPlayer player, BlockPos pos, BlockState state) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(state.getBlock().asItem()));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, 0.5, 0), Direction.UP, pos.below(), false);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		helper.assertTrue(helper.getLevel().getBlockState(pos).is(state.getBlock()), "couldn't place " + state + " at " + pos);
	}

	private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
		int n = 0;
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(item)) {
				n += stack.getCount();
			}
		}
		return n;
	}
}
