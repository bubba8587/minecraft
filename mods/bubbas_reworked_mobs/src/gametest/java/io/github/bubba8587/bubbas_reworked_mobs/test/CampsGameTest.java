// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampBanners;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampKind;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPiece;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampRoster;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSite;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSites;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Monster camps (specs/monster-camps.md "Milestone test objectives"). Milestone; the roster test is core (through
 * ReworkedMobsCoreGameTest).
 */
public class CampsGameTest {
	private static final String FIELD = CreeperGameTest.FIELD;
	private static final BlockPos CENTRE = new BlockPos(11, 1, 8);

	private static CampPiece camp(GameTestHelper helper, CampKind kind, long seed) {
		return CampCommand.build(helper.getLevel(), helper.absolutePos(CENTRE), kind, CampTuning.Size.SMALL, seed);
	}

	private static CampSite site(GameTestHelper helper, CampPiece piece) {
		for (CampSite site : CampSites.all(helper.getLevel())) {
			if (site.centre().equals(piece.centre())) {
				return site;
			}
		}
		throw helper.assertionException("camp not registered");
	}

	private static List<BlockPos> banners(ServerLevel level, BoundingBox box) {
		List<BlockPos> found = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (level.getBlockEntity(pos) instanceof BannerBlockEntity) {
				found.add(pos.immutable());
			}
		}
		return found;
	}

	/** Core. A survival player within range makes the roster appear: a small camp's three and one larger, tougher, better-geared leader. */
	public void rosterAppearsWithALeader(GameTestHelper helper) {
		CampPiece piece = camp(helper, CampKind.SKELETON, 7L);
		CampSite site = site(helper, piece);
		helper.assertTrue(site.armed() && !site.spawned(), "a new camp is armed and waiting");
		helper.assertTrue(!banners(helper.getLevel(), piece.getBoundingBox()).isEmpty(), "the camp should fly at least one banner");
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		helper.succeedWhen(() -> {
			helper.assertTrue(site.spawned(), "the roster should appear with a player near");
			helper.assertTrue(site.members().size() == CampTuning.Size.SMALL.roster + 1, "roster of " + site.members().size());
			int leaders = 0;
			for (var uuid : site.members()) {
				Entity e = helper.getLevel().getEntity(uuid);
				helper.assertTrue(e instanceof Mob && e.getType() == CampKind.SKELETON.mob(), "a camp member is a skeleton: " + e);
				Mob mob = (Mob) e;
				if (CampRoster.isLeader(mob)) {
					leaders++;
					helper.assertTrue(mob.getAttributeValue(Attributes.SCALE) > 1.0 && mob.getMaxHealth() > 20.0F
							&& mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "the leader is larger, tougher, geared");
					helper.assertTrue(!(mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof net.minecraft.world.item.BannerItem), "no banner on the leader");
				}
			}
			helper.assertTrue(leaders == 1, "one leader, found " + leaders);
			helper.getLevel().getServer().getPlayerList().remove(player);
			for (var uuid : site.members()) {
				Entity e = helper.getLevel().getEntity(uuid);
				if (e != null) {
					e.discard();
				}
			}
		});
	}

	/**
	 * Breaking every banner stops a cleared camp for good (it never resets); putting one of its banners back in the
	 * area re-arms it, and it resets at the next check.
	 */
	@GameTest(structure = FIELD, maxTicks = 40)
	public void bannersAreTheKey(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = camp(helper, CampKind.ZOMBIE, 11L);
		CampSite site = site(helper, piece);
		List<Mob> roster = CampRoster.spawn(level, site);
		helper.assertTrue(!roster.isEmpty(), "roster spawned");
		for (BlockPos banner : banners(level, piece.getBoundingBox())) {
			level.setBlock(banner, Blocks.AIR.defaultBlockState(), 3);
		}
		helper.assertTrue(Boolean.FALSE.equals(CampSites.recheck(level, site)) && !site.armed(), "no banner standing: disarmed");
		roster.forEach(Entity::discard);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(site.members().isEmpty(), "the roster's removal is counted: " + site.members().size());
			CampSites.resetCheck(level, true);
			helper.assertTrue(site.spawned(), "a camp with no banner never resets");
			// Put a zombie camp banner back at the edge of the camp.
			BlockPos edge = piece.centre().offset(4, 1, 0);
			level.setBlock(edge, CampBanners.standing(CampKind.ZOMBIE).defaultBlockState().setValue(BannerBlock.ROTATION, 0), 3);
			CampBanners.paint(level.getBlockEntity(edge), CampKind.ZOMBIE, level.registryAccess());
			helper.assertTrue(Boolean.TRUE.equals(CampSites.recheck(level, site)) && site.armed(), "a banner put back re-arms it");
			CampSites.resetCheck(level, false);
			helper.assertTrue(!site.spawned(), "re-armed, it resets at the next check");
			helper.succeed();
		});
	}

	/** A banner of another kind doesn't arm a camp. */
	@GameTest(structure = FIELD)
	public void otherBannersDontCount(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = camp(helper, CampKind.STRAY, 13L);
		CampSite site = site(helper, piece);
		for (BlockPos banner : banners(level, piece.getBoundingBox())) {
			level.setBlock(banner, Blocks.AIR.defaultBlockState(), 3);
		}
		BlockPos edge = piece.centre().offset(4, 1, 0);
		level.setBlock(edge, CampBanners.standing(CampKind.SKELETON).defaultBlockState(), 3);
		CampBanners.paint(level.getBlockEntity(edge), CampKind.SKELETON, level.registryAccess());
		helper.assertTrue(Boolean.FALSE.equals(CampSites.recheck(level, site)), "a skeleton banner doesn't arm a stray camp");
		helper.succeed();
	}

	/** An outpost with every banner broken spawns no pillagers; putting one back restores them. */
	@GameTest(structure = FIELD)
	public void outpostWithoutBannersSpawnsNoPillagers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos banner = helper.absolutePos(new BlockPos(5, 1, 5));
		BoundingBox box = new BoundingBox(helper.absolutePos(new BlockPos(0, 0, 0))).encapsulate(helper.absolutePos(new BlockPos(23, 3, 15)));
		level.setBlock(banner, Blocks.BANNER.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(), 3);
		CampSite outpost = CampSites.registerOutpost(level, box);
		BlockPos inside = helper.absolutePos(new BlockPos(10, 1, 10));
		helper.assertTrue(Boolean.TRUE.equals(CampSites.recheck(level, outpost)) && !CampSites.disarmedOutpostAt(level, inside), "banner up: armed");
		level.setBlock(banner, Blocks.AIR.defaultBlockState(), 3);
		helper.assertTrue(Boolean.FALSE.equals(CampSites.recheck(level, outpost)) && CampSites.disarmedOutpostAt(level, inside), "banner gone: disarmed");
		var spawns = level.getChunkSource().getGenerator().getMobsAt(level, level.structureManager(), net.minecraft.world.entity.MobCategory.MONSTER, inside);
		helper.assertTrue(spawns.unwrap().stream().noneMatch(d -> d.value().type() == net.minecraft.world.entity.EntityTypes.PILLAGER), "no pillagers");
		level.setBlock(banner, Blocks.BANNER.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(), 3);
		helper.assertTrue(Boolean.TRUE.equals(CampSites.recheck(level, outpost)) && !CampSites.disarmedOutpostAt(level, inside), "banner back: armed again");
		helper.succeed();
	}
}
