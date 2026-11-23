// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModCreativeTabs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
import io.github.bubba8587.bubbas_reworked_mobs.spawn.MonsterCap;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenSkeleton;
import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenZombie;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperWaking;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.PotentialCalculator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Server game tests for part 1 (undead), driven through the real interaction paths: ServerPlayerGameMode for using,
 * placing and mining, Entity#move for footsteps (which emits the step game event), the game-event dispatcher for
 * hearing, scheduled block ticks for waking. Hearing tests each get their own environment so they run in their own
 * batch and noise from other tests can't reach their sleepers.
 */
public class BubbasReworkedMobsGameTest {
	private static final BlockPos SLEEPER = new BlockPos(1, 1, 4);
	/** Large enough that every heard event or theft wakes; 0 means never. */
	private static final float ALWAYS = 1000.0F;

	// --- helpers --------------------------------------------------------------------------------

	private static SleeperBlockEntity placeSleeper(GameTestHelper helper, UndeadVariant variant, BlockPos pos, SleeperPose pose, float sensitivity) {
		helper.setBlock(pos, ModBlocks.sleeper(variant).defaultBlockState().setValue(SleeperBlock.POSE, pose).setValue(SleeperBlock.FACING, Direction.EAST));
		SleeperBlockEntity sleeper = helper.getBlockEntity(pos, SleeperBlockEntity.class);
		sleeper.setSensitivity(sensitivity);
		return sleeper;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	/** A real player standing on the floor at {@code pos}; remove it with {@link #remove}. */
	private static ServerPlayer standingPlayer(GameTestHelper helper, BlockPos pos, boolean sneaking) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		player.setOnGround(true);
		player.setShiftKeyDown(sneaking);
		return player;
	}

	/** Walks the player along +z through Entity#move, which emits a step game event about once per block. */
	private static void walk(ServerPlayer player, int moves) {
		for (int i = 0; i < moves; i++) {
			player.setOnGround(true);
			player.move(MoverType.SELF, new Vec3(0.0, 0.0, 0.25));
		}
	}

	/**
	 * Records what first woke {@code sleeper} (where the noise came from, and who made it), so a test that fails
	 * because a sleeper woke says why.
	 */
	private static String[] watchWake(GameTestHelper helper, SleeperBlockEntity sleeper) {
		String[] cause = {"nothing"};
		helper.onEachTick(() -> {
			if (cause[0].equals("nothing") && sleeper.isWakePending()) {
				Vec3 from = sleeper.getWakeSource();
				net.minecraft.world.entity.Entity who = sleeper.getWaker() == null ? null : helper.getLevel().getEntity(sleeper.getWaker());
				cause[0] = "noise from " + (from == null ? "?" : helper.relativeVec(from)) + " by " + (who == null ? sleeper.getWaker() : who.getType().toShortString())
						+ " on tick " + helper.getTick();
			}
		});
		return cause;
	}

