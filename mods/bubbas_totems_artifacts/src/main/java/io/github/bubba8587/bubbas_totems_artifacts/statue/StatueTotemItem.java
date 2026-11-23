// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A totem that can be set down as its statue: sneak and use it on a block (statue/TotemStatueBlock#placeFrom). Without
 * sneaking the click passes on, so the totem's own use (a teleport, a ripple...) still happens. Every totem but
 * Warding (which has its own item class) extends this; its statue type comes from {@link TotemType#forItem}.
 */
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
