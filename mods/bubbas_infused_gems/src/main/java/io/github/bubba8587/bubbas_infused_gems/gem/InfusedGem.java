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
		Balance b = Balance.get();
		if (this.form == GemForm.ITEM) {
			Balance.EffectData e = b.effect(this.key());
			Balance.Variant v = e == null ? null : e.variants().get(this.variant);
			if (v != null && v.maxTier() > 0) {
				return v.maxTier(); // the model's per-source cap
			}
		}
		return b.formMaxTier.getOrDefault(this.form.getSerializedName(), 1);
	}

	/** Potion factor x Infusion factor: the gem's quality before the enchant grade. */
	public double baseQuality() {
		Balance b = Balance.get();
		Balance.EffectData e = b.effect(this.key());
		double variant = e == null ? 1.0 : e.variantFactor(this.variant);
		return variant * b.infusion(this.infusion).factor();
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components) {
		// One short line (owner): the name already has the effect, Infusion and form; no how-to lines.
		out.accept((this.form == GemForm.ITEM ? Component.translatable(GemText.KEY + "tooltip.item", this.maxTier())
				: Component.translatable(GemText.KEY + "tooltip.potion", Component.translatable(GemText.KEY + "variant." + this.variant),
						this.maxTier())).withStyle(ChatFormatting.GRAY));
	}
}
