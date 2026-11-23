// dte:C3,B15
package io.github.bubba8587.bubbas_extra_recipes.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_extra_recipes.blockstate.SyntheticOre;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Synthetic ore gives no experience when mined: XP is code, not loot table data. */
@Mixin(DropExperienceBlock.class)
public abstract class DropExperienceBlockMixin {
	@WrapWithCondition(method = "spawnAfterBreak", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/DropExperienceBlock;tryDropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/util/valueproviders/IntProvider;)V"))
	private boolean bubbas_extra_recipes$noXpFromSynthetic(DropExperienceBlock block, ServerLevel level, BlockPos pos, ItemStack tool, IntProvider xpRange,
			@Local(argsOnly = true) BlockState state) {
		return !SyntheticOre.isSynthetic(state);
	}
}
