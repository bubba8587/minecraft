// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

/**
 * Wiring for the caretaker endermen (specs/endermen.md): the cairn spawner ticks, the footing worldgen feature, and
 * E4's hostility (breaking a cairn block, or hitting an enderman near a site, angers the endermen around).
 */
public final class Endermen {
	public static final ResourceKey<PlacedFeature> CAIRN_FOOTING = ResourceKey.create(Registries.PLACED_FEATURE, BubbasReworkedMobs.id("cairn_footing"));

	private Endermen() {
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, BubbasReworkedMobs.id("cairn_footing"), CairnFootingFeature.CODEC);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.SURFACE_STRUCTURES, CAIRN_FOOTING);
		ServerTickEvents.END_LEVEL_TICK.register(EndermanSpawning::tick);
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server && isStandingCairnBlock(server, pos, state)) {
				anger(server, pos, player);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseTaken, taken, blocked) -> {
			if (entity instanceof Enderman enderman && source.getEntity() instanceof Player player && EndermanMind.of(enderman) != null
					&& enderman.level() instanceof ServerLevel server
					&& CairnSites.nearest(server, enderman.blockPosition(), EndermanTuning.SITE_RADIUS) != null) {
				anger(server, enderman.blockPosition(), player);
			}
		});
	}

	/** Whether {@code state}, just broken at {@code pos}, was a block of a cairn standing as designed. */
	static boolean isStandingCairnBlock(ServerLevel level, BlockPos pos, BlockState state) {
		CairnSite site = CairnSites.covering(level, pos);
		BlockState want = site == null ? null : site.at(pos);
		return want != null && TerrainKinds.matches(state, want);
	}

	/** Every caretaker enderman within ANGER_RADIUS of {@code pos} turns on {@code player} (vanilla's anger timer). */
	public static int anger(ServerLevel level, BlockPos pos, Player player) {
		if (player.isCreative() || player.isSpectator()) {
			return 0;
		}
		int count = 0;
		for (Enderman enderman : level.getEntitiesOfClass(Enderman.class, new AABB(pos).inflate(EndermanTuning.ANGER_RADIUS),
				e -> e.isAlive() && EndermanMind.of(e) != null)) {
			enderman.setPersistentAngerTarget(EntityReference.of(player));
			enderman.startPersistentAngerTimer();
			enderman.setTarget(player);
			count++;
		}
		return count;
	}
}
