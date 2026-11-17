// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.warding.WardFleeGoal;
import io.github.bubba8587.bubbas_totems_artifacts.warding.Wards;
import io.github.bubba8587.bubbas_totems_artifacts.warding.WardingTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
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
 * Warding (specs/undead.md "Warding", moved here: specs/totems-artifacts.md): the held Totem of Warding's pulse, ward
 * areas, loot. Core tests run through BubbasTotemsArtifactsCoreGameTest; the rest is milestone. The sleeper parts need
 * Bubba's Reworked Mobs (-PwithMobs) and pass at once without it. (Light and bells at sleepers stayed in Reworked Mobs.)
 */
public class WardingGameTest {
	private static final String FIELD = TestSupport.FIELD;

	// --- the held totem -----------------------------------------------------------------------------

	/**
	 * Core. Held in the main hand, the totem pulses once and goes on vanilla's item cooldown (the hotbar sweep); a
	 * zombie 4 blocks away runs from the holder. Works without Reworked Mobs.
	 */
	public void heldTotemPulsesAndScaresMonsters(GameTestHelper helper) {
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		ItemStack totem = new ItemStack(ModItems.TOTEM_OF_WARDING);
		player.setItemInHand(InteractionHand.MAIN_HAND, totem);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(16, 1, 8));
		zombie.setTarget(player);

		totem.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
		helper.assertTrue(player.getCooldowns().isOnCooldown(totem), "no cooldown sweep after a pulse");
		helper.assertTrue(WardFleeGoal.isScared(zombie), "zombie within 8 blocks was not scared");

