// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Putting relic map shreds together, shapeless, in any crafting grid or a crafter: two or more shreds of the same
 * relic and size, their pieces adding up to no more than the whole map.
 */
public class RelicShredRecipe extends CustomRecipe {
	public static final RelicShredRecipe INSTANCE = new RelicShredRecipe();
	public static final RecipeSerializer<RelicShredRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE),
			StreamCodec.<RegistryFriendlyByteBuf, RelicShredRecipe>unit(INSTANCE));

	private RelicShredRecipe() {
	}

	/** The combined shred, or null when these don't go together. */
	public static @Nullable RelicShred combine(CraftingInput input) {
		RelicShred first = null;
		int total = 0;
		int stacks = 0;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			RelicShred shred = stack.get(Relics.SHRED);
			if (!stack.is(Relics.RELIC_MAP_SHRED) || shred == null || shred.complete()) {
				return null;
			}
			if (first == null) {
				first = shred;
			} else if (!first.sameMap(shred)) {
				return null;
			}
			total += shred.count();
			stacks++;
		}
		if (first == null || stacks < 2 || total > first.size()) {
			return null;
		}
		return new RelicShred(first.name(), first.size(), total);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return combine(input) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		RelicShred shred = combine(input);
		return shred == null ? ItemStack.EMPTY : RelicMaps.shred(shred.name(), shred.size(), shred.count());
	}

	@Override
	public RecipeSerializer<RelicShredRecipe> getSerializer() {
		return SERIALIZER;
	}
}
