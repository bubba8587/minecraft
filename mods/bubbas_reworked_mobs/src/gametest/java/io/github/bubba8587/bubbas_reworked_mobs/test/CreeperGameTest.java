// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.CoverFinder;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperMind;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperTuning;
import io.github.bubba8587.bubbas_reworked_mobs.test.mixin.CreeperAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The nervous-watcher creeper (specs/creepers.md). Each test has its own environment so its batch is alone (a
 * creeper notices anything moving within 16 blocks). Meter tests use a no-AI creeper: its senses and meter tick
 * in Creeper#tick regardless, so the numbers are exact; hiss and watching tests need its goals.
 * Players move with Entity#move once a tick at vanilla speeds (walk 0.216, sprint 0.28).
 */
public class CreeperGameTest {
	static final String FIELD = "bubbas_reworked_mobs-test:hearing_field";
	private static final double WALK = 0.216;
	private static final double SPRINT = 0.28;

	static Creeper creeper(GameTestHelper helper, double x, double z, boolean ai) {
		Creeper creeper = helper.spawn(EntityTypes.CREEPER, new Vec3(x, 1.0, z));
		creeper.setNoAi(!ai);
		creeper.setPersistenceRequired();
		return creeper;
	}

	static ServerPlayer player(GameTestHelper helper, double x, double z) {
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 abs = helper.absoluteVec(new Vec3(x, 1.0, z));
		player.snapTo(abs.x, abs.y, abs.z, 0.0F, 0.0F);
		player.setOnGround(true);
		return player;
	}

