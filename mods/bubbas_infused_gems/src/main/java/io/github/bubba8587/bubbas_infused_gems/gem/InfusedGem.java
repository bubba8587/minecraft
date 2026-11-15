// dte:C4
package io.github.bubba8587.bubbas_infused_gems.gem;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * The {@code infused_gem} component: a brewed, not yet enchanted gem. Effect, potion variant (normal/long/strong, sets
 * the potion factor), form (sets the highest enchant tier) and Infusion level I-VI (the base gem, sets the Infusion
 * factor).
 */
public record InfusedGem(Holder<MobEffect> effect, String variant, GemForm form, int infusion) implements TooltipProvider {
	public static final Codec<InfusedGem> CODEC = RecordCodecBuilder.create(i -> i.group(
			MobEffect.CODEC.fieldOf("effect").forGetter(InfusedGem::effect),
			Codec.STRING.optionalFieldOf("variant", "normal").forGetter(InfusedGem::variant),
			GemForm.CODEC.optionalFieldOf("form", GemForm.NORMAL).forGetter(InfusedGem::form),
			Codec.intRange(1, 6).fieldOf("infusion").forGetter(InfusedGem::infusion)
	).apply(i, InfusedGem::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, InfusedGem> STREAM_CODEC = StreamCodec.composite(
			MobEffect.STREAM_CODEC, InfusedGem::effect,
			ByteBufCodecs.STRING_UTF8, InfusedGem::variant,
			GemForm.STREAM_CODEC, InfusedGem::form,
			ByteBufCodecs.VAR_INT, InfusedGem::infusion,
			InfusedGem::new);

	public String key() {
		return GemText.key(this.effect);
	}

	/** The highest enchant tier this gem can reach (owner: normal 1, splash 2, lingering 3). */
	public int maxTier() {
		return Balance.get().formMaxTier.getOrDefault(this.form.getSerializedName(), 1);
	}

	/** Potion factor x Infusion factor: the gem's quality before the enchant grade. */
	public double baseQuality() {
		Balance b = Balance.get();
		Balance.EffectData e = b.effect(this.key());
		double variant = e == null ? 1.0 : e.variantFactor(this.variant);
		return variant * b.infusion(this.infusion).factor();
	}

	public InfusedGem withForm(GemForm form) {
		return new InfusedGem(this.effect, this.variant, form, this.infusion);
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components) {
		out.accept(Component.translatable(GemText.KEY + "tooltip.potion", Component.translatable(GemText.KEY + "variant." + this.variant),
				Component.translatable(GemText.KEY + "form." + this.form.getSerializedName())).withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable(GemText.KEY + "tooltip.max_tier", this.maxTier()).withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable(GemText.KEY + "tooltip.enchant_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
