// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.effect.GemEntity;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.station.Sockets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Armor gems on the wearer: the cached totals, the tick and damage hooks, gravity, and broken gear popping its gems. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements GemEntity {
	@Unique
	private GemTotals bubbas$worn = GemTotals.NONE;
	@Unique
	private long bubbas$strikeReadyAt;
	@Unique
	private int bubbas$lastHurtTick = -100000;

	@Override
	public GemTotals bubbas$wornGems() {
		return this.bubbas$worn;
	}

	@Override
	public void bubbas$setWornGems(GemTotals totals) {
		this.bubbas$worn = totals;
	}

	@Override
	public long bubbas$strikeReadyAt() {
		return this.bubbas$strikeReadyAt;
	}

	@Override
	public void bubbas$setStrikeReadyAt(long time) {
		this.bubbas$strikeReadyAt = time;
	}

	@Override
	public int bubbas$lastHurtTick() {
		return this.bubbas$lastHurtTick;
	}

	@Override
	public void bubbas$setLastHurtTick(int tick) {
		this.bubbas$lastHurtTick = tick;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void bubbas$tickGems(CallbackInfo ci) {
		if (!this.bubbas$worn.isEmpty() && ((LivingEntity) (Object) this).level() instanceof ServerLevel level) {
			WornEffects.tick(level, (LivingEntity) (Object) this, this.bubbas$worn);
		}
	}

	@ModifyReturnValue(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"))
	private float bubbas$gemDamage(float damage, DamageSource source) {
		return WornEffects.modifyDamage((LivingEntity) (Object) this, source, damage);
	}

	@ModifyReturnValue(method = "getEffectiveGravity", at = @At("RETURN"))
	private double bubbas$gemGravity(double gravity) {
		return this.bubbas$worn.isEmpty() ? gravity : WornEffects.gravity((LivingEntity) (Object) this, gravity);
	}

	@Inject(method = "onEquippedItemBroken", at = @At("TAIL"))
	private void bubbas$popGems(ItemStack brokenItem, EquipmentSlot inSlot, CallbackInfo ci) {
		if (Sockets.hasGems(brokenItem)) {
			Sockets.popOut((LivingEntity) (Object) this, brokenItem);
		}
	}
}
