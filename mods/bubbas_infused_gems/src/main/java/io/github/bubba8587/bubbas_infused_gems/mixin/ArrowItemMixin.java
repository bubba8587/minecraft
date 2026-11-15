// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.effect.GemArrow;
import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bows and crossbows with gems tip each arrow they fire with the gems' hit; the pickup item stays as it was, so a plain
 * arrow comes back plain (owner). Hooked where every bow and crossbow makes its arrow (players and skeletons alike).
 */
@Mixin(ArrowItem.class)
public abstract class ArrowItemMixin {
	@Inject(method = "createArrow", at = @At("RETURN"))
	private void bubbas$tipArrow(Level level, ItemStack ammo, LivingEntity owner, @Nullable ItemStack weapon,
			CallbackInfoReturnable<AbstractArrow> cir) {
		if (weapon != null && !level.isClientSide() && cir.getReturnValue() instanceof Arrow arrow) {
			List<GemHits.Entry> plan = GemHits.plan(weapon);
			if (!plan.isEmpty()) {
				((GemArrow) arrow).bubbas$setGemHit(plan);
			}
		}
	}
}
