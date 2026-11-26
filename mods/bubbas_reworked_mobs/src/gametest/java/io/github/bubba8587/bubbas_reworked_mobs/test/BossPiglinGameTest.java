// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglin;
import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglins;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampKind;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPiece;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampRoster;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSite;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSites;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** The Boss Piglin (specs/boss-piglin.md "Milestone test objectives"). Milestone; one core smoke test. */
public class BossPiglinGameTest {
	private static final BlockPos OUTPOST = new BlockPos(16, 8, 24);

	/** A medium outpost built in the test field, with its portal in the state asked for (null: any). */
	private static CampPiece outpost(GameTestHelper helper, CampPiece.Portal portal) {
		BlockPos at = helper.absolutePos(OUTPOST);
		long seed = 5L;
		while (portal != null && new CampPiece(seed, at, CampKind.PIGLIN, CampTuning.Size.MEDIUM).portal() != portal) {
			seed++;
		}
		return CampCommand.build(helper.getLevel(), at, CampKind.PIGLIN, CampTuning.Size.MEDIUM, seed);
	}

	private static CampSite site(GameTestHelper helper, CampPiece piece) {
		for (CampSite site : CampSites.all(helper.getLevel())) {
			if (site.centre().equals(piece.centre())) {
				return site;
			}
		}
		throw helper.assertionException("outpost not registered");
	}

	/** The outpost's roster, its hoglins and its boss, cleared away when the test is done. */
	private static List<Mob> roster(GameTestHelper helper, CampSite site) {
		ServerLevel level = helper.getLevel();
		List<Mob> roster = CampRoster.spawn(level, site);
		TestCleanup.whenDone(helper, () -> {
			clear(helper, site);
			BossPiglin.of(level, site).forEach(Entity::discard);
			level.getEntitiesOfClass(Hoglin.class, AABB.of(site.box()), h -> h.entityTags().contains(CampRoster.LIVESTOCK_TAG)).forEach(Entity::discard);
		});
		return roster;
	}

	/** Every living member of the outpost's roster gone (the camp counts them off). */
	private static void clear(GameTestHelper helper, CampSite site) {
		for (var uuid : site.members()) {
			Entity e = helper.getLevel().getEntity(uuid);
			if (e != null) {
				e.discard();
			}
		}
	}

	private static BossPiglin boss(GameTestHelper helper, CampSite site) {
		List<BossPiglin> bosses = BossPiglin.of(helper.getLevel(), site);
		helper.assertTrue(bosses.size() == 1, "one boss for the outpost, found " + bosses.size());
		return bosses.getFirst();
	}

