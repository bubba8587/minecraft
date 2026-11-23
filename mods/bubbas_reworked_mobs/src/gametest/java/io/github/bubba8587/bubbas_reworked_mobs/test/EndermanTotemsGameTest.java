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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * What Reworked Mobs does for Bubba's Totems & Artifacts' Totems of the Beginning and the End, through api/Endermen
 * (specs/endermen.md): the ripple back to generated ground and the once-ever offering (no reward for completing a cairn, owner 2026-11-22).
 * Gifts are stood in by vanilla items here, so this runs without that mod.
 */
public class EndermanTotemsGameTest {
	private static final String FIELD = EndermanGameTest.FIELD;

	/**
	 * A ripple fills a hole with the ground that generated there (grass on stone), takes away a dirt pillar and
	 * puts grass back where sand lies on the top layer; a pit with a plank floor (a build) keeps its air.
	 */
	@GameTest(structure = FIELD, maxTicks = 100)
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
		ServerPlayer player = TestPlayers.survivalAt(helper, 4.5, 4.5);
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
			TestPlayers.remove(helper, player);
			witness.discard();
			if (spot != null) {
				level.setBlock(spot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			EndermanGameTest.removeBuilt(level, site);
		}
		helper.succeed();
	}

	/** Pulse field: generated grass at the floor, iron ore two under it, stone elsewhere; real ground dug out 4 deep. */
	private static Runnable pulseField(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int floor = helper.absolutePos(BlockPos.ZERO).getY();
		BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(4, -8, 0)), helper.absolutePos(new BlockPos(20, 9, 16)));
		Runnable undo = TargetWorld.overrideTerrain(level, box,
				pos -> pos.getY() > floor ? Blocks.AIR.defaultBlockState() : pos.getY() == floor ? Blocks.GRASS_BLOCK.defaultBlockState()
						: pos.getY() == floor - 2 ? Blocks.IRON_ORE.defaultBlockState() : Blocks.STONE.defaultBlockState(),
				(x, z) -> floor);
		for (int x = 5; x <= 19; x++) {
			for (int z = 1; z <= 15; z++) {
				for (int y = -7; y <= 8; y++) {
					helper.setBlock(new BlockPos(x, y, z), y > -4 ? Blocks.AIR : Blocks.STONE);
				}
			}
		}
		return undo;
	}

	/** Columns within 5 of the pulse centre (12, 8): clear of the field's edge protection. */
	private static java.util.List<BlockPos> cells(int y) {
		java.util.List<BlockPos> list = new java.util.ArrayList<>();
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				if (dx * dx + dz * dz <= 25) {
					list.add(new BlockPos(12 + dx, y, 8 + dz));
				}
			}
		}
		return list;
	}

	private static int filled(GameTestHelper helper, int... ys) {
		int n = 0;
		for (int y : ys) {
			for (BlockPos pos : cells(y)) {
				if (!helper.getBlockState(pos).isAir()) {
					n++;
				}
			}
		}
		return n;
	}

	/**
	 * Pulses at 12% (ore 1.2%): one pulse fills roughly 12% of dug-out terrain cells (bounds 5-22% of 243); after ten
	 * pulses terrain is mostly back (over 50%) while ore spots stay mostly empty (under 30%, and under a third of terrain).
	 */
	@GameTest(structure = FIELD, maxTicks = 400)
	public void pulseOddsTerrainOverOre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Runnable undo = pulseField(helper);
		BlockPos centre = helper.absolutePos(new BlockPos(12, 1, 8));
		int[] firstPulse = new int[1];
		for (int i = 0; i < 10; i++) {
			int pulse = i;
			helper.runAtTickTime(1 + i * 30, () -> {
				if (pulse == 1) {
					firstPulse[0] = filled(helper, 0, -1, -3);
				}
				helper.assertTrue(Endermen.pulseAround(level, centre, 7, 1, 0.12F, 0.012F, null), "the pulse didn't start");
			});
		}
		helper.runAtTickTime(330, () -> {
			try {
				int terrainCells = cells(0).size() * 3;
				int oreCells = cells(-2).size();
				double first = firstPulse[0] / (double) terrainCells;
				double terrain = filled(helper, 0, -1, -3) / (double) terrainCells;
				double ore = filled(helper, -2) / (double) oreCells;
				helper.assertTrue(first >= 0.05 && first <= 0.22, "one pulse filled " + first + " of terrain");
				helper.assertTrue(terrain > 0.5, "ten pulses filled only " + terrain + " of terrain");
				helper.assertTrue(ore < 0.3 && ore < terrain / 3, "ten pulses filled " + ore + " of ore spots (terrain " + terrain + ")");
				for (BlockPos pos : cells(-2)) {
					BlockState state = helper.getBlockState(pos);
					helper.assertTrue(state.isAir() || state.is(Blocks.IRON_ORE), "ore spot became " + state);
				}
			} finally {
				undo.run();
			}
			helper.succeed();
		});
	}

	/**
	 * At chance 1: blocks a pulse reverted and that were dug out again come back on the next pulse (no hidden window);
	 * stone placed in an ore spot stays stone; a plank in the ground keeps the columns round it as they are.
	 */
	@GameTest(structure = FIELD, maxTicks = 100)
	public void pulseRepeatsAndBuilds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Runnable undo = pulseField(helper);
		BlockPos centre = helper.absolutePos(new BlockPos(12, 1, 8));
		helper.setBlock(new BlockPos(12, -2, 8), Blocks.STONE);
		helper.setBlock(new BlockPos(18, -3, 8), Blocks.OAK_PLANKS);
		helper.assertTrue(Endermen.pulseAround(level, centre, 7, 1, 1.0F, 1.0F, null), "the pulse didn't start");
		helper.runAtTickTime(20, () -> {
			helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(10, 0, 8));
			helper.assertBlockPresent(Blocks.IRON_ORE, new BlockPos(10, -2, 8));
			helper.assertBlockPresent(Blocks.STONE, new BlockPos(12, -2, 8));
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(18, 0, 8));
			helper.setBlock(new BlockPos(10, 0, 8), Blocks.AIR);
			helper.setBlock(new BlockPos(10, -2, 8), Blocks.AIR);
			helper.assertTrue(Endermen.pulseAround(level, centre, 7, 1, 1.0F, 1.0F, null), "the second pulse didn't start");
		});
		helper.runAtTickTime(40, () -> {
			try {
				helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(10, 0, 8));
				helper.assertBlockPresent(Blocks.IRON_ORE, new BlockPos(10, -2, 8));
				helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(18, -3, 8));
			} finally {
				undo.run();
			}
			helper.succeed();
		});
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
