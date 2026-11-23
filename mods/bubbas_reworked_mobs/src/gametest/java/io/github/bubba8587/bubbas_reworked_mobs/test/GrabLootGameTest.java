// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Grabbing what's lying around (specs/undead.md "Loot on display"): the waking grab (weapons first, then anything)
 * and the idle {@code GrabLootGoal} (real pathing in the level). Each walking test has its own environment, so it runs
 * in a batch of its own: tests in a batch sit 5 blocks apart, inside the 8-block notice range, and mobs would take
 * each other's loot. All at night: in daylight woken undead burn and flee the sun instead.
 */
public class GrabLootGameTest {
	private static final String ENV = "bubbas_reworked_mobs-test:grab_loot";

	private static void display(GameTestHelper helper, BlockPos pos, DisplayPlacement placement, Item item) {
		LootDisplays.place(helper.getLevel(), helper.absolutePos(pos), placement, Direction.SOUTH, new ItemStack(item));
	}

	/** A woken (or any) mob with free will, empty hands unless given, no vanilla contact pickup, never despawning. */
	private static <T extends Mob> T mob(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
		T mob = helper.spawn(type, pos);
		mob.setPersistenceRequired();
		mob.setCanPickUpLoot(false);
		mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return mob;
	}

	private static Monster zombie(GameTestHelper helper, BlockPos pos) {
		return mob(helper, ModEntities.woken(UndeadVariant.ZOMBIE), pos);
	}

