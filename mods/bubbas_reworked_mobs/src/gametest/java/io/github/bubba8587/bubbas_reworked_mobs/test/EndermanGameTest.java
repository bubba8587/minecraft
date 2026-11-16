// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanMind;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanSpawning;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanTuning;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanWards;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TargetWorld;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TerrainKinds;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TerrainOracle;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import io.github.bubba8587.bubbas_reworked_mobs.test.mixin.EndermanAccessor;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The caretaker endermen (specs/endermen.md). The game test world is superflat, so live tests stand in a known target
 * for the test field ({@link #stoneField}: stone up to the floor, air above; everything outside the field counts as
 * already right). Each live test has its own night environment, so its batch is alone and no sun makes endermen
 * teleport.
 */
public class EndermanGameTest {
	static final String FIELD = CreeperGameTest.FIELD;

	/** The target inside the field: stone to the floor, air above (plus {@code exceptions}); outside it: what is there. */
	static Runnable stoneField(GameTestHelper helper, Map<BlockPos, BlockState> exceptions) {
		ServerLevel level = helper.getLevel();
		AABB bounds = helper.getBounds();
		int floor = helper.absolutePos(BlockPos.ZERO).getY();
		int minX = (int) Math.floor(bounds.minX);
		int minZ = (int) Math.floor(bounds.minZ);
		int maxX = (int) Math.ceil(bounds.maxX) - 1;
		int maxZ = (int) Math.ceil(bounds.maxZ) - 1;
		Map<BlockPos, BlockState> absolute = new java.util.HashMap<>();
		exceptions.forEach((pos, state) -> absolute.put(helper.absolutePos(pos), state));
		BoundingBox box = new BoundingBox(minX - 32, level.getMinY(), minZ - 32, maxX + 32, level.getMaxY(), maxZ + 32);
		return TargetWorld.overrideTerrain(level, box, pos -> {
			BlockState exception = absolute.get(pos);
			if (exception != null) {
				return exception;
			}
			boolean inside = pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
			if (!inside) {
				return level.getBlockState(pos);
			}
			return pos.getY() <= floor ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}, (x, z) -> x >= minX && x <= maxX && z >= minZ && z <= maxZ ? floor : level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1);
	}

	/** Takes a built cairn down again (its end rods would light up neighbouring tests) and forgets the site. */
	static void removeBuilt(ServerLevel level, CairnSite site) {
		for (BlockPos pos : site.order()) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		CairnSites.removeSite(level, site);
	}

	static Enderman enderman(GameTestHelper helper, double x, double z, boolean ai) {
		Enderman enderman = helper.spawn(EntityTypes.ENDERMAN, new Vec3(x, 1.0, z));
		enderman.setNoAi(!ai);
		enderman.setPersistenceRequired();
		return enderman;
	}

	static EndermanMind mind(GameTestHelper helper, Enderman enderman) {
		EndermanMind mind = EndermanMind.of(enderman);
		helper.assertTrue(mind != null, "an overworld enderman should be a caretaker");
		return mind;
	}

	// --- the target world --------------------------------------------------------------------------

	/** The oracle regenerates the superflat layers exactly, and a cairn's design overrides terrain inside its site. */
	public void terrainOracleAnswersFlatLayersAndCairns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(5, 1, 5));
		long started = System.nanoTime();
		TerrainOracle.TerrainChunk chunk = TerrainOracle.getNow(level, at.getX() >> 4, at.getZ() >> 4);
		System.out.printf("[endermen] flat terrain chunk in %.2f ms%n", (System.nanoTime() - started) / 1e6);
		NoiseColumn column = level.getChunkSource().getGenerator().getBaseColumn(at.getX(), at.getZ(), level, level.getChunkSource().randomState());
		for (int y = level.getMinY(); y < level.getMinY() + 16; y++) {
			BlockPos pos = at.atY(y);
			helper.assertTrue(chunk.state(pos) == column.getBlock(y), "oracle and generator disagree at y " + y + ": " + chunk.state(pos) + " vs " + column.getBlock(y));
		}
		helper.assertTrue(chunk.state(at.atY(level.getMinY())).is(Blocks.BEDROCK), "the flat world's bottom should be bedrock");
		helper.assertTrue(chunk.state(at.atY(level.getMaxY() - 1)).isAir(), "the sky should be air");
		CairnSite site = CairnSites.addSite(level, helper.absolutePos(new BlockPos(12, 1, 8)), 42L);
		TerrainOracle.getNow(level, site.origin().getX() >> 4, site.origin().getZ() >> 4);
		try {
			for (BlockPos pos : site.order()) {
				helper.assertTrue(TargetWorld.target(level, pos) == site.at(pos), "the target at a cairn block should be the cairn's block at " + pos);
			}
			helper.assertTrue(TargetWorld.target(level, site.origin().above(40)) == null,
					"outside a cairn, the test server's endermen judge only where a test stands in a target");
			helper.assertTrue(TerrainOracle.getNow(level, site.origin().getX() >> 4, site.origin().getZ() >> 4).state(site.origin().above(40)).isAir(),
					"far above the cairn the terrain is air");
		} finally {
			CairnSites.removeSite(level, site);
		}
		helper.succeed();
	}

	/** Designs are the same for the same seed, vary between seeds, and their structure is only End blocks. */
	public void cairnDesignsVaryAndUseOnlyEndBlocks(GameTestHelper helper) {
		Set<Block> structural = Set.of(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_STAIRS, Blocks.PURPUR_SLAB, Blocks.END_STONE,
				Blocks.END_STONE_BRICKS, Blocks.END_STONE_BRICK_SLAB, Blocks.END_ROD, Blocks.STAINED_GLASS.magenta());
		Set<Integer> heights = new HashSet<>();
		Set<Integer> sizes = new HashSet<>();
		int multiTrunk = 0;
		int withTnt = 0;
		int designs = 0;
		for (CairnDesign.Offerings palette : CairnDesign.Offerings.values()) {
			for (long seed = 1; seed <= 12; seed++) {
				Direction pointer = Direction.from2DDataValue((int) (seed % 4));
				List<Map.Entry<BlockPos, BlockState>> design = CairnDesign.design(seed * 7919L, palette, pointer, (x, z) -> -1);
				helper.assertTrue(design.equals(CairnDesign.design(seed * 7919L, palette, pointer, (x, z) -> -1)), "a design should be the same for the same seed");
				designs++;
				sizes.add(design.size());
				int trunks = 0;
				int maxY = Integer.MIN_VALUE;
				BlockPos tallestRod = null;
				boolean offering = false;
				for (Map.Entry<BlockPos, BlockState> e : design) {
					BlockState state = e.getValue();
					helper.assertFalse(state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN), "never obsidian (E3)");
					if (state.is(BlockTags.ENDERMAN_HOLDABLE)) {
						offering = true;
						withTnt += state.is(Blocks.TNT) ? 1 : 0;
						continue;
					}
					helper.assertTrue(structural.contains(state.getBlock()), "unexpected structural block " + state);
					helper.assertFalse(TerrainKinds.isNatural(state), "a cairn's structure must not generate naturally: " + state);
					maxY = Math.max(maxY, e.getKey().getY());
					if (state.is(Blocks.PURPUR_PILLAR) && e.getKey().getY() == 0) {
						trunks++;
					}
					if (state.is(Blocks.END_ROD) && (tallestRod == null || e.getKey().getY() > tallestRod.getY())) {
						tallestRod = e.getKey();
					}
				}
				helper.assertTrue(offering, "every cairn should have offerings");
				helper.assertTrue(tallestRod != null, "every cairn should have end rods");
				BlockPos rod = tallestRod;
				BlockState rodState = design.stream().filter(e -> e.getKey().equals(rod)).findFirst().orElseThrow().getValue();
				helper.assertTrue(rodState.getValue(BlockStateProperties.FACING) == pointer,
						"the tallest end rod should point " + pointer + ", it points " + rodState.getValue(BlockStateProperties.FACING) + " (seed " + seed + ")");
				helper.assertTrue(design.stream().filter(e -> e.getValue().is(Blocks.END_ROD) && e.getKey().getY() == rod.getY()).count() == 1,
						"the pointer should be the only rod at the top");
				heights.add(maxY);
				multiTrunk += trunks > 1 ? 1 : 0;
			}
		}
		helper.assertTrue(heights.size() >= 5, "heights should vary, got " + heights);
		helper.assertTrue(sizes.size() >= 15, "sizes should vary, got " + sizes.size());
		helper.assertTrue(multiTrunk >= designs / 5, "some cairns should have several trunks: " + multiTrunk + " of " + designs);
		helper.assertTrue(withTnt > 0 && withTnt < designs / 2, "TNT should be rare but present: " + withTnt + " of " + designs);
		helper.succeed();
	}

	// --- jobs --------------------------------------------------------------------------------------

	/** E1: a block where the ground was open is taken, and carried to a hole whose original block is its family. */
	public void endermanTakesStrayBlockAndFillsHole(GameTestHelper helper) {
		Runnable restore = stoneField(helper, Map.of());
		BlockPos pile = new BlockPos(10, 1, 8);
		BlockPos hole = new BlockPos(13, 0, 8);
		helper.setBlock(pile, Blocks.COBBLESTONE);
		helper.setBlock(hole.below(), Blocks.STONE);
		helper.setBlock(hole, Blocks.AIR);
		Enderman enderman = enderman(helper, 11.5, 6.5, true);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, pile);
			helper.assertBlockPresent(Blocks.STONE, hole);
			helper.assertTrue(enderman.getCarriedBlock() == null, "it should have put the block down");
			restore.run();
			enderman.discard();
		});
	}

	/** A builder carrying a missing cairn block walks over and puts it in place; the cairn is complete again. */
	public void builderEndermanDeliversCairnBlock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CairnSite site = CairnSites.addSite(level, helper.absolutePos(new BlockPos(16, 1, 8)), 1234L);
		site.buildAll(level);
		BlockPos spot = site.order().stream().filter(p -> p.getY() == site.origin().getY() + 1 && site.at(p).is(Blocks.PURPUR_PILLAR)).findFirst()
				.orElseThrow();
		level.setBlockAndUpdate(spot, Blocks.AIR.defaultBlockState());
		helper.assertFalse(site.complete(level), "the cairn should be missing a block");
		Enderman enderman = enderman(helper, 4.5, 4.5, true);
		enderman.setCarriedBlock(site.at(spot));
		mind(helper, enderman).set(EndermanMind.Job.DELIVER, spot, level.getGameTime());
		helper.succeedWhen(() -> {
			helper.assertTrue(site.complete(level), "the builder should have finished the cairn");
			helper.assertTrue(enderman.getCarriedBlock() == null, "it should have put its block in");
			removeBuilt(level, site);
			enderman.discard();
		});
	}

	/** E4: eye contact angers an idle enderman, but one busy on a job ignores it. */
	public void busyEndermanIgnoresEyeContact(GameTestHelper helper) {
		Enderman enderman = enderman(helper, 12.5, 10.5, false);
		ServerPlayer player = CreeperGameTest.player(helper, 12.5, 2.5);
		player.lookAt(EntityAnchorArgument.Anchor.EYES, enderman.getEyePosition());
		EndermanAccessor stare = (EndermanAccessor) enderman;
		helper.assertTrue(stare.brmTest$isBeingStaredBy(player), "an idle enderman should notice the stare");
		EndermanMind mind = mind(helper, enderman);
		mind.set(EndermanMind.Job.TAKE, enderman.blockPosition(), helper.getLevel().getGameTime());
		mind.setBusy(true);
		helper.assertFalse(stare.brmTest$isBeingStaredBy(player), "a busy enderman should ignore the stare");
		mind.set(EndermanMind.Job.PERCH, enderman.blockPosition(), helper.getLevel().getGameTime());
		helper.assertTrue(stare.brmTest$isBeingStaredBy(player), "standing on a cairn is not a job");
		CreeperGameTest.remove(helper, player);
		enderman.discard();
		helper.succeed();
	}

	/** Jumping on farmland where dirt belongs turns it back to dirt (owner's example). */
	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_trample", structure = FIELD, maxTicks = 400)
	public void trampleFarmlandBackToDirt(GameTestHelper helper) {
		BlockPos farmland = new BlockPos(8, 0, 8);
		BlockPos water = new BlockPos(8, 0, 11);
		Runnable restore = stoneField(helper, Map.of(farmland, Blocks.DIRT.defaultBlockState(), water, Blocks.WATER.defaultBlockState()));
		helper.setBlock(water, Blocks.WATER);
		helper.setBlock(farmland, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		// The test field is only 4 blocks high and barrier-roofed: open the roof over the farmland so it can jump.
		for (BlockPos roof : BlockPos.betweenClosed(7, 3, 7, 9, 6, 9)) {
			helper.setBlock(roof, Blocks.AIR);
		}
		Enderman enderman = enderman(helper, 9.5, 9.5, true);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.DIRT, farmland);
			restore.run();
			enderman.discard();
		});
	}

	/** E4: breaking a standing cairn block angers the endermen around; so does hitting one near a site. */
	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_anger", structure = FIELD, maxTicks = 20)
	public void breakingCairnAngersEndermen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CairnSite site = CairnSites.addSite(level, helper.absolutePos(new BlockPos(16, 1, 8)), 77L);
		site.buildAll(level);
		Enderman watcher = enderman(helper, 6.5, 4.5, false);
		Enderman other = enderman(helper, 6.5, 12.5, false);
		ServerPlayer player = CreeperGameTest.player(helper, 12.5, 2.5);
		try {
			player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 0, 2)));
			helper.assertTrue(watcher.getTarget() == null, "breaking a floor block shouldn't anger anyone");
			BlockPos cairnBlock = site.order().stream().filter(p -> p.getY() > site.origin().getY() && site.at(p).is(Blocks.PURPUR_BLOCK)).findFirst().orElseThrow();
			player.gameMode.destroyBlock(cairnBlock);
			helper.assertTrue(watcher.getTarget() == player && other.getTarget() == player, "breaking a cairn block should anger the endermen around");
			watcher.setTarget(null);
			other.setTarget(null);
			watcher.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
			helper.assertTrue(other.getTarget() == player, "hitting one near a cairn should anger the others");
		} finally {
			CreeperGameTest.remove(helper, player);
			removeBuilt(level, site);
			watcher.discard();
			other.discard();
		}
		helper.succeed();
	}

	// --- spawning (D2) -----------------------------------------------------------------------------

	/** Vanilla overworld enderman spawns are blocked while the rule is on; a site spawns builders up to its cap. */
	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_spawn", structure = FIELD, maxTicks = 40)
	public void siteSpawnsBuildersUpToItsCap(GameTestHelper helper) {
		// The environment's night takes effect on the level's next tick (sky darkness is updated per tick).
		helper.runAtTickTime(5, () -> spawnsBuildersUpToItsCap(helper));
	}

	private void spawnsBuildersUpToItsCap(GameTestHelper helper) {
		CreeperGameTest.forceNight(helper);
		ServerLevel level = helper.getLevel();
		helper.assertTrue(EndermanSpawning.blocksVanillaSpawn(level, EntityTypes.ENDERMAN), "vanilla overworld enderman spawns should be blocked");
		helper.assertFalse(EndermanSpawning.blocksVanillaSpawn(level, EntityTypes.ZOMBIE), "only endermen are blocked");
		level.getGameRules().set(ModGameRules.CAIRN_ENDERMEN, false, level.getServer());
		helper.assertFalse(EndermanSpawning.blocksVanillaSpawn(level, EntityTypes.ENDERMAN), "the rule turns the block off");
		level.getGameRules().set(ModGameRules.CAIRN_ENDERMEN, true, level.getServer());
		helper.assertTrue(EndermanSpawning.chance(true, 0) > EndermanSpawning.chance(false, 0), "a complete cairn spawns faster");
		helper.assertTrue(EndermanSpawning.chance(false, 2) > EndermanSpawning.chance(false, 0), "more endermen about, faster (snowball)");

		CairnSite site = CairnSites.addSite(level, helper.absolutePos(new BlockPos(12, 1, 8)), 99L);
		List<Enderman> spawned = new ArrayList<>();
		// Endermen left by earlier tests nearby would count toward this site's cap.
		EndermanSpawning.endermenAt(level, site).forEach(Enderman::discard);
		try {
			for (int i = 0; i < 40 && spawned.size() < EndermanTuning.CAP_UNFINISHED + 1; i++) {
				Enderman e = EndermanSpawning.trySpawnAt(level, site, true, 4.0, 7.0);
				if (e != null) {
					spawned.add(e);
				}
			}
			helper.assertTrue(spawned.size() == EndermanTuning.CAP_UNFINISHED, "an unfinished site should spawn up to its cap, got " + spawned.size() + " (nearest player " + level.getNearestPlayer(site.origin().getX(), site.origin().getY(), site.origin().getZ(), 40.0, false) + ", light " + level.getMaxLocalRawBrightness(site.origin().east(5)) + ", sky " + level.getSkyDarken() + ")");
			for (Enderman e : spawned) {
				double d = Math.sqrt(e.blockPosition().distSqr(site.origin().atY(e.getBlockY())));
				helper.assertTrue(d >= 3.0 && d <= 8.0, "spawned " + d + " blocks out");
			}
			// Jobs for newcomers: most carry one of the unfinished cairn's missing blocks (going in now when it can).
			int builders = 0;
			for (int i = 0; i < 20; i++) {
				Enderman e = EntityTypes.ENDERMAN.create(level, net.minecraft.world.entity.EntitySpawnReason.NATURAL);
				e.snapTo(Vec3.atBottomCenterOf(site.origin().offset(5, 0, 0)));
				EndermanSpawning.giveJob(level, e, site, false, level.getRandom());
				EndermanMind mind = EndermanMind.of(e);
				if (e.getCarriedBlock() != null && site.wants(level, e.getCarriedBlock().getBlock())
						&& (mind.job() == null || mind.job() == EndermanMind.Job.DELIVER && site.at(mind.target()) == e.getCarriedBlock())) {
					builders++;
				}
				e.discard();
			}
			helper.assertTrue(builders >= 6 && builders < 20, "about two thirds of newcomers should be builders, got " + builders + " of 20");
			// Builders split the work: each newcomer brings the lowest missing block nobody there carries yet.
			List<Enderman> carriers = new ArrayList<>();
			List<BlockPos> claimed = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				BlockPos next = site.nextUnclaimed(level, carriers);
				helper.assertTrue(next != null && !claimed.contains(next), "newcomer " + i + " should bring a block nobody carries, got " + next + " after " + claimed);
				Enderman carrier = EntityTypes.ENDERMAN.create(level, net.minecraft.world.entity.EntitySpawnReason.NATURAL);
				carrier.setCarriedBlock(site.at(next));
				carriers.add(carrier);
				claimed.add(next);
			}
			carriers.forEach(Enderman::discard);
		} finally {
			spawned.forEach(e -> e.discard());
			CairnSites.removeSite(level, site);
		}
		helper.succeed();
	}

	/** E2: a ward over a site stops it spawning and keeps endermen's judgement out. */
	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_ward", structure = FIELD, maxTicks = 20)
	public void wardStopsSpawningAndWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CairnSite site = CairnSites.addSite(level, helper.absolutePos(new BlockPos(12, 1, 8)), 5L);
		Object key = new Object();
		try {
			BlockPos block = site.order().getFirst();
			helper.assertTrue(TargetWorld.target(level, block) != null, "unwarded, the cairn has a target");
			EndermanWards.put(level, key, new AABB(site.origin()).inflate(20.0), 400);
			helper.assertTrue(TargetWorld.target(level, block) == null, "warded, endermen judge nothing");
			for (int i = 0; i < 10; i++) {
				helper.assertTrue(EndermanSpawning.trySpawnAt(level, site, true) == null, "a warded site spawns nothing");
			}
			EndermanWards.remove(level, key);
			helper.assertTrue(TargetWorld.target(level, block) != null, "released, the target is back");
		} finally {
			EndermanWards.remove(level, key);
			CairnSites.removeSite(level, site);
		}
		helper.succeed();
	}

	/** A caretaker carrying a block still despawns (vanilla keeps carriers forever): what it couldn't place vanishes. */
	@GameTest(maxTicks = 20)
	public void carryingCaretakerStillDespawns(GameTestHelper helper) {
		Enderman enderman = enderman(helper, 12.5, 8.5, false);
		enderman.setCarriedBlock(Blocks.PURPUR_BLOCK.defaultBlockState());
		helper.assertFalse(enderman.requiresCustomPersistence(), "a carrying caretaker should still be allowed to despawn");
		enderman.discard();
		helper.succeed();
	}
}
