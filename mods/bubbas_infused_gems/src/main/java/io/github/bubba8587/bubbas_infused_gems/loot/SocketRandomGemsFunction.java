// dte:C4
package io.github.bubba8587.bubbas_infused_gems.loot;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.station.Sockets;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Loot function {@code bubbas_infused_gems:socket_random_gems}: puts {@code count} gems rolled from {@link GemRollSpec}
 * into gear that takes gems (up to its free slots); {@code gear} limits it to {@code "armor"} or {@code "weapon"}
 * (default {@code "any"}). Anything else passes through unchanged.
 */
public class SocketRandomGemsFunction extends LootItemConditionalFunction {
	public static final MapCodec<SocketRandomGemsFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
			.and(ContextIntProviders.CODEC.optionalFieldOf("count", ContextIntProviders.exactly(1)).forGetter(f -> f.count))
			.and(com.mojang.serialization.Codec.STRING.optionalFieldOf("gear", "any").forGetter(f -> f.gear))
			.and(GemRollSpec.MAP_CODEC.forGetter(f -> f.spec))
			.apply(i, SocketRandomGemsFunction::new));

	private final Holder<ContextIntProvider> count;
	private final String gear;
	private final GemRollSpec spec;

	private SocketRandomGemsFunction(Optional<Holder<LootItemCondition>> condition, Holder<ContextIntProvider> count, String gear, GemRollSpec spec) {
		super(condition);
		this.count = count;
		this.gear = gear;
		this.spec = spec;
	}

	@Override
	public void validate(ValidationContext context) {
		super.validate(context);
		Validatable.validateHolder(context, "count", this.count);
	}

	@Override
	public MapCodec<SocketRandomGemsFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		if (("armor".equals(this.gear) && !stack.is(Sockets.ARMOR)) || ("weapon".equals(this.gear) && !stack.is(Sockets.WEAPONS))) {
			return stack;
		}
		int free = Sockets.slots(stack) - Sockets.gems(stack).size();
		if (free <= 0 || stack.getCount() != 1) {
			return stack;
		}
		int n = Math.min(free, this.count.value().getInt(context));
		var gems = Sockets.gems(stack);
		for (int k = 0; k < n; k++) {
			gems = gems.with(this.spec.roll(context.getRandom()));
		}
		if (n > 0) {
			stack.set(ModComponents.SOCKETED_GEMS, gems);
		}
		return stack;
	}
}
