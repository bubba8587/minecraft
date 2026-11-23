// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSites;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pillager outposts become camps too (owner, specs/monster-camps.md): inside an outpost whose banners are all broken,
 * the outpost's spawn override (pillagers) no longer applies; the biome's usual spawns do. Putting a banner back brings
 * the pillagers back (camp/CampSites keeps which outposts have no banner).
 */
@Mixin(ChunkGenerator.class)
abstract class OutpostSpawnMixin {
	@Inject(method = "getMobsAt", at = @At("HEAD"), cancellable = true)
	private void brm$disarmedOutpostsSpawnNoPillagers(Level level, StructureManager structureManager, MobCategory mobCategory, BlockPos pos,
			CallbackInfoReturnable<WeightedList<MobSpawnSettings.SpawnerData>> cir) {
		if (level instanceof ServerLevel serverLevel && CampSites.disarmedOutpostAt(serverLevel, pos)) {
			cir.setReturnValue(level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, pos).getMobsToSpawn(mobCategory));
		}
	}
}
