// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_mining.curve.MiningCurve;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The mining curve's speed (specs/reworked-mining.md "The curve"). Vanilla: tool speed, plus Efficiency's flat bonus,
 * then Haste, Mining Fatigue, the block_break_speed attribute (Infused Gems' Haste gems), water and air. Here the
 * tool speed gets the tier factor and the Efficiency multiplier, and vanilla's flat Efficiency bonus is taken back out
 * for tools on the curve; everything after it (Haste, gems) still multiplies on top.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Shadow
	@Final
	Inventory inventory;

	@ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
	private float bubbas_reworked_mining$curve(float speed, @Local(argsOnly = true) BlockState state) {
		return MiningCurve.speed(this.inventory.getSelectedItem(), state, speed);
	}

	@ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D", ordinal = 0))
	private double bubbas_reworked_mining$withoutFlatEfficiency(double value) {
		ItemStack stack = this.inventory.getSelectedItem();
		if (!MiningCurve.replacesEfficiency(stack)) {
			return value;
		}
		AttributeInstance instance = ((Player) (Object) this).getAttribute(Attributes.MINING_EFFICIENCY);
		AttributeModifier modifier = instance == null ? null : instance.getModifier(MiningCurve.VANILLA_EFFICIENCY_MODIFIER);
		return modifier == null ? value : value - modifier.amount();
	}
}
