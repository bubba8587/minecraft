// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soul fire sets Soulfire burning (soulfire/SoulBurning): vanilla's fire contact with the soul ignite run before the
 * shared fire ignite, so ordinary fire or lava touched in the same tick still wins, and soul fire damage.
 */
@Mixin(BaseFireBlock.class)
abstract class SoulFireBlockMixin {
	@Shadow
	@Final
	private float fireDamage;

	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void brm$soulFire(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise,
			CallbackInfo ci) {
		if (!((Object) this instanceof SoulFireBlock)) {
			return;
		}
		float damage = this.fireDamage;
		effectApplier.apply(InsideBlockEffectType.CLEAR_FREEZE);
		effectApplier.runBefore(InsideBlockEffectType.FIRE_IGNITE, SoulBurning::contactIgnite);
		effectApplier.runAfter(InsideBlockEffectType.FIRE_IGNITE, e -> SoulBurning.hurtByContact(e, false, damage));
		ci.cancel();
	}
}
