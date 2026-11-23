// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.lava;

import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Where and when lava geysers erupt. */
public final class LavaGeysers {
	public static final EntityType<LavaGeyser> LAVA_GEYSER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceKey.create(Registries.ENTITY_TYPE, BubbasWorldTweaks.id("lava_geyser")),
			EntityType.Builder.<LavaGeyser>of(LavaGeyser::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(20)
					.noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, BubbasWorldTweaks.id("lava_geyser"))));

	private LavaGeysers() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % WorldTweaksTuning.GEYSER_CHECK_TICKS != 0) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator()) {
					sample(level, player);
				}
			}
		});
	}

	private static void sample(ServerLevel level, ServerPlayer player) {
		checkAround(level, player, level.getGameTime() / WorldTweaksTuning.GEYSER_CHECK_TICKS);
	}

	/** One eruption check around a player, as of this check step. */
	public static void checkAround(ServerLevel level, ServerPlayer player, long step) {
		int r = WorldTweaksTuning.GEYSER_SEARCH_RADIUS;
		int minY = Math.max(level.getMinY(), player.getBlockY() - 16 - WorldTweaksTuning.MAGMA_DEPTH - 1);
		int maxY = Math.min(level.getMaxY(), player.getBlockY() + 8);
		for (int cx = (player.getBlockX() - r) >> 4; cx <= (player.getBlockX() + r) >> 4; cx++) {
			for (int cz = (player.getBlockZ() - r) >> 4; cz <= (player.getBlockZ() + r) >> 4; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk != null) {
					ventsIn(level, chunk, minY, maxY, step);
				}
			}
		}
	}

	/** Magma blocks in this chunk's sections between minY and maxY: each erupts on its own rhythm. */
	private static void ventsIn(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk, int minY, int maxY, long step) {
		net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int i = level.getSectionIndex(minY); i <= level.getSectionIndex(maxY) && i < sections.length; i++) {
			net.minecraft.world.level.chunk.LevelChunkSection section = sections[i];
			if (section.hasOnlyAir() || !section.maybeHas(st -> st.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK))) {
				continue;
			}
			int baseY = level.getSectionYFromSectionIndex(i) << 4;
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						if (section.getBlockState(x, y, z).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) {
							p.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
							ventAt(level, p, step);
						}
					}
				}
			}
		}
	}

	private static void ventAt(ServerLevel level, BlockPos magma, long step) {
		BlockPos.MutableBlockPos q = magma.mutable();
		for (int depth = 1; depth <= WorldTweaksTuning.MAGMA_DEPTH + 1; depth++) {
			q.move(0, 1, 0);
			if (!level.getFluidState(q).is(FluidTags.LAVA)) {
				return;
			}
			if (level.getBlockState(q.above()).isAir()) {
				if (eruptsAt(level.getSeed(), magma, depth, step)) {
					erupt(level, q.immutable());
				}
				return;
			}
		}
	}

	/** Whether the vent on this magma block starts erupting on this check step: a fixed rhythm from the world seed and its position. */
	public static boolean eruptsAt(long worldSeed, BlockPos magma, int depth, long step) {
		RandomSource vent = RandomSource.create(worldSeed ^ net.minecraft.util.Mth.getSeed(magma) ^ WorldTweaksTuning.VENT_SALT);
		int quietSeconds = WorldTweaksTuning.VENT_QUIET_BASE + vent.nextInt(WorldTweaksTuning.VENT_QUIET_SPREAD + 1)
				+ WorldTweaksTuning.VENT_QUIET_PER_DEPTH * (depth - 1);
		int period = Math.max(1, (quietSeconds * 20 + WorldTweaksTuning.GEYSER_DURATION_TICKS) / WorldTweaksTuning.GEYSER_CHECK_TICKS);
		int phase = vent.nextInt(period);
		return Math.floorMod(step + phase, period) == 0;
	}

	/** The top lava block (air above it) in a column between top and bottom, or null. */
	public static @Nullable BlockPos lavaSurface(ServerLevel level, int x, int top, int bottom, int z) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, top, z);
		boolean airAbove = level.getBlockState(p).isAir();
		for (int y = top - 1; y >= bottom; y--) {
			p.setY(y);
			if (level.getFluidState(p).is(FluidTags.LAVA) && airAbove) {
				return p.immutable();
			}
			airAbove = level.getBlockState(p).isAir();
		}
		return null;
	}

	/** Starts an eruption at a bubbling spot (unless one is already going close by). */
	public static @Nullable LavaGeyser erupt(ServerLevel level, BlockPos surface) {
		double s = WorldTweaksTuning.GEYSER_SPACING;
		if (!level.getEntitiesOfClass(LavaGeyser.class, new AABB(surface).inflate(s)).isEmpty()) {
			return null;
		}
		LavaGeyser geyser = LAVA_GEYSER.create(level, EntitySpawnReason.TRIGGERED);
		if (geyser == null) {
			return null;
		}
		geyser.setPos(surface.getX() + 0.5, surface.getY() + 0.9, surface.getZ() + 0.5);
		level.addFreshEntity(geyser);
		return geyser;
	}
}