	private static SleeperBlockEntity sleeper(GameTestHelper helper, UndeadVariant variant, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.sleeper(variant).defaultBlockState());
		SleeperBlockEntity sleeper = helper.getBlockEntity(pos, SleeperBlockEntity.class);
		sleeper.setSensitivity(0.0F);
		return sleeper;
	}

	// --- waking -----------------------------------------------------------------------------------

	/** Weapons first: a loose sword 2.5 blocks off beats a golden carrot on display right next to the sleeper. */
	// Core test: registered by ReworkedMobsCoreGameTest (test-lite, specs/quality.md).
	public void wakePrefersAWeaponOverACloserOtherItem(GameTestHelper helper) {
		TestFloors.stone(helper);
		BlockPos at = new BlockPos(4, 1, 4);
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.ZOMBIE, at);
		BlockPos carrot = at.south();
		display(helper, carrot, DisplayPlacement.GROUND, Items.GOLDEN_CARROT);
		helper.spawnItem(Items.IRON_SWORD, Vec3.atBottomCenterOf(at).add(-2.5, 0.0, 0.0));
		sleeper.scheduleWake(helper.getLevel(), null, null, 1);
		helper.succeedWhen(() -> {
			Monster mob = helper.findOneEntity(ModEntities.woken(UndeadVariant.ZOMBIE));
			helper.assertTrue(mob.getMainHandItem().is(Items.IRON_SWORD), "zombie holds " + mob.getMainHandItem());
			helper.assertBlockPresent(ModBlocks.LOOT_DISPLAY, carrot);
			helper.assertItemEntityNotPresent(Items.IRON_SWORD);
		});
	}

	/** No weapon in reach: anything else lying around (here a pickaxe on display; any placement, even buried). */
	@GameTest(environment = ENV, maxTicks = 40)
	public void wakeTakesAnyItemWhenNoWeaponIsInReach(GameTestHelper helper) {
		TestFloors.stone(helper);
		BlockPos at = new BlockPos(4, 1, 4);
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.SKELETON, at);
		BlockPos pick = at.west(2);
		display(helper, pick, DisplayPlacement.BURIED, Items.IRON_PICKAXE);
		sleeper.scheduleWake(helper.getLevel(), null, null, 1);
		helper.succeedWhen(() -> {
			Monster mob = helper.findOneEntity(ModEntities.woken(UndeadVariant.SKELETON));
			helper.assertTrue(mob.getMainHandItem().is(Items.IRON_PICKAXE), "skeleton holds " + mob.getMainHandItem());
			helper.assertBlockPresent(Blocks.AIR, pick);
		});
	}

	// --- the idle goal ----------------------------------------------------------------------------

	/** Displays come first: the zombie goes for a display 5 blocks off rather than a golden carrot lying 2 blocks off. */
	@GameTest(environment = "bubbas_reworked_mobs-test:grab_prefers_display", maxTicks = 400)
	public void idleZombiePrefersADisplayOverACloserLooseItem(GameTestHelper helper) {
		TestFloors.stone(helper);
		Monster zombie = zombie(helper, new BlockPos(1, 1, 1));
		ItemEntity carrot = helper.spawnItem(Items.GOLDEN_CARROT, Vec3.atBottomCenterOf(new BlockPos(3, 1, 1)));
		BlockPos pick = new BlockPos(1, 1, 6);
		display(helper, pick, DisplayPlacement.STUCK, Items.GOLDEN_PICKAXE);
		helper.succeedWhen(() -> {
			helper.assertTrue(zombie.getMainHandItem().is(Items.GOLDEN_PICKAXE), "zombie holds " + zombie.getMainHandItem());
			helper.assertBlockPresent(Blocks.AIR, pick);
			helper.assertTrue(carrot.isAlive(), "the loose carrot should still lie there");
		});
	}

	/** With nothing on display, a loose item is the target: picked up after walking to it. */
	@GameTest(environment = "bubbas_reworked_mobs-test:grab_walk_item", maxTicks = 400)
	public void idleZombieWalksToALooseItem(GameTestHelper helper) {
		TestFloors.stone(helper);
		Monster zombie = zombie(helper, new BlockPos(1, 1, 1));
		ItemEntity carrot = helper.spawnItem(Items.GOLDEN_CARROT, Vec3.atBottomCenterOf(new BlockPos(6, 1, 5)));
		helper.succeedWhen(() -> {
			helper.assertTrue(zombie.getMainHandItem().is(Items.GOLDEN_CARROT), "zombie holds " + zombie.getMainHandItem());
			helper.assertTrue(carrot.isRemoved(), "carrot still on the ground");
		});
	}

	/** Skeletons grab too, and not only bows: a sword stuck in the ground. */
	@GameTest(environment = "bubbas_reworked_mobs-test:grab_skeleton_sword", maxTicks = 400)
	public void idleSkeletonTakesADisplayedSword(GameTestHelper helper) {
		TestFloors.stone(helper);
		Monster skeleton = mob(helper, ModEntities.woken(UndeadVariant.SKELETON), new BlockPos(1, 1, 1));
		BlockPos sword = new BlockPos(6, 1, 2);
		display(helper, sword, DisplayPlacement.STUCK, Items.IRON_SWORD);
		helper.succeedWhen(() -> {
			helper.assertTrue(skeleton.getMainHandItem().is(Items.IRON_SWORD), "skeleton holds " + skeleton.getMainHandItem());
			helper.assertBlockPresent(Blocks.AIR, sword);
		});
	}

	/** A full hand ignores displays (5 s, five searches), and so do mobs not in #grabs_loot (a vanilla zombie). */
	@GameTest(environment = "bubbas_reworked_mobs-test:grab_full_hand", maxTicks = 120)
	public void mobsThatCantGrabIgnoreDisplays(GameTestHelper helper) {
		TestFloors.stone(helper);
		Monster zombie = zombie(helper, new BlockPos(1, 1, 1));
		zombie.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		BlockPos pick = new BlockPos(4, 1, 1);
		display(helper, pick, DisplayPlacement.GROUND, Items.IRON_PICKAXE);
		Mob vanilla = mob(helper, EntityTypes.ZOMBIE, new BlockPos(1, 1, 6));
		helper.assertFalse(vanilla.is(ModTags.GRABS_LOOT), "vanilla zombie in #grabs_loot");
		BlockPos axe = new BlockPos(3, 1, 6);
		display(helper, axe, DisplayPlacement.GROUND, Items.IRON_AXE);
		helper.runAfterDelay(100, () -> {
			helper.assertBlockPresent(ModBlocks.LOOT_DISPLAY, pick);
			helper.assertTrue(zombie.getMainHandItem().is(Items.STICK), "zombie holds " + zombie.getMainHandItem());
			helper.assertBlockPresent(ModBlocks.LOOT_DISPLAY, axe);
			helper.assertTrue(vanilla.getMainHandItem().isEmpty(), "vanilla zombie holds " + vanilla.getMainHandItem());
			helper.succeed();
		});
	}

	// --- chasing ----------------------------------------------------------------------------------

	/**
	 * While it has a target the zombie walks straight over a display on its way without taking it; once the target
	 * is gone (idle again) it goes back for it.
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:grab_loot_chase", maxTicks = 600)
	public void chasingIgnoresDisplaysIdleGoesBackForThem(GameTestHelper helper) {
		TestFloors.stone(helper);
		Monster zombie = zombie(helper, new BlockPos(0, 1, 4));
		Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(7, 1, 4));
		BlockPos carrot = new BlockPos(3, 1, 4);
		display(helper, carrot, DisplayPlacement.GROUND, Items.GOLDEN_CARROT);
		zombie.setTarget(villager);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(helper.relativeVec(zombie.position()).x > 5.0, "zombie still at " + helper.relativeVec(zombie.position())))
				.thenExecute(() -> {
					helper.assertBlockPresent(ModBlocks.LOOT_DISPLAY, carrot);
					helper.assertTrue(zombie.getMainHandItem().isEmpty(), "took it while chasing: " + zombie.getMainHandItem());
					villager.discard();
					zombie.setTarget(null);
				})
				.thenWaitUntil(() -> {
					helper.assertTrue(zombie.getMainHandItem().is(Items.GOLDEN_CARROT), "zombie holds " + zombie.getMainHandItem());
					helper.assertBlockPresent(Blocks.AIR, carrot);
				})
				.thenSucceed();
	}

	/**
	 * Contact pickup stays vanilla's: a woken mob has vanilla's pickup off (it comes from the natural-spawn roll, which
	 * woken mobs skip), so it walks over a loose sword while chasing and leaves it; with the vanilla flag on it picks
	 * it up on contact even mid-chase.
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:grab_loot_contact", maxTicks = 600)
	public void contactPickupWhileChasingIsVanillas(GameTestHelper helper) {
		TestFloors.stone(helper);
		helper.assertFalse(ModEntities.woken(UndeadVariant.ZOMBIE).create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED)
				.canPickUpLoot(), "woken mobs start with vanilla pickup off");
		Monster zombie = zombie(helper, new BlockPos(0, 1, 4));
		zombie.setCanPickUpLoot(true);
		Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(7, 1, 4));
		ItemEntity sword = helper.spawnItem(Items.IRON_SWORD, Vec3.atBottomCenterOf(new BlockPos(3, 1, 4)));
		zombie.setTarget(villager);
		helper.succeedWhen(() -> {
			helper.assertTrue(zombie.getMainHandItem().is(Items.IRON_SWORD), "zombie holds " + zombie.getMainHandItem());
			helper.assertTrue(sword.isRemoved(), "sword still on the ground");
			helper.assertTrue(zombie.getTarget() == villager, "stopped chasing");
		});
	}
}
