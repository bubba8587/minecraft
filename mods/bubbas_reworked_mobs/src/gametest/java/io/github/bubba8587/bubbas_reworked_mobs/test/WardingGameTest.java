// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBells;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.warding.WardFleeGoal;
import io.github.bubba8587.bubbas_reworked_mobs.warding.Wards;
import io.github.bubba8587.bubbas_reworked_mobs.warding.WardingTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Warding (specs/undead.md "Warding"): the held Totem of Warding's pulse, light and bells at sleepers, ward areas.
 * Core tests run through ReworkedMobsCoreGameTest; the rest is milestone.
 */
public class WardingGameTest {
	private static final String FIELD = "bubbas_reworked_mobs-test:hearing_field";

	private static SleeperBlockEntity sleeper(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.sleeper(UndeadVariant.SKELETON).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.SITTING)
				.setValue(SleeperBlock.FACING, Direction.EAST));
		return helper.getBlockEntity(pos, SleeperBlockEntity.class);
	}

	private static long now(GameTestHelper helper) {
		return helper.getLevel().getGameTime();
	}

	// --- the held totem -----------------------------------------------------------------------------

	/**
	 * Core. Held in the main hand, the totem pulses once and goes on vanilla's item cooldown (the hotbar sweep); a
	 * zombie 4 blocks away runs from the holder; a stirred sleeper settles by the calm points; a second tick on cooldown
	 * does nothing.
	 */
	public void heldTotemPulsesScaresMonstersAndCalmsSleepers(GameTestHelper helper) {
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		ItemStack totem = new ItemStack(ModItems.TOTEM_OF_WARDING);
		player.setItemInHand(InteractionHand.MAIN_HAND, totem);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(16, 1, 8));
		zombie.setTarget(player);
		SleeperBlockEntity sleeper = sleeper(helper, new BlockPos(9, 1, 8));
		sleeper.disturb(now(helper), 70.0F);

		totem.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
		helper.assertTrue(player.getCooldowns().isOnCooldown(totem), "no cooldown sweep after a pulse");
		helper.assertTrue(WardFleeGoal.isScared(zombie), "zombie within 8 blocks was not scared");
		float calmed = sleeper.getDisturbance(now(helper));
		helper.assertTrue(Math.abs(calmed - (70.0F - WardingTuning.PULSE_CALM_POINTS)) < 0.6F, "sleeper meter after the pulse: " + calmed);

		// On cooldown: no second pulse.
		totem.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
		helper.assertTrue(Math.abs(sleeper.getDisturbance(now(helper)) - calmed) < 0.01F, "a second pulse while on cooldown");

		double start = zombie.distanceTo(player);
		helper.runAfterDelay(40, () -> {
			double after = zombie.distanceTo(player);
			helper.getLevel().getServer().getPlayerList().remove(player);
			helper.assertTrue(after > start + 2.0, "zombie did not run: " + start + " -> " + after);
			helper.succeed();
		});
	}

	/** Milestone. The totem in the off hand pulses too; in the backpack it does nothing. */
	@GameTest(environment = "bubbas_reworked_mobs-test:warding_pulse", structure = FIELD, maxTicks = 20)
	public void offHandPulsesBackpackDoesNot(GameTestHelper helper) {
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		ItemStack totem = new ItemStack(ModItems.TOTEM_OF_WARDING);
		totem.inventoryTick(helper.getLevel(), player, null);
		helper.assertFalse(player.getCooldowns().isOnCooldown(totem), "pulsed from the backpack");
		totem.inventoryTick(helper.getLevel(), player, EquipmentSlot.OFFHAND);
		helper.assertTrue(player.getCooldowns().isOnCooldown(totem), "did not pulse from the off hand");
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	// --- light and bells at sleepers ---------------------------------------------------------------

	/**
	 * Core. A sleeper in the dark takes noise in full; with a glowstone block next to it (light 14) it takes x0.7.
	 * The test server's light engine catches up slowly (light was still 0 after 40 ticks), hence the long timeout.
	 */
	public void strongLightCalmsSleepers(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		SleeperBlockEntity sleeper = sleeper(helper, pos);
		helper.assertTrue(sleeper.effectiveSensitivity(helper.getLevel()) == 1.0F, "dark sleeper not at full sensitivity");
		helper.setBlock(pos.east(), Blocks.GLOWSTONE);
		helper.succeedWhen(() -> helper.assertTrue(
				Math.abs(sleeper.effectiveSensitivity(helper.getLevel()) - WardingTuning.LIGHT_CALM_FACTOR) < 1.0E-4F,
				"lit sleeper sensitivity: " + sleeper.effectiveSensitivity(helper.getLevel()) + ", block light "
						+ helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(pos))));
	}

	/**
	 * Core. A bell rung 14 blocks away (beyond the 12 where ordinary noise fades out) wakes a sleeper; one 23 blocks away
	 * only stirs (the bell's own linear falloff, 32 blocks), and the bell's plain vibration is not counted twice.
	 */
	public void bellsAreLoudToSleepers(GameTestHelper helper) {
		BlockPos bell = new BlockPos(0, 1, 8);
		helper.setBlock(bell, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
		SleeperBlockEntity near = sleeper(helper, new BlockPos(14, 1, 8));
		SleeperBlockEntity far = sleeper(helper, new BlockPos(23, 1, 8));
		((BellBlock) Blocks.BELL).attemptToRing(helper.getLevel(), helper.absolutePos(bell), Direction.EAST);
		helper.assertTrue(near.isWakePending(), "bell 14 blocks off did not wake the sleeper");
		float expected = SleeperBells.loudness(23.0);
		float heard = far.getDisturbance(now(helper));
		helper.assertTrue(!far.isWakePending() && heard >= UndeadTuning.STIR_THRESHOLD && Math.abs(heard - expected) < 1.0F,
				"bell 23 blocks off: meter " + heard + ", expected " + expected);
		helper.succeed();
	}

	// --- wards -------------------------------------------------------------------------------------

	/**
	 * Core. A ward over part of the field: natural monster spawns are refused inside it (not animals, not outside); a
	 * sleeper inside a full-pyramid ward never wakes, one inside a smaller ward hears as usual; a zombie inside walks out.
	 */
	public void wardStopsSpawnsKeepsSleepersAndSendsMonstersOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))), Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(10, 4, 16))));
		Object key = new Object();
		Wards.put(level, key, area, true, 400);
		BlockPos inside = helper.absolutePos(new BlockPos(5, 1, 8));
		BlockPos outside = helper.absolutePos(new BlockPos(20, 1, 8));
		helper.assertTrue(Wards.blocksNaturalSpawn(level, EntityTypes.ZOMBIE, inside), "zombie may spawn inside a ward");
		helper.assertFalse(Wards.blocksNaturalSpawn(level, EntityTypes.COW, inside), "a ward stopped a cow");
		helper.assertFalse(Wards.blocksNaturalSpawn(level, EntityTypes.ZOMBIE, outside), "a ward stopped a spawn outside it");

		SleeperBlockEntity warded = sleeper(helper, new BlockPos(3, 1, 3));
		helper.assertTrue(warded.effectiveSensitivity(level) == 0.0F, "sleeper in a full ward can still wake");
		Wards.put(level, key, area, false, 400);
		helper.assertTrue(warded.effectiveSensitivity(level) == 1.0F, "a smaller ward calmed the sleeper");

		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(8, 1, 8));
		helper.succeedWhen(() -> {
			helper.assertFalse(area.contains(zombie.position()), "zombie still inside the ward at " + zombie.position());
			Wards.remove(level, key);
		});
	}

	/** Milestone. A zombie outside a ward can't path to a point inside it: its path stops short, every node outside. */
	@GameTest(environment = "bubbas_reworked_mobs-test:warding_ward_path", structure = FIELD, maxTicks = 40)
	public void monstersDoNotPathIntoAWard(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))), Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(10, 4, 16))));
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(18, 1, 8));
		BlockPos goal = helper.absolutePos(new BlockPos(4, 1, 8));
		// Ground navigation only paths from the ground: wait for the zombie to land.
		helper.runAfterDelay(10, () -> {
			Path free = zombie.getNavigation().createPath(goal, 0);
			helper.assertTrue(free != null && free.canReach(), "test field has no path even without a ward");
			Object key = new Object();
			Wards.put(level, key, area, false, 100);
			Path path = zombie.getNavigation().createPath(goal, 0);
			Wards.remove(level, key);
			if (path != null) {
				helper.assertFalse(path.canReach(), "zombie found a path into the ward");
				for (int i = 0; i < path.getNodeCount(); i++) {
					Node node = path.getNode(i);
					helper.assertFalse(area.contains(node.x + 0.5, node.y + 0.5, node.z + 0.5), "path node inside the ward: " + node.asBlockPos());
				}
			}
			helper.succeed();
		});
	}

	// --- loot -------------------------------------------------------------------------------------

	/** Milestone. Every evoker drops a Totem of Warding (alongside its Totem of Undying); woodland mansion chests sometimes hold one. */
	@GameTest(environment = "bubbas_reworked_mobs-test:warding_loot", maxTicks = 20)
	public void totemIsFoundInLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootTable mansion = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.WOODLAND_MANSION);
		int found = 0;
		for (int i = 0; i < 400; i++) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
			for (ItemStack stack : mansion.getRandomItems(params)) {
				if (stack.is(ModItems.TOTEM_OF_WARDING)) {
					found++;
				}
			}
		}
		// 20 % a chest: 80 expected out of 400.
		helper.assertTrue(found > 40 && found < 130, "mansion chests with a Totem of Warding: " + found + " of 400");
		var evoker = helper.spawn(EntityTypes.EVOKER, new BlockPos(1, 1, 1));
		evoker.kill(level);
		helper.succeedWhen(() -> {
			helper.assertItemEntityPresent(ModItems.TOTEM_OF_WARDING);
			helper.assertItemEntityPresent(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
		});
	}
}
