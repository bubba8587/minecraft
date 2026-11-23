// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPostCommand;
import io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPostPiece;
import io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPosts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Trader posts (specs/monster-camps.md "Trader posts"): biomes, footprint plan, sound blocks and clean fences,
 * walkability, the trader's arrival with its llamas tethered in the pens, and vanilla placement left alone otherwise.
 * {@link #traderArrivesWithLlamasPenned} is also in the core suite (ReworkedMobsCoreGameTest).
 */
public class TraderPostGameTest {
	private static final String FIELD = CampsGameTest.FIELD;
	private static final BlockPos CENTRE = new BlockPos(16, 8, 24);

	/** Builds a post on a cleared field (whatever an earlier build left above the floor is taken away first). */
	private static TraderPostPiece post(GameTestHelper helper, long seed) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(CENTRE);
		BoundingBox box = new BoundingBox(c.getX() - 15, c.getY(), c.getZ() - 15, c.getX() + 15, c.getY() + 6, c.getZ() + 15);
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY() - 1, box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			level.setBlock(pos, pos.getY() == c.getY() - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
		}
		return TraderPostCommand.build(level, c, seed);
	}

	/** The first seed (from {@code from}) whose plan has this many pens and a wagon or not. */
	private static long seedWith(GameTestHelper helper, int pens, boolean wagon, long from) {
		for (long s = from; s < from + 500; s++) {
			TraderPostPiece p = new TraderPostPiece(s, BlockPos.ZERO);
			if (p.penCount() == pens && p.hasWagon() == wagon) {
				return s;
			}
		}
		throw helper.assertionException("no seed with " + pens + " pens, wagon " + wagon);
	}

	@GameTest(structure = FIELD, maxTicks = 1)
	public void traderPostBiomes(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		TagKey<Biome> tag = TagKey.create(Registries.BIOME, io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs.id("has_structure/trader_post"));
		List<ResourceKey<Biome>> in = List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.DESERT);
		List<ResourceKey<Biome>> out = List.of(Biomes.FOREST, Biomes.DARK_FOREST, Biomes.TAIGA, Biomes.JUNGLE, Biomes.SWAMP, Biomes.RIVER, Biomes.OCEAN,
				Biomes.BEACH, Biomes.SNOWY_PLAINS, Biomes.BADLANDS, Biomes.MUSHROOM_FIELDS, Biomes.WINDSWEPT_HILLS);
		for (ResourceKey<Biome> b : in) {
			helper.assertTrue(biomes.getOrThrow(b).is(tag), "trader posts belong in " + b.identifier());
		}
		for (ResourceKey<Biome> b : out) {
			helper.assertFalse(biomes.getOrThrow(b).is(tag), "no trader posts in " + b.identifier());
		}
		helper.succeed();
	}

	@GameTest(structure = FIELD, maxTicks = 1)
	public void postPiecesNeverOverlap(GameTestHelper helper) {
		List<String> faults = new ArrayList<>();
		for (long seed = 0; seed < 300; seed++) {
			TraderPostPiece piece = new TraderPostPiece(seed, new BlockPos(0, 64, 0));
			var all = piece.footprints();
			var plaza = piece.plaza();
			for (int i = 0; i < all.size(); i++) {
				var a = all.get(i);
				if (a.minX() < plaza.minX() || a.maxX() > plaza.maxX() || a.minZ() < plaza.minZ() || a.maxZ() > plaza.maxZ()) {
					faults.add("seed " + seed + ": " + a.what() + " leaves the plaza");
				}
				for (int j = i + 1; j < all.size(); j++) {
					if (!a.clearOf(all.get(j), 1)) {
						faults.add("seed " + seed + ": " + a.what() + " touches " + all.get(j).what());
					}
				}
			}
		}
		helper.assertTrue(faults.isEmpty(), faults.size() + " overlaps, e.g. " + faults.subList(0, Math.min(6, faults.size())));
		helper.succeed();
	}

	@GameTest(structure = FIELD, maxTicks = 5)
	public void everyPostBlockIsSupportedAndFencesEndCleanly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> faults = new ArrayList<>();
		long[] seeds = { seedWith(helper, 2, true, 0), seedWith(helper, 1, false, 0), seedWith(helper, 1, true, 100), seedWith(helper, 2, false, 100) };
		for (long seed : seeds) {
			TraderPostPiece piece = post(helper, seed);
			for (String f : CampsGameTest.unsupported(level, piece.getBoundingBox())) {
				faults.add("seed " + seed + " loose " + f);
			}
			for (String f : CampsGameTest.fenceFaults(level, piece.getBoundingBox())) {
				faults.add("seed " + seed + " fence " + f);
			}
		}
		helper.assertTrue(faults.isEmpty(), faults.size() + " faults, e.g. " + faults.subList(0, Math.min(10, faults.size())));
		helper.succeed();
	}

	@GameTest(structure = FIELD, maxTicks = 20)
	public void postCanBeWalkedThrough(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TraderPostPiece piece = post(helper, seedWith(helper, 2, true, 0));
		Mob walker = EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.COMMAND);
		BlockPos start = piece.entrance();
		walker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(48.0);
		walker.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(walker);
		List<BlockPos> targets = new ArrayList<>(piece.gateFronts());
		targets.add(piece.traderSpot());
		helper.runAfterDelay(1, () -> {
			walker.setOnGround(true);
			List<String> faults = new ArrayList<>();
			for (BlockPos target : targets) {
				var path = walker.getNavigation().createPath(target, 0);
				if (path == null || !path.canReach()) {
					faults.add(target.toShortString() + (path == null ? " no path" : " ends at " + path.getEndNode().asBlockPos().toShortString()));
				}
			}
			walker.discard();
			helper.assertTrue(faults.isEmpty(), "from the entrance " + start.toShortString() + " a trader should reach the counter and the pen gates: " + faults);
			helper.succeed();
		});
	}

	public void traderArrivesWithLlamasPenned(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int pens : new int[] { 2, 1 }) {
			TraderPostPiece piece = post(helper, seedWith(helper, pens, false, 0));
			WanderingTrader trader = TraderPosts.redirectTo(level, piece.centre().offset(40, 0, 0), piece);
			helper.assertTrue(trader != null, "a trader should arrive at a post 40 blocks from the player (" + pens + " pens)");
			helper.assertTrue(trader.blockPosition().closerThan(piece.traderSpot(), 1.5), "the trader arrives behind the counter, at " + trader.blockPosition());
			helper.assertTrue(trader.getDespawnDelay() == TraderPosts.STAY_TICKS, "vanilla's stay");
			AABB pensBox = piece.penInsides().stream().reduce(AABB::minmax).orElseThrow();
			List<TraderLlama> llamas = level.getEntitiesOfClass(TraderLlama.class, pensBox.inflate(10.0));
			helper.assertTrue(llamas.size() == 2, "two llamas, found " + llamas.size());
			for (TraderLlama llama : llamas) {
				helper.assertTrue(piece.penInsides().stream().anyMatch(b -> b.contains(llama.position())), "llama penned: " + llama.position());
				helper.assertTrue(llama.getLeashHolder() instanceof LeashFenceKnotEntity, "llama tethered to the pen fence");
				helper.assertTrue(TraderPosts.tetheredAtPost(llama), "tethered at the post, so it leaves with the trader");
			}
			if (pens == 2) {
				helper.assertTrue(piece.penInsides().stream().allMatch(b -> llamas.stream().anyMatch(l -> b.contains(l.position()))), "one llama in each pen");
			}
			llamas.forEach(l -> l.discard());
			level.getEntitiesOfClass(LeashFenceKnotEntity.class, pensBox.inflate(4.0)).forEach(k -> k.discard());
			trader.discard();
		}
		helper.succeed();
	}

	@GameTest(structure = FIELD, maxTicks = 40)
	public void tetheredLlamasLeaveWhenTheirStayIsUp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TraderPostPiece piece = post(helper, seedWith(helper, 2, false, 0));
		WanderingTrader trader = TraderPosts.arrive(level, piece);
		helper.assertTrue(trader != null, "a trader arrives");
		AABB around = piece.penInsides().stream().reduce(AABB::minmax).orElseThrow().inflate(4.0);
		List<TraderLlama> llamas = level.getEntitiesOfClass(TraderLlama.class, around);
		llamas.forEach(l -> l.setDespawnDelay(2));
		// a trader llama tied to some other fence by a player stays, as in vanilla
		BlockPos fence = piece.at(0, 1, 9);
		level.setBlock(fence, Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
		TraderLlama kept = EntityTypes.TRADER_LLAMA.spawn(level, fence.offset(1, 0, 0), EntitySpawnReason.COMMAND);
		kept.setDespawnDelay(2);
		kept.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(level, fence), true);
		trader.discard();
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(llamas.stream().allMatch(l -> l.isRemoved()), "the post's llamas leave when their stay is up");
			helper.assertTrue(level.getEntitiesOfClass(LeashFenceKnotEntity.class, around).isEmpty(), "their lead knots go too");
			helper.assertFalse(kept.isRemoved(), "a llama tied up elsewhere stays (vanilla)");
			kept.discard();
			level.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(fence).inflate(2.0)).forEach(k -> k.discard());
			helper.succeed();
		});
	}

	@GameTest(structure = FIELD, maxTicks = 20)
	public void vanillaPlacementWithoutANearbyPost(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TraderPostPiece piece = post(helper, 5L);
		BlockPos far = piece.centre().offset(TraderPosts.DRAW_RANGE + 20, 0, 0);
		helper.assertTrue(TraderPosts.redirect(level, piece.centre()) == null, "a post built by command (no structure start) is not natural; nothing natural near the test field");
		helper.assertTrue(TraderPosts.redirectTo(level, far, piece) == null, "a post more than 96 blocks off leaves the trader to vanilla");
		BlockPos bell = piece.at(0, 1, 9);
		level.setBlock(bell, Blocks.BELL.defaultBlockState(), 3);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(TraderPosts.redirectTo(level, piece.centre().offset(20, 0, 0), piece) == null, "a village bell within 48 blocks wins, as in vanilla");
			level.setBlock(bell, Blocks.AIR.defaultBlockState(), 3);
			helper.assertTrue(level.getEntitiesOfClass(WanderingTrader.class, new AABB(piece.centre()).inflate(30.0)).isEmpty(), "no trader came");
			helper.succeed();
		});
	}
}
