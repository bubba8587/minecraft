// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import net.minecraft.world.level.storage.loot.ValidationContext;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

/**
 * Item modifier bubbas_reworked_mobs:fabricator_map: makes the filled map an explorer map of the nearest Ancient
 * Fabricator (its teleporter pad), like vanilla's exploration_map. Leaves the stack unchanged when none is in range.
 */
public class FabricatorMapFunction extends LootItemConditionalFunction {
	public static final MapCodec<FabricatorMapFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, FabricatorMapFunction::new));

	FabricatorMapFunction(Optional<Holder<LootItemCondition>> condition) {
		super(condition);
	}

	@Override
	protected void validateCustom(ValidationContext context) {
	}

	@Override
	public MapCodec<FabricatorMapFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
		if (stack.isEmpty() || origin == null) {
			return stack;
		}
		ServerLevel level = context.getLevel();
		FabricatorSite site = FabricatorSites.nearest(level, BlockPos.containing(origin), FabricatorTuning.MAP_SEARCH_RADIUS);
		if (site == null) {
			return stack;
		}
		BlockPos pad = site.pad();
		MapItem.applyNewSavedData(level, stack, pad.getX(), pad.getZ(), FabricatorTuning.MAP_ZOOM, true, true);
		MapItem.renderBiomePreviewMap(level, stack);
		MapItemSavedData.addTargetDecoration(stack, pad, "+", MapDecorationTypes.TRIAL_CHAMBERS);
		return stack;
	}
}
