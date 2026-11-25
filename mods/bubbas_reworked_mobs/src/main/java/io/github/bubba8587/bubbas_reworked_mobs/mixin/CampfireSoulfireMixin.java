// dte:C1,B17,B15
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lit campfires (soulfire/SoulBurning): a soul campfire sets Soulfire burning; an ordinary one turns it to normal burning. */
@Mixin(CampfireBlock.class)
abstract class CampfireSoulfireMixin {
	@WrapOperation(method = "entityInside", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
	private void brm$campfire(Entity entity, DamageSource source, float amount, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
		if (entity.level().isClientSide()) {
			original.call(entity, source, amount);
		} else if (state.is(Blocks.SOUL_CAMPFIRE)) {
			SoulBurning.contactIgnite(entity);
			SoulBurning.hurtByContact(entity, true, amount);
		} else {
			SoulBurning.clear(entity);
			original.call(entity, source, amount);
		}
	}
}
