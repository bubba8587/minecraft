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
	/** A deep 32 x 48 field (crypt yards fit too): camps (box out to 10 from the centre, room 5 down) stay inside it, away from other tests. */
	static final String FIELD = "bubbas_reworked_mobs-test:camp_field";
	private static final BlockPos CENTRE = new BlockPos(16, 8, 16);

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
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 8.0, 2.5));
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
	@GameTest(structure = CreeperGameTest.FIELD)
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

	/**
	 * Lighting a fire quiets a camp (owner): with a lit campfire in its area the roster doesn't appear and the camp
	 * doesn't reset; put the fire out and both work again.
	 */
	@GameTest(structure = FIELD, maxTicks = 120)
	public void aLitFireQuietsTheCamp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = camp(helper, CampKind.SKELETON, 17L);
		CampSite site = site(helper, piece);
		BlockPos fire = piece.centre().offset(-4, 1, 3);
		level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true), 3);
		CampSites.recheck(level, site);
		helper.assertTrue(site.quiet(), "a lit campfire in the camp quiets it");
		helper.assertTrue(piece.getBoundingBox().isInside(fire), "the fire is inside the camp");
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 8.0, 2.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(!site.spawned(), "no roster while the fire burns");
			// Quiet camps don't reset either: a cleared, armed camp stays cleared.
			level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 3);
		});
		helper.runAfterDelay(90, () -> {
			helper.assertTrue(!site.quiet() && site.spawned(), "fire out: the roster appears");
			helper.getLevel().getServer().getPlayerList().remove(player);
			for (var uuid : site.members()) {
				Entity e = level.getEntity(uuid);
				if (e != null) {
					e.discard();
				}
			}
			helper.runAfterDelay(2, () -> {
				level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true), 3);
				CampSites.recheck(level, site);
				CampSites.resetCheck(level, true);
				helper.assertTrue(site.spawned(), "a quiet camp doesn't reset");
				helper.succeed();
			});
		});
	}

	/** Every camp has shade to spare: the underground room (all its floor cells usable) plus the lean-to and the towers. */
	@GameTest(structure = FIELD, maxTicks = 80)
	public void campsHaveShadeAndARoom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = CampCommand.build(level, helper.absolutePos(CENTRE), CampKind.ZOMBIE, CampTuning.Size.SMALL, 23L);
		CampSite site = site(helper, piece);
		helper.runAfterDelay(40, () -> shadeCounted(helper, level, piece, site)); // once the light has settled
	}

	private static void shadeCounted(GameTestHelper helper, ServerLevel level, CampPiece piece, CampSite site) {
		int room = 0;
		int above = 0;
		for (BlockPos spot : site.shade()) {
			BlockPos at = io.github.bubba8587.bubbas_reworked_mobs.camp.CampShade.resolve(level, spot);
			if (at == null) {
				continue;
			}
			helper.assertTrue(io.github.bubba8587.bubbas_reworked_mobs.camp.CampShade.covered(level, at.above(2)), "a shade spot is roofed: " + at.toShortString());
			if (at.getY() < piece.centre().getY()) {
				room++;
			} else {
				above++;
			}
		}
		helper.assertTrue(room >= 20, "most of the small camp's 5x5 room should be usable shade (a few cells hold bones), found " + room);
		helper.assertTrue(above >= 4, "shade above ground (lean-to, towers), found " + above);
		// A zombie on the tunnel's stairs can walk down into the room. (The tunnel surfaces just past this small test
		// field's barrier wall; the whole walk from the surface is checked on a natural camp in the client part camps.)
		Mob walker = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		BlockPos start = piece.tunnelStep(2);
		walker.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(walker);
		BlockPos roomCell = piece.centre().below(4);
		helper.runAfterDelay(1, () -> {
			walker.setOnGround(true); // a mob in the air plans no path
			var path = walker.getNavigation().createPath(roomCell, 0);
			walker.discard();
			helper.assertTrue(path != null && path.canReach(), "the room should be reachable down its tunnel from the surface: "
					+ (path == null ? "no path" : "ends at " + path.getEndNode() + " target " + roomCell.toShortString()));
			helper.succeed();
		});
	}

	/** At noon the roster appears in shade, and a camp zombie burning in the sun takes cover (vanilla's sun goals). */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_noon", structure = FIELD, maxTicks = 300)
	public void exposedCampMobsSeekShade(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = camp(helper, CampKind.ZOMBIE, 29L);
		CampSite site = site(helper, piece);
		helper.runAfterDelay(40, () -> seekShade(helper, level, site)); // once the light has settled
	}

	private static void seekShade(GameTestHelper helper, ServerLevel level, CampSite site) {
		List<Mob> roster = CampRoster.spawn(level, site);
		for (Mob mob : roster) {
			helper.assertTrue(io.github.bubba8587.bubbas_reworked_mobs.camp.CampShade.covered(level, BlockPos.containing(mob.getX(), mob.getEyeY(), mob.getZ())),
					"by day the roster appears in the shade: " + mob.blockPosition().toShortString() + " centre " + site.centre().toShortString()
					+ " leader " + CampRoster.isLeader(mob));
		}
		// A zombie (not the helmeted leader; bare-headed, as a helmet keeps the sun off) out by the fire, burning: it
		// takes cover with vanilla's flee-the-sun goal, which camp zombies are given.
		Mob walker = roster.getLast();
		walker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
		BlockPos open = site.centre().offset(2, 1, 2);
		walker.snapTo(open.getX() + 0.5, open.getY(), open.getZ() + 0.5, 0.0F, 0.0F);
		walker.igniteForSeconds(8.0F);
		helper.succeedWhen(() -> {
			helper.assertTrue(!level.canSeeSky(BlockPos.containing(walker.getX(), walker.getEyeY(), walker.getZ())), "the burning zombie should take cover");
			roster.forEach(Entity::discard);
		});
	}

	/**
	 * Every block of a built camp can stay where it is: nothing that needs support (ladders, banners, carpets, plants,
	 * skulls) is placed without it, so no block update pops it off. One camp of each kind.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void everyCampBlockIsSupported(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> loose = new ArrayList<>();
		long seed = 31L;
		for (CampKind kind : CampKind.values()) {
			CampPiece piece = CampCommand.build(level, helper.absolutePos(CENTRE), kind, CampTuning.Size.SMALL, seed++);
			loose.addAll(unsupported(level, piece.getBoundingBox()));
		}
		helper.assertTrue(loose.isEmpty(), "blocks that would pop off: " + loose.subList(0, Math.min(12, loose.size())));
		helper.succeed();
	}

	/** Blocks in the box that couldn't survive a block update where they are. */
	static List<String> unsupported(ServerLevel level, BoundingBox box) {
		List<String> loose = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			var state = level.getBlockState(pos);
			if (!state.isAir() && !state.canSurvive(level, pos)) {
				loose.add(state.getBlock().getDescriptionId().replaceFirst("^.*[.]", "") + "@" + pos.toShortString() + " on "
						+ level.getBlockState(pos.below()).getBlock().getDescriptionId().replaceFirst("^.*[.]", ""));
			}
		}
		return loose;
	}
}
