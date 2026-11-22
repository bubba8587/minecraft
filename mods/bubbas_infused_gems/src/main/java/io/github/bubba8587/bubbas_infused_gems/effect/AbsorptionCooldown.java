// dte:C4,B19
package io.github.bubba8587.bubbas_infused_gems.effect;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.SocketedGems;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * The worn Absorption shield's refill wait, shown with vanilla's item cooldown sweep (B19: show any rhythm with
 * vanilla UI; owner, 2026-11-22: "wind charges and totems have an in-inventory-cell cooldown timer, use that").
 * Every armor piece carrying an Absorption gem (as its main effect or its bonus) shares one cooldown group, and a
 * player's group cooldown runs exactly while the shield waits to refill: after damage, or after an equip inside the
 * equip-fill throttle. Players only (mobs have no item cooldowns; their refill is unchanged). Side effect, kept on
 * purpose: while it runs, those pieces can't be right-click equipped, which suits the throttle against swapping gear.
 */
public final class AbsorptionCooldown {
	public static final Identifier GROUP = BubbasInfusedGems.id("absorption_refill");

	private AbsorptionCooldown() {
	}

	/** Whether this is armor with an Absorption gem in it (read by the cooldown-group mixin, client and server). */
	public static boolean carriesAbsorption(ItemStack stack) {
		SocketedGems gems = stack.get(ModComponents.SOCKETED_GEMS);
		if (gems == null || gems.gems().isEmpty()) {
			return false;
		}
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || !equippable.slot().isArmor()) {
			return false;
		}
		for (Gem gem : gems.gems()) {
			if (gem.effect().is(MobEffects.ABSORPTION) || gem.other().map(r -> r.effect().is(MobEffects.ABSORPTION)).orElse(false)) {
				return true;
			}
		}
		return false;
	}

	/** The shield now waits {@code ticks} to refill: show it on the player's Absorption armor. */
	public static void start(LivingEntity entity, int ticks) {
		if (ticks > 0 && entity instanceof ServerPlayer player && GemValues.rawWorn(entity, "absorption") > 0.0) {
			player.getCooldowns().addCooldown(GROUP, ticks);
		}
	}
}
