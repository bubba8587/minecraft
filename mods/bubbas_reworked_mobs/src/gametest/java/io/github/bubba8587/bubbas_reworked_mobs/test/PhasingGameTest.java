// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.Soulfire;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulfireTuning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing;
import io.github.bubba8587.bubbas_reworked_mobs.test.mixin.MoveCheckInvoker;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Eye of the Soul and Phasing (specs/soulfire.md "Eye of the Soul: phasing"). Milestone. Movement is driven by calling
 * the player's own physics (LivingEntity.travel, the code a client runs for its player) tick by tick on a server test
 * player, so the collision, climbing and sinking rules run for real; the server's move check is tested directly.
 */
public class PhasingGameTest {
	/** A survival test player standing at (x, y, z), facing +x, whose "client" has loaded (so it can be hurt). */
	private static ServerPlayer player(GameTestHelper helper, double x, double y, double z) {
		ServerPlayer player = TestPlayers.survival(helper);
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
		player.snapTo(at.x, at.y, at.z, -90.0F, 0.0F);
		player.setOnGround(true);
		player.setSpeed(0.1F);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return player;
	}

	/** Runs the player's movement for {@code ticks} ticks: forward input, holding jump, holding sneak. */
	private static void move(ServerPlayer player, int ticks, float forward, boolean jump, boolean sneak) {
		player.setShiftKeyDown(sneak);
		for (int i = 0; i < ticks; i++) {
			if (jump && player.onGround()) {
				player.jumpFromGround();
			}
			player.setJumping(jump);
			player.travel(new Vec3(0.0, 0.0, forward));
		}
		player.setJumping(false);
		player.setShiftKeyDown(false);
	}

	/** Test players' connections aren't ticked by the server: tick the players themselves (effects, fire, suffocation). */
	private static void ticked(GameTestHelper helper, ServerPlayer... players) {
		helper.onEachTick(() -> {
			for (ServerPlayer player : players) {
				player.doTick();
			}
		});
	}

	private static Vec3 rel(GameTestHelper helper, ServerPlayer player) {
		return helper.relativeVec(player.position());
	}

