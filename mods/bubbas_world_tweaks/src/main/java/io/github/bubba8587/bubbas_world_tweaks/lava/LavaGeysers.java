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
		RandomSource random = level.getRandom();
		int r = WorldTweaksTuning.GEYSER_SEARCH_RADIUS;
		for (int i = 0; i < WorldTweaksTuning.GEYSER_COLUMNS_PER_CHECK; i++) {
			int x = player.getBlockX() + random.nextInt(2 * r + 1) - r;
			int z = player.getBlockZ() + random.nextInt(2 * r + 1) - r;
			BlockPos surface = lavaSurface(level, x, player.getBlockY() + 8, player.getBlockY() - 16, z);
			if (surface != null && LavaGeyser.magmaUnder(level, surface) && random.nextFloat() < WorldTweaksTuning.GEYSER_CHANCE) {
				erupt(level, surface);
			}
		}
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
