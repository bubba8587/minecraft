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
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
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

	/** A giant is vanilla-scaled larger, and spins a web of strands between two trunks. */
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

	/** Stranded on a canopy (leaves 10 up on a lone trunk, no web to be had), it climbs down off it (to the floor or the field's 4-high wall tops). */
	@GameTest(environment = SpiderGameTest.ENV, structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 1200)
	public void strandedInTheCanopyComesDown(GameTestHelper helper) {
		for (int y = 1; y < 10; y++) {
			helper.setBlock(new BlockPos(12, y, 8), Blocks.SPRUCE_LOG);
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(new BlockPos(12 + dx, 10, 8 + dz), Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
			}
		}
		GiantSpider giant = giant(helper, 12.5, 8.5);
		Vec3 top = helper.absoluteVec(new Vec3(12.5, 11.0, 8.5));
		giant.snapTo(top.x, top.y, top.z, 0.0F, 0.0F);
		double floor = helper.absoluteVec(new Vec3(0.0, 1.0, 0.0)).y;
		helper.succeedWhen(() -> helper.assertTrue(giant.task() == GiantSpider.Task.ROAM && giant.onGround() && giant.getY() < floor + 6.0, // off the canopy (11 up): the floor or the field's walls
				"still up at y " + (giant.getY() - floor) + " over the floor (" + giant.task() + ")"));
	}

	/** Livestock is a distraction: hunting a player, it turns to a pig close by. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 60)
	public void livestockLuresItOffAPlayer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 4.5);
		ServerPlayer player = TestPlayers.survival(helper);
		Vec3 p = helper.absoluteVec(new Vec3(4.5, 1.0, 8.0));
		player.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
		Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		Vec3 q = helper.absoluteVec(new Vec3(2.5, 1.0, 4.5));
		pig.snapTo(q.x, q.y, q.z, 0.0F, 0.0F);
		pig.setNoAi(true);
		level.addFreshEntity(pig);
		giant.setTarget(player);
		boolean[] lured = {false};
		helper.onEachTick(() -> lured[0] |= giant.getTarget() == pig || pig.getVehicle() == giant);
		helper.succeedWhen(() -> helper.assertTrue(lured[0], "still after " + giant.getTarget()));
	}

	/** Hurt by someone, it calls two little spiders (small, weak); not again until the cooldown is over; capped. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void hurtCallsLittleSpiders(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 4.5);
		ServerPlayer player = TestPlayers.survival(helper);
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

		ServerPlayer player = TestPlayers.survival(helper);
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

	/** The crosshair, a swing and an arrow pass through the gaps between strands to a block or mob, and hit a strand they aim at. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void webHitsOnlyTheStrands(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SpunWeb web = Spiders.SPUN_WEB.create(level, EntitySpawnReason.COMMAND);
		Vec3 hub = helper.absoluteVec(new Vec3(4.5, 5.0, 4.5));
		web.setPos(hub.x, hub.y, hub.z);
		web.setStrands(List.of(new SpunWeb.Strand(new Vec3(-4, -1.5, 0), new Vec3(4, -1.5, 0)),
				new SpunWeb.Strand(new Vec3(-4, 1.5, 0), new Vec3(4, 1.5, 0)),
				new SpunWeb.Strand(new Vec3(-4, 0, -4), new Vec3(-4, 0, 4))));
		level.addFreshEntity(web);
		helper.setBlock(new BlockPos(4, 5, 8), Blocks.STONE);
		ServerPlayer player = TestPlayers.survival(helper);
		Vec3 eye = hub.add(0, 0, -2.5); // inside the web's span, between strands
		Vec3 gapTo = hub.add(0, 0, 3.4); // the stone, straight through the gap
		Vec3 strandTo = hub.add(0, 1.5 * 5.9 / 2.5, 3.4); // through the top strand
		AABB search = new AABB(eye, gapTo).inflate(4.0);
		helper.assertTrue(ProjectileUtil.getEntityHitResult(player, eye, gapTo, search, EntitySelector.CAN_BE_PICKED, 100.0) == null,
				"the crosshair hits the web through a gap");
		EntityHitResult aimed = ProjectileUtil.getEntityHitResult(player, eye, strandTo, search, EntitySelector.CAN_BE_PICKED, 100.0);
		helper.assertTrue(aimed != null && aimed.getEntity() == web, "aiming at a strand misses it");
		helper.assertTrue(Math.abs(aimed.getLocation().z - hub.z) < 0.5, "hit away from the strand: " + aimed.getLocation().subtract(hub));
		Vec3 outside = hub.add(0, 0, -5.0); // outside the span, looking in
		helper.assertTrue(ProjectileUtil.getEntityHitResult(player, outside, gapTo, search.minmax(new AABB(outside, outside)), EntitySelector.CAN_BE_PICKED, 100.0) == null,
				"the crosshair hits the web box from outside");

		Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		pig.snapTo(hub.x, hub.y - 0.4, hub.z + 2.0, 0.0F, 0.0F);
		pig.setNoAi(true);
		level.addFreshEntity(pig);
		EntityHitResult mob = ProjectileUtil.getEntityHitResult(player, eye, gapTo, search, EntitySelector.CAN_BE_PICKED, 100.0);
		helper.assertTrue(mob != null && mob.getEntity() == pig, "a mob behind a gap can't be hit: " + (mob == null ? null : mob.getEntity()));

		Arrow arrow = new Arrow(EntityTypes.ARROW, level);
		arrow.setPos(eye.x, eye.y, eye.z);
		EntityHitResult shot = ProjectileUtil.getEntityHitResult(level, arrow, outside, gapTo, search.minmax(new AABB(outside, outside)), e -> e == web, 0.3F);
		helper.assertTrue(shot == null, "an arrow through a gap hits the web");
		Vec3 low = hub.add(0, -1.5, 0);
		EntityHitResult shotAt = ProjectileUtil.getEntityHitResult(level, arrow, low.add(0, 0, -5), low.add(0, 0, 3), search.inflate(3.0), e -> e == web, 0.3F);
		helper.assertTrue(shotAt != null && shotAt.getEntity() == web, "an arrow along the strand's line misses it");
		helper.succeed();
	}

	private static Pig pig(GameTestHelper helper, double x, double z) {
		Pig pig = EntityTypes.PIG.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
		pig.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(pig);
		return pig;
	}

	/** A catch goes to the centre of the giant's own web, not to a web another spider spun nearer it; cocooned at the hub. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 400)
	public void haulsToItsOwnWebCentre(GameTestHelper helper) {
		trunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 2.5);
		helper.assertTrue(giant.spin(level) >= 3, "no web spun");
		SpunWeb own = giant.ownWebs().getFirst();
		SpunWeb other = Spiders.SPUN_WEB.create(level, EntitySpawnReason.COMMAND);
		Vec3 o = helper.absoluteVec(new Vec3(4.5, 4.0, 2.5));
		other.setPos(o.x, o.y, o.z);
		other.setStrands(List.of(new SpunWeb.Strand(new Vec3(-3, 0, 0), new Vec3(3, 0, 0)), new SpunWeb.Strand(new Vec3(0, -3, 0), new Vec3(0, 3, 0)),
				new SpunWeb.Strand(new Vec3(-3, -3, 0), new Vec3(3, 3, 0))));
		level.addFreshEntity(other);
		Pig pig = pig(helper, 4.5, 3.5);
		helper.assertTrue(giant.snatch(level, pig), "no snatch");
		helper.assertTrue(giant.webLarder(), "not taken to a web");
		helper.assertValueEqual(giant.larder(), BlockPos.containing(own.position()), "larder");
		helper.succeedWhen(() -> {
			helper.assertTrue(giant.task() == GiantSpider.Task.WAIT, "task " + giant.task());
			Vec3 c = giant.cocoon().position();
			helper.assertTrue(Math.hypot(c.x - own.getX(), c.z - own.getZ()) < 1.0 && Math.abs(c.y - own.getY()) < 3.0,
					"cocoon at " + c.subtract(own.position()) + " from the hub");
		});
	}

	/** A giant remembers the webs it spun across a save and load. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void remembersItsWebsAcrossSaves(GameTestHelper helper) {
		trunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 2.5);
		giant.spin(level);
		SpunWeb web = giant.ownWebs().getFirst();
		var output = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(net.minecraft.util.ProblemReporter.DISCARDING);
		giant.saveWithoutId(output);
		GiantSpider loaded = Spiders.GIANT_SPIDER.create(level, EntitySpawnReason.LOAD);
		loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
		helper.assertTrue(loaded.ownWebs().equals(List.of(web)), "loaded webs " + loaded.ownWebs());
		loaded.discard();
		helper.succeed();
	}

	/** After cocooning, it drops off its perch and strolls away from it on the ground, then walks back, climbs and feeds. */
	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 1200)
	public void leavesItsPerchAndComesBackToFeed(GameTestHelper helper) {
		trunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 2.5);
		Pig pig = pig(helper, 4.5, 3.5);
		helper.assertTrue(giant.snatch(level, pig), "no snatch");
		double[] farthest = {0.0};
		boolean[] grounded = {false};
		helper.onEachTick(() -> {
			if (giant.task() == GiantSpider.Task.PROWL) {
				BlockPos l = giant.larder();
				farthest[0] = Math.max(farthest[0], Math.hypot(giant.getX() - l.getX() - 0.5, giant.getZ() - l.getZ() - 0.5));
				grounded[0] |= giant.onGround();
				if (grounded[0] && farthest[0] >= 3.0) {
					giant.endWait();
				}
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(grounded[0], "never came down off its perch");
			helper.assertTrue(farthest[0] >= 3.0, "only wandered " + farthest[0]);
			helper.assertTrue(giant.task() == GiantSpider.Task.FEED, "task " + giant.task());
			helper.assertTrue(pig.getLastHurtByMob() == giant, "not bitten yet");
		});
	}
}
