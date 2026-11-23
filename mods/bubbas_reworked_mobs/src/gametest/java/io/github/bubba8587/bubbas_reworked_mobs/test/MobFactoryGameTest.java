// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess;
import io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorMobs;
import io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSite;
import io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites;
import io.github.bubba8587.bubbas_reworked_mobs.factory.Fabricators;
import io.github.bubba8587.bubbas_reworked_mobs.factory.FactoryDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Ancient Fabricators at work (specs/mob-factory.md). Each test puts up a whole Fabricator far out from the test grid
 * (its own spot, chunks force-loaded), waits until its chunks tick entities, then runs it without a player
 * ({@code Fabricators.force}) and sends what the teleporter fires to a stone ledge 30 above the pad
 * ({@code Fabricators.destinationOverride}; the superflat test world has no surface above it). It stands in the open
 * there, so the tests use mobs that don't burn in daylight. Milestone suite; the core suite runs the teleporter test.
 * <p>
 * Isolation (the flaky Fabricator tests, BACKLOG.md): every site gets its own absolute spot {@link #SPACING} blocks
 * from the next, whatever the test's place on the grid (sites placed relative to the grid could land almost on top of
 * each other, so one test's Warden counted in another's box and un-forcing one test's chunks dropped a neighbour's).
 * Forced chunks are counted per chunk, and the site, its chunks, its entities and its players are cleaned up when the
 * test is done, pass or fail ({@link TestCleanup}).
 */
public class MobFactoryGameTest {
	private static final AtomicInteger NEXT = new AtomicInteger();
	/**
	 * Where the first test site goes (absolute, far from the test grid), and the gap to the next: well over a site's box
	 * plus its Warden search (16) and its forced chunks (3 chunks either side).
	 */
	private static final int ORIGIN_X = 30000;
	private static final int ORIGIN_Z = 30000;
	private static final int SPACING = 256;
	/** Running sites per forced chunk. */
	private static final java.util.Map<Long, Integer> FORCED = new java.util.HashMap<>();
	/** A full count: 8 lamps x 40 ticks, then the 20-tick lift. */
	static final int COUNT = 8 * 40 + 20;
	/** Ticks allowed for the forced chunks to start ticking entities. */
	static final int SETTLE = 100;
	/** Ticks allowed for the first mob to drop onto the deck once a site runs. */
	static final int DROP_WAIT = 150;

	/** A running test Fabricator: {@code tick} is called every tick once it runs, with the ticks since it started. */
	interface Body {
		void tick(FabricatorSite site, int t);
	}

