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
		Object registerSavedSites = CairnSites.SAVED_SITES; // registers the attachment before any level loads
		Registry.register(BuiltInRegistries.FEATURE_TYPE, BubbasReworkedMobs.id("cairn_footing"), CairnFootingFeature.CODEC);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.SURFACE_STRUCTURES, CAIRN_FOOTING);
		ServerTickEvents.END_LEVEL_TICK.register(EndermanSpawning::tick);
		ServerTickEvents.END_LEVEL_TICK.register(CairnGlow::tick);
		ServerTickEvents.END_LEVEL_TICK.register(EndermanHoles::tick);
		Object registerGifts = CairnGifts.GIFTS; // registers the attachment before any level loads
		ServerTickEvents.END_LEVEL_TICK.register(CairnGifts::tick);
		ServerTickEvents.END_LEVEL_TICK.register(EndermanNexus::tick);
		ServerTickEvents.END_LEVEL_TICK.register(Ripples::tick);
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server) {
				if (isStandingCairnBlock(server, pos, state)) {
					anger(server, pos, player);
				}
				EndermanHoles.broken(server, pos);
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

	/**
	 * Players endermen count as their own (set by another mod through api/Endermen: Bubba's Totems & Artifacts, a held
	 * Totem of the End): breaking a cairn or hitting an enderman near one doesn't anger the others at them.
	 */
	private static java.util.function.Predicate<Player> kin = player -> false;

	public static void setKin(java.util.function.Predicate<Player> predicate) {
		kin = predicate == null ? player -> false : predicate;
	}

	public static boolean isKin(Player player) {
		return kin.test(player);
	}

	/** Every caretaker enderman within ANGER_RADIUS of {@code pos} turns on {@code player} (vanilla's anger timer). */
	public static int anger(ServerLevel level, BlockPos pos, Player player) {
		if (player.isCreative() || player.isSpectator() || isKin(player)) {
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
