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

	private static ServerPlayer player(GameTestHelper helper, double x, double z) {
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/** Core: a player standing right under the nest is dropped on, latched and reeled up into the nest. */
	@GameTest(structure = ROOM, maxTicks = 300)
	public void sleeperDropsLatchesAndReelsUp(GameTestHelper helper) {
		NestSpider spider = nest(helper);
		ServerPlayer player = player(helper, 4.5, 4.5);
		double floor = player.getY();
		helper.succeedWhen(() -> {
			helper.assertTrue(spider.isAlive() && spider.victim() == player, "not latched: phase " + spider.phase());
			helper.assertTrue(spider.phase() == NestSpider.Phase.HOLDING, "still " + spider.phase());
			helper.assertTrue(player.getVehicle() == spider, "player not held");
			helper.assertTrue(player.getY() > floor + 8.0, "player only at " + (player.getY() - floor) + " above the floor");
			helper.assertTrue(player.getHealth() < player.getMaxHealth(), "never bitten");
		});
	}

	/** A player inside the trigger area but not under the sleeper: it drops, misses, hangs on its thread, and is a vanilla spider. */
	@GameTest(structure = ROOM, maxTicks = 100)
	public void aMissTurnsItIntoAVanillaSpider(GameTestHelper helper) {
		NestSpider spider = nest(helper);
		ServerPlayer player = player(helper, 4.5 + 2.2, 4.5);
		boolean[] dangled = {false};
		helper.onEachTick(() -> dangled[0] |= !spider.isRemoved() && spider.phase() == NestSpider.Phase.DANGLING);
		helper.succeedWhen(() -> {
			helper.assertTrue(dangled[0], "it never hung on its thread");
			helper.assertTrue(spider.isRemoved(), "nest spider still there: " + spider.phase());
			List<Spider> spiders = helper.getLevel().getEntitiesOfClass(Spider.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12.0),
					s -> s.getType() == EntityTypes.SPIDER);
			helper.assertTrue(spiders.size() == 1, "vanilla spiders: " + spiders.size());
			helper.assertTrue(!spiders.getFirst().isNoAi(), "the vanilla spider has no AI");
			helper.assertTrue(player.getVehicle() == null, "player held after a miss");
		});
	}

	/** A missed drop from a 14-high nest leaves the released spider at full health (no fall damage from the drop: owner bug). */
	@GameTest(structure = ROOM, maxTicks = 140)
	public void aMissedDropTakesNoFallDamage(GameTestHelper helper) {
		NestSpider spider = nest(helper);
		player(helper, 4.5 + 2.2, 4.5);
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(spider.isRemoved(), "nest spider still there: " + spider.phase());
			List<Spider> spiders = helper.getLevel().getEntitiesOfClass(Spider.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12.0),
					s -> s.getType() == EntityTypes.SPIDER);
			helper.assertTrue(spiders.size() == 1, "vanilla spiders: " + spiders.size());
			Spider released = spiders.getFirst();
			helper.assertTrue(released.getHealth() == released.getMaxHealth(), "released spider hurt: " + released.getHealth() + "/" + released.getMaxHealth());
			helper.succeed();
		});
	}

	/** Hitting a spider that holds you loosens its grip; at 0 it lets go and becomes a vanilla spider. */
	@GameTest(structure = ROOM, maxTicks = 300)
	public void hitsBreakTheGrip(GameTestHelper helper) {
		NestSpider spider = nest(helper);
		ServerPlayer player = player(helper, 4.5, 4.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		helper.runAfterDelay(40, () -> helper.assertTrue(spider.victim() == player, "not latched after 40 ticks: " + spider.phase()));
		// Four iron axe hits, past the hurt cooldown each time: (6 + 3 x 2) x 1.5 = 18 grip points a hit.
		for (int i = 0; i < 4; i++) {
			helper.runAfterDelay(41 + 12 * i, () -> {
				if (!spider.isRemoved()) {
					spider.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 2.0F);
				}
			});
		}
		helper.runAfterDelay(41 + 12 * 4, () -> {
			helper.assertTrue(spider.isRemoved(), "still holding, grip " + spider.grip());
			helper.assertTrue(player.getVehicle() == null, "player still held");
			helper.succeed();
		});
	}

	/** Worldgen: a nest of cobwebs with a brain and sleepers at the top of a tall room; none in a low one. */
	@GameTest(structure = ROOM, maxTicks = 20)
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
