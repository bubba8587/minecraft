// dte:C6,B17,B18
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.List;
import java.util.Optional;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModComponents;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A Totem of Seeking in a beacon (specs/totems-artifacts.md): every compass held (either hand, by anyone, B18) in the
 * beacon's area points to the nearest structure of the totem's type, searched from the beacon. A pointed compass is a
 * vanilla compass with an untracked lodestone target (so it shimmers like a lodestone compass) plus
 * {@link ModComponents#SEEKING_BEACON_UNTIL}; {@code CompassItemMixin} turns it back into a plain compass when that
 * time passes, i.e. {@link TotemsTuning#SEEK_COMPASS_LIFETIME_TICKS} after it was last held in range. Compasses already
 * bound to a lodestone are left alone.
 */
public final class SeekingBeacon {
	private SeekingBeacon() {
	}

	/** One beacon pulse. Returns how many compasses were pointed (0 while the first search runs). */
	public static int apply(ServerLevel level, ItemStack totem, BlockPos beacon, List<LivingEntity> inArea) {
		Optional<SeekingTypes.SeekType> type = SeekingTypes.byId(totem.get(ModComponents.SEEKING_TYPE));
		if (type.isEmpty()) {
			return 0;
		}
		Optional<BlockPos> found = StructureSeeker.lookup(level, type.get(), beacon);
		if (found == null) {
			return 0;
		}
		long until = level.getGameTime() + TotemsTuning.SEEK_COMPASS_LIFETIME_TICKS;
		int pointed = 0;
		for (LivingEntity entity : inArea) {
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack stack = entity.getItemInHand(hand);
				if (stack.is(Items.COMPASS) && (!stack.has(DataComponents.LODESTONE_TRACKER) || stack.has(ModComponents.SEEKING_BEACON_UNTIL))) {
					TotemOfSeekingItem.pointAt(stack, level, found.orElse(null));
					stack.set(ModComponents.SEEKING_BEACON_UNTIL, until);
					pointed++;
				}
			}
		}
		return pointed;
	}

	/** A compass whose beacon pointing ran out: plain again. Called from the compass's inventory tick. */
	public static void lapse(ItemStack stack, ServerLevel level) {
		Long until = stack.get(ModComponents.SEEKING_BEACON_UNTIL);
		if (until != null && level.getGameTime() > until) {
			stack.remove(ModComponents.SEEKING_BEACON_UNTIL);
			stack.remove(DataComponents.LODESTONE_TRACKER);
		}
	}
}
