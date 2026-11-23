// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.spider.GiantSpider;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SilkCocoon;
import io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Cocoons (specs/spiders.md "Cocoons", owner 2026-11-22). In the 9x16x9 nest room. */
public class CocoonGameTest {
	private static Vec3 at(GameTestHelper helper, double x, double y, double z) {
		return helper.absoluteVec(new Vec3(x, y, z));
	}

	private static Pig pig(GameTestHelper helper, double x, double z) {
		Pig pig = EntityTypes.PIG.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		Vec3 p = at(helper, x, 6.0, z);
		pig.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(pig);
		return pig;
	}

	private static SilkCocoon wrap(GameTestHelper helper, LivingEntity victim) {
		SilkCocoon cocoon = SilkCocoon.wrap(helper.getLevel(), victim, victim.position().add(0.0, 4.0, 0.0));
		helper.assertTrue(cocoon != null && victim.getVehicle() == cocoon, "not cocooned");
		return cocoon;
	}

	/** Hits from inside until the cocoon gives; the number of hits it took. */
	private static int hitsToFree(GameTestHelper helper, ServerPlayer player, SilkCocoon cocoon) {
		ServerLevel level = helper.getLevel();
		int hits = 0;
		while (!cocoon.isRemoved() && hits < 50) {
			cocoon.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
			hits++;
		}
		helper.assertTrue(cocoon.isRemoved() && player.getVehicle() == null, "never got free");
		return hits;
	}

	/** A player cuts their way out faster with a sword than with a bare hand. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void swordCutsFreeFasterThanAHand(GameTestHelper helper) {
		ServerPlayer bare = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 p = at(helper, 2.5, 6.0, 2.5);
		bare.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
		int hand = hitsToFree(helper, bare, wrap(helper, bare));

		ServerPlayer armed = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 q = at(helper, 6.5, 6.0, 6.5);
		armed.snapTo(q.x, q.y, q.z, 0.0F, 0.0F);
		armed.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		int sword = hitsToFree(helper, armed, wrap(helper, armed));
		helper.assertTrue(sword < hand, "sword " + sword + " hits, hand " + hand);
		helper.succeed();
	}

	/** Someone else cuts a cocooned pig free (shears, then a sword), and it comes out alive. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void anotherPlayerCutsAPigFree(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Pig pig = pig(helper, 4.5, 4.5);
		SilkCocoon cocoon = wrap(helper, pig);
		ServerPlayer rescuer = SleeperNoiseGameTest.survivalPlayer(helper);
		rescuer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		cocoon.interact(rescuer, InteractionHand.MAIN_HAND, cocoon.position());
		helper.assertTrue(!cocoon.isRemoved(), "one shear cut freed it at once");
		rescuer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		for (int i = 0; i < 5 && !cocoon.isRemoved(); i++) {
			cocoon.hurtServer(level, level.damageSources().playerAttack(rescuer), 6.0F);
		}
		helper.assertTrue(cocoon.isRemoved(), "still wrapped, silk " + cocoon.silk());
		helper.assertTrue(pig.isAlive() && pig.getVehicle() == null, "pig not free and alive");
		helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "the cutting hurt the pig: " + pig.getHealth());
		helper.succeed();
	}

	/** The poison of a cocoon never kills: a pig on its last hearts, wrapped for a long while, is still alive. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 400)
	public void poisonNeverKills(GameTestHelper helper) {
		Pig pig = pig(helper, 4.5, 4.5);
		pig.setHealth(3.0F);
		wrap(helper, pig);
		helper.runAfterDelay(300, () -> {
			helper.assertTrue(pig.isAlive(), "the poison killed it");
			helper.assertTrue(pig.getHealth() > 0.0F && pig.getHealth() <= 1.0F, "health " + pig.getHealth());
			helper.succeed();
		});
	}

	/** After its wait, the giant comes back to its cocooned pig and bites it. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 500)
	public void spiderComesBackToFeed(GameTestHelper helper) {
		for (int y = 1; y <= 12; y++) {
			helper.setBlock(new BlockPos(1, y, 4), Blocks.SPRUCE_LOG);
		}
		ServerLevel level = helper.getLevel();
		GiantSpider giant = Spiders.GIANT_SPIDER.create(level, EntitySpawnReason.COMMAND);
		Vec3 g = at(helper, 3.0, 1.0, 4.5);
		giant.snapTo(g.x, g.y, g.z, 0.0F, 0.0F);
		giant.setPersistenceRequired(); // other tests' players are far off: don't let vanilla despawn it first
		level.addFreshEntity(giant);
		Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		Vec3 p = at(helper, 3.5, 1.0, 3.0);
		pig.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
		level.addFreshEntity(pig);
		helper.assertTrue(giant.snatch(level, pig), "no snatch");
		boolean[] skipped = {false};
		helper.onEachTick(() -> {
			if (!skipped[0] && giant.task() == GiantSpider.Task.WAIT) {
				helper.assertTrue(pig.getLastHurtByMob() == null, "bitten before the wait was over");
				giant.endWait();
				skipped[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(skipped[0], "never cocooned it and waited: " + giant.task());
			helper.assertTrue(giant.task() == GiantSpider.Task.FEED, "task " + giant.task());
			helper.assertTrue(pig.getLastHurtByMob() == giant, "not bitten by the giant yet");
		});
	}
}
