// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gravestone's text: vanilla's sign block entity (front and back text, dye, glow ink, wax, editing, saving) with its
 * own type and narrower lines, since the stone's face is 12 pixels wide where a sign's board is 16.
 */
public class GravestoneBlockEntity extends SignBlockEntity {
	/** Line width in font pixels (a sign's is 90): what fits across the face at the sign's text scale. */
	public static final int MAX_LINE_WIDTH = 60;

	public GravestoneBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GRAVESTONE, pos, state);
	}

	@Override
	public int getMaxTextLineWidth() {
		return MAX_LINE_WIDTH;
	}

	@Override
	public int getTextLineHeight() {
		return 10;
	}
}
