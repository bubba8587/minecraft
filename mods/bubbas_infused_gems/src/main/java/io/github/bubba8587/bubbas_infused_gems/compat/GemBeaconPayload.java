// dte:C4
package io.github.bubba8587.bubbas_infused_gems.compat;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.effect.GemBeacons;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Infused Gems in Bubba's Reworked Beacons. */
public final class GemBeaconPayload implements BeaconPayload {
	@Override
	public boolean accepts(ItemStack stack) {
		return stack.has(ModComponents.GEM);
	}

	/** Corner particles in the gem's main effect colour. */
	@Override
	public int color(ItemStack stack) {
		Gem gem = stack.get(ModComponents.GEM);
		return gem == null ? 0xFFFFFF : gem.effect().value().getColor();
	}

	@Override
	public int power(ItemStack stack) {
		Gem gem = stack.get(ModComponents.GEM);
		return gem == null ? 1 : gem.infusion();
	}

	/** The gem's own tooltip words. */
	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		List<Component> out = new ArrayList<>();
		Gem gem = stack.get(ModComponents.GEM);
		if (gem != null) {
			gem.lines(out::add, "");
		}
		return out;
	}

	/** With Infused Gems installed, the standalone gem-base payments are off. */
	@Override
	public boolean replacesStandalonePayments() {
		return true;
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		Gem gem = stack.get(ModComponents.GEM);
		if (gem == null) {
			return;
		}
		long until = level.getGameTime() + context.effectDurationTicks();
		for (LivingEntity entity : context.livingEntities()) {
			GemBeacons.receive(entity, context.pos(), gem, context.effectivePower(), until);
		}
	}
}
