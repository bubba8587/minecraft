// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.gathering.GatheringRing;
import io.github.bubba8587.bubbas_totems_artifacts.growth.Growth;
import io.github.bubba8587.bubbas_totems_artifacts.herding.Herding;
import io.github.bubba8587.bubbas_totems_artifacts.skies.ClearSkies;
import io.github.bubba8587.bubbas_totems_artifacts.statue.StatueEffects;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** The totems and changes of 2026-11-22. */
public class NewTotemsGameTest {
	private static boolean inArea(ServerLevel level, TotemAreas.Kind kind, BlockPos pos) {
		return TotemAreas.all(level, kind).stream().anyMatch(a -> a.box().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
	}

	private static ItemEntity drop(GameTestHelper helper, Vec3 rel, ItemStack stack) {
		Vec3 at = helper.absoluteVec(rel);
		ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
		item.setDeltaMovement(Vec3.ZERO);
		helper.getLevel().addFreshEntity(item);
		return item;
	}

	/** Core. */
	public void gatheringRingFeedsASideHopper(GameTestHelper helper) {
		BlockPos beacon = new BlockPos(4, 1, 8);
		helper.setBlock(beacon, Blocks.BEACON);
		helper.setBlock(beacon.east(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.setBlock(beacon.east(2), Blocks.CHEST);
		ItemEntity item = drop(helper, new Vec3(10.5, 1.1, 8.5), new ItemStack(Items.DIRT, 5));
		BlockPos absolute = helper.absolutePos(beacon);
		ServerLevel level = helper.getLevel();
		GatheringRing.put(level, absolute, new AABB(absolute).inflate(10.0), 400);
		helper.succeedWhen(() -> {
			ChestBlockEntity chest = helper.getBlockEntity(beacon.east(2), ChestBlockEntity.class);
			helper.assertTrue(chest.getItem(0).is(Items.DIRT) && chest.getItem(0).getCount() == 5,
					"chest holds " + chest.getItem(0) + "; item at " + item.position() + " alive " + item.isAlive());
		});
	}

	/** Growth plants one dropped seed on farmland. */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void growthPlantsDroppedSeeds(GameTestHelper helper) {
		for (int x = 3; x <= 5; x++) {
			for (int z = 7; z <= 9; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.FARMLAND);
			}
		}
		ItemEntity seeds = drop(helper, new Vec3(4.5, 1.0, 8.5), new ItemStack(Items.WHEAT_SEEDS, 3));
		AABB box = new AABB(helper.absolutePos(new BlockPos(4, 1, 8))).inflate(4.0);
		boolean planted = false;
		for (int i = 0; i < 80 && !planted; i++) {
			planted = Growth.plant(helper.getLevel(), box);
		}
		helper.assertTrue(planted, "never planted");
		helper.assertTrue(seeds.getItem().getCount() == 2, "seeds left " + seeds.getItem().getCount());
		int wheat = 0;
		for (int x = 3; x <= 5; x++) {
			for (int z = 7; z <= 9; z++) {
				wheat += helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.WHEAT) ? 1 : 0;
			}
		}
		helper.assertTrue(wheat == 1, "wheat planted " + wheat);
		helper.assertFalse(Growth.plantable(new ItemStack(Items.DIRT)), "dirt counts as a seed");
		helper.succeed();
	}

	/** Growth never plants on top of another plant: sugar cane, bamboo and cactus don't stack; a sapling still goes on dirt. */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void growthNeverStacksPlants(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(2, 0, 3), Blocks.SAND);
		helper.setBlock(new BlockPos(1, 0, 3), Blocks.WATER);
		helper.setBlock(new BlockPos(2, 1, 3), Blocks.SUGAR_CANE);
		helper.setBlock(new BlockPos(6, 0, 3), Blocks.DIRT);
		helper.setBlock(new BlockPos(6, 1, 3), Blocks.BAMBOO);
		helper.setBlock(new BlockPos(10, 0, 3), Blocks.SAND);
		helper.setBlock(new BlockPos(10, 1, 3), Blocks.CACTUS);
		record Case(BlockPos top, ItemStack stack) {
		}
		java.util.List<Case> cases = java.util.List.of(new Case(new BlockPos(2, 2, 3), new ItemStack(Items.SUGAR_CANE, 8)),
				new Case(new BlockPos(6, 2, 3), new ItemStack(Items.BAMBOO, 8)), new Case(new BlockPos(10, 2, 3), new ItemStack(Items.CACTUS, 8)));
		for (Case c : cases) {
			drop(helper, Vec3.atBottomCenterOf(c.top()).add(0, 0.05, 0), c.stack());
			AABB box = new AABB(helper.absolutePos(c.top())).inflate(0.6);
			for (int i = 0; i < 200; i++) {
				Growth.plant(level, box);
			}
			helper.assertTrue(helper.getBlockState(c.top()).isAir(), c.stack().getItem() + " planted on its own kind: " + helper.getBlockState(c.top()));
		}
		helper.assertTrue(Growth.isPlant(Blocks.WHEAT.defaultBlockState()) && Growth.isPlant(Blocks.OAK_SAPLING.defaultBlockState())
				&& Growth.isPlant(Blocks.POPPY.defaultBlockState()), "crops, saplings or flowers don't count as plants");
		for (int x = 13; x <= 15; x++) {
			for (int z = 7; z <= 9; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
			}
		}
		ItemEntity sapling = drop(helper, new Vec3(14.5, 1.0, 8.5), new ItemStack(Items.OAK_SAPLING));
		AABB saplingBox = new AABB(helper.absolutePos(new BlockPos(14, 1, 8))).inflate(1.5);
		boolean planted = false;
		for (int i = 0; i < 200 && !planted; i++) {
			planted = Growth.plant(level, saplingBox);
		}
		helper.assertTrue(planted && !sapling.isAlive(), "the sapling was never planted on dirt");
		helper.succeed();
	}

