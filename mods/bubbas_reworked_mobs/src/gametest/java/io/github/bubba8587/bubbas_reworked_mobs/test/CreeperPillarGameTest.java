// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.CoverFinder;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperMind;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperPillar;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Pillar creepers (specs/creepers.md, "Making its own cover"): on a flat grass field with no cover a watcher stacks a
 * 2-high pillar of the ground's own blocks and peeks from behind it; with mobGriefing off nothing changes.
 */
public class CreeperPillarGameTest {
	private static final int W = 24;
	private static final int D = 16;
	private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();

	/** Grass floor over dirt across the whole field. */
	private static void grassField(GameTestHelper helper) {
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				helper.setBlock(new BlockPos(x, -1, z), Blocks.DIRT);
				helper.setBlock(new BlockPos(x, 0, z), GRASS);
			}
		}
	}

	/** Floor cells (y 0) that are air: the dents it dug. Fails on any 2-deep hole. */
	private static int dents(GameTestHelper helper) {
		int dents = 0;
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				if (helper.getBlockState(new BlockPos(x, 0, z)).isAir()) {
					dents++;
					helper.assertTrue(!helper.getBlockState(new BlockPos(x, -1, z)).isAir(), "a 2-deep hole at " + x + ", " + z);
				}
			}
		}
		return dents;
	}

	/** Fresh field: a 2-high grass pillar, two 1-deep dents, then a peek spot behind it. */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_pillar", structure = CreeperGameTest.FIELD, maxTicks = 900)
	public void buildsPillarOfGroundBlocksAndPeeks(GameTestHelper helper) {
		grassField(helper);
		Creeper creeper = CreeperGameTest.creeper(helper, 14.5, 8.5, true);
		ServerPlayer player = TestPlayers.survivalAt(helper, 1.5, 8.5, 90.0F);
		CreeperMind mind = CreeperAccess.mind(creeper);
		helper.runAtTickTime(2, () -> mind.watch(player, helper.getLevel().getGameTime()));
		helper.onEachTick(() -> {
			mind.watch(player, helper.getLevel().getGameTime());
			BlockPos base = mind.pillar();
			if (base == null || !mind.spotReached()) {
				return;
			}
			helper.assertTrue(helper.getLevel().getBlockState(base).equals(GRASS) && helper.getLevel().getBlockState(base.above()).equals(GRASS),
					"the pillar should be two grass blocks as they were, got " + helper.getLevel().getBlockState(base) + " / " + helper.getLevel().getBlockState(base.above()));
			helper.assertTrue(dents(helper) == 2, "two 1-deep dents, got " + dents(helper));
			CoverFinder.PeekSpot spot = mind.spot();
			helper.assertTrue(spot != null && spot.cell().distSqr(base) <= 2 * 2 + 1, "it should peek from beside its pillar, spot " + (spot == null ? null : spot.cell()) + " pillar " + base);
			helper.assertTrue(!mind.openGround() && !creeper.hasAttached(CreeperPillar.CARRIED), "done building, not in the open");
			creeper.discard();
			TestPlayers.remove(helper, player);
			helper.succeed();
		});
	}

	/** mobGriefing off: no block changes, the open-ground stance as before. */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_pillar_off", structure = CreeperGameTest.FIELD, maxTicks = 400)
	public void noPillarWithoutMobGriefing(GameTestHelper helper) {
		grassField(helper);
		Creeper creeper = CreeperGameTest.creeper(helper, 14.5, 8.5, true);
		ServerPlayer player = TestPlayers.survivalAt(helper, 1.5, 8.5, 90.0F);
		CreeperMind mind = CreeperAccess.mind(creeper);
		helper.runAtTickTime(2, () -> mind.watch(player, helper.getLevel().getGameTime()));
		helper.onEachTick(() -> {
			mind.watch(player, helper.getLevel().getGameTime());
			helper.assertTrue(!creeper.hasAttached(CreeperPillar.CARRIED), "it picked up a block with mobGriefing off");
		});
		helper.runAtTickTime(300, () -> {
			helper.assertTrue(mind.openGround(), "it should take the open-ground stance");
			helper.assertTrue(mind.pillar() == null && dents(helper) == 0, "no blocks should change");
			for (int x = 0; x < W; x++) {
				for (int z = 0; z < D; z++) {
					helper.assertTrue(helper.getBlockState(new BlockPos(x, 1, z)).isAir(), "a block appeared at " + x + ", 1, " + z);
				}
			}
			creeper.discard();
			TestPlayers.remove(helper, player);
			helper.succeed();
		});
	}

	/** A lone grass block sticking up at a good spot: it adds only one block on top. */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_pillar_bump", structure = CreeperGameTest.FIELD, maxTicks = 900)
	public void addsOneBlockOnALoneBlock(GameTestHelper helper) {
		grassField(helper);
		BlockPos bump = new BlockPos(12, 1, 8);
		helper.setBlock(bump, GRASS);
		Creeper creeper = CreeperGameTest.creeper(helper, 14.5, 8.5, true);
		ServerPlayer player = TestPlayers.survivalAt(helper, 1.5, 8.5, 90.0F);
		CreeperMind mind = CreeperAccess.mind(creeper);
		helper.runAtTickTime(2, () -> mind.watch(player, helper.getLevel().getGameTime()));
		helper.onEachTick(() -> {
			mind.watch(player, helper.getLevel().getGameTime());
			BlockPos base = mind.pillar();
			if (base == null || !mind.spotReached()) {
				return;
			}
			helper.assertTrue(base.equals(helper.absolutePos(bump)), "it should build on the lone block, built at " + helper.relativePos(base));
			helper.assertTrue(helper.getBlockState(bump.above()).equals(GRASS), "one grass block on top");
			helper.assertTrue(helper.getBlockState(bump.above(2)).isAir(), "only one added");
			helper.assertTrue(dents(helper) == 1, "one dent, got " + dents(helper));
			creeper.discard();
			TestPlayers.remove(helper, player);
			helper.succeed();
		});
	}
}
