// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Loot function bubbas_totems_artifacts:random_relic_shred: a shred of a random relic (RelicMaps.random). */
public class RelicShredFunction extends LootItemConditionalFunction {
	public static final MapCodec<RelicShredFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, RelicShredFunction::new));

	RelicShredFunction(Optional<Holder<LootItemCondition>> condition) {
		super(condition);
	}

	@Override
	public MapCodec<RelicShredFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		return RelicMaps.random(context.getRandom());
	}
}
