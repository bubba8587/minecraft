// dte:A2,A4
package io.github.bubba8587.refmod.item;

import io.github.bubba8587.refmod.pseudoblock.CharmPseudoBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Used on any block: places a lantern charm pseudo-block on top of it ({@link CharmPseudoBlock}). */
public class LanternCharmItem extends Item {
	public LanternCharmItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		float yRot = context.getHorizontalDirection().getOpposite().toYRot();
		if (CharmPseudoBlock.place(level, context.getClickedPos(), yRot) == null) {
			return InteractionResult.FAIL;
		}
		context.getItemInHand().consume(1, context.getPlayer());
		return InteractionResult.SUCCESS;
	}
}