	static void run(GameTestHelper helper, FactoryDesign.Mob mob, Body body) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = new BlockPos(ORIGIN_X + NEXT.getAndIncrement() * SPACING, 40, ORIGIN_Z);
		chunks(level, origin, true);
		FabricatorSite site = FabricatorSites.addSite(level, new FabricatorSite(origin, Rotation.NONE, 4242L, mob, null));
		TestCleanup.whenDone(helper, () -> cleanUp(level, site));
		site.design().place(level, origin, Rotation.NONE);
		Fabricators.destinationOverride = (l, s) -> {
			BlockPos to = s.pad().above(30);
			l.setBlock(to.below(), Blocks.STONE.defaultBlockState(), 2);
			return to;
		};
		int[] started = { -1 };
		int[] t = { 0 };
		helper.onEachTick(() -> {
			if (started[0] < 0) {
				if (level.isPositionEntityTicking(site.pad()) && level.isPositionEntityTicking(site.wardenSpot())
						&& level.isPositionEntityTicking(site.pad().above(30))) {
					started[0] = 0;
					Fabricators.prepare(level, site);
					Fabricators.force(level, site, true);
				}
				return;
			}
			body.tick(site, t[0]++);
		});
	}

	/** The test passed. The site itself is cleaned up by the hook {@link #run} registered, which runs on a failure too. */
	static void done(GameTestHelper helper, FabricatorSite site) {
		helper.succeed();
	}

	/** Stops the site and takes it away: its entities, the test players near it, its forced chunks. */
	private static void cleanUp(ServerLevel level, FabricatorSite site) {
		Fabricators.force(level, site, false);
		FabricatorSites.removeSite(level, site);
		AABB area = AABB.of(site.box()).inflate(40);
		for (ServerPlayer p : level.getServer().getPlayerList().getPlayers().stream()
				.filter(p -> p.getGameProfile().name().startsWith("test-") && area.contains(p.position())).toList()) {
			level.getServer().getPlayerList().remove(p);
		}
		for (Entity e : level.getEntitiesOfClass(Entity.class, area, e -> !(e instanceof ServerPlayer))) {
			e.discard();
		}
		chunks(level, site.origin(), false);
	}

	/** Forces (or releases) the 7 x 7 chunks round {@code origin}; a chunk is released only once no running site uses it. */
	private static void chunks(ServerLevel level, BlockPos origin, boolean on) {
		int cx = origin.getX() >> 4;
		int cz = origin.getZ() >> 4;
		synchronized (FORCED) {
			for (int x = cx - 3; x <= cx + 3; x++) {
				for (int z = cz - 3; z <= cz + 3; z++) {
					long key = net.minecraft.world.level.ChunkPos.pack(x, z);
					int users = FORCED.merge(key, on ? 1 : -1, Integer::sum);
					if (on && users == 1) {
						level.setChunkForced(x, z, true);
					} else if (!on && users <= 0) {
						FORCED.remove(key);
						level.setChunkForced(x, z, false);
					}
				}
			}
		}
	}

	static String state(ServerLevel level, FabricatorSite site) {
		Fabricators.Running r = Fabricators.running(level, site);
		return "running " + (r == null ? "no" : "step " + r.step()) + ", mobs in box " + level.getEntitiesOfClass(Mob.class, AABB.of(site.box())).size()
				+ ", game time " + level.getGameTime() + ", entity ticking " + level.isPositionEntityTicking(site.pad());
	}

	static List<Mob> deactivated(ServerLevel level, FabricatorSite site) {
		return level.getEntitiesOfClass(Mob.class, site.deckArea(), FabricatorMobs::isDeactivated);
	}

	/**
	 * The dripper drops a deactivated mob onto the pad (limp, looking down, unhurt by the fall, silent, no target); the
	 * lamps count up, it is lifted (Levitation), then sent up to its destination, where it wakes as a reworked mob (a
	 * watcher creeper, not a vanilla one).
	 */
	// core: ReworkedMobsCoreGameTest
	public void teleporterSendsItUpAndItWakesReworked(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		boolean[] seen = new boolean[4]; // lamps counted, lifted, arrived, landed limp on the pad
		run(helper, FactoryDesign.Mob.CREEPER, (site, t) -> {
			if (!seen[3]) {
				for (Mob mob : deactivated(level, site)) {
					if (mob.onGround() && site.padArea().contains(mob.position())) {
						helper.assertTrue(mob.getXRot() > 45.0F, "a deactivated mob looks down, pitch " + mob.getXRot());
						helper.assertTrue(mob.getHealth() == mob.getMaxHealth(), "the drop doesn't hurt it");
						helper.assertTrue(mob.isSilent(), "deactivated mobs are silent");
						helper.assertTrue(mob.getTarget() == null, "and target nothing");
						seen[3] = true;
					}
				}
			}
			Fabricators.Running running = Fabricators.running(level, site);
			if (running != null && running.step() >= 4) {
				long lit = site.timerLamps().stream().filter(p -> level.getBlockState(p).getValue(RedstoneLampBlock.LIT)).count();
				seen[0] |= lit == running.step();
			}
			for (Creeper c : level.getEntitiesOfClass(Creeper.class, site.padArea(), FabricatorMobs::isDeactivated)) {
				seen[1] |= c.hasEffect(MobEffects.LEVITATION);
			}
			BlockPos to = site.pad().above(30);
			for (Creeper c : level.getEntitiesOfClass(Creeper.class, new AABB(to).inflate(2))) {
				FabricatorMobs.State state = c.getAttached(FabricatorMobs.DEACTIVATED);
				if (state != null && state.stage() == FabricatorMobs.ARRIVED) {
					seen[2] = true;
				} else if (state == null && seen[2]) {
					helper.assertTrue(seen[0], "the timer lamps lit one by one with the count");
					helper.assertTrue(seen[1], "the mob on the pad was lifted before it fired");
					helper.assertTrue(seen[3], "it landed limp on the pad first");
					helper.assertFalse(((CreeperAccess) c).brm$isVanilla(), "it wakes as a reworked (watcher) creeper");
					helper.assertFalse(c.isSilent(), "awake, it makes sounds again");
					done(helper, site);
					return;
				}
			}
		});
	}

	/** Redstone into the pad's lodestone stops the teleporter: the lamps stay dark and the mobs stay on the deck. */
	@GameTest(maxTicks = SETTLE + COUNT + 100)
	public void redstoneStopsTheTeleporter(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		run(helper, FactoryDesign.Mob.SPIDER, (site, t) -> {
			if (t == 0) {
				level.setBlock(site.control().below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
			}
			Fabricators.Running running = Fabricators.running(level, site);
			helper.assertTrue(running == null || running.step() == 0, "stopped: the count never starts");
			for (BlockPos lamp : site.timerLamps()) {
				helper.assertFalse(level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "stopped: the lamps stay dark");
			}
			if (t == COUNT + 60) {
				helper.assertTrue(!deactivated(level, site).isEmpty(), "the dropped mob is still on the deck, deactivated");
				helper.assertTrue(level.getEntitiesOfClass(Mob.class, new AABB(site.pad().above(30)).inflate(3)).isEmpty(), "nothing was sent up");
				done(helper, site);
			}
		});
	}

	/**
	 * Breaking the hidden spawner stops it for good: once a mob has dropped onto the deck the spawner is broken; the
	 * mob on the deck wakes there and no more drop.
	 */
	@GameTest(maxTicks = SETTLE + DROP_WAIT + 30 + COUNT + 40)
	public void breakingTheHiddenSpawnerStopsItForGood(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int[] broke = { -1 };
		run(helper, FactoryDesign.Mob.ENDERMAN, (site, t) -> {
			if (broke[0] < 0) {
				if (deactivated(level, site).isEmpty()) {
					helper.assertTrue(t < DROP_WAIT, "no mob dropped within " + DROP_WAIT + " ticks; " + state(level, site));
					return;
				}
				helper.assertTrue(level.getBlockState(site.spawner()).is(Blocks.SPAWNER), "the hidden spawner is where the design says");
				level.destroyBlock(site.spawner(), false);
				broke[0] = t;
			} else if (t == broke[0] + 30) {
				helper.assertTrue(FabricatorSites.has(level, site, FabricatorSites.BROKEN), "the Fabricator knows it's broken");
				helper.assertTrue(deactivated(level, site).isEmpty(), "the mob on the deck woke up");
			} else if (t == broke[0] + 30 + COUNT) {
				helper.assertTrue(deactivated(level, site).isEmpty(), "nothing more dropped (a new drop would lie deactivated on the deck)");
				done(helper, site);
			}
		});
	}

	/** The Warden stands passive in its office window until a player goes for the machine; then it's angry at them. */
	@GameTest(maxTicks = SETTLE + 120)
	public void wardenWakesWhenItHearsAPlayerAtTheMachine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer[] player = new ServerPlayer[1];
		run(helper, FactoryDesign.Mob.SPIDER, (site, t) -> {
			List<Warden> wardens = Fabricators.wardens(level, site);
			if (t == 1) {
				helper.assertTrue(wardens.size() == 1, "one Warden per Fabricator, found " + wardens.size());
				Warden warden = wardens.getFirst();
				helper.assertTrue(warden.isNoAi() && Boolean.TRUE.equals(warden.getAttached(Fabricators.WARDEN)), "it starts passive");
				helper.assertTrue(warden.isPersistenceRequired(), "it never despawns");
				player[0] = TestPlayers.survival(helper);
				BlockPos near = site.floorBeacon().east(2);
				player[0].snapTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
			} else if (t == 50) {
				Warden warden = wardens.getFirst();
				helper.assertFalse(warden.isNoAi(), "it heard the player at the beacon and woke; " + state(level, site) + " player at " + player[0].blockPosition() + " in level " + level.players().contains(player[0]));
				helper.assertTrue(warden.getAngerManagement().getActiveAnger(player[0]) >= 80, "angry at them");
				helper.getLevel().getServer().getPlayerList().remove(player[0]);
				done(helper, site);
			}
		});
	}

	/**
	 * With Infused Gems, the office chests' spare gems carry their effect (bug 2026-11-16: the entries used the old
	 * "functions" key, which 26.3 ignores, so they came out blank). Rolls the table until a few gems turn up.
	 */
	@GameTest
	public void officeGemsCarryTheirEffect(GameTestHelper helper) {
		if (!io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLink.gemsLoaded()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var table = level.getServer().reloadableRegistries().getLootTable(io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLoot.OFFICE_WITH_GEMS);
		var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
				.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
		var gemItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("bubbas_infused_gems:gem"));
		var gemComponent = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(net.minecraft.resources.Identifier.parse("bubbas_infused_gems:gem"));
		int gems = 0;
		for (int i = 0; i < 300 && gems < 5; i++) {
			for (var stack : table.getRandomItems(params)) {
				if (stack.is(gemItem)) {
					gems++;
					helper.assertTrue(gemComponent != null && stack.has(gemComponent), "an office gem without its effect: " + stack.getComponents());
				}
			}
		}
		helper.assertTrue(gems >= 5, "office chests should hold spare gems (" + gems + " in 300 rolls)");
		helper.succeed();
	}

	/**
	 * The Fabricator chest tables (specs/mob-factory.md): rejects and buried chests both roll items, reject armor and
	 * weapons come out damaged (the 26.3 "modifier" key works), and every office chest holds valuables and a treasure.
	 * No Fabricator needed: the tables are rolled directly.
	 */
	@GameTest
	public void chestTablesRollTheirLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
				.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
		boolean damaged = false;
		for (int i = 0; i < 20; i++) {
			var rejects = level.getServer().reloadableRegistries().getLootTable(io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLoot.REJECTS).getRandomItems(params);
			var buried = level.getServer().reloadableRegistries().getLootTable(io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLoot.BURIED).getRandomItems(params);
			helper.assertTrue(!rejects.isEmpty() && !buried.isEmpty(), "both tables roll items");
			for (var stack : rejects) {
				damaged |= stack.isDamageableItem() && stack.getDamageValue() > 0;
			}
		}
		helper.assertTrue(damaged, "reject armor and weapons come out damaged");
		// Office chests are the Warden's hoard (owner, 2026-11-22: "it should be rewarding"): every one holds valuables
		// and a treasure (an enchanted book or diamond gear, a template, a totem, a god apple, a compass or a disc).
		for (int i = 0; i < 20; i++) {
			var office = level.getServer().reloadableRegistries().getLootTable(io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLoot.OFFICE).getRandomItems(params);
			helper.assertTrue(office.stream().anyMatch(s -> s.isEnchanted() || s.has(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)
					|| s.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE) || s.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)
					|| s.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE) || s.is(net.minecraft.world.item.Items.RECOVERY_COMPASS)
					|| s.is(net.minecraft.world.item.Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) || s.is(net.minecraft.world.item.Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE)
					|| s.is(net.minecraft.world.item.Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE)), "every office chest holds a treasure: " + office);
			helper.assertTrue(office.stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.DIAMOND) || s.is(net.minecraft.world.item.Items.EMERALD)
					|| s.is(net.minecraft.world.item.Items.GOLD_INGOT) || s.is(net.minecraft.world.item.Items.IRON_INGOT)
					|| s.is(net.minecraft.world.item.Items.ECHO_SHARD) || s.is(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE)), "and valuables: " + office);
		}
		helper.succeed();
	}

	/**
	 * The generated area's secrets stand as built (specs/mob-factory.md): the reject vault's tell over a ladder, its
	 * chest and the buried chests with their tables, the intake's throat of twisting vines from a chain, and the
	 * spawner still hidden on all six sides. Checked on the debuffs test's Fabricator, as soon as it runs.
	 */
	private static void assertSecretsBuilt(GameTestHelper helper, FabricatorSite site) {
		ServerLevel level = helper.getLevel();
		FactoryDesign d = site.design();
		helper.assertTrue(d.rejectTell() != null && level.getBlockState(site.world(d.rejectTell())).is(Blocks.CHISELED_DEEPSLATE), "the vault's tell is chiseled");
		helper.assertTrue(level.getBlockState(site.world(d.rejectTell()).below()).is(Blocks.LADDER), "a ladder under the tell");
		helper.assertTrue(!d.rejectChests().isEmpty() && d.buriedChests().size() >= 2, "a reject chest and buried chests, got " + d.buriedChests().size());
		for (BlockPos p : d.rejectChests()) {
			helper.assertTrue(level.getBlockEntity(site.world(p)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c
					&& io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLoot.REJECTS.equals(c.getLootTable()), "reject chest table at " + p);
		}
		for (BlockPos p : d.buriedChests()) {
			helper.assertTrue(level.getBlockEntity(site.world(p)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c
					&& io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLoot.BURIED.equals(c.getLootTable()), "buried chest table at " + p);
			helper.assertFalse(level.getBlockState(site.world(p).above()).isRedstoneConductor(level, site.world(p).above()), "a buried chest opens at " + p);
		}
		helper.assertTrue(d.throat() != null, "the intake has a throat");
		helper.assertTrue(level.getBlockState(site.world(d.throat())).is(Blocks.TWISTING_VINES_PLANT), "twisting vines climb the throat");
		helper.assertTrue(level.getBlockState(site.world(d.intakeFloor()).above()).isAir(), "the intake chamber is open");
		BlockPos spawner = site.spawner();
		helper.assertTrue(level.getBlockState(spawner).is(Blocks.SPAWNER), "the spawner is where the design says");
		for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
			helper.assertFalse(level.getBlockState(spawner.relative(dir)).isAir(), "the spawner stays hidden (" + dir + ")");
		}
	}

	/**
	 * Without a beacon mod, the floor beacon's core gives players in the area Mining Fatigue III, Weakness, Slowness.
	 * Its Fabricator also stands in for the secrets check ({@link #assertSecretsBuilt}), so that needs no build of its own.
	 */
	@GameTest(maxTicks = SETTLE + 120)
	public void debuffsWithoutReworkedBeacons(GameTestHelper helper) {
		ServerPlayer[] player = new ServerPlayer[1];
		run(helper, FactoryDesign.Mob.SPIDER, (site, t) -> {
			if (t == 0) {
				assertSecretsBuilt(helper, site);
				player[0] = TestPlayers.survival(helper);
				BlockPos at = site.pad().south(6);
				player[0].snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			} else if (t == 90) {
				if (!Fabricators.beaconsLoaded()) {
					MobEffectInstance fatigue = player[0].getEffect(MobEffects.MINING_FATIGUE);
					helper.assertTrue(fatigue != null && fatigue.getAmplifier() == 2, "Mining Fatigue III");
					helper.assertTrue(player[0].hasEffect(MobEffects.WEAKNESS) && player[0].hasEffect(MobEffects.SLOWNESS), "Weakness and Slowness");
				}
				helper.getLevel().getServer().getPlayerList().remove(player[0]);
				done(helper, site);
			}
		});
	}
}
