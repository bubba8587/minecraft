// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanAccess;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Which reworked mobs stay vanilla (owner, 2026-11-16): endermen from vanilla's random natural spawning
 * (specs/endermen.md; cairn spawns are marked by EndermanSpawning), and creepers and endermen from spawners and trial
 * spawners ("spawners should spawn vanilla mobs, for now"). Spawn eggs and commands give reworked ones. Drowned do the
 * same in their own finalizeSpawn hook (DrownedMixin), which must run before vanilla picks their gear.
 */
@Mixin(Mob.class)
abstract class MobFinalizeSpawnMixin {
	@Inject(method = "finalizeSpawn", at = @At("HEAD"))
	private void brm$naturalEndermenStayVanilla(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
		boolean spawner = EntitySpawnReason.isSpawner(reason);
		// Bogged from spawners (trial chambers) stay vanilla, like drowned: no lurking (bogged/BoggedLurk).
		if (spawner && (Object) this instanceof net.minecraft.world.entity.monster.skeleton.Bogged bogged) {
			bogged.addTag(io.github.bubba8587.bubbas_reworked_mobs.bogged.BoggedTuning.VANILLA_TAG);
		}
		if (this instanceof EndermanAccess access && (spawner || reason == EntitySpawnReason.NATURAL && !access.brm$isCairnSpawn())) {
			access.brm$makeVanilla();
		}
		if (spawner && this instanceof CreeperAccess creeper) {
			creeper.brm$makeVanilla();
		}
	}
}
