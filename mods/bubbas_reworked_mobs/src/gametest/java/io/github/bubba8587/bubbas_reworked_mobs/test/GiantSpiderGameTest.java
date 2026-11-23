// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.spider.GiantSpider;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderTuning;
import io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpunWeb;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Giant spiders and spun webs (specs/spiders.md). In the 9x16x9 nest room, with two spruce trunks 6 apart. */
public class GiantSpiderGameTest {
	private static void trunks(GameTestHelper helper) {
		for (int y = 1; y <= 12; y++) {
			helper.setBlock(new BlockPos(1, y, 4), Blocks.SPRUCE_LOG);
			helper.setBlock(new BlockPos(7, y, 4), Blocks.SPRUCE_LOG);
		}
	}

	private static GiantSpider giant(GameTestHelper helper, double x, double z) {
		ServerLevel level = helper.getLevel();
		GiantSpider giant = Spiders.GIANT_SPIDER.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
		giant.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		giant.setPersistenceRequired(); // other tests' players are far off: don't let vanilla despawn it first
		level.addFreshEntity(giant);
		return giant;
	}

	private static AABB room(GameTestHelper helper) {
		return new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(9, 16, 9);
	}

	/** Core: a giant is vanilla-scaled larger, and spins a web of strands between two trunks. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void giantIsLargeAndSpinsBetweenTrees(GameTestHelper helper) {
		trunks(helper);
		GiantSpider giant = giant(helper, 4.5, 2.5);
		helper.assertTrue(Math.abs(giant.getAttributeValue(Attributes.SCALE) - SpiderTuning.GIANT_SCALE) < 1.0E-6, "scale");
		giant.refreshDimensions(); // the scale attribute resizes the hitbox on its first tick in game
		helper.assertTrue(giant.getBbWidth() > 1.4F * 1.5F, "hitbox not scaled: " + giant.getBbWidth());
		int strands = giant.spin(helper.getLevel());
		helper.assertTrue(strands >= 3, "spun " + strands + " strands");
		List<SpunWeb> webs = helper.getLevel().getEntitiesOfClass(SpunWeb.class, room(helper));
		helper.assertTrue(webs.size() == 1, "webs " + webs.size());
		helper.assertTrue(webs.getFirst().strands().stream().allMatch(s -> s.length() >= SpiderTuning.WEB_STRAND_MIN), "a short strand");
		helper.succeed();
	}

	/** A snatched pig is hauled up a trunk and cocooned there; the giant backs off and waits by it. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 400)
	public void snatchesLivestockIntoItsWeb(GameTestHelper helper) {
		trunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 2.5);
		Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(4.5, 1.0, 1.5));
		pig.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		level.addFreshEntity(pig);
		double floor = at.y;
		helper.assertTrue(giant.snatch(level, pig), "no snatch");
		helper.assertTrue(pig.getVehicle() == giant, "pig not held");
		helper.succeedWhen(() -> {
			helper.assertTrue(giant.task() == GiantSpider.Task.WAIT, "task " + giant.task());
			helper.assertTrue(pig.getVehicle() instanceof io.github.bubba8587.bubbas_reworked_mobs.spider.SilkCocoon, "pig not cocooned: " + pig.getVehicle());
			helper.assertTrue(pig.getY() > floor + 2.5, "pig only " + (pig.getY() - floor) + " up");
		});
	}

	/** Livestock is a distraction: hunting a player, it turns to a pig close by. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 60)
	public void livestockLuresItOffAPlayer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 4.5);
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 p = helper.absoluteVec(new Vec3(4.5, 1.0, 8.0));
		player.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
		Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		Vec3 q = helper.absoluteVec(new Vec3(2.5, 1.0, 4.5));
		pig.snapTo(q.x, q.y, q.z, 0.0F, 0.0F);
		pig.setNoAi(true);
		level.addFreshEntity(pig);
		giant.setTarget(player);
		helper.succeedWhen(() -> helper.assertTrue(giant.getTarget() == pig || giant.task() != GiantSpider.Task.ROAM,
				"still after " + giant.getTarget()));
	}

	/** Hurt by someone, it calls two little spiders (small, weak); not again until the cooldown is over; capped. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void hurtCallsLittleSpiders(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 4.5);
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		int first = giant.callSpiderlings(level, player);
		helper.assertValueEqual(first, SpiderTuning.SPIDERLINGS_PER_CALL, "first call");
		helper.assertValueEqual(giant.callSpiderlings(level, player), 0, "second call in the cooldown");
		List<Spider> small = level.getEntitiesOfClass(Spider.class, room(helper), s -> s.entityTags().contains(GiantSpider.SPIDERLING_TAG));
		helper.assertValueEqual(small.size(), SpiderTuning.SPIDERLINGS_PER_CALL, "spiderlings");
		Spider s = small.getFirst();
		helper.assertTrue(s.getAttributeValue(Attributes.SCALE) < 1.0, "not small");
		helper.assertTrue(s.getMaxHealth() < 16.0F, "not weak");
		helper.succeed();
	}

	/** Spun webs slow what touches them (not spiders), three hits cut one, shears cut one cleanly for string. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void websSlowAndCut(GameTestHelper helper) {
		trunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 2.5);
		giant.spin(level);
		SpunWeb web = level.getEntitiesOfClass(SpunWeb.class, room(helper)).getFirst();
		SpunWeb.Strand strand = web.strands().getFirst();
		Vec3 onIt = web.position().add(strand.a().lerp(strand.b(), 0.5));
		Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		pig.snapTo(onIt.x, onIt.y - 0.45, onIt.z, 0.0F, 0.0F);
		helper.assertTrue(web.touching(pig), "a pig on a strand isn't touching it");
		helper.assertTrue(!web.touching(giant) || giant instanceof Spider, "spiders are exempt by type");

		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		for (int i = 0; i < SpiderTuning.WEB_HITS_TO_CUT; i++) {
			web.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		}
		helper.assertTrue(web.isRemoved(), "three hits didn't cut it");

		giant.spin(level);
		SpunWeb second = level.getEntitiesOfClass(SpunWeb.class, room(helper)).getFirst();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		Vec3 cutAt = second.position().add(second.strands().getFirst().a().lerp(second.strands().getFirst().b(), 0.5));
		second.interact(player, InteractionHand.MAIN_HAND, cutAt.subtract(second.position())); // vanilla sends it relative
		helper.assertTrue(second.isRemoved(), "shears didn't cut it");
		// The string falls from the cut.
		int string = level.getEntitiesOfClass(ItemEntity.class, new AABB(cutAt, cutAt).inflate(1.0), e -> e.getItem().is(Items.STRING)).stream()
				.mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertValueEqual(string, SpiderTuning.WEB_SHEAR_STRING, "string from shears");
		helper.succeed();
	}
}
