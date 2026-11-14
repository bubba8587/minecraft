// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.loot;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Mobs in {@code #bubbas_reworked_mobs:grabs_loot} taking whatever is lying around: items on loot displays (the
 * block goes) and loose item entities. Any item, so loot rooms can put specific things in their hands. Used by the
 * waking grab (sleepers) and by {@link GrabLootGoal}. Shared code, not undead-specific (specs/undead.md
 * "Grabbing what's lying around").
 */
public final class LootGrabbing {
	/** A mob that wakes empty-handed grabs something this close at once. */
	public static final double WAKE_RANGE = 3.0;
	/** An idle empty-handed mob notices a display or loose item this close and walks to it. */
	public static final double NOTICE_RANGE = 8.0;

	private LootGrabbing() {
	}

	public static boolean grabsLoot(Mob mob) {
		return mob.is(ModTags.GRABS_LOOT);
	}

	/** Weapons a waking mob reaches for before anything else ({@code #preferred_on_wake}). */
	public static boolean isWakeWeapon(ItemStack stack) {
		return stack.is(ModTags.PREFERRED_ON_WAKE);
	}

	/**
	 * Waking grab: the nearest weapon within {@link #WAKE_RANGE} (display first, then loose), else the nearest
	 * anything (display first, then loose). Does nothing if the hand is full or the mob is not in the tag.
	 *
	 * @return whether it took something
	 */
	public static boolean grabOnWake(ServerLevel level, Mob mob) {
		if (!mob.getMainHandItem().isEmpty() || !grabsLoot(mob)) {
			return false;
		}
		return grabNearest(level, mob, WAKE_RANGE, LootGrabbing::isWakeWeapon) || grabNearest(level, mob, WAKE_RANGE, stack -> true);
	}

	private static boolean grabNearest(ServerLevel level, Mob mob, double range, Predicate<ItemStack> wanted) {
		BlockPos display = LootDisplays.findNearest(level, mob.position(), range, d -> wanted.test(d.getItem()));
		if (display != null) {
			return takeDisplay(level, mob, display);
		}
		ItemEntity item = nearestItem(level, mob, range, wanted, null);
		return item != null && takeItem(mob, item);
	}

	/** The nearest live item entity within {@code range} matching {@code wanted}, skipping {@code skip}. */
	static @Nullable ItemEntity nearestItem(ServerLevel level, Mob mob, double range, Predicate<ItemStack> wanted, @Nullable ItemEntity skip) {
		double rangeSqr = range * range;
		Optional<ItemEntity> found = level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(range),
				item -> item != skip && item.isAlive() && !item.getItem().isEmpty() && item.distanceToSqr(mob) <= rangeSqr && wanted.test(item.getItem()))
				.stream().min(Comparator.comparingDouble(item -> item.distanceToSqr(mob)));
		return found.orElse(null);
	}

	/** Takes the item off the display at {@code pos} into the main hand (the block goes). */
	public static boolean takeDisplay(ServerLevel level, Mob mob, BlockPos pos) {
		ItemStack stack = LootDisplays.takeForMob(level, pos);
		if (stack.isEmpty()) {
			return false;
		}
		hold(mob, stack);
		return true;
	}

	/** Picks one of a loose item up into the main hand (with vanilla's pickup animation). */
	public static boolean takeItem(Mob mob, ItemEntity item) {
		if (!item.isAlive() || item.getItem().isEmpty()) {
			return false;
		}
		mob.take(item, 1);
		hold(mob, item.getItem().split(1));
		if (item.getItem().isEmpty()) {
			item.discard();
		}
		return true;
	}

	/** Into the main hand, always dropped on death (skeletons re-pick their attack goal through onEquipItem). */
	private static void hold(Mob mob, ItemStack stack) {
		mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
		mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
	}
}
