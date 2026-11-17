// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Builds Ancient Fabricators as their chunks generate (specs/mob-factory.md): every overworld chunk asks the
 * ancient-city cells round it for their Fabricator (a cheap seed hash and one biome lookup per cell, cached), and
 * when a site's rough reach (the cavern, and the tunnel to its city) touches the chunk, places the part of the build
 * inside the chunk. Last decoration step, so the city (an underground-decoration structure) and the deep dark's own
 * sculk stand already: the tunnel cuts its way into the city.
 */
public record FabricatorFeature() implements Feature {
	public static final MapCodec<FabricatorFeature> CODEC = MapCodec.unit(FabricatorFeature::new);
	/** The cavern's reach round its origin, horizontally (the build is about 70 x 90). */
	private static final int CAVERN_REACH = 60;

	@Override
	public MapCodec<FabricatorFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		ServerLevel server = level.getLevel();
		if (!FabricatorSites.naturalSites(server)) {
			return false;
		}
		int minX = origin.getX() & ~15;
		int minZ = origin.getZ() & ~15;
		boolean placed = false;
		for (FabricatorSite site : FabricatorSites.near(server, new BlockPos(minX + 8, 0, minZ + 8), FabricatorSites.CITY_DISTANCE + CAVERN_REACH)) {
			if (!roughlyTouches(site, minX, minZ)) {
				continue;
			}
			BoundingBox chunk = new BoundingBox(minX, level.getMinY(), minZ, minX + 15, level.getMaxY(), minZ + 15);
			if (!site.box().intersects(chunk)) {
				continue;
			}
			site.design().placeIn(level, chunkGenerator, site.origin(), site.rotation(), chunk, RandomSource.create(site.seed() ^ chunk.minX() * 31L ^ chunk.minZ()));
			placed = true;
		}
		return placed;
	}

	/** Cheap test before the design is worked out: near the cavern, or near the line to its city. */
	private static boolean roughlyTouches(FabricatorSite site, int minX, int minZ) {
		double px = minX + 8;
		double pz = minZ + 8;
		double ox = site.origin().getX();
		double oz = site.origin().getZ();
		if (Math.hypot(px - ox, pz - oz) <= CAVERN_REACH + 12) {
			return true;
		}
		if (site.cityLocal() == null) {
			return false;
		}
		BlockPos city = site.world(site.cityLocal());
		double vx = city.getX() - ox;
		double vz = city.getZ() - oz;
		double len2 = vx * vx + vz * vz;
		double t = Math.max(0.0, Math.min(1.0, ((px - ox) * vx + (pz - oz) * vz) / len2));
		return Math.hypot(px - (ox + vx * t), pz - (oz + vz * t)) <= 20;
	}
}
