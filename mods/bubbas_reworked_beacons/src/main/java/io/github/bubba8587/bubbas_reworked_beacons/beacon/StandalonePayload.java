// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.beacon;

import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Reworked Beacons alone (specs/reworked-beacons.md, Standalone): the payment is a gem base item and it is the
 * power choice. The pyramid needed follows vanilla's power order, except Regeneration works from 3 layers; a
 * 4-layer pyramid upgrades the power to level II. Registered under the payload entrypoint like any other mod's
 * items, at the lowest priority.
 */
public class StandalonePayload implements BeaconPayload {
	/** One vanilla power bought by a payment item, and the pyramid it needs. */
	public record Power(Holder<MobEffect> effect, int requiredLevels) {
	}

	public static final Map<Item, Power> POWERS = Map.of(
			Items.AMETHYST_SHARD, new Power(MobEffects.SPEED, 1),
			Items.QUARTZ, new Power(MobEffects.HASTE, 1),
			Items.ENDER_PEARL, new Power(MobEffects.RESISTANCE, 2),
			Items.DIAMOND, new Power(MobEffects.JUMP_BOOST, 2),
			Items.PRISMARINE_SHARD, new Power(MobEffects.STRENGTH, 3),
			Items.NETHER_STAR, new Power(MobEffects.REGENERATION, 3));

	/** 4 layers: level II (amplifier 1). */
	public static int amplifierFor(int levels) {
		return levels >= 4 ? 1 : 0;
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return POWERS.containsKey(stack.getItem());
	}

	@Override
	public int priority() {
		return -1000;
	}

	@Override
	public int requiredLevels(ItemStack stack) {
		Power p = POWERS.get(stack.getItem());
		return p == null ? 1 : p.requiredLevels();
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		Power p = POWERS.get(stack.getItem());
		if (p == null) {
			return List.of();
		}
		// Vanilla's potion tooltip words: "Speed", "Speed II", in the effect category's colour.
		MutableComponent name = Component.translatable(p.effect().value().getDescriptionId());
		int amp = amplifierFor(context.levels());
		MutableComponent line = amp > 0
				? Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + amp))
				: name;
		return List.of(line.withStyle(p.effect().value().getCategory().getTooltipFormatting()));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		Power p = POWERS.get(stack.getItem());
		if (p == null) {
			return;
		}
		int amp = amplifierFor(context.levels());
		for (LivingEntity e : context.livingEntities()) {
			e.addEffect(new MobEffectInstance(p.effect(), context.effectDurationTicks(), amp, true, true));
		}
	}
}
