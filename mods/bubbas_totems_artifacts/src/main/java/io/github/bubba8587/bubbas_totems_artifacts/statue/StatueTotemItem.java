// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A totem that can be set down as its statue. */
public class StatueTotemItem extends Item {
	public StatueTotemItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		TotemType type = TotemType.forItem(this);
		return type == null ? InteractionResult.PASS : TotemStatueBlock.placeFrom(context, type);
	}
}
