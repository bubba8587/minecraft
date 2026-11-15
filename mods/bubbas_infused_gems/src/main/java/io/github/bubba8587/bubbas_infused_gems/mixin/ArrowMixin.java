// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_infused_gems.effect.GemArrow;
import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A gem arrow carries its bow's gem hit beside its pickup item, so it flies, trails and hits like a tipped arrow but is
 * a plain arrow again when picked up (owner). A real tipped arrow's effect and the gem's of the same effect don't add:
 * the stronger applies.
 */
@Mixin(Arrow.class)
public abstract class ArrowMixin extends AbstractArrow implements GemArrow {
	@Shadow
	@Final
	private static EntityDataAccessor<Integer> ID_EFFECT_COLOR;
	@Unique
	private List<GemHits.Entry> bubbas$gemHit = List.of();
	@Unique
	private boolean bubbas$charged;

	protected ArrowMixin(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
	}

	@Override
	public List<GemHits.Entry> bubbas$gemHit() {
		return this.bubbas$gemHit;
	}

	@Override
	public void bubbas$setGemHit(List<GemHits.Entry> entries, boolean charged) {
		this.bubbas$gemHit = List.copyOf(entries);
		this.bubbas$charged = charged;
		int color = -1;
		for (GemHits.Entry e : entries) {
			if (!e.drawback() && ModEffects.gem(e.key()) != null) {
				color = ModEffects.gem(e.key()).value().getColor();
				break;
			}
		}
		if (color != -1 && this.entityData.get(ID_EFFECT_COLOR) == -1) {
			this.entityData.set(ID_EFFECT_COLOR, color);
		}
	}

	@WrapOperation(method = "doPostHurtEffects", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/alchemy/PotionContents;forEachEffect(Ljava/util/function/Consumer;F)V"))
	private void bubbas$strongerApplies(PotionContents contents, Consumer<MobEffectInstance> consumer, float scale, Operation<Void> original) {
		if (this.bubbas$gemHit.isEmpty()) {
			original.call(contents, consumer, scale);
			return;
		}
		List<GemHits.Entry> kept = new ArrayList<>(this.bubbas$gemHit);
		Consumer<MobEffectInstance> filtered = effect -> {
			String key = GemHits.vanillaKey(effect.getEffect());
			GemHits.Entry gem = kept.stream().filter(e -> !e.drawback() && e.key().equals(key)).findFirst().orElse(null);
			if (gem == null) {
				consumer.accept(effect);
			} else if (GemHits.vanillaFraction(key, effect.getAmplifier()) >= GemHits.potency(gem)) {
				kept.remove(gem);
				consumer.accept(effect);
			}
		};
		original.call(contents, filtered, scale);
		this.bubbas$gemHit = kept;
	}

	@Inject(method = "doPostHurtEffects", at = @At("TAIL"))
	private void bubbas$applyGemHit(LivingEntity mob, CallbackInfo ci) {
		if (!this.bubbas$gemHit.isEmpty() && this.level() instanceof ServerLevel level) {
			GemHits.apply(level, mob, this.getOwner(), this, this.bubbas$gemHit, this.bubbas$charged);
		}
	}
}
