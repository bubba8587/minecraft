// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Loot function bubbas_totems_artifacts:random_relic_map: the finished map of a random relic of one size (RelicMaps.map). */
public class RelicMapFunction extends LootItemConditionalFunction {
	public static final MapCodec<RelicMapFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
			.and(Codec.INT.validate(s -> RelicNames.SIZES.contains(s) ? DataResult.success(s)
					: DataResult.error(() -> "size must be 4 or 9: " + s)).optionalFieldOf("size", 4).forGetter(f -> f.size))
			.apply(i, RelicMapFunction::new));

	private final int size;

	RelicMapFunction(Optional<Holder<LootItemCondition>> condition, int size) {
		super(condition);
		this.size = size;
	}

	@Override
	public MapCodec<RelicMapFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		String name = RelicNames.ALL.get(context.getRandom().nextInt(RelicNames.ALL.size()));
		return RelicMaps.map(context.getLevel(), new RelicShred(name, size, size));
	}
}