	/** Chased toward the edge of a high ledge with a lava lake below, a fleeing boss runs off it into the lava. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 300)
	public void chasedOffACliffIntoLava(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// the field's grass (y 7) is the ledge; beside it a pit 6 deep with a lava lake at its bottom (the field's ceiling is barriers at y 14)
		for (int x = 15; x <= 28; x++) {
			for (int z = 4; z <= 28; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.LAVA);
				for (int y = 2; y <= 7; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		TestCleanup.whenDone(helper, () -> CampsGameTest.resetField(helper));
		BossPiglin boss = BossPiglins.BOSS_PIGLIN.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(11.5, 8.0, 16.5));
		boss.snapTo(at.x, at.y, at.z, 90.0F, 0.0F);
		level.addFreshEntity(boss);
		ServerPlayer player = TestPlayers.survival(helper);
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 30, 4, false, false));
		Vec3 p = helper.absoluteVec(new Vec3(4.5, 8.0, 16.5));
		player.snapTo(p.x, p.y, p.z, -90.0F, 0.0F);
		helper.startSequence()
				.thenIdle(200)
				.thenExecute(() -> helper.assertTrue(boss.isRemoved() || boss.isInLava() || boss.getY() < at.y - 4.0,
						"didn't run off the ledge: at " + helper.relativeVec(boss.position()) + " fleeing " + boss.isFleeing() + " onGround " + boss.onGround() + " noGravity " + boss.isNoGravity() + " below " + level.getBlockState(boss.blockPosition().below()) + " v " + boss.getDeltaMovement() + " player " + helper.relativeVec(player.position())))
				.thenSucceed();
	}

	/** Core: a built medium outpost has a throne, and its roster comes with a boss seated on it, not counted as a member. */
	public void mediumOutpostHasASeatedBoss(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = outpost(helper, null);
		CampSite site = site(helper, piece);
		CampSite.Throne throne = site.throne();
		helper.assertTrue(throne != null, "a medium outpost has a throne");
		helper.assertTrue(BossPiglins.stands(level, throne), "the throne is built");
		List<Mob> roster = roster(helper, site);
		BossPiglin boss = boss(helper, site);
		helper.assertTrue(!site.members().contains(boss.getUUID()) && site.members().size() == roster.size(), "the boss is not on the roster");
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(boss.isAlive() && boss.isSeated(), "the boss sits");
			helper.assertTrue(boss.position().distanceTo(throne.seat()) < 0.1, "the boss sits on its throne: " + boss.position() + " seat " + throne.seat());
			helper.succeed();
		});
	}

	/** Thrones only in medium and large Nether outposts, and every one of those has one. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 1)
	public void thronesInMediumAndLargeOutposts(GameTestHelper helper) {
		BlockPos at = helper.absolutePos(OUTPOST);
		List<String> wrong = new ArrayList<>();
		for (CampKind kind : CampKind.values()) {
			for (CampTuning.Size size : CampTuning.Size.values()) {
				boolean wanted = kind.nether() && size != CampTuning.Size.SMALL;
				for (long seed = 0; seed < 64; seed++) {
					CampPiece piece = new CampPiece(seed * 7919L + kind.ordinal(), at, kind, size);
					boolean has = piece.throne() != null && piece.footprints().stream().anyMatch(f -> f.what().equals("throne"));
					if (has != wanted) {
						wrong.add(kind.id() + "/" + size.id() + "/" + seed + (has ? ": a throne" : ": no throne"));
					}
				}
			}
		}
		helper.assertTrue(wrong.isEmpty(), wrong.size() + " wrong, e.g. " + wrong.subList(0, Math.min(6, wrong.size())));
		helper.succeed();
	}

	/**
	 * Seated and harmless while its roster lives (a gold-wearing player right by it is never touched); once the last
	 * member dies it stands and moves away from the player.
	 */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 240)
	public void seatedWhileTheRosterLivesThenFlees(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = outpost(helper, CampPiece.Portal.UNLIT);
		CampSite site = site(helper, piece);
		List<Mob> roster = roster(helper, site);
		BossPiglin boss = boss(helper, site);
		Vec3 seat = site.throne().seat();
		Vec3 front = seat.add(site.throne().facing().getStepX() * 3.2, 0.0, site.throne().facing().getStepZ() * 3.2);
		ServerPlayer player = TestPlayers.survival(helper);
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 30, 4, false, false));
		player.snapTo(front.x, Math.floor(seat.y), front.z, 0.0F, 0.0F);
		double[] start = new double[1];
		helper.startSequence()
				.thenIdle(50)
				.thenExecute(() -> {
					helper.assertTrue(boss.isSeated() && boss.position().distanceTo(seat) < 0.1, "seated while its roster lives");
					helper.assertTrue(player.getHealth() == player.getMaxHealth() && boss.getLastHurtMob() == null, "harmless while seated");
					clear(helper, site);
				})
				.thenWaitUntil(() -> helper.assertTrue(!boss.isSeated() && boss.isFleeing(), "stands once the roster is gone"))
				.thenExecute(() -> start[0] = boss.distanceTo(player))
				.thenWaitUntil(() -> helper.assertTrue(boss.distanceTo(player) > start[0] + 3.0,
						"moves away from the player: " + boss.distanceTo(player) + " from " + start[0]))
				.thenSucceed();
	}

	/**
	 * With a lit portal: if a path from its throne fits him he walks into the portal and is gone, dropping nothing;
	 * if he is too wide for the tunnel (intended, owner) he gives up on the portal with a grunt and keeps fleeing.
	 */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 900)
	public void headsForALitPortal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = outpost(helper, CampPiece.Portal.LIT);
		CampSite site = site(helper, piece);
		helper.assertTrue(level.getBlockState(piece.portalInside()).is(Blocks.NETHER_PORTAL), "the portal is lit");
		roster(helper, site);
		BossPiglin boss = boss(helper, site);
		helper.runAfterDelay(2, () -> clear(helper, site));
		AABB room = AABB.of(site.box()).inflate(4.0);
		boolean[] fits = new boolean[1];
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(boss.isFleeing(), "stands once the roster is gone"))
				.thenIdle(5)
				.thenExecute(() -> {
					var path = boss.getNavigation().createPath(piece.portalInside(), 0);
					fits[0] = path != null && path.canReach();
					System.out.println("BOSS PIGLIN: a path from the throne to the portal " + (fits[0] ? "fits him" : "doesn't fit him: "
							+ (path == null ? "none" : "ends at " + path.getEndNode())));
				})
				.thenWaitUntil(() -> helper.assertTrue(fits[0] ? boss.isRemoved() : boss.gaveUpOnPortal(), fits[0]
						? "escaped: now at " + boss.blockPosition().toShortString() + ", portal " + piece.portalInside().toShortString()
						: "gives up on the portal"))
				.thenExecute(() -> {
					if (fits[0]) {
						helper.assertTrue(boss.getRemovalReason() == Entity.RemovalReason.DISCARDED, "gone, not killed: " + boss.getRemovalReason());
						helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, room).isEmpty(), "no drops left behind");
					} else {
						helper.assertTrue(boss.isAlive() && boss.isFleeing(), "gave up: still fleeing");
					}
				})
				.thenSucceed();
	}

	/** A fleeing boss that reaches a lit portal (put beside it in the room) steps in and is gone, dropping nothing. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 200)
	public void escapesWhenItReachesTheLitPortal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = outpost(helper, CampPiece.Portal.LIT);
		CampSite site = site(helper, piece);
		roster(helper, site);
		BossPiglin boss = boss(helper, site);
		helper.runAfterDelay(2, () -> clear(helper, site));
		BlockPos before = piece.portalInside().relative(piece.portalStairs(), 2);
		AABB room = AABB.of(site.box()).inflate(4.0);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(boss.isFleeing(), "stands once the roster is gone"))
				.thenExecute(() -> {
					Vec3 at = Vec3.atBottomCenterOf(before).add(piece.portalAlong().getStepX() * 0.5, 0.0, piece.portalAlong().getStepZ() * 0.5);
					boss.snapTo(at.x, at.y, at.z, piece.portalStairs().getOpposite().toYRot(), 0.0F);
				})
				.thenWaitUntil(() -> helper.assertTrue(boss.isRemoved(), "escaped: now at " + boss.blockPosition().toShortString() + ", portal "
						+ piece.portalInside().toShortString()))
				.thenExecute(() -> {
					helper.assertTrue(boss.getRemovalReason() == Entity.RemovalReason.DISCARDED, "gone, not killed: " + boss.getRemovalReason());
					helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, room).isEmpty(), "no drops left behind");
				})
				.thenSucceed();
	}

	/** With an unlit portal it keeps fleeing round the outpost and never leaves. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 300)
	public void staysWhenThePortalIsUnlit(GameTestHelper helper) {
		CampPiece piece = outpost(helper, CampPiece.Portal.UNLIT);
		CampSite site = site(helper, piece);
		roster(helper, site);
		BossPiglin boss = boss(helper, site);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> clear(helper, site))
				.thenWaitUntil(() -> helper.assertTrue(boss.isFleeing(), "stands once the roster is gone (" + site.members().size() + " left)"))
				.thenIdle(200)
				// (fleeing, or sat back down if another test's global reset check brought its roster back)
				.thenExecute(() -> helper.assertTrue(boss.isAlive() && !boss.isRemoved() && (boss.isFleeing() || boss.isSeated()) && !boss.gaveUpOnPortal(),
						"still at the outpost: removed " + boss.getRemovalReason() + " fleeing " + boss.isFleeing()))
				.thenSucceed();
	}

	/** Cornered (a player within 2 blocks for a moment) it slaps about as hard as a piglin's punch, with knockback. */
	@GameTest(maxTicks = 120)
	public void corneredItSlapsLikeAPiglin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestFloors.stone(helper);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					for (int y = 1; y <= 3; y++) {
						helper.setBlock(new BlockPos(3 + dx, y, 3 + dz), Blocks.GLASS);
					}
				}
			}
		}
		BossPiglin boss = BossPiglins.BOSS_PIGLIN.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
		boss.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), EntitySpawnReason.COMMAND, null);
		boss.setImmuneToZombification(true);
		level.addFreshEntity(boss);
		TestCleanup.whenDone(helper, boss::discard);
		ServerPlayer player = TestPlayers.survivalAt(helper, 3.5, 5.1);
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		Piglin piglin = EntityTypes.PIGLIN.create(level, EntitySpawnReason.COMMAND);
		double punch = piglin.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
		piglin.discard();
		helper.assertTrue(boss.getAttributeValue(Attributes.ATTACK_DAMAGE) == punch, "a piglin's punch: " + boss.getAttributeValue(Attributes.ATTACK_DAMAGE)
				+ " vs " + punch);
		helper.assertTrue(boss.getAttributeValue(Attributes.ATTACK_KNOCKBACK) > 0.0, "the slap knocks back");
		helper.succeedWhen(() -> {
			float lost = player.getMaxHealth() - player.getHealth();
			helper.assertTrue(lost > 0.0F, "not slapped yet");
			helper.assertTrue(lost >= punch * 0.5 && lost <= punch * 1.5 + 1.0, "slapped for " + lost + " (a piglin's punch " + punch + ")");
		});
	}

	/** Its outpost's reset brings it back seated: a new one when it's gone, the same one sat back down while it lives. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 100)
	public void comesBackWithTheRoster(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CampPiece piece = outpost(helper, CampPiece.Portal.UNLIT);
		CampSite site = site(helper, piece);
		List<Mob> roster = roster(helper, site);
		BossPiglin first = boss(helper, site);
		helper.runAfterDelay(2, () -> clear(helper, site));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(first.isFleeing(), "stands once the roster is gone"))
				.thenExecute(() -> {
					CampSites.resetCheck(level, true);
					helper.assertTrue(!site.spawned(), "the cleared outpost resets");
					List<Mob> again = roster(helper, site);
					helper.assertTrue(!again.isEmpty(), "the roster is back");
					BossPiglin boss = boss(helper, site);
					helper.assertTrue(boss == first && boss.isSeated() && boss.position().distanceTo(site.throne().seat()) < 0.1, "the living boss sits back down");
					clear(helper, site);
				})
				.thenWaitUntil(() -> helper.assertTrue(first.isFleeing(), "stands again"))
				.thenExecute(() -> {
					first.discard();
					CampSites.resetCheck(level, true);
					roster(helper, site);
					BossPiglin boss = boss(helper, site);
					helper.assertTrue(boss != first && boss.isSeated(), "a new boss on the throne");
				})
				.thenSucceed();
	}

	/** Over many kills: each trimmed armour piece about 1 in 2, gold every time, a gold block sometimes, the snout template about 1 in 3. */
	@GameTest(maxTicks = 5)
	public void dropsRollAsSpecified(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BossPiglin boss = BossPiglins.BOSS_PIGLIN.create(level, EntitySpawnReason.COMMAND);
		boss.snapTo(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
		ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("entities/boss_piglin"));
		helper.assertTrue(boss.getType().getDefaultLootTable().orElse(null) == table, "its loot table is entities/boss_piglin");
		int rolls = 400;
		int[] pieces = new int[BossPiglin.Gear.values().length];
		int blocks = 0;
		int templates = 0;
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> drops = new ArrayList<>();
			boss.dropFromLootTable(level, level.damageSources().generic(), false, table, drops::add);
			helper.assertTrue(drops.stream().anyMatch(s -> s.is(Items.GOLD_INGOT)) && drops.stream().anyMatch(s -> s.is(Items.GOLD_NUGGET)),
					"gold every time: " + drops);
			for (BossPiglin.Gear gear : BossPiglin.Gear.values()) {
				for (ItemStack stack : drops) {
					if (stack.is(gear.item)) {
						var trim = stack.get(DataComponents.TRIM);
						helper.assertTrue(trim != null && trim.pattern().is(gear.pattern) && trim.material().is(gear.material), gear + " keeps its trim: " + trim);
						pieces[gear.ordinal()]++;
					}
				}
			}
			blocks += drops.stream().filter(s -> s.is(Items.GOLD_BLOCK)).count();
			templates += drops.stream().filter(s -> s.is(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE)).count();
		}
		for (BossPiglin.Gear gear : BossPiglin.Gear.values()) {
			int n = pieces[gear.ordinal()];
			helper.assertTrue(n >= rolls * 0.38 && n <= rolls * 0.62, n + " " + gear + " in " + rolls);
		}
		helper.assertTrue(blocks >= rolls * 0.1 && blocks <= rolls * 0.3, blocks + " gold blocks in " + rolls);
		helper.assertTrue(templates >= rolls * 0.23 && templates <= rolls * 0.44, templates + " snout templates in " + rolls);
		helper.succeed();
	}

	/** It wears the trimmed gold armour the loot table drops, and none of it drops as equipment. */
	@GameTest(maxTicks = 5)
	public void wearsTrimmedGold(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BossPiglin boss = BossPiglins.BOSS_PIGLIN.create(level, EntitySpawnReason.COMMAND);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.COMMAND, null);
		for (BossPiglin.Gear gear : BossPiglin.Gear.values()) {
			ItemStack worn = boss.getItemBySlot(gear.slot);
			var trim = worn.get(DataComponents.TRIM);
			helper.assertTrue(worn.is(gear.item) && trim != null && trim.pattern().is(gear.pattern) && trim.material().is(gear.material), gear + ": " + worn);
		}
		helper.assertTrue(boss.getMainHandItem().isEmpty(), "empty hands: it slaps");
		boss.discard();
		helper.succeed();
	}
}
