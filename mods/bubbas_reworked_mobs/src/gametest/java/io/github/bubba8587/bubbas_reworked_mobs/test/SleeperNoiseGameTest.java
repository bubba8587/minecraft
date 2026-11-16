// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import com.mojang.authlib.GameProfile;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperWaking;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The disturbance meter (specs/undead.md "Hearing and waking"), one test per owner target, all at default
 * sensitivity and fully deterministic (no dice). Players move through Entity#move once per tick at vanilla speeds
 * (walking 0.216 blocks a tick, sprinting 0.28), so footsteps come at the real rate and the meter decays between them.
 * The field is 24 x 16 blocks of stone floor; each test has its own environment so its batch is alone.
 */
public class SleeperNoiseGameTest {
	private static final String FIELD = "bubbas_reworked_mobs-test:hearing_field";
	private static final double WALK = 0.216;
	private static final double SPRINT = 0.28;

	// --- helpers --------------------------------------------------------------------------------

	private static SleeperBlockEntity sleeper(GameTestHelper helper, UndeadVariant variant, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.sleeper(variant).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.SITTING)
				.setValue(SleeperBlock.FACING, Direction.EAST));
		return helper.getBlockEntity(pos, SleeperBlockEntity.class);
	}

	/** A real survival player in the level (the helper's own in-level mock player is creative). */
	static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-survivor"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		return player;
	}

	private static ServerPlayer at(GameTestHelper helper, ServerPlayer player, double x, double z) {
		Vec3 abs = helper.absoluteVec(new Vec3(x, 1.0, z));
		player.snapTo(abs.x, abs.y, abs.z, 0.0F, 0.0F);
		player.setOnGround(true);
		return player;
	}

	private static void remove(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private record Run(int wokeAt, float peak) {
	}

	/** A stretch of a test: move (or stand, motion null) for some ticks, then report. */
	private record Phase(int ticks, @org.jspecify.annotations.Nullable ServerPlayer player, @org.jspecify.annotations.Nullable SleeperBlockEntity sleeper,
			@org.jspecify.annotations.Nullable IntFunction<Vec3> motion, java.util.function.Consumer<Run> end) {
	}

	/**
	 * Each test's queue of phases, run from one onEachTick. The helper's tick callbacks can't schedule more callbacks
	 * while they run, so later phases (queued from inside a phase's end) go on this queue instead.
	 */
	private static final java.util.Map<GameTestHelper, java.util.ArrayDeque<Phase>> QUEUES = new java.util.WeakHashMap<>();

	private static void enqueue(GameTestHelper helper, Phase phase) {
		java.util.ArrayDeque<Phase> queue = QUEUES.get(helper);
		if (queue != null) {
			queue.add(phase);
			return;
		}
		java.util.ArrayDeque<Phase> fresh = new java.util.ArrayDeque<>();
		fresh.add(phase);
		QUEUES.put(helper, fresh);
		int[] state = {0, -1}; // tick in phase, woke at
		float[] peak = {0.0F};
		helper.onEachTick(() -> {
			Phase current = fresh.peek();
			if (current == null) {
				return;
			}
			state[0]++;
			if (current.motion() != null) {
				current.player().setOnGround(true);
				current.player().move(MoverType.SELF, current.motion().apply(state[0]));
			}
			if (current.sleeper() != null) {
				peak[0] = Math.max(peak[0], current.sleeper().getDisturbance(helper.getLevel().getGameTime()));
				if (state[1] < 0 && current.sleeper().isWakePending()) {
					state[1] = state[0];
				}
			}
			if (state[0] >= current.ticks()) {
				Run run = new Run(state[1], peak[0]);
				fresh.poll();
				state[0] = 0;
				state[1] = -1;
				peak[0] = 0.0F;
				current.end().accept(run);
			}
		});
	}

	/** Moves the player by {@code motion.apply(tick)} once a tick for {@code ticks} ticks (from 1), after anything queued before. */
	private static void drive(GameTestHelper helper, ServerPlayer player, SleeperBlockEntity sleeper, int ticks, IntFunction<Vec3> motion,
			java.util.function.Consumer<Run> done) {
		enqueue(helper, new Phase(ticks, player, sleeper, motion, done));
	}

	/** Runs {@code action} {@code ticks} ticks after anything queued before (usable inside a phase, unlike runAfterDelay). */
	private static void later(GameTestHelper helper, int ticks, Runnable action) {
		enqueue(helper, new Phase(ticks, null, null, null, run -> action.run()));
	}

	private static IntFunction<Vec3> straight(double speed) {
		return tick -> new Vec3(speed, 0.0, 0.0);
	}

	// --- reckless play close by wakes them fast ---------------------------------------------------

	/** Sprinting in a circle 6 blocks out wakes it within about a second (four sprint steps). A survival waker becomes the target. */
	/**
	 * Milestone: a glowstone next to a sleeper calms it (x0.7), through the real light engine. That engine now and then
	 * never lights a block in a fresh test chunk (about one run in five), so it gets three attempts.
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:warding_light", structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 600, maxAttempts = 3)
	public void strongLightCalmsSleepersInWorld(GameTestHelper helper) {
		new SleeperCalmGameTest().strongLightCalmsSleepers(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:noise_sprint_circle", structure = FIELD, maxTicks = 120)
	public void sprintingCirclesWakeWithinASecond(GameTestHelper helper) {
		BlockPos pos = new BlockPos(12, 1, 8);
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.ZOMBIE, pos);
		ServerPlayer player = at(helper, survivalPlayer(helper), 18.5, 8.5);
		player.setSprinting(true);
		Vec3 centre = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
		double radius = 6.0;
		IntFunction<Vec3> circle = tick -> {
			double angle = tick * SPRINT / radius;
			Vec3 target = centre.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
			return target.subtract(player.position());
		};
		drive(helper, player, sleeper, 40, circle, run -> {
			helper.assertTrue(run.wokeAt() > 0 && run.wokeAt() <= 30, "sprinting circles at 6 blocks: woke at tick " + run.wokeAt() + " (want <= 30)");
			later(helper, 10, () -> {
				Monster mob = (Monster) helper.findOneEntity(ModEntities.woken(UndeadVariant.ZOMBIE));
				helper.assertTrue(mob.getTarget() == player, "a survival player who wakes it is its target: " + mob.getTarget());
				remove(helper, player);
				helper.succeed();
			});
		});
	}

	/** Sprinting straight past 6 blocks out wakes it before the player is 3 blocks past. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_sprint_pass", structure = FIELD, maxTicks = 120)
	public void sprintingPastWakes(GameTestHelper helper) {
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.SKELETON, new BlockPos(12, 1, 2));
		ServerPlayer player = at(helper, survivalPlayer(helper), 0.5, 8.5);
		player.setSprinting(true);
		drive(helper, player, sleeper, 80, straight(SPRINT), run -> {
			int passed3 = (int) Math.ceil((15.5 - 0.5) / SPRINT); // the tick the player is 3 blocks past the sleeper
			helper.assertTrue(run.wokeAt() > 0 && run.wokeAt() <= passed3, "sprint pass at 6: woke at tick " + run.wokeAt() + " (want <= " + passed3 + ")");
			remove(helper, player);
			helper.succeed();
		});
	}

	/** Breaking a block 4 blocks away doesn't wake it alone; the second one does. Placing: the same. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_blocks", structure = FIELD, maxTicks = 40)
	public void breakingOrPlacingTwoBlocksNearbyWakes(GameTestHelper helper) {
		helper.setBlock(16, 1, 2, Blocks.STONE);
		helper.setBlock(16, 1, 3, Blocks.STONE);
		SleeperBlockEntity breaking = sleeper(helper, UndeadVariant.SKELETON, new BlockPos(12, 1, 2));
		SleeperBlockEntity placing = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 13));
		ServerPlayer player = at(helper, helper.makeMockServerPlayerInLevel(), 20.5, 8.5);
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(16, 1, 2)));
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(16, 1, 2));
		helper.assertFalse(breaking.isWakePending(), "one block broken 4 blocks away woke it");
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(16, 1, 3)));
		helper.assertTrue(breaking.isWakePending(), "two blocks broken 4 blocks away did not wake it: " + breaking.getDisturbance(helper.getLevel().getGameTime()));

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 8));
		place(helper, player, new BlockPos(16, 0, 13));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(16, 1, 13));
		helper.assertFalse(placing.isWakePending(), "one block placed 4 blocks away woke it");
		place(helper, player, new BlockPos(16, 0, 12));
		helper.assertTrue(placing.isWakePending(), "two blocks placed 4 blocks away did not wake it: " + placing.getDisturbance(helper.getLevel().getGameTime()));
		remove(helper, player);
		helper.succeed();
	}

	private static void place(GameTestHelper helper, ServerPlayer player, BlockPos onTopOf) {
		BlockPos abs = helper.absolutePos(onTopOf);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.UP, 0.5), Direction.UP, abs, false));
	}

	/** Jumping on the spot 3.5 blocks away: two landings don't wake it, the third does. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_jumps", structure = FIELD, maxTicks = 60)
	public void jumpingNearbyWakesWithinThreeLandings(GameTestHelper helper) {
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 2));
		ServerPlayer player = at(helper, helper.makeMockServerPlayerInLevel(), 16.0, 2.5);
		// A player's landing is checked from its movement packets (the client is authoritative), so do what the
		// packet handler does: fall 1.25 blocks and report it on the ground.
		Runnable jump = () -> {
			player.setOnGround(false);
			player.doCheckFallDamage(0.0, -1.25, 0.0, true);
		};
		jump.run();
		later(helper, 12, () -> {
			jump.run();
			helper.assertFalse(sleeper.isWakePending(), "two landings woke it: " + sleeper.getDisturbance(helper.getLevel().getGameTime()));
			later(helper, 12, () -> {
				jump.run();
				helper.assertTrue(sleeper.isWakePending(), "three landings 3.5 blocks away did not wake it: " + sleeper.getDisturbance(helper.getLevel().getGameTime()));
				remove(helper, player);
				helper.succeed();
			});
		});
	}

	/** Hitting a pig 3 blocks away twice wakes it: fighting is loud, even though the pig's own footsteps never are. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_fight", structure = FIELD, maxTicks = 60)
	public void fightingNearbyIsLoud(GameTestHelper helper) {
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.SKELETON, new BlockPos(12, 1, 2));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(15, 1, 2));
		ServerPlayer player = at(helper, helper.makeMockServerPlayerInLevel(), 19.5, 2.5);
		ServerLevel level = helper.getLevel();
		pig.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertFalse(sleeper.isWakePending(), "one hit woke it");
		helper.runAfterDelay(12, () -> { // past the pig's hurt cooldown
			pig.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
			helper.assertTrue(sleeper.isWakePending(), "two hits 3 blocks away did not wake it: " + sleeper.getDisturbance(level.getGameTime()));
			remove(helper, player);
			helper.succeed();
		});
	}

	/** An explosion 8 blocks away wakes it at once; one 15 blocks from another sleeper does nothing to that one. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_explosion", structure = FIELD)
	public void explosionNearbyWakesAtOnce(GameTestHelper helper) {
		SleeperBlockEntity near = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 2));
		SleeperBlockEntity far = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 15));
		helper.getLevel().gameEvent(GameEvent.EXPLODE, helper.absoluteVec(new Vec3(20.5, 1.5, 2.5)), GameEvent.Context.of((Entity) null));
		helper.assertTrue(near.isWakePending(), "explosion 8 blocks away did not wake it");
		helper.assertFalse(far.isWakePending(), "explosion 15 blocks away woke it");
		helper.assertValueEqual(far.getDisturbance(helper.getLevel().getGameTime()), 0.0F, "far sleeper's meter");
		helper.succeed();
	}

	// --- walking ----------------------------------------------------------------------------------

	/** Walking right past, 3 blocks out, wakes it within a couple of seconds. The owner tests in creative: creative steps count. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_walk_near", structure = FIELD, maxTicks = 120)
	public void walkingPastAt3WakesWithinTwoSeconds(GameTestHelper helper) {
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.SKELETON, new BlockPos(12, 1, 2));
		ServerPlayer player = at(helper, helper.makeMockServerPlayerInLevel(), 6.5, 5.5); // 6 blocks before, 3 out
		helper.assertTrue(player.isCreative() && !player.getAbilities().flying, "creative player on foot");
		drive(helper, player, sleeper, 70, straight(WALK), run -> {
			helper.assertTrue(run.wokeAt() > 0 && run.wokeAt() <= 50, "walk pass at 3: woke at tick " + run.wokeAt() + " (want <= 50)");
			later(helper, 10, () -> {
				Monster mob = (Monster) helper.findOneEntity(ModEntities.woken(UndeadVariant.SKELETON));
				helper.assertTrue(mob.getTarget() == null, "a creative waker is not targeted: " + mob.getTarget());
				remove(helper, player);
				helper.succeed();
			});
		});
	}

	/** Walking past 7 blocks out doesn't wake it, nor does sprinting past 11 out (another hallway). */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_walk_far", structure = FIELD, maxTicks = 240)
	public void walkingAt7OrSprintingAt11DoesNotWake(GameTestHelper helper) {
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 2));
		ServerPlayer player = at(helper, survivalPlayer(helper), 0.5, 9.5);
		drive(helper, player, sleeper, 100, straight(WALK), walk -> {
			helper.assertTrue(walk.wokeAt() < 0, "walking past at 7 woke it (peak " + walk.peak() + ")");
			helper.assertTrue(walk.peak() > 0.0F, "it heard nothing at all: the test can't tell");
			at(helper, player, 0.5, 13.5);
			player.setSprinting(true);
			drive(helper, player, sleeper, 80, straight(SPRINT), sprint -> {
				helper.assertTrue(sprint.wokeAt() < 0, "sprinting past at 11 woke it (peak " + sprint.peak() + ")");
				remove(helper, player);
				helper.succeed();
			});
		});
	}

	/** Sneaking past 2 blocks out is silent (vanilla's rule); the same path walking wakes it. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_sneak", structure = FIELD, maxTicks = 200)
	public void sneakingPastAt2IsSilent(GameTestHelper helper) {
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 2));
		ServerPlayer player = at(helper, survivalPlayer(helper), 0.5, 4.5);
		player.setShiftKeyDown(true);
		drive(helper, player, sleeper, 70, straight(0.065), sneak -> { // vanilla sneaking speed
			helper.assertTrue(player.isSteppingCarefully(), "player not sneaking");
			helper.assertTrue(sneak.wokeAt() < 0 && sneak.peak() == 0.0F, "sneaking was heard (peak " + sneak.peak() + ")");
			player.setShiftKeyDown(false);
			drive(helper, player, sleeper, 60, straight(WALK), walk -> {
				helper.assertTrue(walk.wokeAt() > 0, "walking the same path 2 blocks out did not wake it (peak " + walk.peak() + ")");
				remove(helper, player);
				helper.succeed();
			});
		});
	}

	/**
	 * One footstep 5 blocks away does next to nothing; walking past 5 blocks out stirs it (it groans) but doesn't wake
	 * it, and it settles completely within 10 s.
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_single_step", structure = FIELD, maxTicks = 400)
	public void smallNoisesSettle(GameTestHelper helper) {
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.SKELETON, new BlockPos(12, 1, 2));
		ServerPlayer player = at(helper, survivalPlayer(helper), 11.0, 7.5);
		drive(helper, player, sleeper, 8, straight(WALK), step -> { // 1.7 blocks: exactly one footstep
			helper.assertTrue(step.peak() > 0.0F && step.peak() < UndeadTuning.STIR_THRESHOLD, "one step at 5 blocks: meter " + step.peak());
			at(helper, player, 0.5, 7.5);
			drive(helper, player, sleeper, 100, straight(WALK), pass -> {
				helper.assertTrue(pass.wokeAt() < 0, "walking past at 5 woke it (peak " + pass.peak() + ")");
				helper.assertTrue(pass.peak() >= UndeadTuning.STIR_THRESHOLD, "walking past at 5 should stir it: peak " + pass.peak());
				long settled = helper.getLevel().getGameTime() + 200;
				helper.assertValueEqual(sleeper.getDisturbance(settled), 0.0F, "meter 10 s later");
				remove(helper, player);
				helper.succeed();
			});
		});
	}

	// --- walls and chains ---------------------------------------------------------------------------

	/** Sprinting past 5 blocks out wakes it in the open (see sprintingPastWakes); behind a 2-thick stone wall it doesn't. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_walls", structure = FIELD, maxTicks = 120)
	public void solidWallsMuffle(GameTestHelper helper) {
		for (int x = 0; x < 24; x++) {
			for (int y = 1; y < 4; y++) {
				helper.setBlock(x, y, 4, Blocks.STONE);
				helper.setBlock(x, y, 5, Blocks.STONE);
			}
		}
		SleeperBlockEntity sleeper = sleeper(helper, UndeadVariant.SKELETON, new BlockPos(12, 1, 2));
		ServerPlayer player = at(helper, survivalPlayer(helper), 0.5, 7.5);
		player.setSprinting(true);
		drive(helper, player, sleeper, 80, straight(SPRINT), run -> {
			helper.assertTrue(run.wokeAt() < 0, "heard through 2 blocks of stone (peak " + run.peak() + ")");
			helper.assertTrue(run.peak() > 0.0F, "heard nothing at all: the test can't tell");
			remove(helper, player);
			helper.succeed();
		});
	}

	/** One neighbour waking wakes sleepers within about 6 blocks at once (default sensitivity), not ones 10 blocks off. */
	@GameTest(environment = "bubbas_reworked_mobs-test:noise_awaken", structure = FIELD)
	public void awakenWakesNearNeighbours(GameTestHelper helper) {
		List<SleeperBlockEntity> near = List.of(sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(10, 1, 2)),
				sleeper(helper, UndeadVariant.SKELETON, new BlockPos(14, 1, 2)), sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 7)));
		SleeperBlockEntity far = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 13));
		Monster waker = helper.spawnWithNoFreeWill(ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(12, 1, 2));
		SleeperWaking.emitAwaken(helper.getLevel(), waker);
		for (SleeperBlockEntity sleeper : near) {
			helper.assertTrue(sleeper.isWakePending(), "neighbour at " + sleeper.getBlockPos() + " did not wake");
		}
		helper.assertFalse(far.isWakePending(), "a sleeper 10.5 blocks away woke");
		helper.succeed();
	}

	// --- no wake-up violence ------------------------------------------------------------------------

	/** A group woken by the chain (each by the one before) doesn't fight: nobody takes an undead as its target. */
	@GameTest(environment = "bubbas_reworked_mobs-test:wake_chain_no_infighting", structure = FIELD, maxTicks = 120)
	public void chainWokenUndeadDoNotFightEachOther(GameTestHelper helper) {
		List<BlockPos> group = List.of(new BlockPos(10, 1, 2), new BlockPos(14, 1, 2), new BlockPos(12, 1, 5), new BlockPos(10, 1, 5),
				new BlockPos(14, 1, 5));
		for (int i = 0; i < group.size(); i++) {
			sleeper(helper, i % 2 == 0 ? UndeadVariant.ZOMBIE : UndeadVariant.SKELETON, group.get(i));
		}
		SleeperBlockEntity first = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(12, 1, 2));
		first.scheduleWake(helper.getLevel(), null, null, 1);
		helper.runAfterDelay(60, () -> {
			List<Monster> woken = helper.getLevel().getEntitiesOfClass(Monster.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO))
					.expandTowards(24, 4, 16), m -> m.is(ModTags.WOKEN_UNDEAD));
			helper.assertValueEqual(woken.size(), group.size() + 1, "woken undead");
			for (Monster mob : woken) {
				LivingEntity target = mob.getTarget();
				helper.assertTrue(target == null || !target.is(EntityTypeTags.UNDEAD), mob + " targets fellow undead " + target);
			}
			helper.succeed();
		});
	}

	/** Woken by a survival player: targets them. Woken by a creative player or by another mob: no target. */
	@GameTest(environment = "bubbas_reworked_mobs-test:wake_targets_survival", structure = FIELD, maxTicks = 40)
	public void onlyASurvivalWakerIsTargeted(GameTestHelper helper) {
		ServerPlayer survivor = at(helper, survivalPlayer(helper), 3.5, 2.5);
		ServerPlayer creative = at(helper, helper.makeMockServerPlayerInLevel(), 3.5, 8.5);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 13));
		zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		SleeperBlockEntity bySurvivor = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(20, 1, 2));
		SleeperBlockEntity byCreative = sleeper(helper, UndeadVariant.ZOMBIE, new BlockPos(20, 1, 8));
		SleeperBlockEntity byMob = sleeper(helper, UndeadVariant.SKELETON, new BlockPos(20, 1, 13));
		bySurvivor.scheduleWake(helper.getLevel(), survivor.position(), survivor.getUUID(), 1);
		byCreative.scheduleWake(helper.getLevel(), creative.position(), creative.getUUID(), 1);
		byMob.scheduleWake(helper.getLevel(), zombie.position(), zombie.getUUID(), 1);
		helper.runAfterDelay(3, () -> {
			Monster a = (Monster) helper.getLevel().getEntitiesOfClass(Monster.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(20, 1, 2))).inflate(1.0),
					m -> m.is(ModTags.WOKEN_UNDEAD)).getFirst();
			Monster b = (Monster) helper.getLevel().getEntitiesOfClass(Monster.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(20, 1, 8))).inflate(1.0),
					m -> m.is(ModTags.WOKEN_UNDEAD)).getFirst();
			Monster c = (Monster) helper.findOneEntity(ModEntities.woken(UndeadVariant.SKELETON));
			helper.assertTrue(a.getTarget() == survivor, "survival waker not targeted: " + a.getTarget());
			helper.assertTrue(b.getTarget() != creative, "creative waker targeted");
			helper.assertTrue(c.getTarget() != zombie, "a mob waker (vanilla zombie) targeted");
			remove(helper, survivor);
			remove(helper, creative);
			helper.succeed();
		});
	}

	/** Vanilla retaliation stays (owner): a woken zombie hit by a skeleton fights back. */
	@GameTest(environment = "bubbas_reworked_mobs-test:wake_retaliation", structure = FIELD, maxTicks = 60)
	public void retaliationAgainstUndeadStaysVanilla(GameTestHelper helper) {
		Mob zombie = helper.spawn(ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(10, 1, 8));
		zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		Mob skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(14, 1, 8));
		skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		// Not on its first tick: HurtByTargetGoal ignores a hurt stamped with tick 0 (its own "nothing yet" value).
		helper.runAfterDelay(5, () -> zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(skeleton), 1.0F));
		helper.succeedWhen(() -> helper.assertTrue(zombie.getTarget() == skeleton, "zombie did not retaliate: target " + zombie.getTarget()
				+ ", last hurt by " + zombie.getLastHurtByMob() + ", health " + zombie.getHealth()));
	}
}
