// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gravestone's text: vanilla's sign block entity (front and back text, dye, glow ink, wax, editing, saving) with its
 * own type and narrower lines, since the stone's face is 12 pixels wide where a sign's board is 16.
 */
public class GravestoneBlockEntity extends SignBlockEntity {
	/** Line width in font pixels (a sign's is 90). */
	public static final int MAX_LINE_WIDTH = GraveTextLayout.MAX_LINE_WIDTH;

	public GravestoneBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GRAVESTONE, pos, state);
	}

	/**
	 * Carves {@code front} as the front text without touching the level (worldgen: the block entity isn't in a level
	 * yet, and vanilla's {@code setText} notifies it). Loaded the way a saved gravestone loads.
	 */
	public void carve(SignText front, HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.put("front_text", SignText.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), front).getOrThrow());
		loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
	}

	@Override
	public int getMaxTextLineWidth() {
		return MAX_LINE_WIDTH;
	}

	@Override
	public int getTextLineHeight() {
		return GraveTextLayout.LINE_HEIGHT;
	}
}
