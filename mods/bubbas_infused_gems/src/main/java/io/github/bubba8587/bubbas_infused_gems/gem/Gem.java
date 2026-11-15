// dte:C4
package io.github.bubba8587.bubbas_infused_gems.gem;

import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * The {@code gem} component: a finished (enchanted) gem, ready for the smithing table. {@code effect} is the gem's
 * intended effect (it names and colors the gem); {@code main} its strength (0 when tier 1 rolled a drawback alone);
 * {@code other} an extra effect of the same kind; {@code drawback} a mild effect of the opposite kind. Strengths are
 * gem quality (potion x Infusion x enchant grade, at most 1.0); a drawback's is its fixed size.
 */
public record Gem(Holder<MobEffect> effect, int infusion, int tier, float main, Optional<Roll> other, Optional<Roll> drawback)
		implements TooltipProvider {
	public record Roll(Holder<MobEffect> effect, float strength) {
		public static final Codec<Roll> CODEC = RecordCodecBuilder.create(i -> i.group(
				MobEffect.CODEC.fieldOf("effect").forGetter(Roll::effect),
				Codec.floatRange(0.0F, 1.0F).fieldOf("strength").forGetter(Roll::strength)
		).apply(i, Roll::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Roll> STREAM_CODEC = StreamCodec.composite(
				MobEffect.STREAM_CODEC, Roll::effect, ByteBufCodecs.FLOAT, Roll::strength, Roll::new);

		public String key() {
			return GemText.key(this.effect);
		}
	}

	public static final Codec<Gem> CODEC = RecordCodecBuilder.create(i -> i.group(
			MobEffect.CODEC.fieldOf("effect").forGetter(Gem::effect),
			Codec.intRange(1, 6).fieldOf("infusion").forGetter(Gem::infusion),
			Codec.intRange(1, 3).fieldOf("tier").forGetter(Gem::tier),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("main", 0.0F).forGetter(Gem::main),
			Roll.CODEC.optionalFieldOf("other").forGetter(Gem::other),
			Roll.CODEC.optionalFieldOf("drawback").forGetter(Gem::drawback)
	).apply(i, Gem::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, Gem> STREAM_CODEC = StreamCodec.composite(
			MobEffect.STREAM_CODEC, Gem::effect,
			ByteBufCodecs.VAR_INT, Gem::infusion,
			ByteBufCodecs.VAR_INT, Gem::tier,
			ByteBufCodecs.FLOAT, Gem::main,
			ByteBufCodecs.optional(Roll.STREAM_CODEC), Gem::other,
			ByteBufCodecs.optional(Roll.STREAM_CODEC), Gem::drawback,
			Gem::new);

	public String key() {
		return GemText.key(this.effect);
	}

	public boolean hasMain() {
		return this.main > 0.0F;
	}

	/** One line per rolled effect, colored by kind: main and other in the effect's color, the drawback in red. */
	public void lines(Consumer<Component> out, String indent) {
		if (this.hasMain()) {
			out.accept(Component.literal(indent).append(GemText.effectName(this.effect)).append(" " + GemText.percent(this.main))
					.withColor(GemColors.text(this.effect.value().getColor())));
		}
		this.other.ifPresent(r -> out.accept(Component.literal(indent + "+ ").append(GemText.effectName(r.effect()))
				.append(" " + GemText.percent(r.strength())).withColor(GemColors.text(r.effect().value().getColor()))));
		this.drawback.ifPresent(r -> out.accept(Component.literal(indent).append(
				Component.translatable(GemText.KEY + "tooltip.drawback", GemText.effectName(r.effect()))).withStyle(ChatFormatting.RED)));
	}

	/** A one-line summary for the socketed item's tooltip. */
	public MutableComponent summary() {
		MutableComponent line = Component.literal("◆ ").withColor(GemColors.text(this.effect.value().getColor()));
		if (this.hasMain()) {
			line.append(GemText.effectName(this.effect).append(" " + GemText.percent(this.main)));
		}
		this.other.ifPresent(r -> line.append(Component.literal(", + ").append(GemText.effectName(r.effect()))
				.append(" " + GemText.percent(r.strength())).withColor(GemColors.text(r.effect().value().getColor()))));
		// A drawback-only gem (owner: no special name) shows just its drawback.
		this.drawback.ifPresent(r -> line.append(Component.literal(this.hasMain() || this.other.isPresent() ? ", " : "").append(
				Component.translatable(GemText.KEY + "tooltip.drawback", GemText.effectName(r.effect()))).withStyle(ChatFormatting.RED)));
		return line;
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components) {
		// Short (owner): the effects, then the tier. The name already says the Infusion level; no how-to lines.
		this.lines(out, "");
		out.accept(Component.translatable(GemText.KEY + "tooltip.tier", this.tier).withStyle(ChatFormatting.GRAY));
	}
}
