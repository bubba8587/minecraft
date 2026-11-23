// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.spider.NestSpider;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderNest;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderNestFeature;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderTuning;
import io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Cave nests (specs/spiders.md). The room: 9x16x9, stone floor (y 0) and ceiling (y 15), 14 blocks of air between.
 */
public class SpiderGameTest {
	public static final String ROOM = "bubbas_reworked_mobs-test:nest_room";
	/** Spider tests run as their own batch, so their rooms don't shift the big Fabricator tests' layout. */
	public static final String ENV = "bubbas_reworked_mobs-test:spiders";
	/** Air just under the ceiling, middle of the room. */
	private static final BlockPos TOP = new BlockPos(4, 14, 4);

	/** A nest with one sleeper hanging over the middle; returns the sleeper. */
	private static NestSpider nest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(TOP, Blocks.COBWEB);
		helper.setBlock(TOP.east(), Blocks.COBWEB);
		helper.setBlock(TOP.west(), Blocks.COBWEB);
		helper.setBlock(TOP.north(), Blocks.COBWEB);
		SpiderNest nest = Spiders.SPIDER_NEST.create(level, EntitySpawnReason.STRUCTURE);
		Vec3 centre = helper.absoluteVec(Vec3.atCenterOf(TOP));
		nest.setPos(centre.x, centre.y, centre.z);
		nest.setDepth(14);
		level.addFreshEntity(nest);
		NestSpider spider = Spiders.NEST_SPIDER.create(level, EntitySpawnReason.STRUCTURE);
		spider.hangFrom(helper.absoluteVec(new Vec3(TOP.getX() + 0.5, TOP.getY() + 1.0, TOP.getZ() + 0.5)));
		level.addFreshEntity(spider);
		return spider;
	}

	/** Core: a player standing right under the nest is dropped on, latched, reeled up into the nest and cocooned there. */
	public void sleeperDropsLatchesAndReelsUp(GameTestHelper helper) {
		NestSpider spider = nest(helper);
		ServerPlayer player = TestPlayers.survivalAt(helper, 4.5, 4.5);
		double floor = player.getY();
		helper.succeedWhen(() -> {
			helper.assertTrue(spider.isAlive() && spider.phase() == NestSpider.Phase.WAITING, "not waiting by a cocoon: " + spider.phase());
			helper.assertTrue(player.getVehicle() instanceof io.github.bubba8587.bubbas_reworked_mobs.spider.SilkCocoon, "player not cocooned: " + player.getVehicle());
			helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "not poisoned");
			helper.assertTrue(player.getY() > floor + 8.0, "player only at " + (player.getY() - floor) + " above the floor");
		});
	}

	/**
	 * A player inside the trigger area but not under the sleeper: it drops, misses, hangs on its thread, and is a
	 * vanilla spider, at full health (a missed drop from a 14-high nest takes no fall damage: owner bug).
	 */
	@GameTest(environment = SpiderGameTest.ENV, structure = ROOM, maxTicks = 160)
	public void aMissTurnsItIntoAVanillaSpider(GameTestHelper helper) {
		NestSpider spider = nest(helper);
		ServerPlayer player = TestPlayers.survivalAt(helper, 4.5 + 2.2, 4.5);
		boolean[] dangled = {false};
		helper.onEachTick(() -> dangled[0] |= !spider.isRemoved() && spider.phase() == NestSpider.Phase.DANGLING);
		helper.succeedWhen(() -> {
			helper.assertTrue(dangled[0], "it never hung on its thread");
			helper.assertTrue(spider.isRemoved(), "nest spider still there: " + spider.phase());
			List<Spider> spiders = helper.getLevel().getEntitiesOfClass(Spider.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12.0),
					s -> s.getType() == EntityTypes.SPIDER);
			helper.assertTrue(spiders.size() == 1, "vanilla spiders: " + spiders.size());
			helper.assertTrue(!spiders.getFirst().isNoAi(), "the vanilla spider has no AI");
			Spider released = spiders.getFirst();
			helper.assertTrue(released.onGround(), "the released spider is still falling");
			helper.assertTrue(released.getHealth() == released.getMaxHealth(), "released spider hurt: " + released.getHealth() + "/" + released.getMaxHealth());
			helper.assertTrue(player.getVehicle() == null, "player held after a miss");
		});
	}

	/** Hitting a spider that holds you loosens its grip, slowly; at 0 it lets go and becomes a vanilla spider. */
	@GameTest(environment = SpiderGameTest.ENV, structure = ROOM, maxTicks = 400)
	public void hitsBreakTheGrip(GameTestHelper helper) {
		NestSpider spider = nest(helper);
		ServerPlayer player = TestPlayers.survivalAt(helper, 4.5, 4.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		// Tough enough not to die first: this measures the grip, not the kill (an axe kills a nest spider in two hits).
		spider.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500.0);
		spider.setHealth(500.0F);
		helper.runAfterDelay(40, () -> helper.assertTrue(spider.victim() == player, "not latched after 40 ticks: " + spider.phase()));
		// Full iron axe hits (9) every 22 ticks, an axe's attack speed: (6 + 3 x 9) x 1.5 x GRIP_HIT_FACTOR a hit against
		// a grip that keeps tightening. It takes many (owner: hard to break free), but it does let go.
		int[] hits = {0};
		helper.onEachTick(() -> {
			if (helper.getTick() > 40 && (helper.getTick() - 41) % 22 == 0 && !spider.isRemoved() && spider.victim() == player) {
				spider.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 9.0F);
				hits[0]++;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(spider.isRemoved(), "still holding, grip " + spider.grip() + " after " + hits[0] + " hits");
			helper.assertTrue(player.getVehicle() == null, "player still held");
			helper.assertTrue(hits[0] >= 3, "broke free too easily: " + hits[0] + " hits");
			helper.assertTrue(player.getY() > helper.absoluteVec(Vec3.ZERO).y + 5.0, "freed too low to matter: " + (player.getY() - helper.absoluteVec(Vec3.ZERO).y));
		});
	}

	/** Worldgen: a nest of cobwebs with a brain and sleepers at the top of a tall room; none in a low one. */
	@GameTest(environment = SpiderGameTest.ENV, structure = ROOM, maxTicks = 20)
	public void nestsGenerateAtTheTopOfTallRooms(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(BlockPos.ZERO);
		BlockPos middle = helper.absolutePos(new BlockPos(4, 0, 4));
		int drop = SpiderNestFeature.findCeiling(level, middle.getX(), middle.getZ(), floor.getY() + 15, floor.getY() + 1, RandomSource.create(42));
		helper.assertTrue(drop >= SpiderTuning.NEST_MIN_DROP, "no nest found, drop " + drop);
		int webs = 0;
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 12, 0)), helper.absolutePos(new BlockPos(8, 14, 8)))) {
			if (level.getBlockState(pos).is(Blocks.COBWEB)) {
				webs++;
			}
		}
		helper.assertTrue(webs >= SpiderTuning.NEST_MIN_WEBS_ALIVE, "webs " + webs);
		AABB room = new AABB(floor).expandTowards(9, 16, 9);
		helper.assertTrue(level.getEntitiesOfClass(SpiderNest.class, room).size() == 1, "nest brains");
		int sleepers = level.getEntitiesOfClass(NestSpider.class, room, NestSpider::sleeping).size();
		helper.assertTrue(sleepers >= SpiderTuning.NEST_SLEEPERS_MIN, "sleepers " + sleepers);

		// A low room: put a ceiling at y 6 and search below it.
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 6, 0)), helper.absolutePos(new BlockPos(8, 6, 8)))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		int low = SpiderNestFeature.findCeiling(level, middle.getX(), middle.getZ(), floor.getY() + 6, floor.getY() + 1, RandomSource.create(7));
		helper.assertTrue(low == Integer.MIN_VALUE, "nest in a low room, drop " + low);
		helper.succeed();
	}
}
