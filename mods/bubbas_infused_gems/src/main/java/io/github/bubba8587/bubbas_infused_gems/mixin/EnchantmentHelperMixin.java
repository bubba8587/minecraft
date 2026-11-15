// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Weapon gems on melee hits: vanilla runs this after every successful melee hit, by players and mobs alike. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@Inject(method = "doPostAttackEffects", at = @At("HEAD"))
	private static void bubbas$weaponGems(ServerLevel level, Entity victim, DamageSource source, CallbackInfo ci) {
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && victim instanceof LivingEntity target) {
			GemHits.onMeleeHit(level, attacker, target, attacker.getWeaponItem());
		}
	}
}
