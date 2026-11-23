// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.traderpost;

import java.util.List;
import java.util.Optional;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Trader posts (specs/monster-camps.md "Trader posts"): registration, finding the nearest post, and a wandering
 * trader's arrival there with its llamas tethered in the pens.
 */
public final class TraderPosts {
	public static final StructureType<TraderPostStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, BubbasReworkedMobs.id("trader_post"),
			() -> TraderPostStructure.CODEC);
	public static final StructurePieceType PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, BubbasReworkedMobs.id("trader_post"),
			(StructurePieceType.ContextlessType) TraderPostPiece::new);
	public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, BubbasReworkedMobs.id("trader_post"));
	public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, BubbasReworkedMobs.id("trader_posts"));
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/trader_post"));
	public static final ResourceKey<LootTable> CART_LOOT = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/trader_post_cart"));
	public static final String SIGN_LINE_1 = "sign." + BubbasReworkedMobs.MOD_ID + ".trader_post.line1";
	public static final String SIGN_LINE_2 = "sign." + BubbasReworkedMobs.MOD_ID + ".trader_post.line2";
	/** Blocks from the player within which a post draws the trader. */
	public static final int DRAW_RANGE = 96;
	/** Vanilla's reach for a village meeting point (WanderingTraderSpawner); a village bell that near wins. */
	public static final int MEETING_RANGE = 48;
	/** How far the trader keeps to the post. */
	public static final int HOME_RADIUS = 12;
	/** Vanilla's trader stay (WanderingTraderSpawner). */
	public static final int STAY_TICKS = 48000;
	/** Tag on llamas tethered at a post; with {@link #TETHER_PREFIX}+the fence's position they despawn with the trader. */
	public static final String TETHER_TAG = BubbasReworkedMobs.MOD_ID + ".trader_post";
	public static final String TETHER_PREFIX = BubbasReworkedMobs.MOD_ID + ".tether:";

	private TraderPosts() {
	}

	public static void initialize() {
	}

	/** The nearest post whose centre is within {@code range} blocks (across) of {@code at}; null when none. */
	public static @Nullable TraderPostPiece nearest(ServerLevel level, BlockPos at, int range) {
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(KEY);
		var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(SET);
		if (structure == null || set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
			return null;
		}
		int cell = placement.spacing() * 16;
		int rings = range / cell + 1;
		int gx0 = Math.floorDiv(at.getX() >> 4, placement.spacing());
		int gz0 = Math.floorDiv(at.getZ() >> 4, placement.spacing());
		TraderPostPiece best = null;
		double bestD = (double) range * range;
		for (int gx = gx0 - rings; gx <= gx0 + rings; gx++) {
			for (int gz = gz0 - rings; gz <= gz0 + rings; gz++) {
				ChunkPos chunk = placement.getPotentialStructureChunk(level.getSeed(), gx * placement.spacing(), gz * placement.spacing());
				double cdx = chunk.getMiddleBlockX() - at.getX();
				double cdz = chunk.getMiddleBlockZ() - at.getZ();
				if (cdx * cdx + cdz * cdz > bestD) {
					continue;
				}
				ChunkAccess access = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS, true);
				StructureStart start = access == null ? null : access.getStartForStructure(structure);
				if (start != null && start.isValid() && start.getPieces().getFirst() instanceof TraderPostPiece piece) {
					double dx = piece.centre().getX() - at.getX();
					double dz = piece.centre().getZ() - at.getZ();
					if (dx * dx + dz * dz <= bestD) {
						bestD = dx * dx + dz * dz;
						best = piece;
					}
				}
			}
		}
		return best;
	}

	/**
	 * WanderingTraderSpawner's choice of place, after its own odds have said "spawn now": when no village meeting
	 * point is within vanilla's 48 blocks but a post is within {@link #DRAW_RANGE}, the trader arrives at the post.
	 *
	 * @return the trader, or null to let vanilla place it as usual
	 */
	public static @Nullable WanderingTrader redirect(ServerLevel level, BlockPos playerPos) {
		if (meetingNear(level, playerPos)) {
			return null;
		}
		return inRange(level, playerPos, nearest(level, playerPos, DRAW_RANGE));
	}

	/** {@link #redirect} with the post already found (tests). */
	public static @Nullable WanderingTrader redirectTo(ServerLevel level, BlockPos playerPos, @Nullable TraderPostPiece post) {
		return meetingNear(level, playerPos) ? null : inRange(level, playerPos, post);
	}

	private static @Nullable WanderingTrader inRange(ServerLevel level, BlockPos playerPos, @Nullable TraderPostPiece post) {
		if (post == null) {
			return null;
		}
		double dx = post.centre().getX() - playerPos.getX();
		double dz = post.centre().getZ() - playerPos.getZ();
		return dx * dx + dz * dz > (double) DRAW_RANGE * DRAW_RANGE ? null : arrive(level, post);
	}

	private static boolean meetingNear(ServerLevel level, BlockPos playerPos) {
		Optional<BlockPos> meeting = level.getPoiManager().find(p -> p.is(PoiTypes.MEETING), p -> true, playerPos, MEETING_RANGE, PoiManager.Occupancy.ANY);
		return meeting.isPresent();
	}

	/** A trader arrives at the post, its two llamas tethered in the pens; null when the post isn't loaded or the spot is blocked. */
	public static @Nullable WanderingTrader arrive(ServerLevel level, TraderPostPiece post) {
		BlockPos spot = post.traderSpot();
		List<BlockPos> llamaSpots = post.llamaSpots();
		if (!level.isLoaded(spot) || llamaSpots.stream().anyMatch(p -> !level.isLoaded(p)) || !standable(level, spot)
				|| level.getBiome(spot).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
			return null;
		}
		WanderingTrader trader = EntityTypes.WANDERING_TRADER.spawn(level, spot, EntitySpawnReason.EVENT);
		if (trader == null) {
			return null;
		}
		trader.setDespawnDelay(STAY_TICKS);
		trader.setWanderTarget(post.centre());
		trader.setHomeTo(post.centre(), HOME_RADIUS);
		List<BlockPos> posts = post.tetherPosts();
		for (int i = 0; i < llamaSpots.size(); i++) {
			BlockPos at = llamaSpots.get(i);
			if (!standable(level, at)) {
				continue;
			}
			TraderLlama llama = EntityTypes.TRADER_LLAMA.spawn(level, at, EntitySpawnReason.EVENT);
			if (llama == null) {
				continue;
			}
			BlockPos fence = posts.get(i);
			if (level.getBlockState(fence).is(BlockTags.FENCES)) {
				llama.setDespawnDelay(STAY_TICKS - 1);
				llama.addTag(TETHER_TAG);
				llama.addTag(TETHER_PREFIX + fence.asLong());
				llama.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(level, fence), true);
			} else {
				llama.setLeashedTo(trader, true);
			}
		}
		return trader;
	}

	/** Whether a trader or llama can stand here: a sturdy floor and two clear blocks. */
	static boolean standable(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
				&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
	}

	/** Whether this trader llama is still tethered to its post's fence (then it leaves when its stay is up, as if led by the trader). */
	public static boolean tetheredAtPost(TraderLlama llama) {
		if (!llama.entityTags().contains(TETHER_TAG) || !(llama.getLeashHolder() instanceof LeashFenceKnotEntity knot)) {
			return false;
		}
		return llama.entityTags().contains(TETHER_PREFIX + knot.getPos().asLong());
	}
}
