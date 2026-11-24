// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.spider.GiantSpider;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderTerritories;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderTuning;
import io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpunWeb;
import io.github.bubba8587.bubbas_reworked_mobs.spider.WebSettleGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Giant spider territories (specs/spiders.md "Territories"). The field is 20x16x20, open, stone floor; two rows of
 * spruce trunks 8 apart, so two webs fit 8 apart and no web spans the rows.
 */
public class SpiderTerritoryGameTest {
	static final String FIELD = "bubbas_reworked_mobs-test:territory_field";

	private static void trunkRows(GameTestHelper helper) {
		for (int y = 1; y <= 12; y++) {
			for (int x : new int[] {3, 9}) {
				helper.setBlock(new BlockPos(x, y, 4), Blocks.SPRUCE_LOG);
				helper.setBlock(new BlockPos(x, y, 12), Blocks.SPRUCE_LOG);
			}
		}
	}

	private static void roomTrunks(GameTestHelper helper) {
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
		giant.setPersistenceRequired();
		giant.setNoAi(true);
		level.addFreshEntity(giant);
		return giant;
	}

	private static SpunWeb fakeWeb(GameTestHelper helper, double x, double y, double z) {
		ServerLevel level = helper.getLevel();
		SpunWeb web = Spiders.SPUN_WEB.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
		web.setPos(at.x, at.y, at.z);
		web.setStrands(List.of(new SpunWeb.Strand(new Vec3(-1.5, 0, 0), new Vec3(1.5, 0, 0)), new SpunWeb.Strand(new Vec3(0, -1.5, 0), new Vec3(0, 1.5, 0))));
		level.addFreshEntity(web);
		return web;
	}

