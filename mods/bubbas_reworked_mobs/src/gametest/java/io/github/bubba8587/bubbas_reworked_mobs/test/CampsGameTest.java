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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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

	/** Every camp biome gives its kind: husks in desert and badlands, bogged in swamps, strays in every snowy one, else skeleton or zombie. */
	@GameTest(structure = FIELD, maxTicks = 1)
	public void campKindFollowsTheBiome(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		Object[][] cases = {
				{ Biomes.DESERT, CampKind.HUSK }, { Biomes.BADLANDS, CampKind.HUSK }, { Biomes.WOODED_BADLANDS, CampKind.HUSK },
				{ Biomes.ERODED_BADLANDS, CampKind.HUSK }, { Biomes.SWAMP, CampKind.BOGGED }, { Biomes.MANGROVE_SWAMP, CampKind.BOGGED },
				{ Biomes.SNOWY_PLAINS, CampKind.STRAY }, { Biomes.SNOWY_TAIGA, CampKind.STRAY }, { Biomes.ICE_SPIKES, CampKind.STRAY },
				{ Biomes.GROVE, CampKind.STRAY }, { Biomes.SNOWY_SLOPES, CampKind.STRAY }, { Biomes.JAGGED_PEAKS, CampKind.STRAY },
				{ Biomes.FROZEN_PEAKS, CampKind.STRAY }, { Biomes.PLAINS, null }, { Biomes.FOREST, null }, { Biomes.TAIGA, null },
				{ Biomes.SAVANNA, null }, { Biomes.WINDSWEPT_HILLS, null }, { Biomes.STONY_PEAKS, null }, { Biomes.CHERRY_GROVE, null } };
		RandomSource random = RandomSource.create(1);
		for (Object[] c : cases) {
			@SuppressWarnings("unchecked")
			var key = (ResourceKey<Biome>) c[0];
			for (int i = 0; i < 8; i++) {
				CampKind kind = CampKind.forBiome(biomes.getOrThrow(key), random);
				boolean ok = c[1] == null ? kind == CampKind.SKELETON || kind == CampKind.ZOMBIE : kind == c[1];
				helper.assertTrue(ok, "%s gave a %s camp".formatted(key.identifier(), kind.id()));
			}
		}
		helper.succeed();
	}

	/** Core. A survival player within range makes the roster appear: a small camp's three and one larger, tougher, better-geared leader. */
	public void rosterAppearsWithALeader(GameTestHelper helper) {
		CampPiece piece = camp(helper, CampKind.SKELETON, 7L);
		CampSite site = site(helper, piece);
		helper.assertTrue(site.armed() && !site.spawned(), "a new camp is armed and waiting");
		helper.assertTrue(!banners(helper.getLevel(), piece.getBoundingBox()).isEmpty(), "the camp should fly at least one banner");
		ServerPlayer player = TestPlayers.survival(helper);
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
	 * Breaking every banner stops a cleared camp for good (it never resets); a banner of another kind put in the area
	 * doesn't arm it, but putting one of its own banners back re-arms it, and it resets at the next check.
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
			// A skeleton camp's banner at the edge of this zombie camp doesn't count.
			BlockPos edge = piece.centre().offset(4, 1, 0);
			level.setBlock(edge, CampBanners.standing(CampKind.SKELETON).defaultBlockState(), 3);
			CampBanners.paint(level.getBlockEntity(edge), CampKind.SKELETON, level.registryAccess());
			helper.assertTrue(Boolean.FALSE.equals(CampSites.recheck(level, site)) && !site.armed(), "another kind's banner doesn't arm it");
			// Its own kind's banner put back there does.
			level.setBlock(edge, CampBanners.standing(CampKind.ZOMBIE).defaultBlockState().setValue(BannerBlock.ROTATION, 0), 3);
			CampBanners.paint(level.getBlockEntity(edge), CampKind.ZOMBIE, level.registryAccess());
			helper.assertTrue(Boolean.TRUE.equals(CampSites.recheck(level, site)) && site.armed(), "a banner put back re-arms it");
			CampSites.resetCheck(level, false);
			helper.assertTrue(!site.spawned(), "re-armed, it resets at the next check");
			helper.succeed();
		});
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
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_night", structure = FIELD, maxTicks = 120)
	public void aLitFireQuietsTheCamp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = camp(helper, CampKind.SKELETON, 17L);
		CampSite site = site(helper, piece);
		BlockPos fire = piece.centre().offset(-4, 1, 3);
		level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true), 3);
		CampSites.recheck(level, site);
		helper.assertTrue(site.quiet(), "a lit campfire in the camp quiets it");
		helper.assertTrue(piece.getBoundingBox().isInside(fire), "the fire is inside the camp");
		ServerPlayer player = TestPlayers.survival(helper);
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
		helper.assertTrue(room >= 4, "the room should be usable shade, found " + room);
		helper.assertTrue(above >= 2, "shade above ground (lean-tos, towers), found " + above);
		// A zombie on the tunnel's stairs can walk down into the room. (The tunnel surfaces just past this small test
		// field's barrier wall; the whole walk from the surface is checked on a natural camp in the client part camps.)
		Mob walker = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		BlockPos start = piece.tunnelStep(2);
		walker.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(walker);
		BlockPos roomCell = piece.roomSpot();
		helper.runAfterDelay(1, () -> {
			walker.setOnGround(true); // a mob in the air plans no path
			var path = walker.getNavigation().createPath(roomCell, 0);
			walker.discard();
			helper.assertTrue(path != null && path.canReach(), "the room should be reachable down its tunnel from the surface: "
					+ (path == null ? "no path" : "ends at " + path.getEndNode() + " target " + roomCell.toShortString()));
			helper.succeed();
		});
	}

	/**
	 * Room stairs: a zombie in the underground room, and one the size of a leader, can path up the tunnel and out onto
	 * the clearing beside the fire, every kind and size, several seeds each (the field is put back between camps), and
	 * once each on land rising round the camp.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void zombieCanClimbOutOfTheRoom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(16, 8, 24));
		List<String> stuck = new ArrayList<>();
		int tried = 0;
		for (CampKind kind : CampKind.values()) {
			for (CampTuning.Size size : CampTuning.Size.values()) {
				for (long seed = 0; seed < 4; seed++) {
					resetField(helper);
					if (seed == 3) {
						if (kind.nether()) {
							continue;
						}
						bowl(helper); // the land rising round the camp, so the tunnel surfaces into a slope
					}
					long s = seed * 104729L + kind.ordinal() * 31L + size.ordinal();
					CampPiece piece = CampCommand.build(level, at, kind, size, s);
					for (int k = 1; k <= 2; k++) {
						BlockPos past = piece.tunnelTop().relative(piece.stairsOut(), k);
						boolean flush = !level.getBlockState(past).getCollisionShape(level, past).isEmpty()
								&& level.getBlockState(past.above()).getCollisionShape(level, past.above()).isEmpty()
								&& level.getBlockState(past.above(2)).getCollisionShape(level, past.above(2)).isEmpty();
						if (!flush) {
							stuck.add(kind.id() + "/" + size.id() + "/" + s + (seed == 3 ? " on a slope" : "") + ": the way out isn't flush with the top step: "
									+ mouth(level, piece));
							break;
						}
					}
					BlockPos target = piece.fireSide();
					for (double scale : new double[] { 1.0, CampTuning.LEADER_SCALE }) {
						net.minecraft.world.entity.monster.zombie.Zombie walker = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level,
								net.minecraft.world.entity.EntitySpawnReason.COMMAND);
						walker.setBaby(false);
						walker.getAttribute(Attributes.SCALE).setBaseValue(scale);
						walker.refreshDimensions();
						BlockPos start = piece.roomSpot();
						walker.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0.0F, 0.0F);
						level.addFreshEntity(walker);
						walker.setOnGround(true);
						var path = walker.getNavigation().createPath(target, 1);
						walker.discard();
						tried++;
						if (path == null || !path.canReach()) {
							stuck.add(kind.id() + "/" + size.id() + "/" + s + (scale > 1.0 ? " leader-sized" : "") + " "
									+ (path == null ? "no path" : "ends at " + path.getEndNode().asBlockPos().subtract(start).toShortString()) + " target "
									+ target.subtract(start).toShortString() + " top " + mouth(level, piece));
						}
					}
				}
			}
		}
		resetField(helper);
		helper.assertTrue(stuck.isEmpty(), stuck.size() + " faults in " + tried + " walks out of the room: " + stuck.subList(0, Math.min(4, stuck.size())));
		helper.succeed();
	}

	/**
	 * Stables: every large camp and about half the medium ones have one (none small, none in Nether outposts); its
	 * mounts appear with the roster, 1 or 2, the kind's undead mount or a captive horse or donkey, all leashed to the
	 * tether post, persistent, under the roof and off the roster. They are re-made only when the stable has none and
	 * its post stands.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void stablesHoldTetheredMounts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(16, 8, 24));
		int medium = 0;
		int mediumWith = 0;
		List<String> largeWithout = new ArrayList<>();
		for (CampKind kind : CampKind.values()) {
			for (long seed = 0; seed < 32; seed++) {
				for (CampTuning.Size size : CampTuning.Size.values()) {
					boolean has = new CampPiece(seed * 7919L + kind.ordinal(), at, kind, size).hasStable();
					if (kind.nether() || size == CampTuning.Size.SMALL) {
						helper.assertTrue(!has, "no stable in a " + size.id() + " " + kind.id() + " camp");
					} else if (size == CampTuning.Size.LARGE) {
						if (!has) {
							largeWithout.add(kind.id() + " seed " + (seed * 7919L + kind.ordinal()));
						}
					} else {
						medium++;
						mediumWith += has ? 1 : 0;
					}
				}
			}
		}
		helper.assertTrue(largeWithout.isEmpty(), "every large camp has a stable; " + largeWithout.size() + " of 160 without: "
				+ largeWithout.subList(0, Math.min(6, largeWithout.size())));
		helper.assertTrue(mediumWith > medium * 0.3 && mediumWith < medium * 0.7, "about half of medium camps have a stable: " + mediumWith + " of " + medium);
		for (CampKind kind : CampKind.values()) {
			if (kind.nether()) {
				continue;
			}
			resetField(helper);
			CampPiece piece = CampCommand.build(level, at, kind, CampTuning.Size.LARGE, 17L + kind.ordinal());
			CampSite site = site(helper, piece);
			helper.assertTrue(site.hasStable() && piece.hasStable(), "a large camp's site knows its stable");
			BlockPos post = piece.tetherPosts().getFirst();
			helper.assertTrue(level.getBlockState(post).getBlock() instanceof net.minecraft.world.level.block.FenceBlock, "the stable's tether post stands at " + post.toShortString());
			List<Mob> roster = CampRoster.spawn(level, site);
			List<Mob> mounts = mounts(level, post);
			helper.assertTrue(!mounts.isEmpty() && mounts.size() <= CampTuning.STABLE_MOUNTS_MAX, kind.id() + ": 1-2 mounts, found " + mounts.size());
			for (Mob mount : mounts) {
				var type = mount.getType();
				helper.assertTrue(type == kind.undeadMount() || type == net.minecraft.world.entity.EntityTypes.HORSE || type == net.minecraft.world.entity.EntityTypes.DONKEY,
						kind.id() + " stable holds " + type);
				helper.assertTrue(mount instanceof net.minecraft.world.entity.Leashable l && l.getLeashHolder() instanceof net.minecraft.world.entity.decoration.LeashFenceKnotEntity knot
						&& knot.getPos().equals(post), "a stable mount is leashed to the post");
				helper.assertTrue(mount.isPersistenceRequired(), "a stable mount is persistent");
				helper.assertTrue(!site.members().contains(mount.getUUID()), "a stable mount isn't on the roster");
				helper.assertTrue(io.github.bubba8587.bubbas_reworked_mobs.camp.CampShade.covered(level, BlockPos.containing(mount.getX(), mount.getEyeY(), mount.getZ())), "a stable mount stands under the roof: " + type + " at " + mount.blockPosition().subtract(post).toShortString() + " from the post, facing "
						+ piece.stableFacing() + ", centre " + piece.centre().subtract(post).toShortString());
			}
			roster.forEach(Entity::discard);
			CampRoster.spawn(level, site).forEach(Entity::discard);
			helper.assertTrue(mounts(level, post).size() == mounts.size(), "mounts aren't re-made while the stable has some");
			mounts.forEach(Entity::discard);
			CampRoster.spawn(level, site).forEach(Entity::discard);
			List<Mob> again = mounts(level, post);
			helper.assertTrue(!again.isEmpty(), "an empty stable is restocked with the roster");
			again.forEach(Entity::discard);
			level.setBlock(post, Blocks.AIR.defaultBlockState(), 3);
			CampRoster.spawn(level, site).forEach(Entity::discard);
			helper.assertTrue(mounts(level, post).isEmpty(), "no mounts once the tether post is gone");
		}
		resetField(helper);
		helper.succeed();
	}

	private static List<Mob> mounts(ServerLevel level, BlockPos post) {
		return level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(post).inflate(5.0, 3.0, 5.0), m -> m.entityTags().contains(CampRoster.LIVESTOCK_TAG));
	}

	/** Chest tiers: the winding tower's chest in its core and the skull rock's (full and half-buried) at its back, each with its loot table. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void towerAndSkullChestsHoldTheirLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(16, 8, 24));
		record Case(String what, java.util.function.Function<CampPiece, BlockPos> chest, java.util.function.Predicate<CampPiece> wants, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
		}
		List<Case> cases = List.of(new Case("winding tower", CampPiece::windingChest, p -> p.windingChest() != null, io.github.bubba8587.bubbas_reworked_mobs.camp.Camps.CAMP_TOWER_LOOT),
				new Case("skull rock", CampPiece::skullChest, p -> p.skullChest() != null && !p.skullBuried(), io.github.bubba8587.bubbas_reworked_mobs.camp.Camps.CAMP_SKULL_LOOT),
				new Case("buried skull", CampPiece::skullChest, p -> p.skullChest() != null && p.skullBuried(), io.github.bubba8587.bubbas_reworked_mobs.camp.Camps.CAMP_SKULL_LOOT));
		for (Case c : cases) {
			for (CampKind kind : new CampKind[] { CampKind.SKELETON, CampKind.HUSK }) {
				long seed = 0;
				while (!c.wants().test(new CampPiece(seed, at, kind, CampTuning.Size.LARGE))) {
					helper.assertTrue(++seed < 2000, "no large camp with a " + c.what());
				}
				resetField(helper);
				CampPiece piece = CampCommand.build(level, at, kind, CampTuning.Size.LARGE, seed);
				BlockPos chest = c.chest().apply(piece);
				helper.assertTrue(level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity box && c.loot().equals(box.getLootTable()),
						c.what() + " (" + kind.id() + " seed " + seed + "): a chest with " + c.loot().identifier() + " at " + chest.toShortString() + ", found "
								+ level.getBlockState(chest).getBlock().getDescriptionId());
				helper.assertTrue(level.getBlockState(chest.above()).getCollisionShape(level, chest.above()).isEmpty(), c.what() + ": the chest can open");
			}
		}
		resetField(helper);
		helper.succeed();
	}

	/** What stands at the tunnel's top: the top step and the two cells past it, ground block and the two above (diagnostics). */
	private static String mouth(ServerLevel level, CampPiece piece) {
		StringBuilder out = new StringBuilder();
		BlockPos top = piece.tunnelTop();
		for (int k = 0; k <= 2; k++) {
			BlockPos cell = top.relative(piece.stairsOut(), k);
			out.append(k).append(':');
			for (int dy = 0; dy <= 2; dy++) {
				out.append(level.getBlockState(cell.above(dy)).getBlock().getDescriptionId().replaceFirst("^.*[.]", "")).append(dy < 2 ? "/" : " ");
			}
		}
		for (int dy = 0; dy <= 3; dy++) {
			out.append("\n room+").append(dy).append(' ');
			for (int dz = -5; dz <= 5; dz++) {
				out.append('|');
				for (int dx = -5; dx <= 5; dx++) {
					BlockPos c = piece.roomSpot().offset(dx, dy, dz);
					var st = level.getBlockState(c);
					out.append(st.isAir() ? '.' : st.getBlock() instanceof net.minecraft.world.level.block.StairBlock ? 'S' : st.is(Blocks.BONE_BLOCK) ? 'b'
							: st.is(Blocks.CHEST) ? 'c' : st.is(Blocks.COBWEB) ? 'w' : '#');
				}
			}
		}
		return out.toString();
	}

	/** Re-reads the worldgen surface heightmap camps build from (a live chunk keeps it as first read). */
	private static void reprimeSurface(GameTestHelper helper) {
		BlockPos a = helper.absolutePos(new BlockPos(-6, 0, -6));
		BlockPos b = helper.absolutePos(new BlockPos(37, 0, 53));
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(helper.getLevel().getChunk(cx, cz),
						java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
			}
		}
	}

	/** Raises the field's ground round its middle (16, 24): a block per two blocks out past 6, three at most. */
	private static void bowl(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 32; x++) {
			for (int z = 0; z < 48; z++) {
				int raise = Math.clamp((int) (Math.hypot(x - 16, z - 24) - 6.0) / 2, 0, 3);
				for (int y = 8; y <= 7 + raise; y++) {
					level.setBlock(helper.absolutePos(new BlockPos(x, y - 1, z)), Blocks.DIRT.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
					level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), Blocks.GRASS_BLOCK.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
		}
		reprimeSurface(helper);
	}

	/** Puts the camp field back as it was (stone, dirt, grass, air above) and clears what camps left round it; no entities. */
	static void resetField(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = -6; x < 38; x++) {
			for (int z = -6; z < 54; z++) {
				boolean field = x >= 0 && x < 32 && z >= 0 && z < 48;
				for (int y = field ? 0 : 8; y < 32; y++) {
					var state = y < 5 ? Blocks.STONE.defaultBlockState() : y < 7 ? Blocks.DIRT.defaultBlockState() : y == 7 ? Blocks.GRASS_BLOCK.defaultBlockState()
							: Blocks.AIR.defaultBlockState();
					BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
					if (level.getBlockState(pos) != state) {
						level.setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		BoundingBox box = new BoundingBox(helper.absolutePos(new BlockPos(-6, 0, -6))).encapsulate(helper.absolutePos(new BlockPos(37, 31, 53)));
		for (Entity e : level.getEntitiesOfClass(Entity.class, net.minecraft.world.phys.AABB.of(box).inflate(8.0), e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
			e.discard();
		}
		reprimeSurface(helper);
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
	 * skulls) is placed without it, so no block update pops it off. And camps are dark (owner, 2026-11-22: no fire, no
	 * light at all): no block in one gives off light. One camp of each kind.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void everyCampBlockIsSupported(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> loose = new ArrayList<>();
		List<String> lit = new ArrayList<>();
		long seed = 31L;
		for (CampKind kind : CampKind.values()) {
			CampPiece piece = CampCommand.build(level, helper.absolutePos(CENTRE), kind, CampTuning.Size.SMALL, seed++);
			loose.addAll(unsupported(level, piece.getBoundingBox()));
			BoundingBox box = piece.getBoundingBox();
			for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				if (!kind.nether() && level.getBlockState(pos).getLightEmission() > 0) {
					lit.add(level.getBlockState(pos).getBlock().getDescriptionId().replaceFirst("^.*[.]", "") + "@" + pos.toShortString());
				}
			}
		}
		helper.assertTrue(loose.isEmpty(), "blocks that would pop off: " + loose.subList(0, Math.min(12, loose.size())));
		helper.assertTrue(lit.isEmpty(), "camps are dark, but these give off light: " + lit.subList(0, Math.min(12, lit.size())));
		helper.succeed();
	}

	/** Camp footprint plan: no two pieces' footprints overlap or touch, for 32 seeds of every kind and size. */
	@GameTest(structure = FIELD, maxTicks = 5)
	public void campPiecesNeverOverlap(GameTestHelper helper) {
		List<String> clashes = new ArrayList<>();
		BlockPos at = helper.absolutePos(CENTRE);
		for (CampKind kind : CampKind.values()) {
			for (CampTuning.Size size : CampTuning.Size.values()) {
				for (long seed = 0; seed < 32; seed++) {
					List<CampPiece.Footprint> all = new CampPiece(seed * 7919L + kind.ordinal(), at, kind, size).footprints();
					for (int i = 0; i < all.size(); i++) {
						for (int j = i + 1; j < all.size(); j++) {
							if (!all.get(i).clearOf(all.get(j), 0)) {
								clashes.add(kind.id() + "/" + size.id() + "/" + seed + ": " + all.get(i) + " x " + all.get(j));
							}
						}
					}
				}
			}
		}
		helper.assertTrue(clashes.isEmpty(), clashes.size() + " overlaps, e.g. " + clashes.subList(0, Math.min(4, clashes.size())));
		helper.succeed();
	}

	/** The skull rock: a zombie before its mouth can walk inside. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void skullRockCanBeWalkedInto(GameTestHelper helper) {
		walkable(helper, CampKind.SKELETON, p -> p.skullInside() == null || p.skullBuried() ? null : new BlockPos[] { p.skullMouth(), p.skullInside() }, "skull rock");
	}

	/** The half-buried skull rock: a zombie before one eye socket can walk inside. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void buriedSkullCanBeWalkedInto(GameTestHelper helper) {
		walkable(helper, CampKind.STRAY, p -> !p.skullBuried() ? null : new BlockPos[] { p.skullMouth(), p.skullInside() }, "buried skull");
	}

	/** The ribcage: a zombie at one end of its path can walk under the ribs to the other. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void ribcagePathRunsUnderTheRibs(GameTestHelper helper) {
		walkable(helper, CampKind.BOGGED, CampPiece::ribcageEnds, "ribcage");
	}

	/** Fences and chains in built camps: fence sides joined where a neighbour takes them, no lone rail, no chain end hanging in air. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void campFencesEndCleanly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> faults = new ArrayList<>();
		long seed = 3L;
		for (CampKind kind : CampKind.values()) {
			CampPiece piece = CampCommand.build(level, helper.absolutePos(new BlockPos(16, 8, 24)), kind, CampTuning.Size.LARGE, seed++);
			faults.addAll(fenceFaults(level, piece.getBoundingBox()));
		}
		helper.assertTrue(faults.isEmpty(), faults.size() + " fence faults, e.g. " + faults.subList(0, Math.min(8, faults.size())));
		helper.succeed();
	}

	/** Fences whose sides don't match their neighbours, or that join nothing with nothing on top and no fence below. */
	static List<String> fenceFaults(ServerLevel level, BoundingBox box) {
		List<String> faults = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			var state = level.getBlockState(pos);
			if (state.getBlock() instanceof net.minecraft.world.level.block.ChainBlock) {
				net.minecraft.core.Direction.Axis axis = state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
				for (net.minecraft.core.Direction.AxisDirection sign : net.minecraft.core.Direction.AxisDirection.values()) {
					net.minecraft.core.Direction d = net.minecraft.core.Direction.fromAxisAndDirection(axis, sign);
					if (axis == net.minecraft.core.Direction.Axis.Y && d == net.minecraft.core.Direction.DOWN) {
						continue;
					}
					BlockPos n = pos.relative(d);
					var ns = level.getBlockState(n);
					boolean held = ns.getBlock() instanceof net.minecraft.world.level.block.ChainBlock && ns.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == axis
							|| ns.getBlock() instanceof net.minecraft.world.level.block.FenceBlock || ns.getBlock() instanceof net.minecraft.world.level.block.WallBlock
							|| ns.isFaceSturdy(level, n, d.getOpposite());
					if (!held) {
						faults.add("chain loose " + d.getName() + " @" + pos.toShortString());
					}
				}
				continue;
			}
			if (!(state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock fence)) {
				continue;
			}
			boolean any = false;
			for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
				BlockPos n = pos.relative(d);
				var ns = level.getBlockState(n);
				boolean want = fence.connectsTo(ns, ns.isFaceSturdy(level, n, d.getOpposite()), d.getOpposite());
				boolean has = state.getValue(io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild.propertyOf(d));
				any |= has;
				if (want != has) {
					faults.add("side " + d.getName() + " @" + pos.toShortString());
				}
			}
			if (!any && level.getBlockState(pos.above()).isAir() && !(level.getBlockState(pos.below()).getBlock() instanceof net.minecraft.world.level.block.FenceBlock)) {
				faults.add("lone @" + pos.toShortString());
			}
		}
		return faults;
	}

	/** The winding tower: a zombie at the foot of its stair can climb it to the top. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void windingTowerCanBeClimbed(GameTestHelper helper) {
		walkable(helper, CampKind.ZOMBIE, p -> p.windingTop() == null ? null : new BlockPos[] { p.windingFoot(), p.windingTop() }, "winding tower");
	}

	/** A tower ramp: a zombie at its foot can walk up onto the platform. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void towerRampCanBeClimbed(GameTestHelper helper) {
		walkable(helper, CampKind.HUSK, CampPiece::rampEnds, "ramp");
	}

	/** A bridge: a zombie on one tower's platform can walk across to the other's. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void bridgeJoinsTwoTowers(GameTestHelper helper) {
		walkable(helper, CampKind.STRAY, CampPiece::bridgeEnds, "bridge");
	}

	/** Builds the first large camp of this kind that has the piece, checks nothing in it would pop off and a zombie can path from one end to the other. */
	private static void walkable(GameTestHelper helper, CampKind kind, java.util.function.Function<CampPiece, BlockPos[]> ends, String what) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(16, 8, 24));
		long seed = 0;
		while (ends.apply(new CampPiece(seed, at, kind, CampTuning.Size.LARGE)) == null) {
			helper.assertTrue(++seed < 2000, "no large camp with a " + what);
		}
		CampPiece piece = CampCommand.build(level, at, kind, CampTuning.Size.LARGE, seed);
		BlockPos[] e = ends.apply(piece);
		List<String> loose = unsupported(level, piece.getBoundingBox());
		helper.assertTrue(loose.isEmpty(), what + ": blocks that would pop off: " + loose.subList(0, Math.min(12, loose.size())));
		List<String> fences = fenceFaults(level, piece.getBoundingBox());
		helper.assertTrue(fences.isEmpty(), what + ": fence faults " + fences.subList(0, Math.min(8, fences.size())));
		Mob walker = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		walker.snapTo(e[0].getX() + 0.5, e[0].getY(), e[0].getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(walker);
		long s = seed;
		helper.runAfterDelay(1, () -> {
			walker.setOnGround(true);
			var path = walker.getNavigation().createPath(e[1], 0);
			walker.discard();
			helper.assertTrue(path != null && path.canReach(), what + " (seed " + s + ") should be walkable from " + e[0].toShortString() + " to "
					+ e[1].toShortString() + ": " + (path == null ? "no path" : "ends at " + path.getEndNode()));
			helper.succeed();
		});
	}

	// --- Nether outposts ----------------------------------------------------------------------------

	/** Nether outposts may only be placed in crimson forests and the Nether wastes. */
	@GameTest(structure = FIELD, maxTicks = 1)
	public void netherOutpostBiomes(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		var structure = registries.lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(io.github.bubba8587.bubbas_reworked_mobs.camp.Camps.OUTPOST_KEY);
		var biomes = registries.lookupOrThrow(Registries.BIOME);
		for (var key : List.of(Biomes.CRIMSON_FOREST, Biomes.NETHER_WASTES)) {
			helper.assertTrue(structure.biomes().contains(biomes.getOrThrow(key)), "outposts belong in " + key.identifier());
		}
		for (var key : List.of(Biomes.BASALT_DELTAS, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.PLAINS, Biomes.DESERT)) {
			helper.assertTrue(!structure.biomes().contains(biomes.getOrThrow(key)), "no outposts in " + key.identifier());
		}
		helper.succeed();
	}

	/** An outpost's roster: piglins with a brute leader in enchanted gold; its hoglins appear in their pen, one leashed, and stay penned. */
	@GameTest(environment = "bubbas_reworked_mobs-test:camp_night", structure = FIELD, maxTicks = 200)
	public void outpostRosterAndPennedHoglins(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = CampCommand.build(level, helper.absolutePos(new BlockPos(16, 8, 24)), CampKind.PIGLIN, CampTuning.Size.MEDIUM, 5L);
		CampSite site = site(helper, piece);
		helper.assertTrue(!piece.tetherPosts().isEmpty(), "an outpost has a hoglin pen");
		ServerPlayer player = TestPlayers.survival(helper);
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 8.0, 2.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		BlockPos post = piece.tetherPosts().getFirst();
		net.minecraft.world.phys.AABB pen = new net.minecraft.world.phys.AABB(post).inflate(1.9, 2.0, 1.9);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(site.spawned() && site.members().size() == CampTuning.Size.MEDIUM.roster + 1, "roster of " + site.members().size());
			int leaders = 0;
			for (var uuid : site.members()) {
				Entity e = level.getEntity(uuid);
				if (e == null) {
					continue; // wandered out of loaded ground beside a neighbouring test
				}
				helper.assertTrue(e instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin, "an outpost member is a piglin: " + e);
				if (CampRoster.isLeader((Mob) e)) {
					leaders++;
					helper.assertTrue(e.getType() == net.minecraft.world.entity.EntityTypes.PIGLIN_BRUTE && ((Mob) e).getMaxHealth() > 50.0F
							&& ((Mob) e).getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE), "the leader is a geared brute");
				}
			}
			helper.assertTrue(leaders <= 1 && site.members().stream().map(level::getEntity).filter(java.util.Objects::nonNull).count() >= CampTuning.Size.MEDIUM.roster, "one leader, found " + leaders);
			var hoglins = level.getEntitiesOfClass(net.minecraft.world.entity.monster.hoglin.Hoglin.class, pen.inflate(8.0),
					h -> h.entityTags().contains(CampRoster.LIVESTOCK_TAG));
			helper.assertTrue(hoglins.size() == 2, "a medium outpost keeps two hoglins, found " + hoglins.size());
			helper.assertTrue(hoglins.stream().anyMatch(h -> h.isLeashed()), "one hoglin is leashed to the tether post");
			for (var h : hoglins) {
				helper.assertTrue(pen.contains(h.position()), "hoglin stays in its pen: " + h.blockPosition().toShortString() + " pen " + post.toShortString());
				helper.assertTrue(!h.canBeHunted(), "outpost hoglins are not hunted");
			}
			level.getServer().getPlayerList().remove(player);
			for (var uuid : site.members()) {
				Entity e = level.getEntity(uuid);
				if (e != null) {
					e.discard();
				}
			}
			hoglins.forEach(Entity::discard);
			helper.succeed();
		});
	}

	/** An outpost: a mob can walk in through its gate, up a ramp onto a tower, and down the tunnel to the portal room. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void outpostGateCanBeWalkedThrough(GameTestHelper helper) {
		walkable(helper, CampKind.PIGLIN, CampPiece::gateEnds, "gate");
	}

	@GameTest(structure = FIELD, maxTicks = 20)
	public void outpostRampCanBeClimbed(GameTestHelper helper) {
		walkable(helper, CampKind.PIGLIN, CampPiece::rampEnds, "outpost ramp");
	}

	@GameTest(structure = FIELD, maxTicks = 20)
	public void outpostPortalRoomCanBeReached(GameTestHelper helper) {
		walkable(helper, CampKind.PIGLIN, p -> new BlockPos[] { p.tunnelStep(3), p.portalBase().relative(p.portalAlong(), 1).relative(p.portalStairs(), 2) },
				"portal room");
	}

	/** The portal: a lit one is a whole obsidian frame filled with portal; an unlit one whole and empty; a broken one missing a block or two. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void outpostPortalsMatchTheirState(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		java.util.EnumSet<CampPiece.Portal> seen = java.util.EnumSet.noneOf(CampPiece.Portal.class);
		for (long seed = 0; seed < 40 && seen.size() < 3; seed++) {
			CampPiece probe = new CampPiece(seed, helper.absolutePos(CENTRE), CampKind.PIGLIN, CampTuning.Size.SMALL);
			if (!seen.add(probe.portal())) {
				continue;
			}
			CampPiece piece = CampCommand.build(level, helper.absolutePos(CENTRE), CampKind.PIGLIN, CampTuning.Size.SMALL, seed);
			int frame = 0;
			int inner = 0;
			for (int u = 0; u <= 3; u++) {
				for (int v = 0; v <= 4; v++) {
					var state = level.getBlockState(piece.portalBase().relative(piece.portalAlong(), u).above(v));
					if (u == 0 || u == 3 || v == 0 || v == 4) {
						frame += state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) ? 1 : 0;
					} else {
						inner += state.is(Blocks.NETHER_PORTAL) ? 1 : 0;
					}
				}
			}
			switch (piece.portal()) {
				case LIT -> helper.assertTrue(frame == 14 && inner == 6, "lit portal: frame " + frame + " portal " + inner);
				case UNLIT -> helper.assertTrue(frame == 14 && inner == 0, "unlit portal: frame " + frame + " portal " + inner);
				case BROKEN -> helper.assertTrue(frame >= 12 && frame < 14 && inner == 0, "broken portal: frame " + frame + " portal " + inner);
			}
		}
		helper.assertTrue(seen.size() == 3, "all three portal states in 40 seeds: " + seen);
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
