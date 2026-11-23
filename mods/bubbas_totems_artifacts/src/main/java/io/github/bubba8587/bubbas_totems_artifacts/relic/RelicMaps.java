// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Making shreds and the finished maps. */
public final class RelicMaps {
	/** The finished map's zoom (explorer maps use 2). */
	public static final byte MAP_SCALE = 2;

	private RelicMaps() {
	}

	/** A shred stack: {@code count} of the {@code size} pieces of {@code name}'s map. */
	public static ItemStack shred(String name, int size, int count) {
		ItemStack stack = new ItemStack(Relics.RELIC_MAP_SHRED);
		set(stack, new RelicShred(name, size, Math.max(1, Math.min(count, size))));
		return stack;
	}

	public static void set(ItemStack stack, RelicShred shred) {
		stack.set(Relics.SHRED, shred);
		String look = shred.complete() ? "whole" : String.valueOf(shred.size());
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(look), List.of()));
	}

	/** A random shred of a random relic, as found in loot: mostly 1 piece, sometimes 2 or 3. */
	public static ItemStack random(RandomSource random) {
		String name = RelicNames.ALL.get(random.nextInt(RelicNames.ALL.size()));
		int size = random.nextFloat() < RelicTuning.LOOT_NINE_CHANCE ? 9 : 4;
		float roll = random.nextFloat();
		int count = roll < RelicTuning.LOOT_THREE_CHANCE ? 3 : roll < RelicTuning.LOOT_THREE_CHANCE + RelicTuning.LOOT_TWO_CHANCE ? 2 : 1;
		return shred(name, size, Math.min(count, size - 1));
	}

	/**
	 * The finished map of a complete shred: a filled explorer-style map of the Overworld centred on that relic's vault,
	 * with a red X on it. Empty when the relic has no vault (an unknown name).
	 */
	public static ItemStack map(ServerLevel any, RelicShred shred) {
		ServerLevel level = any.getServer().overworld();
		RelicSites.Site site = RelicSites.site(level.getSeed(), shred.name(), shred.size());
		if (site == null) {
			return ItemStack.EMPTY;
		}
		BlockPos target = site.centre();
		ItemStack map = MapItem.create(level, target.getX(), target.getZ(), MAP_SCALE, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
		map.set(DataComponents.ITEM_NAME, Component.translatable("item." + BubbasTotemsArtifacts.MOD_ID + ".relic_map", RelicNames.display(shred.name()),
				shred.size()));
		map.set(Relics.SHRED, shred);
		return map;
	}
}
