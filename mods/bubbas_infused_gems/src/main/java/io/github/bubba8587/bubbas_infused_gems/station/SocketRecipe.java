// dte:C4
package io.github.bubba8587.bubbas_infused_gems.station;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/** Smithing table: socket a finished gem into gear. */
public class SocketRecipe implements SmithingRecipe {
	public static final MapCodec<SocketRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
			Ingredient.CODEC.fieldOf("addition").forGetter(r -> r.addition)
	).apply(i, SocketRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, SocketRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, r -> r.base, Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition, SocketRecipe::new);
	public static final RecipeSerializer<SocketRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final Ingredient base;
	private final Ingredient addition;

	public SocketRecipe(Ingredient base, Ingredient addition) {
		this.base = base;
		this.addition = addition;
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		return SmithingRecipe.super.matches(input, level) && Sockets.canSocket(input.base(), input.addition());
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		return Sockets.socket(input.base(), input.addition());
	}

	@Override
	public Optional<Ingredient> templateIngredient() {
		return Optional.empty();
	}

	@Override
	public Ingredient baseIngredient() {
		return this.base;
	}

	@Override
	public Optional<Ingredient> additionIngredient() {
		return Optional.of(this.addition);
	}

	@Override
	public RecipeSerializer<SocketRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}
}