	/** Core. */
	public void herdingPastureKeepsLivestockIn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var cow = helper.spawn(EntityTypes.COW, new BlockPos(4, 1, 8));
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(5, 1, 8));
		BlockPos out = helper.absolutePos(new BlockPos(12, 1, 8));
		BlockPos in = helper.absolutePos(new BlockPos(5, 1, 9));
		helper.assertFalse(Herding.keepsIn(cow, out.getX(), out.getY(), out.getZ()), "blocked with no pasture");
		TotemAreas.put(level, TotemAreas.Kind.HERDING, "test", new AABB(helper.absolutePos(new BlockPos(4, 1, 8))).inflate(3.0), 1, 100);
		helper.assertTrue(Herding.keepsIn(cow, out.getX(), out.getY(), out.getZ()), "cow may walk out of the pasture");
		helper.assertFalse(Herding.keepsIn(cow, in.getX(), in.getY(), in.getZ()), "cow can't move inside the pasture");
		helper.assertFalse(Herding.keepsIn(zombie, out.getX(), out.getY(), out.getZ()), "a zombie is kept in");
		TotemAreas.remove(level, TotemAreas.Kind.HERDING, "test");
		cow.discard();
		zombie.discard();
		helper.succeed();
	}

	/** Core. */
	public void clearSkiesKeepsRainOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 8));
		TotemAreas.put(level, TotemAreas.Kind.CLEAR_SKIES, "test", new AABB(centre).inflate(2.0), 1, 100);
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(ClearSkies.clearAt(level, centre.getX() + 2, centre.getZ()), "edge column not clear");
			helper.assertFalse(ClearSkies.clearAt(level, centre.getX() + 3, centre.getZ()), "column outside is clear");
			float before = level.getRainLevel(1.0F);
			level.setRainLevel(1.0F);
			try {
				BlockPos inside = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, centre);
				helper.assertTrue(level.precipitationAt(inside) == Biome.Precipitation.NONE, "it rains inside: " + level.precipitationAt(inside));
				helper.assertFalse(level.isRainingAt(inside), "isRainingAt inside");
			} finally {
				level.setRainLevel(before);
				TotemAreas.remove(level, TotemAreas.Kind.CLEAR_SKIES, "test");
			}
			helper.succeed();
		});
	}

	/** Core. */
	public void statuesPulseTheirArea(GameTestHelper helper) {
		BlockPos growth = new BlockPos(3, 1, 8);
		BlockPos herding = new BlockPos(9, 1, 8);
		helper.setBlock(growth, TotemStatueBlock.stateFor(TotemType.GROWTH));
		helper.setBlock(herding, TotemStatueBlock.stateFor(TotemType.HERDING));
		helper.setBlock(new BlockPos(15, 1, 8), TotemStatueBlock.stateFor(TotemType.KEEPING));
		helper.assertFalse(StatueEffects.pulses(TotemType.KEEPING), "Keeping pulses");
		ServerLevel level = helper.getLevel();
		helper.succeedWhen(() -> {
			helper.assertTrue(inArea(level, TotemAreas.Kind.GROWTH, helper.absolutePos(growth.offset(2, 0, 2))), "no growth area");
			helper.assertFalse(inArea(level, TotemAreas.Kind.GROWTH, helper.absolutePos(growth.offset(5, 0, 0))), "growth area too big");
			helper.assertTrue(inArea(level, TotemAreas.Kind.HERDING, helper.absolutePos(herding.offset(-3, 0, 0))), "no pasture");
		});
	}
}
