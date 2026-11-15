// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.loot.GemLoot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.monster.piglin.Piglin;

/** Piglins pick their golden armor without Mob's code, so their pieces get gems here (same item modifier). */
@Mixin(Piglin.class)
abstract class PiglinMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void bubbas_infused_gems$gemSpawnArmor(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
		GemLoot.gemSpawnArmor((Piglin) (Object) this);
	}
}
