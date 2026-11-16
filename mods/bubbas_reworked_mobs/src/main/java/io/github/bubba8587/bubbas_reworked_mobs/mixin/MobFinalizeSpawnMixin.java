// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

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
 * Endermen from vanilla's random natural spawning keep vanilla behavior (owner, 2026-11-16; specs/endermen.md): only
 * cairn spawns (marked by EndermanSpawning), spawn eggs, spawners and commands become caretakers.
 */
@Mixin(Mob.class)
abstract class MobFinalizeSpawnMixin {
	@Inject(method = "finalizeSpawn", at = @At("HEAD"))
	private void brm$naturalEndermenStayVanilla(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
		if (reason == EntitySpawnReason.NATURAL && this instanceof EndermanAccess access && !access.brm$isCairnSpawn()) {
			access.brm$makeVanilla();
		}
	}
}
