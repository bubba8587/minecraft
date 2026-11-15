// dte:C4
package io.github.bubba8587.bubbas_infused_gems.station;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Brewing stand: an Infused Gem of form {@code from} in the bottle slot plus the reagent (gunpowder, then dragon
 * breath) raises its form by one and keeps everything else, like a potion. Hand-rolled as one recipe per step because
 * vanilla brewing outputs are fixed item templates; copying the gem data with vanilla recipes would take one file per
 * effect x variant x Infusion level (hundreds).
 */
public class RaiseFormRecipe extends BrewingRecipe {
	public static final MapCodec<RaiseFormRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PotionIngredient.MAP_CODEC.fieldOf("reagent").forGetter(BrewingRecipe::getReagent),
			GemForm.CODEC.fieldOf("from").forGetter(r -> r.from)
	).apply(i, RaiseFormRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, RaiseFormRecipe> STREAM_CODEC = StreamCodec.composite(
			PotionIngredient.STREAM_CODEC, BrewingRecipe::getReagent,
			GemForm.STREAM_CODEC, r -> r.from, RaiseFormRecipe::new);
	public static final RecipeSerializer<RaiseFormRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final GemForm from;

	public RaiseFormRecipe(PotionIngredient reagent, GemForm from) {
		super(PotionIngredient.of(ModItems.INFUSED_GEM), reagent, new ItemStackTemplate(ModItems.INFUSED_GEM));
		this.from = from;
	}

	@Override
	public boolean matches(BrewingInput input) {
		InfusedGem gem = input.input().get(ModComponents.INFUSED_GEM);
		return super.matches(input) && gem != null && gem.form() == this.from && this.from != GemForm.LINGERING;
	}

	@Override
	public ItemStack assemble(BrewingInput input) {
		ItemStack out = input.input().copyWithCount(1);
		InfusedGem gem = out.get(ModComponents.INFUSED_GEM);
		out.set(ModComponents.INFUSED_GEM, gem.withForm(this.from.next()));
		return out;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public RecipeSerializer<BrewingRecipe> getSerializer() {
		return (RecipeSerializer) SERIALIZER;
	}
}
