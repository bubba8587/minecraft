// dte:C6,C1
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Warding statues and Reworked Mobs' crypts (specs/totems-artifacts.md, specs/graveyards.md). Milestone. Needs Bubba's
 * Reworked Mobs (-PwithMobs); passes at once without it.
 */
public class CryptWardsGameTest {
	private static final String CRYPT_FIELD = TestSupport.NS + ":crypt_field";

	/** A warding statue keeps a sleeper 5 blocks off asleep through an explosion; one 7 blocks off wakes. */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 40)
	public void wardStatueKeepsNearSleepersAsleep(GameTestHelper helper) {
		if (!TestSupport.withMobs()) {
			helper.succeed();
			return;
		}
		BlockPos statue = new BlockPos(3, 1, 8);
		BlockPos near = statue.east(TotemsTuning.STATUE_RADIUS);
		BlockPos far = statue.east(TotemsTuning.STATUE_RADIUS + 2);
		helper.setBlock(statue, TotemStatueBlock.stateFor(TotemType.WARDING));
		TestSupport.Mobs.sleeper(helper, near);
		TestSupport.Mobs.sleeper(helper, far);
		helper.runAtTickTime(5, () -> {
			boom(helper, near.south(3));
			boom(helper, far.south(3));
			helper.assertFalse(Crypts.wakePending(helper, near), "a sleeper 5 blocks from a warding statue woke");
			helper.assertTrue(Crypts.wakePending(helper, far), "a sleeper 7 blocks from a warding statue didn't wake");
			helper.succeed();
		});
	}

	/**
	 * Each crypt kind built with a statue in every wing (a seed that rolls them all): warding statues on the spots,
	 * facing as planned, the statues never cutting off part of the crypt floor, and the noble crypt's sleepers near its statues
	 * kept asleep.
	 */
	@GameTest(structure = CRYPT_FIELD, maxTicks = 40)
	public void cryptStatuesStandInEveryWing(GameTestHelper helper) {
		if (!TestSupport.withMobs()) {
			helper.succeed();
			return;
		}
		List<BlockPos> statues = Crypts.buildAll(helper);
		helper.runAtTickTime(5, () -> {
			Crypts.checkAsleep(helper, statues);
			helper.succeed();
		});
	}

	private static void boom(GameTestHelper helper, BlockPos at) {
		helper.getLevel().gameEvent(net.minecraft.world.level.gameevent.GameEvent.EXPLODE, helper.absoluteVec(Vec3.atCenterOf(at)),
				net.minecraft.world.level.gameevent.GameEvent.Context.of((net.minecraft.world.entity.Entity) null));
	}

	/** Reworked Mobs' side; only loaded with that mod. */
	private static final class Crypts {
		private Crypts() {
		}

		static boolean wakePending(GameTestHelper helper, BlockPos pos) {
			return helper.getBlockEntity(pos, io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.class).isWakePending();
		}

		/** Builds pauper, family, then noble (each over the last) and checks each; returns the noble's statues. */
		static List<BlockPos> buildAll(GameTestHelper helper) {
			ServerLevel level = helper.getLevel();
			List<BlockPos> last = List.of();
			BlockPos gate = helper.absolutePos(new BlockPos(16, 7, 1));
			for (var wealth : io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth.values()) {
				int wings = wealth == io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth.NOBLE ? 3 : 1;
				long seed = 1;
				while (spots(level, gate, seed, wealth).size() < wings) {
					seed++;
				}
				var piece = io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand.build(level, gate, seed, 15, 13, true,
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.OVERGROWN, wealth);
				var spots = piece.wardSpots();
				helper.assertTrue(spots.size() == wings, wealth + ": " + spots.size() + " statue spots, expected " + wings);
				List<BlockPos> placed = new ArrayList<>();
				for (var spot : spots) {
					BlockState state = level.getBlockState(spot.pos());
					helper.assertTrue(state.getBlock() instanceof TotemStatueBlock && TotemStatueBlock.type(state) == TotemType.WARDING
							&& state.getValue(TotemStatueBlock.FACING) == spot.facing(), wealth + ": no warding statue at " + spot + ", found " + state);
					placed.add(spot.pos());
				}
				int with = walkablePockets(level, piece.getBoundingBox(), piece.vaultCentre().getY(), Set.of());
				int without = walkablePockets(level, piece.getBoundingBox(), piece.vaultCentre().getY(), Set.copyOf(placed));
				helper.assertTrue(with == without, wealth + ": the statues split the crypt floor into " + with + " walkable areas (" + without + " without them)");
				last = placed;
			}
			return last;
		}

		private static List<io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece.WardSpot> spots(ServerLevel level, BlockPos gate, long seed,
				io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth wealth) {
			return new io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece(seed, gate, 15, 13, true,
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardTrees.forBiome(level.getBiome(gate)),
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.OVERGROWN, wealth,
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Climate.TEMPERATE).wardSpots();
		}

		/** Connected areas of standable cells (2 high, no collision, solid below) at feet height y in box; {@code ignored} counts as clear. */
		private static int walkablePockets(ServerLevel level, BoundingBox box, int y, Set<BlockPos> ignored) {
			Set<BlockPos> open = new HashSet<>();
			for (int x = box.minX(); x <= box.maxX(); x++) {
				for (int z = box.minZ(); z <= box.maxZ(); z++) {
					BlockPos pos = new BlockPos(x, y, z);
					if ((ignored.contains(pos) || clear(level, pos)) && clear(level, pos.above()) && !clear(level, pos.below())) {
						open.add(pos);
					}
				}
			}
			int pockets = 0;
			Set<BlockPos> seen = new HashSet<>();
			for (BlockPos from : open) {
				if (!seen.add(from)) {
					continue;
				}
				pockets++;
				ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(from));
				while (!queue.isEmpty()) {
					BlockPos at = queue.poll();
					for (Direction d : Direction.Plane.HORIZONTAL) {
						BlockPos next = at.relative(d);
						if (open.contains(next) && seen.add(next)) {
							queue.add(next);
						}
					}
				}
			}
			return pockets;
		}

		private static boolean clear(ServerLevel level, BlockPos pos) {
			return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
		}

		/** Every sleeper within a statue's ward box is kept asleep. */
		static void checkAsleep(GameTestHelper helper, List<BlockPos> statues) {
			ServerLevel level = helper.getLevel();
			int r = TotemsTuning.STATUE_RADIUS;
			int checked = 0;
			for (BlockPos statue : statues) {
				for (BlockPos pos : BlockPos.betweenClosed(statue.offset(-r, -r, -r), statue.offset(r, r, r))) {
					if (level.getBlockState(pos).getBlock() instanceof io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock) {
						checked++;
						helper.assertTrue(TestSupport.Mobs.keptAsleep(level, pos), "sleeper at " + pos.immutable() + " near a crypt statue isn't kept asleep");
					}
				}
			}
			helper.assertTrue(checked > 0, "no sleepers near the noble crypt's statues");
		}
	}
}
