// dte:C4
package io.github.bubba8587.bubbas_infused_gems.station;

import com.mojang.serialization.MapCodec;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Brewing stand: a finished gem with a drawback + a fermented spider eye inverts the gem (GemInversion). A vanilla
 * brewing recipe underneath, so the stand's own slots, timing and hoppers work; only gems that can invert match.
 */
public class InvertGemRecipe extends BrewingRecipe {
	public static final InvertGemRecipe INSTANCE = new InvertGemRecipe();
	public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE),
			StreamCodec.<RegistryFriendlyByteBuf, BrewingRecipe>unit(INSTANCE));

	private InvertGemRecipe() {
		super(PotionIngredient.of(ModItems.GEM), PotionIngredient.of(Items.FERMENTED_SPIDER_EYE), new ItemStackTemplate(ModItems.GEM));
	}

	@Override
	public boolean matches(BrewingInput input) {
		return super.matches(input) && GemInversion.canInvert(input.input());
	}

	@Override
	public boolean matches(BrewingInput input, Level level) {
		return this.matches(input);
	}

	@Override
	public ItemStack assemble(BrewingInput input) {
		return GemInversion.invert(input.input());
	}

	@Override
	public RecipeSerializer<BrewingRecipe> getSerializer() {
		return SERIALIZER;
	}
}
