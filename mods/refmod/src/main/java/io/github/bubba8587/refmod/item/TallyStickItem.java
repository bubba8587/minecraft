// dte:A2,A4
package io.github.bubba8587.refmod.item;

import io.github.bubba8587.refmod.component.TallyCount;
import io.github.bubba8587.refmod.registry.ModComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Each use increments the {@link ModComponents#TALLY_COUNT} component on the stack. */
public class TallyStickItem extends Item {
	public TallyStickItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			stack.update(ModComponents.TALLY_COUNT, TallyCount.ZERO, TallyCount::increment);
		}
		return InteractionResult.SUCCESS;
	}
}
