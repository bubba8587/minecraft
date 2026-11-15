// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.loot.GemLoot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;

/** Gems on mobs' spawn armor (owner): after vanilla picks the armor, each piece may get gems (data: item_modifier/mob_armor). */
@Mixin(Mob.class)
abstract class MobMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void bubbas_infused_gems$gemSpawnArmor(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
		GemLoot.gemSpawnArmor((Mob) (Object) this);
	}
}
