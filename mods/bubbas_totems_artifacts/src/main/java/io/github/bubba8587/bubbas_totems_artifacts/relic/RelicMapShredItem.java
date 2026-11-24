// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.function.Consumer;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/** Relic Map shred: pieces of one map, put together in a crafting grid; complete, it turns into the map. */
public class RelicMapShredItem extends Item {
	private static final String KEY = "item." + BubbasTotemsArtifacts.MOD_ID + ".relic_map_shred";

	public RelicMapShredItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		RelicShred shred = stack.get(Relics.SHRED);
		if (shred == null) {
			return Component.translatable(KEY);
		}
		if (shred.unnamed()) {
			return Component.translatable(KEY + ".unnamed", shred.count(), shred.size());
		}
		return Component.translatable(KEY + ".named", RelicNames.display(shred.name()), shred.count(), shred.size());
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		RelicShred shred = stack.get(Relics.SHRED);
		if (shred == null) {
			return;
		}
		if (shred.unnamed()) {
			return;
		}
		if (shred.complete()) {
			builder.accept(Component.translatable(KEY + ".whole").withStyle(ChatFormatting.GOLD));
		} else {
			builder.accept(Component.translatable(KEY + ".pieces", shred.count(), shred.size()).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		RelicShred shred = stack.get(Relics.SHRED);
		if (shred == null || !(owner instanceof Player player)) {
			return;
		}
		if (shred.unnamed()) {
			RelicMaps.set(stack, new RelicShred(RelicNames.ALL.get(level.getRandom().nextInt(RelicNames.ALL.size())), shred.size(), shred.count()));
			return;
		}
		if (!shred.complete()) {
			return;
		}
		ItemStack map = RelicMaps.map(level, shred);
		if (map.isEmpty()) {
			return;
		}
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i) == stack) {
				inventory.setItem(i, map);
				return;
			}
		}
	}
}
