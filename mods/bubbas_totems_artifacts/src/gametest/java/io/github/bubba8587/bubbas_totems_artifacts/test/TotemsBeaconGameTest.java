// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The new totems in a Bubba's Reworked Beacons beacon (payloads in compat/). Milestone; without Reworked Beacons (the
 * default test run) each passes at once; run with -PwithBeacons -PmilestoneTests to test them for real. The beacon is
 * simulated: one payload pulse with a real BeaconContext (sky area, the given layers) at a beacon position.
 */
public class TotemsBeaconGameTest {
	private static boolean withBeacons() {
		return FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons");
	}

	/** Gathering: a pulse starts the ring; a dropped item 10 blocks out joins it. */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 60)
	public void gatheringBeaconStartsTheRing(GameTestHelper helper) {
		if (!withBeacons()) {
			helper.succeed();
			return;
		}
		BlockPos beacon = new BlockPos(4, 3, 8);
		Vec3 at = helper.absoluteVec(new Vec3(14.5, 1.1, 8.5));
		ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.DIRT, 7));
		helper.getLevel().addFreshEntity(item);
		WithBeacons.pulse(helper, ModItems.TOTEM_OF_GATHERING, new ItemStack(ModItems.TOTEM_OF_GATHERING), beacon, 1);
		helper.succeedWhen(() -> helper.assertTrue(io.github.bubba8587.bubbas_totems_artifacts.gathering.GatheringRing
				.members(helper.getLevel(), helper.absolutePos(beacon)).contains(item), "the item didn't join the ring"));
	}

	/** Fortification: a pulse sets an area; an explosion inside it breaks nothing. */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void fortificationBeaconProtectsItsArea(GameTestHelper helper) {
		if (!withBeacons()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos beacon = new BlockPos(1, 1, 1);
		WithBeacons.pulse(helper, ModItems.TOTEM_OF_FORTIFICATION, new ItemStack(ModItems.TOTEM_OF_FORTIFICATION), beacon, 1);
		for (BlockPos p : BlockPos.betweenClosed(new BlockPos(10, 1, 6), new BlockPos(14, 3, 10))) {
			helper.setBlock(p, Blocks.DIRT);
		}
		Vec3 blast = helper.absoluteVec(new Vec3(12.5, 2.5, 8.5));
		level.explode(null, blast.x, blast.y, blast.z, 3.0F, Level.ExplosionInteraction.TNT);
		int left = 0;
		for (BlockPos p : BlockPos.betweenClosed(new BlockPos(10, 1, 6), new BlockPos(14, 3, 10))) {
			left += helper.getBlockState(p).is(Blocks.DIRT) ? 1 : 0;
		}
		TotemAreas.remove(level, TotemAreas.Kind.FORTIFICATION, helper.absolutePos(beacon));
		helper.assertTrue(left == 75, "the explosion broke " + (75 - left) + " blocks in a Fortification beacon's area");
		helper.succeed();
	}

	/** Growth: a pulse sets an area whose strength grows with the pyramid (1 layer: +1, 4 layers: +4). */
	@GameTest(maxTicks = 20)
	public void growthBeaconSetsItsArea(GameTestHelper helper) {
		if (!withBeacons()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos beacon = new BlockPos(1, 1, 1);
		WithBeacons.pulse(helper, ModItems.TOTEM_OF_GROWTH, new ItemStack(ModItems.TOTEM_OF_GROWTH), beacon, 4);
		var areas = TotemAreas.all(level, TotemAreas.Kind.GROWTH);
		TotemAreas.remove(level, TotemAreas.Kind.GROWTH, helper.absolutePos(beacon));
		helper.assertTrue(areas.stream().anyMatch(a -> a.strength() == 4 && a.box().contains(Vec3.atCenterOf(helper.absolutePos(beacon).offset(45, 0, 0)))),
				"no 4-layer growth area reaching 45 blocks: " + areas);
		helper.succeed();
	}

	/** Every new payload accepts its own totem only; Keeping has no beacon use. */
	@GameTest(maxTicks = 5)
	public void payloadsAcceptTheirTotems(GameTestHelper helper) {
		if (!withBeacons()) {
			helper.succeed();
			return;
		}
		helper.assertTrue(WithBeacons.acceptsOnly(), "a payload accepts the wrong item");
		helper.succeed();
	}

	/** Touches the beacons API; loaded only when that mod is installed. */
	private static final class WithBeacons {
		static io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload payloadFor(Item item) {
			if (item == ModItems.TOTEM_OF_GATHERING) {
				return new io.github.bubba8587.bubbas_totems_artifacts.compat.GatheringBeaconPayload();
			} else if (item == ModItems.TOTEM_OF_FORTIFICATION) {
				return new io.github.bubba8587.bubbas_totems_artifacts.compat.FortificationBeaconPayload();
			}
			return new io.github.bubba8587.bubbas_totems_artifacts.compat.GrowthBeaconPayload();
		}

		static boolean acceptsOnly() {
			Item[] totems = {ModItems.TOTEM_OF_GATHERING, ModItems.TOTEM_OF_FORTIFICATION, ModItems.TOTEM_OF_GROWTH};
			for (Item owner : totems) {
				var payload = payloadFor(owner);
				for (Item other : new Item[] {ModItems.TOTEM_OF_GATHERING, ModItems.TOTEM_OF_FORTIFICATION, ModItems.TOTEM_OF_GROWTH,
						ModItems.TOTEM_OF_SEEKING, ModItems.TOTEM_OF_KEEPING, ModItems.TOTEM_OF_WARDING}) {
					if (payload.accepts(new ItemStack(other)) != (other == owner)) {
						return false;
					}
				}
			}
			return true;
		}

		static void pulse(GameTestHelper helper, Item item, ItemStack stack, BlockPos relative, int levels) {
			var level = helper.getLevel();
			BlockPos beacon = helper.absolutePos(relative);
			var payload = payloadFor(item);
			var context = new io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext(level, beacon,
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode.SKY, levels, payload.power(stack),
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext.skyArea(level, beacon, levels));
			payload.apply(level, stack, context);
		}
	}
}
