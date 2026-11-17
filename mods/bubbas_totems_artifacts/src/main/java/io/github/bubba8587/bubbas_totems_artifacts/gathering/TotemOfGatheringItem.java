// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.gathering;

import io.github.bubba8587.bubbas_totems_artifacts.totems.Held;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Totem of Gathering, held in either hand: dropped items nearby fly to the holder (gathering/Gathering). */
public class TotemOfGatheringItem extends Item {
	private static final Held HELD = new Held();

	public TotemOfGatheringItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (Held.inHand(owner, slot) && HELD.firstThisTick(owner, level.getGameTime())) {
			Gathering.pull(level, (LivingEntity) owner);
		}
	}
}
