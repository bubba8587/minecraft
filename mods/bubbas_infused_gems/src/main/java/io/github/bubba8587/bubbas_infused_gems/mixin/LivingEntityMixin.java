// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import java.util.Collection;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect;
import io.github.bubba8587.bubbas_infused_gems.effect.GemValues;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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

/**
 * Armor gems on the wearer: the cached totals, the tick and damage hooks, gravity, water drag, stealth, effect changes
 * (a gem and a vanilla effect of the same kind don't add: the stronger applies), and broken gear popping its gems.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements GemEntity {
	@Shadow
	public abstract @Nullable MobEffectInstance getEffect(Holder<MobEffect> effect);

	@Shadow
	public abstract boolean removeEffect(Holder<MobEffect> effect);

	@Unique
	private GemTotals bubbas$worn = GemTotals.NONE;
	@Unique
	private int bubbas$lastHurtTick = -100000;
	@Unique
	private int bubbas$lastEquipFillTick = -100000;
	@Unique
	private float bubbas$foodCarry;

	@Override
	public float bubbas$foodCarry() {
		return this.bubbas$foodCarry;
	}

	@Override
	public void bubbas$setFoodCarry(float carry) {
		this.bubbas$foodCarry = carry;
	}

	@Override
	public int bubbas$lastEquipFillTick() {
		return this.bubbas$lastEquipFillTick;
	}

	@Override
	public void bubbas$setLastEquipFillTick(int tick) {
		this.bubbas$lastEquipFillTick = tick;
	}

	@Override
	public GemTotals bubbas$wornGems() {
		return this.bubbas$worn;
	}

	@Override
	public void bubbas$setWornGems(GemTotals totals) {
		this.bubbas$worn = totals;
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
		return WornEffects.gravity((LivingEntity) (Object) this, gravity);
	}

	@WrapOperation(method = "travelInWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 bubbas$gemWaterDrag(Vec3 movement, double x, double y, double z, Operation<Vec3> original) {
		LivingEntity self = (LivingEntity) (Object) this;
		return original.call(movement, WornEffects.waterDrag(self, x), y, WornEffects.waterDrag(self, z));
	}

	/**
	 * Darkness and Nausea gems worn (owner): the wearer's own screen gets s of the effect's strongest look, steady, with
	 * no vanilla instance (so nothing pulses or restarts a fade). Vanilla reads both through this blend factor.
	 */
	@ModifyReturnValue(method = "getEffectBlendFactor", at = @At("RETURN"))
	private float bubbas$gemScreenEffect(float blend, Holder<MobEffect> effect, float partialTicks) {
		if (!this.bubbas$worn.isEmpty() && ((LivingEntity) (Object) this).level().isClientSide()
				&& (effect == net.minecraft.world.effect.MobEffects.DARKNESS || effect == net.minecraft.world.effect.MobEffects.NAUSEA)) {
			return Math.max(blend, (float) GemValues.rawWorn((LivingEntity) (Object) this, io.github.bubba8587.bubbas_infused_gems.gem.GemText.key(effect)));
		}
		return blend;
	}

	@ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
	private double bubbas$gemStealth(double percent) {
		return WornEffects.visibility((LivingEntity) (Object) this, percent);
	}

	/** A gem effect and a vanilla effect of the same kind don't add (owner's rule, open question 31): the stronger applies. */
	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
	private void bubbas$strongerApplies(MobEffectInstance incoming, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
		if (!Balance.get().strongerApplies) {
			return;
		}
		Holder<MobEffect> effect = incoming.getEffect();
		Holder<MobEffect> other;
		double in;
		double have;
		if (effect.value() instanceof GemMobEffect gem) {
			other = ModEffects.vanilla(gem.key());
			MobEffectInstance inst = other == null ? null : this.getEffect(other);
			if (inst == null) {
				return;
			}
			in = GemMobEffect.potency(incoming.getAmplifier());
			have = GemValues.levelFraction(gem.key(), inst.getAmplifier());
		} else {
			String key = io.github.bubba8587.bubbas_infused_gems.gem.GemText.key(effect);
			other = ModEffects.gem(key);
			MobEffectInstance inst = other == null || ModEffects.vanilla(key) != effect ? null : this.getEffect(other);
			if (inst == null) {
				return;
			}
			in = GemValues.levelFraction(key, incoming.getAmplifier());
			have = GemMobEffect.potency(inst.getAmplifier());
		}
		if (have >= in) {
			cir.setReturnValue(false);
		} else {
			this.removeEffect(other);
		}
	}

	@Inject(method = "onEffectAdded", at = @At("TAIL"))
	private void bubbas$effectAdded(MobEffectInstance effect, @Nullable Entity source, CallbackInfo ci) {
		this.bubbas$effectsChanged();
	}

	@Inject(method = "onEffectUpdated", at = @At("TAIL"))
	private void bubbas$effectUpdated(MobEffectInstance effect, boolean doRefreshAttributes, @Nullable Entity source, CallbackInfo ci) {
		this.bubbas$effectsChanged();
	}

	@Inject(method = "onEffectsRemoved", at = @At("TAIL"))
	private void bubbas$effectsRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
		this.bubbas$effectsChanged();
	}

	/** Worn attribute gems only top up above a vanilla effect of the same kind, so they follow the wearer's effects. */
	@Unique
	private void bubbas$effectsChanged() {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!this.bubbas$worn.isEmpty() && !self.level().isClientSide()) {
			WornEffects.updateAttributes(self);
		}
	}

	@Inject(method = "onEquippedItemBroken", at = @At("TAIL"))
	private void bubbas$popGems(ItemStack brokenItem, EquipmentSlot inSlot, CallbackInfo ci) {
		if (Sockets.hasGems(brokenItem)) {
			Sockets.popOut((LivingEntity) (Object) this, brokenItem);
		}
	}
}