	/** Core: a giant's first web claims the 2x2 chunk square round it; a second giant can't spin inside it. */
	public void claimsATerritoryOthersCantWebIn(GameTestHelper helper) {
		roomTrunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider first = giant(helper, 4.5, 2.5);
		helper.assertTrue(first.spin(level) >= 3, "no home web spun");
		SpiderTerritories.Territory t = first.territory();
		helper.assertTrue(t != null, "no territory claimed");
		SpunWeb home = first.ownWebs().getFirst();
		BlockPos hub = BlockPos.containing(home.position());
		helper.assertTrue(t.contains(hub), "the home web's chunk isn't in it");
		helper.assertValueEqual(t.webs().getFirst().id(), home.getUUID(), "home web");
		int inside = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				ChunkPos c = new ChunkPos((hub.getX() >> 4) + dx, (hub.getZ() >> 4) + dz);
				if (SpiderTerritories.at(level, new BlockPos(c.getMinBlockX(), hub.getY(), c.getMinBlockZ())) == t) {
					inside++;
				}
			}
		}
		helper.assertValueEqual(inside, 4, "chunks claimed round the home web");
		GiantSpider second = giant(helper, 4.5, 2.5);
		helper.assertValueEqual(second.spin(level), 0, "strands a second giant spun inside the territory");
		helper.assertTrue(second.territory() == null, "the second giant claimed land");
		helper.assertTrue(!SpiderTerritories.mayWeb(level, second.getUUID(), home.position()), "the second giant may web there");
		helper.succeed();
	}

	/** Its home web and up to 4 subsidiary webs, all inside the territory: with 5 it spins no more. */
	@GameTest(environment = "bubbas_reworked_mobs-test:spider_territory_webs", structure = FIELD)
	public void homeWebAndFourSubsidiaryWebs(GameTestHelper helper) {
		trunkRows(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 6.5, 8.5);
		helper.assertTrue(giant.spin(level) >= 3, "no home web");
		SpiderTerritories.Territory t = giant.territory();
		helper.assertTrue(t != null, "no territory");
		helper.assertTrue(giant.spin(level) >= 3, "no subsidiary web on the other row");
		helper.assertValueEqual(t.webs().size(), 2, "territory webs");
		for (SpiderTerritories.WebSpot spot : t.webs()) {
			helper.assertTrue(t.contains(spot.hub()), "a web outside the territory at " + spot.hub());
		}
		SpunWeb second = SpiderTerritories.liveWebs(level, t).get(1);
		second.discard();
		List<SpunWeb> fakes = new ArrayList<>();
		double y = second.getY() - helper.absoluteVec(Vec3.ZERO).y;
		for (double[] at : new double[][] {{14.5, 4.5}, {14.5, 12.5}, {17.5, 8.5}, {12.5, 8.5}}) {
			SpunWeb fake = fakeWeb(helper, at[0], y, at[1]);
			SpiderTerritories.addWeb(level, t, fake);
			fakes.add(fake);
		}
		SpiderTerritories.liveWebs(level, t);
		helper.assertValueEqual(t.webs().size(), SpiderTuning.TERRITORY_WEBS, "webs after the fakes");
		helper.assertValueEqual(giant.spin(level), 0, "strands spun past the cap");
		fakes.getFirst().discard();
		helper.assertTrue(giant.spin(level) >= 3, "no web once there was room again");
		helper.assertValueEqual(SpiderTerritories.liveWebs(level, t).size(), SpiderTuning.TERRITORY_WEBS, "live webs");
		helper.assertValueEqual(SpiderTerritories.subsidiaryWebs(level, t).size(), SpiderTuning.TERRITORY_WEBS - 1, "subsidiary webs");
		helper.succeed();
	}

	/** A vanilla spider in the territory settles by a subsidiary web (not the home web): its home is that web's hub. */
	@GameTest(environment = "bubbas_reworked_mobs-test:spider_territory_settle", structure = FIELD, maxTicks = 400)
	public void aSpiderSettlesByASubsidiaryWeb(GameTestHelper helper) {
		trunkRows(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 6.5, 8.5);
		helper.assertTrue(giant.spin(level) >= 3 && giant.spin(level) >= 3, "webs not spun");
		SpiderTerritories.Territory t = giant.territory();
		SpunWeb sub = SpiderTerritories.subsidiaryWebs(level, t).getFirst();
		BlockPos subHub = BlockPos.containing(sub.position());
		Spider spider = EntityTypes.SPIDER.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(10.5, 1.0, 8.5));
		spider.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		spider.setPersistenceRequired();
		level.addFreshEntity(spider);
		helper.succeedWhen(() -> {
			helper.assertTrue(spider.entityTags().contains(WebSettleGoal.SETTLER_TAG), "not settled");
			helper.assertValueEqual(spider.getHomePosition(), subHub, "home");
			double d = Math.hypot(spider.getX() - sub.getX(), spider.getZ() - sub.getZ());
			helper.assertTrue(d < 4.0, "still " + d + " from its web");
		});
	}

	/** The territory is released when its giant dies, and another giant can claim there. */
	@GameTest(environment = "bubbas_reworked_mobs-test:spider_territory_death", structure = SpiderGameTest.ROOM, maxTicks = 100)
	public void deathReleasesTheTerritory(GameTestHelper helper) {
		roomTrunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 2.5);
		helper.assertTrue(giant.spin(level) >= 3, "no home web");
		java.util.UUID id = giant.getUUID();
		helper.assertTrue(SpiderTerritories.of(level, id) != null, "no territory");
		giant.kill(level);
		helper.succeedWhen(() -> {
			helper.assertTrue(giant.isRemoved(), "not gone yet");
			helper.assertTrue(SpiderTerritories.of(level, id) == null, "territory still held");
			GiantSpider next = giant(helper, 4.5, 2.5);
			boolean claimed = next.spin(level) > 0 && next.territory() != null;
			next.discard();
			helper.assertTrue(claimed, "the next giant couldn't claim there");
		});
	}

	/** A territory, its webs and its owner are saved in the level's attachment and read back after a reload. */
	@GameTest(environment = "bubbas_reworked_mobs-test:spider_territory_save", structure = SpiderGameTest.ROOM)
	public void savedAcrossAReload(GameTestHelper helper) {
		roomTrunks(helper);
		ServerLevel level = helper.getLevel();
		GiantSpider giant = giant(helper, 4.5, 2.5);
		helper.assertTrue(giant.spin(level) >= 3, "no home web");
		SpiderTerritories.Territory before = giant.territory();
		var encoded = SpiderTerritories.SAVED.persistenceCodec().encodeStart(NbtOps.INSTANCE, level.getAttached(SpiderTerritories.SAVED)).getOrThrow();
		List<SpiderTerritories.Territory> decoded = SpiderTerritories.SAVED.persistenceCodec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
		helper.assertTrue(decoded.stream().anyMatch(d -> d.owner.equals(giant.getUUID()) && d.minX == before.minX && d.minZ == before.minZ
				&& d.webs().equals(before.webs())), "not in the saved data");
		SpiderTerritories.reloadFromSaved(level);
		SpiderTerritories.Territory after = giant.territory();
		helper.assertTrue(after != null && after.minX == before.minX && after.minZ == before.minZ && after.webs().equals(before.webs()),
				"not read back: " + after);
		helper.assertTrue(SpiderTerritories.at(level, after.webs().getFirst().hub()) == after, "chunk lookup lost");
		helper.succeed();
	}
}
