// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

/** Will-o-wisps: soul fire hints toward crypt yards a player hasn't found yet. */
public final class Wisps {
	/** Yards each player has walked into (start chunk ids). */
	public static final AttachmentType<List<Long>> FOUND = AttachmentRegistry.create(BubbasReworkedMobs.id("found_yards"),
			builder -> builder.persistent(Codec.LONG.listOf()).copyOnDeath());

	/** A yard near players: its box and where wisps lead. */
	public record Yard(long id, BoundingBox box, BlockPos target) {
	}

	private static final Map<ServerLevel, Map<Long, Yard>> YARDS = new WeakHashMap<>();

	private Wisps() {
	}

	public static void initialize() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(Wisps::tick);
	}

	static void tick(ServerLevel level) {
		if (level.getGameTime() % GraveTuning.WISP_TICKS != 0) {
			return;
		}
		RandomSource random = level.getRandom();
		for (ServerPlayer player : level.getPlayers(EntitySelector.NO_SPECTATORS::test)) {
			discover(level, player.chunkPosition());
			for (Yard yard : yards(level).values()) {
				double d = Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(yard.box.getCenter())));
				if (d > GraveTuning.WISP_RANGE || hasFound(player, yard.id)) {
					continue;
				}
				if (inside(player, yard.box)) {
					found(player, yard.id);
					continue;
				}
				if (random.nextFloat() < GraveTuning.WISP_HINT_CHANCE) {
					hint(level, player, yard, random);
				}
				if (level.isDarkOutside() && d < GraveTuning.WISP_NIGHT_RANGE && random.nextFloat() < GraveTuning.WISP_NIGHT_CHANCE) {
					overGraves(level, player, yard, random);
				}
			}
		}
	}

	/** One faint wisp between the player and the yard. */
	public static void hint(ServerLevel level, ServerPlayer player, Yard yard, RandomSource random) {
		Vec3 eye = player.getEyePosition();
		Vec3 toward = Vec3.atCenterOf(yard.box.getCenter()).subtract(eye).multiply(1, 0, 1).normalize();
		double along = GraveTuning.WISP_HINT_MIN + random.nextDouble() * (GraveTuning.WISP_HINT_MAX - GraveTuning.WISP_HINT_MIN);
		Vec3 at = eye.add(toward.scale(along)).add(random.nextGaussian() * 0.6, random.nextGaussian() * 0.4, random.nextGaussian() * 0.6);
		level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, false, false, at.x, at.y, at.z, 0, toward.x, 0.02, toward.z, 0.015);
	}

	/** A wisp over the graves, drifting toward the crypt entrance. */
	public static void overGraves(ServerLevel level, ServerPlayer player, Yard yard, RandomSource random) {
		BoundingBox box = yard.box;
		int x = box.minX() + random.nextInt(box.getXSpan());
		int z = box.minZ() + random.nextInt(box.getZSpan());
		if (!level.isLoaded(new BlockPos(x, 0, z))) {
			return;
		}
		double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 0.6 + random.nextDouble();
		Vec3 at = new Vec3(x + 0.5, y, z + 0.5);
		Vec3 drift = Vec3.atCenterOf(yard.target).subtract(at).normalize();
		level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, false, false, at.x, at.y, at.z, 0, drift.x, drift.y * 0.2, drift.z, 0.02);
	}

	private static boolean inside(ServerPlayer player, BoundingBox box) {
		BlockPos p = player.blockPosition();
		return p.getX() >= box.minX() && p.getX() <= box.maxX() && p.getZ() >= box.minZ() && p.getZ() <= box.maxZ();
	}

	public static boolean hasFound(ServerPlayer player, long id) {
		List<Long> found = player.getAttached(FOUND);
		return found != null && found.contains(id);
	}

	public static void found(ServerPlayer player, long id) {
		List<Long> found = player.getAttachedOrElse(FOUND, List.of());
		if (!found.contains(id)) {
			java.util.ArrayList<Long> next = new java.util.ArrayList<>(found);
			next.add(id);
			player.setAttached(FOUND, List.copyOf(next));
		}
	}

	/** Yards known in this level. */
	public static Map<Long, Yard> yards(ServerLevel level) {
		return YARDS.computeIfAbsent(level, l -> new HashMap<>());
	}

	/** Adds a yard by hand (the build command, tests); returns its id. */
	public static long register(ServerLevel level, CryptYardPiece piece) {
		long id = ChunkPos.containing(piece.gate()).pack();
		yards(level).put(id, new Yard(id, piece.getBoundingBox(), piece.wispTarget()));
		return id;
	}

	/** Registers yards referenced by the loaded chunks round {@code around}. */
	private static void discover(ServerLevel level, ChunkPos around) {
		Structure yard = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Graveyards.CRYPT_YARD_KEY);
		if (yard == null) {
			return;
		}
		Map<Long, Yard> known = yards(level);
		int r = GraveTuning.WISP_DISCOVER_CHUNKS;
		for (int cx = around.x() - r; cx <= around.x() + r; cx++) {
			for (int cz = around.z() - r; cz <= around.z() + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (long ref : chunk.getReferencesForStructure(yard)) {
					if (known.containsKey(ref)) {
						continue;
					}
					ChunkAccess start = level.getChunk(ChunkPos.getX(ref), ChunkPos.getZ(ref), ChunkStatus.STRUCTURE_STARTS, false);
					StructureStart s = start == null ? null : start.getStartForStructure(yard);
					if (s != null && !s.getPieces().isEmpty() && s.getPieces().getFirst() instanceof CryptYardPiece piece) {
						known.put(ref, new Yard(ref, piece.getBoundingBox(), piece.wispTarget()));
					}
				}
			}
		}
	}
}
