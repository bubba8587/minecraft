// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import java.util.function.Consumer;

import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Weapon gems on hits. Every vanilla post-attack path ends here with the weapon that hit: a player's melee and stab
 * attacks (which call the item-source variant directly, so hooking only {@code doPostAttackEffects} missed them), mobs'
 * melee, sweeps, and thrown tridents (the trident is the item). Arrows pass their bow, which is skipped: the arrow
 * carries the gems itself.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@Inject(method = "doPostAttackEffectsWithItemSourceOnBreak", at = @At("HEAD"))
	private static void bubbas$weaponGems(ServerLevel level, Entity victim, DamageSource source, @Nullable ItemStack weapon,
			@Nullable Consumer<ItemStack> onBreak, CallbackInfo ci) {
		if (weapon == null || !(victim instanceof LivingEntity target)) {
			return;
		}
		Entity direct = source.getDirectEntity();
		LivingEntity attacker = source.getEntity() instanceof LivingEntity l ? l : null;
		if ((attacker != null && direct == attacker) || direct instanceof ThrownTrident) {
			GemHits.onWeaponHit(level, attacker, direct, target, weapon);
		}
	}
}
