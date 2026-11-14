// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.client.display;

import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class LootDisplayRenderState extends BlockEntityRenderState {
	public DisplayPlacement placement = DisplayPlacement.GROUND;
	public Direction facing = Direction.NORTH;
	/** A small per-position turn for loot lying on the ground, so rows of it don't look stamped out. Degrees. */
	public float jitter;
	/** The displayed item, resolved through its own item model (FIXED context, as in an item frame). */
	public final ItemStackRenderState item = new ItemStackRenderState();
}