		double start = zombie.distanceTo(player);
		helper.runAfterDelay(40, () -> {
			double after = zombie.distanceTo(player);
			helper.getLevel().getServer().getPlayerList().remove(player);
			helper.assertTrue(after > start + 2.0, "zombie did not run: " + start + " -> " + after);
			helper.succeed();
		});
	}

	/**
	 * Core (with Reworked Mobs; passes at once without it). A pulse settles a stirred sleeper within 8 blocks by the calm
	 * points; a second tick on cooldown doesn't.
	 */
	public void pulseCalmsSleepers(GameTestHelper helper) {
		if (!TestSupport.withMobs()) {
			helper.succeed();
			return;
		}
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		ItemStack totem = new ItemStack(ModItems.TOTEM_OF_WARDING);
		player.setItemInHand(InteractionHand.MAIN_HAND, totem);
		BlockPos sleeper = new BlockPos(9, 1, 8);
		TestSupport.Mobs.sleeper(helper, sleeper);
		TestSupport.Mobs.disturb(helper, sleeper, 70.0F);

		totem.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
		float calmed = TestSupport.Mobs.disturbance(helper, sleeper);
		helper.assertTrue(Math.abs(calmed - (70.0F - WardingTuning.PULSE_CALM_POINTS)) < 0.6F, "sleeper meter after the pulse: " + calmed);
		totem.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
		helper.assertTrue(Math.abs(TestSupport.Mobs.disturbance(helper, sleeper) - calmed) < 0.01F, "a second pulse while on cooldown");
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	/** Milestone. The totem in the off hand pulses too; in the backpack it does nothing. */
	@GameTest(environment = "bubbas_totems_artifacts-test:warding_pulse", structure = FIELD, maxTicks = 20)
	public void offHandPulsesBackpackDoesNot(GameTestHelper helper) {
		ServerPlayer player = TestSupport.survivalPlayer(helper);
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

	// --- wards -------------------------------------------------------------------------------------

	/**
	 * Core. A ward over part of the field: natural monster spawns are refused inside it (not animals, not outside); a
	 * zombie inside walks out. Works without Reworked Mobs.
	 */
	public void wardStopsSpawnsAndSendsMonstersOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))), Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(10, 4, 16))));
		Object key = new Object();
		Wards.put(level, key, area, true, 400);
		BlockPos inside = helper.absolutePos(new BlockPos(5, 1, 8));
		BlockPos outside = helper.absolutePos(new BlockPos(20, 1, 8));
		helper.assertTrue(Wards.blocksNaturalSpawn(level, EntityTypes.ZOMBIE, inside), "zombie may spawn inside a ward");
		helper.assertFalse(Wards.blocksNaturalSpawn(level, EntityTypes.COW, inside), "a ward stopped a cow");
		helper.assertFalse(Wards.blocksNaturalSpawn(level, EntityTypes.ZOMBIE, outside), "a ward stopped a spawn outside it");

		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(8, 1, 8));
		helper.succeedWhen(() -> {
			helper.assertFalse(area.contains(zombie.position()), "zombie still inside the ward at " + zombie.position());
			Wards.remove(level, key);
		});
	}

	/**
	 * Core (with Reworked Mobs; passes at once without it). A sleeper inside a full-pyramid ward never wakes, one inside
	 * a smaller ward hears as usual, and removing the ward lets it hear again.
	 */
	public void fullWardKeepsSleepersAsleep(GameTestHelper helper) {
		if (!TestSupport.withMobs()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))), Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(10, 4, 16))));
		Object key = new Object();
		BlockPos sleeper = new BlockPos(3, 1, 3);
		TestSupport.Mobs.sleeper(helper, sleeper);
		Wards.put(level, key, area, true, 400);
		helper.assertTrue(TestSupport.Mobs.sensitivity(helper, sleeper) == 0.0F, "sleeper in a full ward can still wake");
		Wards.put(level, key, area, false, 400);
		helper.assertTrue(TestSupport.Mobs.sensitivity(helper, sleeper) == 1.0F, "a smaller ward calmed the sleeper");
		Wards.put(level, key, area, true, 400);
		Wards.remove(level, key);
		helper.assertTrue(TestSupport.Mobs.sensitivity(helper, sleeper) == 1.0F, "a removed ward still keeps the sleeper asleep");
		helper.succeed();
	}

	/**
	 * Core. Mobs that don't follow paths are held at the edge (owner): a slime pushed toward the ward every tick never
	 * gets in; a slime and a (baby) happy ghast that start inside drift out.
	 */
	public void wardPushesBackMobsThatDoNotPath(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))), Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(10, 6, 16))));
		Object key = new Object();
		Wards.put(level, key, area, false, 400);
		Slime outsider = helper.spawn(EntityTypes.SLIME, new BlockPos(13, 1, 4));
		outsider.setSize(1, true);
		Slime insider = helper.spawn(EntityTypes.SLIME, new BlockPos(8, 1, 12));
		insider.setSize(1, true);
		// A ghastling: an adult is 4 blocks wide and hits the test area's walls.
		var ghast = helper.spawn(EntityTypes.HAPPY_GHAST, new BlockPos(8, 1, 8));
		ghast.setAge(-24000);
		helper.onEachTick(() -> {
			outsider.setDeltaMovement(-0.3, outsider.getDeltaMovement().y, 0.0);
			helper.assertFalse(area.contains(outsider.position()), "a slime got into the ward at " + outsider.position());
		});
		helper.succeedWhen(() -> {
			helper.assertFalse(area.contains(insider.position()), "slime still inside at " + insider.position());
			helper.assertFalse(area.contains(ghast.position()), "happy ghast still inside at " + ghast.position());
			helper.assertTrue(outsider.getX() < area.maxX + 1.5, "the outside slime never reached the edge");
			Wards.remove(level, key);
		});
	}

	/**
	 * Milestone. A player steering a harnessed happy ghast into a ward is put back outside the edge (the server moves
	 * the vehicle and tells the rider's client, vanilla's vehicle correction).
	 */
	@GameTest(environment = "bubbas_totems_artifacts-test:warding_barrier", structure = FIELD, maxTicks = 20)
	public void riddenHappyGhastIsPushedOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))), Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(10, 6, 16))));
		Object key = new Object();
		Wards.put(level, key, area, false, 100);
		var ghast = helper.spawn(EntityTypes.HAPPY_GHAST, new BlockPos(9, 1, 8));
		ghast.setItemSlot(EquipmentSlot.BODY, new ItemStack(net.minecraft.world.item.Items.HARNESS.white()));
		ServerPlayer rider = TestSupport.survivalPlayer(helper);
		rider.snapTo(ghast.getX(), ghast.getY() + 4, ghast.getZ(), 0.0F, 0.0F);
		rider.startRiding(ghast, true, false);
		helper.assertTrue(ghast.getControllingPassenger() == rider, "the player isn't steering the ghast");
		helper.assertTrue(area.contains(ghast.position()), "ghast didn't start inside");
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(area.contains(ghast.position()), "ridden ghast still inside at " + ghast.position());
			rider.stopRiding();
			level.getServer().getPlayerList().remove(rider);
			Wards.remove(level, key);
			helper.succeed();
		});
	}

	/** Milestone. A zombie outside a ward can't path to a point inside it: its path stops short, every node outside. */
	@GameTest(environment = "bubbas_totems_artifacts-test:warding_ward_path", structure = FIELD, maxTicks = 40)
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

	/** Milestone. Woodland mansion chests hold a Totem of Warding about 20 % of the time, evokers about 5 %. */
	@GameTest(environment = "bubbas_totems_artifacts-test:warding_loot", maxTicks = 20)
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
		// Evokers: a rare 5 % bonus (owner). Rolled from the entity table with the evoker as the victim.
		var evoker = helper.spawnWithNoFreeWill(EntityTypes.EVOKER, new BlockPos(1, 1, 1));
		LootTable evokerLoot = level.getServer().reloadableRegistries().getLootTable(EntityTypes.EVOKER.getDefaultLootTable().orElseThrow());
		int fromEvokers = 0;
		for (int i = 0; i < 1000; i++) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, evoker)
					.withParameter(LootContextParams.ORIGIN, evoker.position())
					.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic()).create(LootContextParamSets.ENTITY);
			for (ItemStack stack : evokerLoot.getRandomItems(params)) {
				if (stack.is(ModItems.TOTEM_OF_WARDING)) {
					fromEvokers++;
				}
			}
		}
		evoker.discard();
		helper.assertTrue(fromEvokers > 25 && fromEvokers < 80, "evokers dropping a Totem of Warding: " + fromEvokers + " of 1000");
		helper.succeed();
	}

	/**
	 * Milestone (bug 2026-11-16, owner: a skeleton in an enclosed beacon ward was "stuck running/jittering"). A skeleton
	 * and a player shut in a stone room under a ward: with no way out, the skeleton counts as trapped, stops trying to
	 * leave and isn't pushed into the wall, so it fights (its bow goal runs).
	 */
	@GameTest(structure = FIELD, maxTicks = 200)
	public void trappedInAnEnclosedWardItStillFights(GameTestHelper helper) {
		// Room inside x 6..14, z 4..12, y 1..3; stone walls and roof round it.
		for (int x = 5; x <= 15; x++) {
			for (int z = 3; z <= 13; z++) {
				for (int y = 1; y <= 4; y++) {
					boolean wall = x == 5 || x == 15 || z == 3 || z == 13 || y == 4;
					helper.setBlock(new BlockPos(x, y, z), wall ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
							: net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				}
			}
		}
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(13.5, 1.0, 8.5));
		player.snapTo(at.x, at.y, at.z, 90.0F, 0.0F);
		Vec3 lo = helper.absoluteVec(new Vec3(6, 1, 4));
		Vec3 hi = helper.absoluteVec(new Vec3(15, 4, 13));
		AABB room = new AABB(Math.min(lo.x, hi.x), lo.y, Math.min(lo.z, hi.z), Math.max(lo.x, hi.x), hi.y, Math.max(lo.z, hi.z));
		Object key = new Object();
		Wards.put(level, key, room, false, 400);
		var skeleton = helper.spawn(EntityTypes.SKELETON, new BlockPos(7, 1, 8));
		skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.BOW));
		skeleton.setPersistenceRequired();
		helper.runAfterDelay(120, () -> {
			var running = ((io.github.bubba8587.bubbas_totems_artifacts.mixin.MobAccessor) skeleton).bta$goalSelector().getAvailableGoals().stream()
					.filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList();
			Wards.remove(level, key);
			level.getServer().getPlayerList().remove(player);
			helper.assertTrue(WardFleeGoal.isTrapped(skeleton), "with no way out it should count as trapped; running " + running);
			helper.assertFalse(running.contains("WardFleeGoal"), "still trying to leave: " + running);
			helper.assertTrue(running.contains("RangedBowAttackGoal"), "it should be fighting: " + running + ", target " + skeleton.getTarget());
			helper.succeed();
		});
	}
}
