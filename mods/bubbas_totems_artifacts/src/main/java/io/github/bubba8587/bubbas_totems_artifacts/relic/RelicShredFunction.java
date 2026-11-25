// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot function bubbas_totems_artifacts:random_relic_shred: a shred of a random relic (RelicMaps.random). With
 * {@code "unnamed": true} the relic is left for the carrier's inventory to pick (trades).
 */
public class RelicShredFunction extends LootItemConditionalFunction {
	public static final MapCodec<RelicShredFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
			.and(Codec.BOOL.optionalFieldOf("unnamed", false).forGetter(f -> f.unnamed))
			.apply(i, RelicShredFunction::new));

	private final boolean unnamed;

	RelicShredFunction(Optional<Holder<LootItemCondition>> condition) {
		this(condition, false);
	}

	RelicShredFunction(Optional<Holder<LootItemCondition>> condition, boolean unnamed) {
		super(condition);
		this.unnamed = unnamed;
	}

	@Override
	public MapCodec<RelicShredFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		ItemStack shred = RelicMaps.random(context.getRandom());
		if (unnamed) {
			RelicShred s = shred.get(Relics.SHRED);
			return RelicMaps.shred("", s.size(), s.count());
		}
		return shred;
	}
}