	static void remove(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static CreeperMind mind(Creeper creeper) {
		return CreeperAccess.mind(creeper);
	}

	private static void hit(GameTestHelper helper, Creeper creeper, ServerPlayer player) {
		ServerLevel level = helper.getLevel();
		creeper.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
	}

	// --- startle meter ------------------------------------------------------------------------------

	/** One hit fills the meter partway and it calms down; three hits in a row light the hiss. */
	public void hitsFillTheMeterAndItDrains(GameTestHelper helper) {
		Creeper creeper = creeper(helper, 12.5, 8.5, false);
		creeper.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
		creeper.setHealth(100.0F);
		ServerPlayer player = player(helper, 12.5, 11.5);
		helper.runAtTickTime(2, () -> {
			hit(helper, creeper, player);
			float meter = mind(creeper).meter();
			helper.assertTrue(meter > CreeperTuning.ATTACK_POINTS - 2 && meter <= CreeperTuning.ATTACK_POINTS, "one hit should add ~" + CreeperTuning.ATTACK_POINTS + ", got " + meter);
		});
		helper.runAtTickTime(2 + 85, () -> helper.assertTrue(mind(creeper).meter() == 0.0F && !mind(creeper).isFusing(),
				"one hit should calm down within ~4 s, meter " + mind(creeper).meter()));
		helper.runAtTickTime(100, () -> hit(helper, creeper, player));
		helper.runAtTickTime(112, () -> {
			hit(helper, creeper, player);
			helper.assertTrue(!mind(creeper).isFusing(), "two hits shouldn't light it yet, meter " + mind(creeper).meter());
		});
		helper.runAtTickTime(124, () -> {
			hit(helper, creeper, player);
			helper.assertTrue(mind(creeper).isFusing(), "three hits in a row should fill it, meter " + mind(creeper).meter());
			creeper.discard();
			remove(helper, player);
			helper.succeed();
		});
	}

	/** Walking right past (2 blocks) doesn't startle it; sprinting past does. Same rule for anyone (B18). */
	public void sprintingNearbyStartlesWalkingDoesNot(GameTestHelper helper) {
		Creeper creeper = creeper(helper, 12.5, 8.5, false);
		ServerPlayer player = player(helper, 4.5, 10.5);
		float[] peak = {0.0F, 0.0F};
		int[] tick = {0};
		helper.onEachTick(() -> {
			tick[0]++;
			if (tick[0] <= 70) {
				player.setOnGround(true);
				player.move(MoverType.SELF, new Vec3(WALK, 0.0, 0.0));
				peak[0] = Math.max(peak[0], mind(creeper).meter());
			} else if (tick[0] <= 125) {
				player.setOnGround(true);
				player.setSprinting(true);
				player.move(MoverType.SELF, new Vec3(-SPRINT, 0.0, 0.0));
				peak[1] = Math.max(peak[1], mind(creeper).meter());
			} else if (tick[0] == 126) {
				helper.assertTrue(peak[0] == 0.0F, "walking past shouldn't startle it, peak " + peak[0]);
				helper.assertTrue(peak[1] >= 3 * CreeperTuning.FAST_MOVE_POINTS, "sprinting past should startle it, peak " + peak[1]);
				helper.assertTrue(mind(creeper).watched() == player, "it should be watching the player");
				creeper.discard();
				remove(helper, player);
				helper.succeed();
			}
		});
	}

	/** A projectile flying past within 2 blocks is a near-miss (owner C4); one far off isn't. */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_near_miss", structure = FIELD, maxTicks = 40)
	public void nearMissesStartle(GameTestHelper helper) {
		Creeper creeper = creeper(helper, 12.5, 8.5, false);
		helper.runAtTickTime(2, () -> {
			Snowball far = new Snowball(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 2.0, 1.0)).x, helper.absoluteVec(new Vec3(2.5, 2.0, 1.0)).y,
					helper.absoluteVec(new Vec3(2.5, 2.0, 1.0)).z, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOWBALL));
			far.setDeltaMovement(1.5, 0.0, 0.0);
			far.setNoGravity(true);
			helper.getLevel().addFreshEntity(far);
			Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 7.5));
			Snowball close = new Snowball(helper.getLevel(), at.x, at.y, at.z, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOWBALL));
			close.setDeltaMovement(1.5, 0.0, 0.0);
			close.setNoGravity(true);
			helper.getLevel().addFreshEntity(close);
		});
		helper.runAtTickTime(20, () -> {
			float meter = mind(creeper).meter();
			helper.assertTrue(meter > CreeperTuning.NEAR_MISS_POINTS - 10 && meter <= CreeperTuning.NEAR_MISS_POINTS,
					"one near-miss (and not the far one) should count, meter " + meter);
			creeper.discard();
			helper.succeed();
		});
	}

	// --- the hiss -----------------------------------------------------------------------------------

	/** A full meter hisses (vanilla swell); the target sneaking defuses it, and so does backing out of range. A hit during the hiss stops a sneak defusing it. */
	public void fullMeterHissesAndDefuses(GameTestHelper helper) {
		Creeper creeper = creeper(helper, 12.5, 8.5, true);
		ServerPlayer player = player(helper, 12.5, 13.0);
		helper.runAtTickTime(5, () -> mind(creeper).startle(CreeperTuning.METER_FULL, player));
		helper.runAtTickTime(8, () -> helper.assertTrue(creeper.getSwellDir() > 0, "a full meter should hiss"));
		helper.runAtTickTime(10, () -> player.setShiftKeyDown(true));
		helper.runAtTickTime(12, () -> {
			helper.assertTrue(!mind(creeper).isFusing() && creeper.getSwellDir() < 0, "sneaking should defuse it");
			helper.assertTrue(mind(creeper).meter() <= CreeperTuning.DEFUSED_METER, "defusing drops the meter");
		});
		helper.runAtTickTime(14, () -> {
			player.setShiftKeyDown(false);
			mind(creeper).startle(CreeperTuning.METER_FULL, player);
		});
		helper.runAtTickTime(17, () -> {
			helper.assertTrue(creeper.getSwellDir() > 0, "it should hiss again");
			Vec3 far = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5 + CreeperTuning.DEFUSE_RANGE + 2.0));
			player.snapTo(far.x, far.y, far.z, 180.0F, 0.0F);
		});
		helper.runAtTickTime(20, () -> helper.assertTrue(!mind(creeper).isFusing() && creeper.getSwellDir() < 0, "backing out of range should defuse it"));
		helper.runAtTickTime(60, () -> {
			helper.assertTrue(creeper.isAlive(), "a defused creeper doesn't explode");
			creeper.discard();
			remove(helper, player);
			helper.succeed();
		});
	}

	/** Hit during the hiss: sneaking no longer saves you. */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_fuse_hit", structure = FIELD, maxTicks = 40)
	public void aHitDuringTheHissStopsDefusing(GameTestHelper helper) {
		Creeper creeper = creeper(helper, 12.5, 8.5, true);
		creeper.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
		creeper.setHealth(100.0F);
		ServerPlayer player = player(helper, 12.5, 12.0);
		helper.runAtTickTime(5, () -> mind(creeper).startle(CreeperTuning.METER_FULL, player));
		helper.runAtTickTime(8, () -> {
			hit(helper, creeper, player);
			player.setShiftKeyDown(true);
		});
		helper.runAtTickTime(20, () -> {
			helper.assertTrue(mind(creeper).isFusing() && creeper.getSwellDir() > 0, "a hit during the hiss means no defusing");
			creeper.discard();
			remove(helper, player);
			helper.succeed();
		});
	}

	// --- blindness and charged ------------------------------------------------------------------------

	/** Blinded, it can't see motion: sprinting round it and hitting it do nothing. */
	public void blindedCreeperIgnoresMotion(GameTestHelper helper) {
		Creeper creeper = creeper(helper, 12.5, 8.5, false);
		creeper.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, -1));
		ServerPlayer player = player(helper, 4.5, 10.5);
		float[] peak = {0.0F};
		int[] tick = {0};
		helper.onEachTick(() -> {
			tick[0]++;
			if (tick[0] <= 55) {
				player.setOnGround(true);
				player.setSprinting(true);
				player.move(MoverType.SELF, new Vec3(SPRINT, 0.0, 0.0));
				peak[0] = Math.max(peak[0], mind(creeper).meter());
			} else if (tick[0] == 56) {
				hit(helper, creeper, player);
				peak[0] = Math.max(peak[0], mind(creeper).meter());
				helper.assertTrue(peak[0] == 0.0F, "a blind creeper shouldn't be startled, peak " + peak[0]);
				helper.assertTrue(mind(creeper).watched() == null, "a blind creeper watches nothing");
				creeper.discard();
				remove(helper, player);
				helper.succeed();
			}
		});
	}

	/** Charged creepers stay vanilla: they target a still player and swell when close; the meter stays empty. */
	public void chargedCreeperStaysVanilla(GameTestHelper helper) {
		Creeper creeper = creeper(helper, 12.5, 8.5, true);
		creeper.getEntityData().set(CreeperAccessor.brmTest$powered(), true);
		ServerPlayer player = player(helper, 12.5, 10.5);
		helper.succeedWhen(() -> {
			helper.assertTrue(creeper.getSwellDir() > 0, "a charged creeper should swell at a player 2 blocks away");
			helper.assertTrue(mind(creeper).meter() == 0.0F, "a charged creeper has no startle meter");
			creeper.discard();
			remove(helper, player);
		});
	}

	// --- watching -------------------------------------------------------------------------------------

	/**
	 * With a wall between it and the player, it walks to a spot half behind the wall (eyes visible, one side hidden).
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_peek", structure = FIELD, maxTicks = 200)
	public void peeksHalfBehindCover(GameTestHelper helper) {
		// A 3-wide, 3-high wall across the field at z = 8, x = 11..13.
		for (int x = 11; x <= 13; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
			}
		}
		Creeper creeper = creeper(helper, 12.5, 11.5, true);
		ServerPlayer player = player(helper, 12.5, 2.5);
		// The player looks away (south is +z; yaw 180 faces north, away from the wall).
		player.setYRot(180.0F);
		player.setYHeadRot(180.0F);
		CreeperMind mind = mind(creeper);
		helper.runAtTickTime(2, () -> mind.watch(player, helper.getLevel().getGameTime()));
		helper.succeedWhen(() -> {
			helper.assertTrue(mind.watched() == player, "watching the player");
			CoverFinder.PeekSpot spot = mind.spot();
			helper.assertTrue(spot != null && mind.spotReached(), "should reach a peek spot");
			helper.assertTrue(creeper.position().distanceTo(spot.half()) < 0.1, "should sit half behind the wall");
			Vec3 eye = player.getEyePosition();
			helper.assertTrue(CoverFinder.clear(helper.getLevel(), eye, creeper.getEyePosition()), "its eyes should be visible");
			creeper.discard();
			remove(helper, player);
		});
	}

	/** Peekaboo: looked at, it slides about 95 % behind the cover; looked away from, it eases back out. */
	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_peekaboo", structure = FIELD, maxTicks = 300)
	public void hidesWhenLookedAt(GameTestHelper helper) {
		for (int x = 11; x <= 13; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
			}
		}
		Creeper creeper = creeper(helper, 12.5, 11.5, true);
		ServerPlayer player = player(helper, 12.5, 2.5);
		player.setYRot(180.0F);
		player.setYHeadRot(180.0F);
		CreeperMind mind = mind(creeper);
		helper.runAtTickTime(2, () -> mind.watch(player, helper.getLevel().getGameTime()));
		int[] stage = {0};
		helper.onEachTick(() -> {
			CoverFinder.PeekSpot spot = mind.spot();
			if (stage[0] == 0 && spot != null && mind.spotReached() && creeper.position().distanceTo(spot.half()) < 0.1) {
				// Look straight at it.
				Vec3 to = creeper.getEyePosition().subtract(player.getEyePosition());
				float yaw = (float) (Math.atan2(to.z, to.x) * 180.0 / Math.PI) - 90.0F;
				player.setYRot(yaw);
				player.setYHeadRot(yaw);
				player.setXRot(0.0F);
				stage[0] = 1;
			} else if (stage[0] == 1 && spot != null && creeper.position().distanceTo(spot.hidden()) < 0.1) {
				helper.assertTrue(!CoverFinder.clear(helper.getLevel(), player.getEyePosition(), creeper.getEyePosition()), "hidden: its eyes are behind the wall");
				player.setYRot(180.0F);
				player.setYHeadRot(180.0F);
				stage[0] = 2;
			} else if (stage[0] == 2 && spot != null && creeper.position().distanceTo(spot.half()) < 0.1) {
				creeper.discard();
				remove(helper, player);
				helper.succeed();
			}
		});
	}
}
