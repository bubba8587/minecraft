// dte:C4
package io.github.bubba8587.bubbas_infused_gems.gem;

import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
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

/** The gem component: a finished gem. */
public record Gem(Holder<MobEffect> effect, int infusion, int tier, float main, Optional<Roll> other, Optional<Roll> drawback, int potionLevel, int mainGrade)
		implements TooltipProvider {
	/** A gem whose potion and main grade are worked out from its strength (old gems, tests). */
	public Gem(Holder<MobEffect> effect, int infusion, int tier, float main, Optional<Roll> other, Optional<Roll> drawback) {
		this(effect, infusion, tier, main, other, drawback, 0, 0);
	}

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
			Roll.CODEC.optionalFieldOf("drawback").forGetter(Gem::drawback),
			Codec.intRange(0, 3).optionalFieldOf("potion_level", 0).forGetter(Gem::potionLevel),
			Codec.intRange(0, 3).optionalFieldOf("main_grade", 0).forGetter(Gem::mainGrade)
	).apply(i, Gem::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, Gem> STREAM_CODEC = StreamCodec.composite(
			MobEffect.STREAM_CODEC, Gem::effect,
			ByteBufCodecs.VAR_INT, Gem::infusion,
			ByteBufCodecs.VAR_INT, Gem::tier,
			ByteBufCodecs.FLOAT, Gem::main,
			ByteBufCodecs.optional(Roll.STREAM_CODEC), Gem::other,
			ByteBufCodecs.optional(Roll.STREAM_CODEC), Gem::drawback,
			ByteBufCodecs.VAR_INT, Gem::potionLevel,
			ByteBufCodecs.VAR_INT, Gem::mainGrade,
			Gem::new);

	public String key() {
		return GemText.key(this.effect);
	}

	public boolean hasMain() {
		return this.main > 0.0F;
	}

	/** One line per rolled effect: "+" for good, "-" for the drawback; exact strengths only on advanced tooltips. */
	public void lines(Consumer<Component> out, String indent) {
		this.lines(out, indent, false);
	}

	public void lines(Consumer<Component> out, String indent, boolean exact) {
		if (this.hasMain()) {
			out.accept(Component.literal(indent + "+ ").append(this.mainName()).append(exact(exact, this.main))
					.withColor(GemColors.text(this.effect.value().getColor())));
		}
		this.other.ifPresent(r -> out.accept(Component.literal(indent + "+ ").append(this.otherName(r)).append(exact(exact, r.strength()))
				.withColor(GemColors.text(r.effect().value().getColor()))));
		this.drawback.ifPresent(r -> out.accept(Component.literal(indent + "- ").append(this.drawbackName(r)).withStyle(ChatFormatting.RED)));
	}

	/** Rows for the socketed item's tooltip: the main effect, then its extra effect and drawback indented under it. */
	public void socketedLines(Consumer<Component> out) {
		MutableComponent head = Component.literal(" ◆ ").withColor(GemColors.text(this.effect.value().getColor()));
		if (this.hasMain()) {
			head.append(this.mainName());
		} else {
			this.drawback.ifPresent(r -> head.append(Component.literal("- ").append(this.drawbackName(r)).withStyle(ChatFormatting.RED)));
		}
		out.accept(head);
		this.other.ifPresent(r -> out.accept(Component.literal("    + ").append(this.otherName(r)).withColor(GemColors.text(r.effect().value().getColor()))));
		if (this.hasMain()) {
			this.drawback.ifPresent(r -> out.accept(Component.literal("    - ").append(this.drawbackName(r)).withStyle(ChatFormatting.RED)));
		}
	}

	private static Component exact(boolean exact, float strength) {
		return exact ? Component.literal(" (" + GemText.percent(strength) + ")").withStyle(ChatFormatting.DARK_GRAY) : Component.empty();
	}

	private MutableComponent mainName() {
		return GemText.effectName(this.effect);
	}

	/** Infusion level from the potion it was brewed with: normal I, long II, strong (or an item) III. */
	public int infusionLevel() {
		return this.levels()[0];
	}

	/** Enchant level the main effect actually has: weak I, medium II, strongest III (an inverted gem drops one). */
	public int enchantLevel() {
		return this.levels()[1];
	}

	/** {infusion, enchant}: as recorded when made, else worked out from the main strength (falls back to {I, tier}). */
	private int[] levels() {
		if (this.potionLevel > 0 && this.mainGrade > 0) {
			return new int[] { this.potionLevel, this.mainGrade };
		}
		Balance b = Balance.get();
		Balance.EffectData e = b.effect(this.key());
		int tier = Math.max(1, Math.min(3, this.tier));
		if (e == null || !this.hasMain()) {
			return new int[] { e != null && !"potion".equals(e.source()) ? 3 : 1, tier };
		}
		double infusion = b.infusion(this.infusion).factor();
		int[] best = { 1, tier };
		double bestDiff = 0.02;
		for (int g = 3; g >= 1; g--) {
			Double grade = b.grades.get(GRADES[g]);
			if (grade == null) {
				continue;
			}
			for (var v : e.variants().entrySet()) {
				double expected = Math.min(1.0, v.getValue().factor() * infusion * grade);
				double diff = Math.abs(expected - this.main) + (g == tier ? 0.0 : 1.0E-3);
				if (diff < bestDiff) {
					bestDiff = diff;
					String name = v.getKey();
					best = new int[] { "long".equals(name) ? 2 : "normal".equals(name) ? 1 : 3, g };
				}
			}
		}
		if (!"potion".equals(e.source())) {
			best[0] = 3;
		}
		return best;
	}

	/** An extra effect: "Minor" when it rolled weak. */
	private MutableComponent otherName(Roll r) {
		double grade = r.strength() / Balance.get().infusion(this.infusion).factor();
		return minor(grade < 0.6, r);
	}

	private MutableComponent drawbackName(Roll r) {
		return minor(r.strength() <= Balance.get().drawbackSize("weak") + 1.0E-4, r);
	}

	private static MutableComponent minor(boolean minor, Roll r) {
		return minor ? Component.translatable(GemText.KEY + "tooltip.minor", GemText.effectName(r.effect())) : GemText.effectName(r.effect());
	}

	/** Infusion level of a potion strength name: normal I, long II, strong or an item III. */
	public static int potionLevel(String variant) {
		return "normal".equals(variant) ? 1 : "long".equals(variant) ? 2 : 3;
	}

	/** Enchant level of a grade name: weak I, medium II, strongest III; 0 for none. */
	public static int gradeLevel(@org.jspecify.annotations.Nullable String grade) {
		return grade == null ? 0 : "weak".equals(grade) ? 1 : "medium".equals(grade) ? 2 : 3;
	}

	/** Grades by enchant level. */
	private static final String[] GRADES = { "", "weak", "medium", "strongest" };

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components) {
		out.accept(Component.translatable(GemText.KEY + "tooltip.base_power", GemText.numeral(this.infusion)).withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable(GemText.KEY + "tooltip.infusion", GemText.roman(this.infusionLevel())).withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable(GemText.KEY + "tooltip.enchant", GemText.roman(this.enchantLevel())).withStyle(ChatFormatting.GRAY));
		this.lines(out, "", flag.isAdvanced());
	}
}
