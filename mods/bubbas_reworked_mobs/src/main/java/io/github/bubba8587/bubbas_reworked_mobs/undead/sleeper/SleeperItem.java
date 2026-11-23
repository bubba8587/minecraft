// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/** One sleeper item per mob kind; the form (pose, statue) is the stack's minecraft:block_state component. */
public class SleeperItem extends BlockItem {
	public SleeperItem(SleeperBlock block, Properties properties) {
		super(block, properties);
	}

	/** The pose a stack places (the kind's default when unset). */
	public static SleeperPose pose(SleeperBlock block, ItemStack stack) {
		SleeperPose pose = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(SleeperBlock.POSE);
		return pose != null ? pose : block.defaultBlockState().getValue(SleeperBlock.POSE);
	}

	/** Whether a stack places a statue (resting, never wakes). */
	public static boolean isStatue(ItemStack stack) {
		return Boolean.TRUE.equals(stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(SleeperBlock.RESTING));
	}

	/** A stack of this kind in the given form. */
	public static ItemStack form(SleeperItem item, SleeperPose pose, boolean statue) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(SleeperBlock.POSE, pose).with(SleeperBlock.RESTING, statue));
		return stack;
	}

	/** Name with the form. */
	@Override
	public Component getName(ItemStack stack) {
		SleeperBlock block = (SleeperBlock) getBlock();
		String base = getDescriptionId() + (isStatue(stack) ? ".statue" : "");
		return Component.translatable("item.bubbas_reworked_mobs.sleeper.form", Component.translatable(base),
				Component.translatable("item.bubbas_reworked_mobs.sleeper.pose." + pose(block, stack).getSerializedName()));
	}
}
