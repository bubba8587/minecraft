// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.growth;

import io.github.bubba8587.bubbas_totems_artifacts.totems.Held;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Totem of Growth (working name), held in either hand: plants nearby grow faster (growth/Growth). */
public class TotemOfGrowthItem extends io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem {
	private static final Held HELD = new Held();

	public TotemOfGrowthItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (Held.inHand(owner, slot) && HELD.firstThisTick(owner, level.getGameTime())) {
			Growth.boost(level, Growth.heldBox(owner.blockPosition()), TotemsTuning.GROWTH_HELD_EXTRA);
		}
	}
}