	private static void remove(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static void useBlock(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction face) {
		BlockPos abs = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false));
	}

	private static List<ItemStack> fullGear() {
		return List.of(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD), new ItemStack(Items.IRON_HELMET),
				new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS));
	}

	private static void giveGear(SleeperBlockEntity sleeper, List<ItemStack> gear) {
		for (int i = 0; i < gear.size(); i++) {
			sleeper.setGear(SleeperBlockEntity.SLOTS.get(i), gear.get(i).copy());
		}
	}

	// --- placing ----------------------------------------------------------------------------------

	// Core test: registered by ReworkedMobsCoreGameTest (test-lite, specs/quality.md).
	public void placedSleeperItemKeepsGearPoseAndFacesPlacer(GameTestHelper helper) {
		helper.setBlock(SLEEPER.below(), Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 stand = helper.absoluteVec(Vec3.atBottomCenterOf(SLEEPER.east(3)));
		player.snapTo(stand.x, stand.y, stand.z, 90.0F, 30.0F); // yaw 90 = looking west, at the sleeper's spot
		ItemStack item = ModCreativeTabs.withPose(new ItemStack(ModItems.sleeper(UndeadVariant.ZOMBIE)), SleeperPose.LYING);
		item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(fullGear()));
		player.setItemInHand(InteractionHand.MAIN_HAND, item);
		useBlock(helper, player, SLEEPER.below(), Direction.UP);

		helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.ZOMBIE), SLEEPER);
		helper.assertBlockProperty(SLEEPER, SleeperBlock.POSE, SleeperPose.LYING);
		helper.assertBlockProperty(SLEEPER, SleeperBlock.FACING, Direction.EAST);
		SleeperBlockEntity sleeper = helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class);
		for (int i = 0; i < SleeperBlockEntity.SLOTS.size(); i++) {
			EquipmentSlot slot = SleeperBlockEntity.SLOTS.get(i);
			helper.assertTrue(sleeper.getGear(slot).is(fullGear().get(i).getItem()), "gear in " + slot + ": " + sleeper.getGear(slot));
		}
		remove(helper, player);
		helper.succeed();
	}

	/**
	 * A sleeper in a crypt wall niche (one block tall under a slab, walled on three sides) wakes standing free in front
	 * of the niche, not stuck in it (owner question, 2026-11-22: "can crypt mobs actually wake up without suffocating
	 * in their walls?").
	 */
	@GameTest(maxTicks = 20)
	public void sleeperInANicheStepsOutWhenItWakes(GameTestHelper helper) {
		floor(helper);
		BlockPos niche = new BlockPos(2, 1, 4);
		helper.setBlock(niche.above(), Blocks.STONE_BRICK_SLAB);
		helper.setBlock(niche.west(), Blocks.STONE_BRICKS);
		helper.setBlock(niche.west().above(), Blocks.STONE_BRICKS);
		helper.setBlock(niche.north(), Blocks.STONE_BRICKS);
		helper.setBlock(niche.north().above(), Blocks.STONE_BRICKS);
		helper.setBlock(niche.south(), Blocks.STONE_BRICKS);
		helper.setBlock(niche.south().above(), Blocks.STONE_BRICKS);
		SleeperBlockEntity sleeper = placeSleeper(helper, UndeadVariant.SKELETON, niche, SleeperPose.LYING, ALWAYS);
		BlockPos abs = helper.absolutePos(niche);
		net.minecraft.world.entity.monster.Monster mob = io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperWaking.wake(
				helper.getLevel(), abs, helper.getLevel().getBlockState(abs), sleeper);
		helper.assertTrue(mob != null, "the sleeper didn't wake");
		helper.assertTrue(helper.getLevel().noCollision(mob, mob.getBoundingBox()), "the woken skeleton is stuck in its niche at " + mob.position());
		helper.assertTrue(mob.blockPosition().equals(abs.east()), "it should step out of the niche's open side, got " + mob.blockPosition());
		mob.discard();
		helper.succeed();
	}

	// --- hearing ----------------------------------------------------------------------------------

	// Core test: registered by ReworkedMobsCoreGameTest (test-lite, specs/quality.md).
	public void walkingNearbyWakesSleeper(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = standingPlayer(helper, new BlockPos(4, 1, 1), false);
		placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, ALWAYS);
		walk(player, 12);
		helper.assertTrue(helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).isWakePending(), "footsteps did not wake the sleeper");
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, SLEEPER);
			helper.assertEntityPresent(ModEntities.woken(UndeadVariant.SKELETON), SLEEPER, 1.0);
			remove(helper, player);
		});
	}

	/** A pig walking right past never wakes a sleeper; a player walking the same path does (so this can't pass by accident). */
	@GameTest(environment = "bubbas_reworked_mobs-test:hearing_animal", maxTicks = 100)
	public void animalsDoNotWakeSleepers(GameTestHelper helper) {
		floor(helper);
		placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, ALWAYS);
		net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(4, 1, 1));
		for (int i = 0; i < 12; i++) {
			pig.setOnGround(true);
			pig.move(MoverType.SELF, new Vec3(0.0, 0.0, 0.25));
		}
		helper.runAfterDelay(30, () -> {
			helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.SKELETON), SLEEPER);
			helper.assertFalse(helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).isWakePending(), "a pig woke the sleeper");
			helper.succeed();
		});
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:hearing_sneak", maxTicks = 60)
	public void sneakingNearbyDoesNotWakeSleeper(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = standingPlayer(helper, new BlockPos(4, 1, 1), true);
		String[] woke = watchWake(helper, placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, ALWAYS));
		walk(player, 12);
		helper.assertTrue(player.isSteppingCarefully(), "player not sneaking");
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(woke[0].equals("nothing"), "the sleeper woke: " + woke[0]);
			helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.SKELETON), SLEEPER);
			helper.assertFalse(helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).isWakePending(), "sneaking woke the sleeper");
			// The same player stops sneaking: now it hears them (so the test above can't pass by accident).
			player.setShiftKeyDown(false);
			walk(player, 8);
			helper.assertTrue(helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).isWakePending(), "walking did not wake the sleeper");
			remove(helper, player);
			helper.succeed();
		});
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:hearing_wool", maxTicks = 60)
	public void woolWallBlocksHearing(GameTestHelper helper) {
		floor(helper);
		for (int y = 1; y < 8; y++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(3, y, z, Blocks.WOOL.white());
			}
		}
		ServerPlayer player = standingPlayer(helper, new BlockPos(5, 1, 1), false);
		String[] woke = watchWake(helper, placeSleeper(helper, UndeadVariant.ZOMBIE, SLEEPER, SleeperPose.LYING, ALWAYS));
		walk(player, 16);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(woke[0].equals("nothing"), "the sleeper woke: " + woke[0]);
			helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.ZOMBIE), SLEEPER);
			helper.assertFalse(helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).isWakePending(), "heard through wool");
			remove(helper, player);
			helper.succeed();
		});
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:hearing_chain")
	public void awakenEventWakesAtMostTheCap(GameTestHelper helper) {
		List<BlockPos> ring = List.of(new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1), new BlockPos(1, 1, 3),
				new BlockPos(5, 1, 3), new BlockPos(1, 1, 5), new BlockPos(3, 1, 5), new BlockPos(5, 1, 5));
		List<SleeperBlockEntity> sleepers = ring.stream().map(p -> placeSleeper(helper, UndeadVariant.ZOMBIE, p, SleeperPose.SITTING, ALWAYS)).toList();
		Monster waker = helper.spawnWithNoFreeWill(ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(3, 1, 3));
		SleeperWaking.emitAwaken(helper.getLevel(), waker);
		long waking = sleepers.stream().filter(SleeperBlockEntity::isWakePending).count();
		helper.assertValueEqual(waking, (long) UndeadTuning.AWAKEN_CAP, "sleepers woken by one awaken event");
		helper.succeed();
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:hearing_placed", maxTicks = 100)
	public void sleeperPlacedFromItemWakesLikeAnyOther(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = standingPlayer(helper, new BlockPos(4, 1, 0), false);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.sleeper(UndeadVariant.ZOMBIE)));
		useBlock(helper, player, SLEEPER.below(), Direction.UP);
		helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.ZOMBIE), SLEEPER);
		helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).setSensitivity(ALWAYS);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		// Walk at once: its own placement must not count as a noise to it.
		walk(player, 12);
		helper.assertTrue(helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).isWakePending(), "placed sleeper did not hear the steps");
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, SLEEPER);
			helper.assertEntityPresent(ModEntities.woken(UndeadVariant.ZOMBIE), SLEEPER, 1.0);
			remove(helper, player);
		});
	}

	// --- waking -----------------------------------------------------------------------------------

	// Core test: registered by ReworkedMobsCoreGameTest (test-lite, specs/quality.md).
	public void wakeSpawnsTunedMobWithGearFacingTheSource(GameTestHelper helper) {
		SleeperBlockEntity sleeper = placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, 0.0F);
		giveGear(sleeper, fullGear());
		Vec3 source = helper.absoluteVec(Vec3.atBottomCenterOf(SLEEPER.south(3))); // +z of the sleeper
		sleeper.scheduleWake(helper.getLevel(), source, null, 2);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, SLEEPER);
			WokenSkeleton mob = (WokenSkeleton) helper.findOneEntity(ModEntities.woken(UndeadVariant.SKELETON));
			for (int i = 0; i < SleeperBlockEntity.SLOTS.size(); i++) {
				EquipmentSlot slot = SleeperBlockEntity.SLOTS.get(i);
				helper.assertTrue(mob.getItemBySlot(slot).is(fullGear().get(i).getItem()), "mob " + slot + ": " + mob.getItemBySlot(slot));
			}
			helper.assertTrue(mob.isPersistenceRequired(), "woken mob should not despawn");
			helper.assertValueInBetween(-1.0F, net.minecraft.util.Mth.wrapDegrees(mob.getYRot()), 1.0F, "yaw toward +z (0)");
			helper.assertItemEntityNotPresent(Items.IRON_SWORD); // the gear went on the mob, not on the floor
		});
	}

	@GameTest
	public void wokenMobsAreTunedAndUndead(GameTestHelper helper) {
		Monster skeleton = helper.spawnWithNoFreeWill(ModEntities.woken(UndeadVariant.SKELETON), SLEEPER);
		Monster zombie = helper.spawnWithNoFreeWill(ModEntities.woken(UndeadVariant.ZOMBIE), SLEEPER.east());
		helper.assertValueEqual(skeleton.getAttributeValue(Attributes.MAX_HEALTH), UndeadTuning.SKELETON_HEALTH, "skeleton health");
		helper.assertValueEqual(skeleton.getHealth(), (float) UndeadTuning.SKELETON_HEALTH, "skeleton starts at full health");
		helper.assertValueEqual(skeleton.getAttributeValue(Attributes.MOVEMENT_SPEED), UndeadTuning.SKELETON_SPEED, "skeleton speed");
		helper.assertValueEqual(skeleton.getAttributeValue(Attributes.ATTACK_DAMAGE), UndeadTuning.FIST_DAMAGE, "skeleton fists");
		helper.assertValueEqual(zombie.getAttributeValue(Attributes.MAX_HEALTH), UndeadTuning.ZOMBIE_HEALTH, "zombie health");
		helper.assertValueEqual(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED), UndeadTuning.ZOMBIE_SPEED, "zombie speed");
		helper.assertValueEqual(zombie.getAttributeValue(Attributes.ATTACK_DAMAGE), UndeadTuning.FIST_DAMAGE, "zombie fists");
		helper.assertTrue(skeleton instanceof WokenSkeleton && zombie instanceof WokenZombie, "woken classes");
		helper.assertTrue(skeleton.is(EntityTypeTags.UNDEAD) && zombie.is(EntityTypeTags.UNDEAD), "woken mobs are #minecraft:undead");
		helper.assertTrue(skeleton.is(EntityTypeTags.SKELETONS) && zombie.is(EntityTypeTags.ZOMBIES), "family tags");
		helper.assertTrue(skeleton.is(ModTags.WOKEN_UNDEAD), "#woken_undead");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void wakingZombiePicksUpNearbyWeapon(GameTestHelper helper) {
		helper.setBlock(SLEEPER.below(), Blocks.STONE);
		helper.setBlock(SLEEPER.below().east(), Blocks.STONE);
		SleeperBlockEntity sleeper = placeSleeper(helper, UndeadVariant.ZOMBIE, SLEEPER, SleeperPose.SITTING, 0.0F);
		ItemEntity sword = helper.spawnItem(Items.IRON_AXE, Vec3.atBottomCenterOf(SLEEPER.east()));
		sleeper.scheduleWake(helper.getLevel(), null, null, 1);
		helper.succeedWhen(() -> {
			Monster mob = (Monster) helper.findOneEntity(ModEntities.woken(UndeadVariant.ZOMBIE));
			helper.assertTrue(mob.getMainHandItem().is(Items.IRON_AXE), "zombie did not pick up the axe: " + mob.getMainHandItem());
			helper.assertTrue(sword.isRemoved(), "axe still on the ground");
		});
	}

	@GameTest(maxTicks = 30)
	public void refusedSpawnKeepsTheSleeper(GameTestHelper helper) {
		SleeperBlockEntity sleeper = placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, 0.0F);
		giveGear(sleeper, fullGear());
		sleeper.setComponents(net.minecraft.core.component.DataComponentMap.builder().addAll(sleeper.components())
				.set(DataComponents.CUSTOM_NAME, Component.literal(RefuseSpawnName.VALUE)).build());
		sleeper.scheduleWake(helper.getLevel(), null, null, 1);
		helper.runAfterDelay(5, () -> {
			helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.SKELETON), SLEEPER);
			helper.assertEntityNotPresent(ModEntities.woken(UndeadVariant.SKELETON));
			helper.assertTrue(helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class).getGear(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "gear kept");
			helper.succeed();
		});
	}

	// --- robbing ----------------------------------------------------------------------------------

	@GameTest
	public void robbingTakesWeaponFirstThenArmorTopDown(GameTestHelper helper) {
		SleeperBlockEntity sleeper = placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, 0.0F);
		giveGear(sleeper, fullGear());
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		List<ItemStack> expected = fullGear();
		for (ItemStack next : expected) {
			useBlock(helper, player, SLEEPER, Direction.NORTH);
			helper.assertTrue(player.getInventory().contains(next), "robbing order: expected " + next + " next");
		}
		helper.assertFalse(sleeper.hasGear(), "gear left after six thefts");
		helper.assertFalse(sleeper.isWakePending(), "sensitivity 0 sleeper woke");
		helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.SKELETON), SLEEPER);
		remove(helper, player);
		helper.succeed();
	}

	@GameTest
	public void robbingRollsWakeChanceArmorRiskier(GameTestHelper helper) {
		helper.assertTrue(UndeadTuning.ROB_ARMOR_WAKE_CHANCE > UndeadTuning.ROB_WEAPON_WAKE_CHANCE, "armor must be riskier than the weapon");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// Sensitivity that makes armor theft certain but leaves weapon theft at a third.
		float armorCertain = 1.0F / UndeadTuning.ROB_ARMOR_WAKE_CHANCE + 0.01F;
		SleeperBlockEntity armored = placeSleeper(helper, UndeadVariant.ZOMBIE, SLEEPER, SleeperPose.LYING, armorCertain);
		armored.setGear(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		useBlock(helper, player, SLEEPER, Direction.UP);
		helper.assertTrue(armored.isWakePending(), "armor theft did not roll a wake");
		helper.assertTrue(player.getInventory().contains(new ItemStack(Items.IRON_CHESTPLATE)), "armor not taken");

		BlockPos other = SLEEPER.east(3);
		SleeperBlockEntity armed = placeSleeper(helper, UndeadVariant.SKELETON, other, SleeperPose.SITTING, 1.0F / UndeadTuning.ROB_WEAPON_WAKE_CHANCE + 0.01F);
		armed.setGear(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
		useBlock(helper, player, other, Direction.UP);
		helper.assertTrue(armed.isWakePending(), "weapon theft did not roll a wake");
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(ModEntities.woken(UndeadVariant.ZOMBIE), SLEEPER, 1.5);
			remove(helper, player);
		});
	}

	// --- mining -----------------------------------------------------------------------------------

	// Core test: registered by ReworkedMobsCoreGameTest (test-lite, specs/quality.md).
	public void miningWithSilkTouchPickaxeGivesSleeperItemAndDropsGear(GameTestHelper helper) {
		SleeperBlockEntity sleeper = placeSleeper(helper, UndeadVariant.ZOMBIE, SLEEPER, SleeperPose.LYING, 0.0F);
		giveGear(sleeper, fullGear());
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, silkTouch(helper, Items.IRON_PICKAXE));
		player.gameMode.destroyBlock(helper.absolutePos(SLEEPER));
		helper.assertBlockPresent(Blocks.AIR, SLEEPER);
		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(SLEEPER)).inflate(2.0));
		ItemEntity sleeperItem = drops.stream().filter(e -> e.getItem().is(ModItems.sleeper(UndeadVariant.ZOMBIE))).findFirst().orElse(null);
		helper.assertTrue(sleeperItem != null, "no sleeper item in " + drops);
		ItemContainerContents contents = sleeperItem.getItem().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		helper.assertTrue(contents.nonEmptyItemCopyStream().findAny().isEmpty(), "sleeper item carries gear");
		helper.assertTrue(sleeperItem.getItem().get(DataComponents.BLOCK_STATE) != null, "sleeper item lost its pose");
		for (ItemStack gear : fullGear()) {
			helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(gear.getItem())), "gear not dropped: " + gear);
		}
		helper.succeed();
	}

	/** Without Silk Touch, or with Silk Touch on anything but a pickaxe, a sleeper leaves remains (and its gear). */
	@GameTest
	public void miningWithoutSilkTouchPickaxeLeavesRemains(GameTestHelper helper) {
		for (ItemStack tool : List.of(new ItemStack(Items.IRON_PICKAXE), silkTouch(helper, Items.IRON_AXE), silkTouch(helper, Items.IRON_SHOVEL))) {
			placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, 0.0F);
			ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, tool);
			player.gameMode.destroyBlock(helper.absolutePos(SLEEPER));
			helper.assertItemEntityNotPresent(ModItems.sleeper(UndeadVariant.SKELETON), SLEEPER, 2.0);
			helper.assertItemEntityPresent(Items.BONE, SLEEPER, 2.0);
			helper.killAllEntitiesOfClass(ItemEntity.class);
		}
		helper.succeed();
	}

	private static ItemStack silkTouch(GameTestHelper helper, net.minecraft.world.item.Item item) {
		ItemStack stack = new ItemStack(item);
		stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
		return stack;
	}

	@GameTest
	public void miningBareHandedLeavesRemains(GameTestHelper helper) {
		placeSleeper(helper, UndeadVariant.SKELETON, SLEEPER, SleeperPose.SITTING, 0.0F);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.gameMode.destroyBlock(helper.absolutePos(SLEEPER));
		helper.assertItemEntityPresent(Items.BONE, SLEEPER, 2.0);
		helper.assertItemEntityNotPresent(ModItems.sleeper(UndeadVariant.SKELETON), SLEEPER, 2.0);
		helper.succeed();
	}

	// --- natural spawning -------------------------------------------------------------------------

	@GameTest
	public void monsterCapRuleScalesTheCap(GameTestHelper helper) throws ReflectiveOperationException {
		ServerLevel level = helper.getLevel();
		int chunks = 17 * 17; // one player's worth: vanilla cap 70
		helper.assertValueEqual(MonsterCap.scaledGlobalCap(level, chunks), 70 * 15 / 100, "default cap (15 %)");

		// The real natural-spawning check (NaturalSpawner$SpawnState#canSpawn, where the mixin sits), with a chosen monster count.
		Class<?> stateClass = Class.forName("net.minecraft.world.level.NaturalSpawner$SpawnState");
		Constructor<?> ctor = stateClass.getDeclaredConstructor(int.class, Object2IntOpenHashMap.class, PotentialCalculator.class, LocalMobCapCalculator.class);
		ctor.setAccessible(true);
		Method canSpawn = stateClass.getDeclaredMethod("canSpawn", EntityType.class, Level.class, BlockPos.class, ChunkAccess.class);
		canSpawn.setAccessible(true);
		BlockPos pos = helper.absolutePos(SLEEPER);
		ChunkAccess chunk = level.getChunk(pos);
		java.util.function.IntFunction<Object> stateWith = monsters -> {
			Object2IntOpenHashMap<MobCategory> counts = new Object2IntOpenHashMap<>();
			counts.put(MobCategory.MONSTER, monsters);
			try {
				return ctor.newInstance(chunks, counts, new PotentialCalculator(), new LocalMobCapCalculator(level.getChunkSource().chunkMap));
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		};
		helper.assertTrue((Boolean) canSpawn.invoke(stateWith.apply(9), EntityTypes.ZOMBIE, level, pos, chunk), "9 monsters: under the cut cap");
		helper.assertFalse((Boolean) canSpawn.invoke(stateWith.apply(10), EntityTypes.ZOMBIE, level, pos, chunk), "10 monsters: at the cut cap");
		helper.assertTrue((Boolean) canSpawn.invoke(stateWith.apply(60), EntityTypes.PIG, level, pos, chunk), "other categories untouched");

		boolean before = level.getGameRules().get(ModGameRules.CUT_MONSTER_SPAWNS);
		level.getGameRules().set(ModGameRules.CUT_MONSTER_SPAWNS, false, level.getServer());
		try {
			helper.assertTrue((Boolean) canSpawn.invoke(stateWith.apply(60), EntityTypes.ZOMBIE, level, pos, chunk), "cut switched off: vanilla cap");
		} finally {
			level.getGameRules().set(ModGameRules.CUT_MONSTER_SPAWNS, before, level.getServer());
		}
		helper.assertValueEqual(MonsterCap.scaledGlobalCap(level, chunks * 2), 140 * 15 / 100, "cap scales with spawnable chunks");
		helper.succeed();
	}
}
