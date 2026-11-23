// dte:C4
package io.github.bubba8587.bubbas_infused_gems.loot;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Loot function bubbas_infused_gems:random_gem. */
public class RandomGemFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomGemFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
			.and(GemRollSpec.MAP_CODEC.forGetter(f -> f.spec))
			.apply(i, RandomGemFunction::new));

	private final GemRollSpec spec;

	private RandomGemFunction(Optional<Holder<LootItemCondition>> condition, GemRollSpec spec) {
		super(condition);
		this.spec = spec;
	}

	@Override
	public MapCodec<RandomGemFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		ItemStack out = stack.is(ModItems.GEM) ? stack : new ItemStack(ModItems.GEM, Math.max(1, stack.getCount()));
		out.set(ModComponents.GEM, this.spec.roll(context.getRandom()));
		return out;
	}
}