	private static void fill(GameTestHelper helper, Block block, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					helper.setBlock(x, y, z, block);
				}
			}
		}
	}

	/** Crafted from soul blaze powder and an ender pearl; using it starts 5 s of Phasing and uses it up. */
	@GameTest(maxTicks = 5)
	public void eyeIsCraftedAndStartsPhasing(GameTestHelper helper) {
		CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Soulfire.SOUL_BLAZE_POWDER), new ItemStack(Items.ENDER_PEARL)));
		var holder = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElse(null);
		helper.assertTrue(holder != null, "no recipe for the Eye of the Soul");
		@SuppressWarnings("unchecked")
		ItemStack eye = ((Recipe<CraftingInput>) holder.value()).assemble(input);
		helper.assertTrue(eye.is(Phasing.EYE_OF_THE_SOUL) && eye.getCount() == 1, "crafted " + eye);
		TestFloors.stone(helper);
		ServerPlayer player = player(helper, 1.5, 1.0, 1.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Phasing.EYE_OF_THE_SOUL, 2));
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		MobEffectInstance phasing = player.getEffect(Phasing.PHASING);
		helper.assertTrue(phasing != null && phasing.getDuration() == SoulfireTuning.PHASING_TICKS, "Phasing after use: " + phasing);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "not used up: " + player.getMainHandItem());
		helper.succeed();
	}

	/** Phasing walks through a stone wall while standing on the stone floor; a barrier wall stops it; without Phasing the stone does. */
	@GameTest(maxTicks = 5)
	public void walksThroughStoneNotBarriers(GameTestHelper helper) {
		TestFloors.stone(helper);
		fill(helper, Blocks.STONE, 3, 1, 0, 3, 2, 7);
		fill(helper, Blocks.BARRIER, 6, 1, 0, 6, 2, 7);
		ServerPlayer phasing = player(helper, 1.5, 1.0, 1.5);
		Phasing.start(phasing);
		phasing.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 200));
		ServerPlayer plain = player(helper, 1.5, 1.0, 5.5);
		move(phasing, 80, 1.0F, false, false);
		move(plain, 80, 1.0F, false, false);
		Vec3 through = rel(helper, phasing);
		Vec3 blocked = rel(helper, plain);
		helper.assertTrue(through.x > 4.5 && through.x < 6.0, "Phasing player at x " + through.x + " (stone wall at 3, barrier at 6)");
		helper.assertTrue(Math.abs(through.y - 1.0) < 0.01, "Phasing player didn't stand on the floor: y " + through.y);
		helper.assertTrue(blocked.x < 3.0, "a player without Phasing got through the stone: x " + blocked.x);
		helper.succeed();
	}

	/** Phasing stands on a stone floor, sinks through it while sneaking, and lands on bedrock. */
	@GameTest(maxTicks = 5)
	public void sinksThroughFloorWhileSneakingBedrockHolds(GameTestHelper helper) {
		TestFloors.stone(helper);
		fill(helper, Blocks.BEDROCK, 1, 0, 1, 3, 0, 3);
		fill(helper, Blocks.STONE, 1, 2, 1, 3, 2, 3);
		ServerPlayer player = player(helper, 2.5, 3.0, 2.5);
		Phasing.start(player);
		player.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 200));
		move(player, 30, 0.0F, false, false);
		helper.assertTrue(Math.abs(rel(helper, player).y - 3.0) < 0.01, "didn't stand on the stone: y " + rel(helper, player).y);
		move(player, 60, 0.0F, false, true);
		helper.assertTrue(Math.abs(rel(helper, player).y - 1.0) < 0.01, "didn't sink through the stone onto the bedrock: y " + rel(helper, player).y);
		helper.succeed();
	}

	/** Holding jump climbs up through a stone ceiling and stands on top; a barrier ceiling holds. */
	@GameTest(maxTicks = 5)
	public void climbsThroughCeilingWhileJumping(GameTestHelper helper) {
		TestFloors.stone(helper);
		fill(helper, Blocks.STONE, 4, 3, 4, 6, 3, 6);
		fill(helper, Blocks.BARRIER, 0, 3, 4, 2, 3, 6);
		ServerPlayer climber = player(helper, 5.5, 1.0, 5.5);
		ServerPlayer barred = player(helper, 1.5, 1.0, 5.5);
		for (ServerPlayer p : List.of(climber, barred)) {
			Phasing.start(p);
			p.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 200));
		}
		move(climber, 60, 0.0F, true, false);
		move(barred, 60, 0.0F, true, false);
		move(climber, 20, 0.0F, false, false);
		helper.assertTrue(Math.abs(rel(helper, climber).y - 4.0) < 0.01, "didn't climb onto the ceiling: y " + rel(helper, climber).y);
		helper.assertTrue(rel(helper, barred).y < 1.3, "climbed through a barrier: y " + rel(helper, barred).y);
		helper.succeed();
	}

	/** The server's move check doesn't reject a Phasing player's move into stone; into bedrock it still does. */
	@GameTest(maxTicks = 5)
	public void serverAcceptsMovesIntoStone(GameTestHelper helper) {
		TestFloors.stone(helper);
		helper.setBlock(3, 1, 1, Blocks.STONE);
		helper.setBlock(3, 2, 1, Blocks.STONE);
		helper.setBlock(1, 1, 3, Blocks.BEDROCK);
		helper.setBlock(1, 2, 3, Blocks.BEDROCK);
		ServerPlayer player = player(helper, 2.5, 1.0, 1.5);
		AABB old = player.getBoundingBox();
		Vec3 stone = helper.absoluteVec(new Vec3(3.5, 1.0, 1.5));
		Vec3 bedrock = helper.absoluteVec(new Vec3(1.5, 1.0, 3.5));
		MoveCheckInvoker check = (MoveCheckInvoker) player.connection;
		helper.assertTrue(check.brmTest$collidingWithAnythingNew(helper.getLevel(), player, old, stone.x, stone.y, stone.z),
				"without Phasing a move into stone passes the check");
		Phasing.start(player);
		helper.assertTrue(!check.brmTest$collidingWithAnythingNew(helper.getLevel(), player, old, stone.x, stone.y, stone.z),
				"a Phasing move into stone is rejected");
		Vec3 nextToBedrock = helper.absoluteVec(new Vec3(1.5, 1.0, 2.5));
		player.snapTo(nextToBedrock.x, nextToBedrock.y, nextToBedrock.z);
		helper.assertTrue(check.brmTest$collidingWithAnythingNew(helper.getLevel(), player, player.getBoundingBox(), bedrock.x, bedrock.y, bedrock.z),
				"a Phasing move into bedrock passes the check");
		helper.succeed();
	}

	/** Phasing sets Soulfire burning for its whole time, except with Soulguard. */
	@GameTest(maxTicks = 60)
	public void phasingBurnsWithSoulfireUnlessSoulguard(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerPlayer burning = player(helper, 1.5, 1.0, 1.5);
		ServerPlayer guarded = player(helper, 5.5, 1.0, 5.5);
		guarded.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 200));
		Phasing.start(burning);
		Phasing.start(guarded);
		ticked(helper, burning, guarded);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(burning.isOnFire() && SoulBurning.isSoulBurning(burning), "a Phasing player isn't Soulfire-burning");
			helper.assertTrue(burning.getRemainingFireTicks() >= burning.getEffect(Phasing.PHASING).getDuration(),
					"fire ends before Phasing: " + burning.getRemainingFireTicks());
			helper.assertTrue(burning.getHealth() < burning.getMaxHealth(), "no Soulfire damage");
			helper.assertTrue(!guarded.isOnFire() && guarded.getHealth() == guarded.getMaxHealth(), "Soulguard burned while Phasing");
			helper.succeed();
		});
	}

	/** Milk (and any other effect clear) doesn't end Phasing; other effects still go. */
	@GameTest(maxTicks = 5)
	public void milkDoesNotClearPhasing(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerPlayer player = player(helper, 1.5, 1.0, 1.5);
		Phasing.start(player);
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
		player.getMainHandItem().finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "milk didn't clear night vision");
		helper.assertTrue(player.hasEffect(Phasing.PHASING), "milk cleared Phasing");
		helper.assertTrue(!player.removeEffect(Phasing.PHASING) && player.hasEffect(Phasing.PHASING), "removeEffect ended Phasing");
		helper.succeed();
	}

	/** No suffocation while Phasing inside stone; once it ends, vanilla suffocation, with no shove out. */
	@GameTest(maxTicks = 100)
	public void endingInsideStoneSuffocates(GameTestHelper helper) {
		TestFloors.stone(helper);
		fill(helper, Blocks.STONE, 1, 1, 1, 3, 3, 3);
		ServerPlayer player = player(helper, 2.5, 1.0, 2.5);
		player.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 400));
		player.addEffect(new MobEffectInstance(Phasing.PHASING, 30, 0, false, false, true));
		Vec3 start = player.position();
		ticked(helper, player);
		helper.startSequence()
				.thenExecuteAfter(25, () -> helper.assertTrue(player.getHealth() == player.getMaxHealth(),
						"hurt inside stone while Phasing: " + player.getHealth()))
				.thenWaitUntil(() -> helper.assertTrue(!player.hasEffect(Phasing.PHASING) && player.getHealth() < player.getMaxHealth(),
						"not suffocating after Phasing ended"))
				.thenExecute(() -> helper.assertTrue(player.position().distanceTo(start) < 0.01, "shoved from " + start + " to " + player.position()))
				.thenSucceed();
	}
}
