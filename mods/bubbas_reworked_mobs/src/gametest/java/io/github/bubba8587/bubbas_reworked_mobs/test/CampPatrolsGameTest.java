// dte:B15,B17,B21,C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.api.Endermen;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampBanners;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampKind;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPatrols;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPiece;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSite;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSites;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Camp patrols (specs/monster-camps.md "Camp patrols", milestone test objectives). Milestone; the spawn test is core
 * (through ReworkedMobsCoreGameTest). Patrols here spawn 6-10 blocks from the player so they stay on the test's field.
 */
public class CampPatrolsGameTest {
	private static final BlockPos CENTRE = new BlockPos(16, 8, 16);
	private static final int NEAR = 6;
	private static final int FAR = 10;

	private static CampSite camp(GameTestHelper helper, CampKind kind, long seed) {
		CampPiece piece = CampCommand.build(helper.getLevel(), helper.absolutePos(CENTRE), kind, CampTuning.Size.SMALL, seed);
		for (CampSite site : CampSites.all(helper.getLevel())) {
			if (site.centre().equals(piece.centre())) {
				return site;
			}
		}
		throw helper.assertionException("camp not registered");
	}

	/** A creative player (no roster appears for it) at the far end of the field. */
	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.connected(helper, GameType.CREATIVE);
		Vec3 at = helper.absoluteVec(new Vec3(16.5, 8.0, 35.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		return player;
	}

	private static String tag(CampSite site) {
		return CampTuning.PATROL_TAG + ":" + site.id();
	}

	private static void discard(GameTestHelper helper, List<Mob> mobs) {
		for (Mob mob : mobs) {
			Entity root = mob.getRootVehicle();
			root.getPassengers().forEach(Entity::discard);
			root.discard();
			mob.discard();
		}
	}

	private static List<Mob> tryUntil(GameTestHelper helper, ServerPlayer player) {
		RandomSource random = RandomSource.create(5);
		for (int i = 0; i < 6; i++) {
			List<Mob> mobs = CampPatrols.trySpawn(helper.getLevel(), player, random, NEAR, FAR);
			if (!mobs.isEmpty()) {
				return mobs;
			}
		}
		return List.of();
	}

	/** Core. At night a player in an armed camp's territory gets a patrol of its kind, size-scaled, led by one banner carrier. */
	public void patrolSpawnsFromAnArmedCamp(GameTestHelper helper) {
		CampSite site = camp(helper, CampKind.SKELETON, 21L);
		ServerPlayer player = player(helper);
		List<Mob> patrol = tryUntil(helper, player);
		TestCleanup.whenDone(helper, () -> discard(helper, patrol));
		helper.assertTrue(!patrol.isEmpty(), "a patrol should appear for a player in an armed camp's territory");
		helper.assertTrue(patrol.size() >= CampTuning.PATROL_SIZE_MIN && patrol.size() <= CampTuning.PATROL_SIZE_MIN + 1, "small camp patrol of " + patrol.size());
		int leaders = 0;
		for (Mob mob : patrol) {
			helper.assertTrue(mob.getType() == EntityTypes.SKELETON && mob.entityTags().contains(tag(site)), "a member of this camp's kind: " + mob);
			helper.assertTrue(!mob.entityTags().contains(CampTuning.CAMP_TAG), "not on the camp's roster");
			if (mob.entityTags().contains(CampTuning.PATROL_LEADER_TAG)) {
				leaders++;
				ItemStack head = mob.getItemBySlot(EquipmentSlot.HEAD);
				helper.assertTrue(head.is(CampBanners.standing(CampKind.SKELETON).asItem())
						&& head.get(DataComponents.BANNER_PATTERNS).equals(CampBanners.item(CampKind.SKELETON, helper.getLevel().registryAccess())
								.get(DataComponents.BANNER_PATTERNS)), "the leader wears the camp's banner: " + head);
			}
		}
		helper.assertTrue(leaders == 1, "one leader, found " + leaders);
		helper.succeed();
	}

	/** No patrol from a disarmed camp (banners broken) or one quieted by a lit fire. */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_night", structure = CampsGameTest.FIELD, maxTicks = 5)
	public void noPatrolFromADisarmedOrQuietCamp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampSite site = camp(helper, CampKind.ZOMBIE, 23L);
		ServerPlayer player = player(helper);
		BlockPos fire = site.centre().offset(-4, 1, 3);
		level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
		CampSites.recheck(level, site);
		helper.assertTrue(site.quiet() && !CampPatrols.ownsTerritory(site), "a quiet camp owns no territory");
		List<Mob> quiet = tryUntil(helper, player);
		TestCleanup.whenDone(helper, () -> discard(helper, quiet));
		helper.assertTrue(quiet.stream().noneMatch(m -> m.entityTags().contains(tag(site))), "no patrol from a quiet camp");
		level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false), 3);
		var box = site.box();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (level.getBlockEntity(pos) instanceof BannerBlockEntity) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		CampSites.recheck(level, site);
		helper.assertTrue(!site.armed() && !site.quiet() && !CampPatrols.ownsTerritory(site), "a disarmed camp owns no territory");
		List<Mob> disarmed = tryUntil(helper, player);
		TestCleanup.whenDone(helper, () -> discard(helper, disarmed));
		helper.assertTrue(disarmed.stream().noneMatch(m -> m.entityTags().contains(tag(site))), "no patrol from a disarmed camp");
		helper.succeed();
	}

	/** Outside every territory there is no camp to send one, and a ward round the player stops the spawn. */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_night", structure = CampsGameTest.FIELD, maxTicks = 5)
	public void noPatrolOutsideTerritoryOrInAWard(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampSite site = camp(helper, CampKind.SKELETON, 25L);
		BlockPos outside = site.centre().offset(CampTuning.PATROL_TERRITORY + 40, 0, 0);
		CampSite other = CampPatrols.campFor(level, outside);
		helper.assertTrue(other != site, "a point beyond the territory isn't this camp's");
		helper.assertTrue(other == null || Math.hypot(other.centre().getX() - outside.getX(), other.centre().getZ() - outside.getZ()) <= CampTuning.PATROL_TERRITORY,
				"a camp found for a point holds it in its territory");
		ServerPlayer player = player(helper);
		Object ward = new Object();
		Endermen.keepOut(level, ward, new AABB(player.blockPosition()).inflate(FAR + 4), 100);
		List<Mob> warded = tryUntil(helper, player);
		Endermen.release(level, ward);
		TestCleanup.whenDone(helper, () -> discard(helper, warded));
		helper.assertTrue(warded.isEmpty(), "no patrol for a warded player");
		helper.succeed();
	}

	/** The leader drops a filled map centred on its camp with the camp marked, and never its banner. */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_night", structure = CampsGameTest.FIELD, maxTicks = 20)
	public void leaderDropsAMapNotItsBanner(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampSite site = camp(helper, CampKind.ZOMBIE, 27L);
		List<Mob> patrol = CampPatrols.spawn(level, site, helper.absolutePos(new BlockPos(16, 8, 34)), helper.absolutePos(new BlockPos(16, 8, 38)), false,
				RandomSource.create(3));
		TestCleanup.whenDone(helper, () -> discard(helper, patrol));
		Mob leader = patrol.stream().filter(m -> m.entityTags().contains(CampTuning.PATROL_LEADER_TAG)).findFirst().orElseThrow();
		Vec3 at = leader.position();
		leader.kill(level);
		helper.runAfterDelay(2, () -> {
			List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(3.0));
			TestCleanup.whenDone(helper, () -> drops.forEach(Entity::discard));
			ItemStack map = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(Items.FILLED_MAP)).findFirst().orElse(null);
			helper.assertTrue(map != null, "the leader drops a filled map: " + drops);
			MapItemSavedData data = level.getMapData(map.get(DataComponents.MAP_ID));
			helper.assertTrue(data != null && data.centerX == site.centre().getX() && data.centerZ == site.centre().getZ() && data.scale == CampPatrols.mapZoom(),
					"the map is centred on the camp at the territory's zoom");
			helper.assertTrue((128 << data.scale) >= 2 * CampTuning.PATROL_TERRITORY, "the territory fits on the map");
			var decorations = map.get(DataComponents.MAP_DECORATIONS);
			helper.assertTrue(decorations != null && !decorations.decorations().isEmpty(), "the camp is marked");
			helper.assertTrue(drops.stream().noneMatch(d -> d.getItem().getItem() instanceof net.minecraft.world.item.BannerItem), "no banner drops");
			helper.succeed();
		});
	}

	/** On the move, the members keep within reach of the leader (its own batch: no other test's players to chase). */
	@GameTest(environment = "bubbas_reworked_mobs-test:patrol_walk", structure = CampsGameTest.FIELD, maxTicks = 600)
	public void membersStayNearTheLeader(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampSite site = camp(helper, CampKind.ZOMBIE, 29L);
		BlockPos from = helper.absolutePos(new BlockPos(5, 8, 40));
		BlockPos to = helper.absolutePos(new BlockPos(28, 8, 40));
		List<Mob> patrol = CampPatrols.spawn(level, site, from, to, false, RandomSource.create(9));
		TestCleanup.whenDone(helper, () -> discard(helper, patrol));
		helper.assertTrue(patrol.size() >= 2, "a patrol of " + patrol.size());
		patrol.forEach(Mob::setPersistenceRequired); // players in other tests would despawn them
		Mob leader = patrol.stream().filter(m -> m.entityTags().contains(CampTuning.PATROL_LEADER_TAG)).findFirst().orElseThrow();
		Vec3 start = leader.position();
		List<Double> spread = new ArrayList<>();
		helper.onEachTick(() -> {
			for (Mob mob : patrol) {
				mob.setTarget(null); // players in neighbouring tests are no part of this one
				spread.add((double) mob.distanceTo(leader));
			}
		});
		helper.succeedWhen(() -> {
			double worst = spread.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
			helper.assertTrue(worst <= CampTuning.PATROL_COMPANION_RANGE, "members stay within reach, worst " + worst);
			helper.assertTrue(leader.isAlive(), "the leader is still there: " + leader.getRemovalReason());
			helper.assertTrue(leader.position().distanceTo(start) > 8.0, "the leader walks: moved " + leader.position().distanceTo(start));
		});
	}

	/** A mounted patrol has riders: zombies on zombie horses, skeletons on skeleton horses, husks on camel husks with a parched. */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_night", structure = CampsGameTest.FIELD, maxTicks = 5)
	public void mountedPatrolHasRiders(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampSite site = camp(helper, CampKind.HUSK, 31L);
		helper.assertTrue(!CampPatrols.campHasStable(site), "a small camp has no stable");
		Object[][] cases = { { CampKind.ZOMBIE, EntityTypes.ZOMBIE_HORSE }, { CampKind.SKELETON, EntityTypes.SKELETON_HORSE },
				{ CampKind.HUSK, EntityTypes.CAMEL_HUSK } };
		int x = 6;
		for (Object[] c : cases) {
			CampKind kind = (CampKind) c[0];
			CampSite kindSite = kind == CampKind.HUSK ? site : CampSites.register(level, new CampPiece(1L, site.centre().offset(x * 100, 0, 0), kind,
					CampTuning.Size.LARGE));
			helper.assertTrue(kind == CampKind.HUSK || CampPatrols.campHasStable(kindSite), "a large camp has a stable");
			List<Mob> patrol = CampPatrols.spawn(level, kindSite, helper.absolutePos(new BlockPos(x, 8, 38)), helper.absolutePos(new BlockPos(x, 8, 30)), true,
					RandomSource.create(4));
			TestCleanup.whenDone(helper, () -> discard(helper, patrol));
			Mob leader = patrol.stream().filter(m -> m.entityTags().contains(CampTuning.PATROL_LEADER_TAG)).findFirst().orElseThrow();
			helper.assertTrue(leader.getType() == kind.mob() && leader.getVehicle() != null && leader.getVehicle().getType() == c[1],
					kind.id() + " leader rides " + c[1] + ": " + leader.getVehicle());
			helper.assertTrue(leader.getVehicle().entityTags().contains(CampTuning.PATROL_MOUNT_TAG), "the mount is tagged");
			if (kind == CampKind.HUSK) {
				helper.assertTrue(leader.getVehicle().getPassengers().stream().anyMatch(p -> p.getType() == EntityTypes.PARCHED), "a parched rides behind");
			}
			x += 10;
		}
		helper.succeed();
	}
}
