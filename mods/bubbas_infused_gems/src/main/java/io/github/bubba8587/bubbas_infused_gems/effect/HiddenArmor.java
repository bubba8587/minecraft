// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/**
 * Owner (2026-11-15): the perfect Invisibility set makes its wearer fully invisible, armor included, by editing the
 * worn pieces the vanilla way: each piece's equippable points at an empty equipment asset (nothing renders, every other
 * equippable field kept), and the real asset is kept in {@code bubbas_infused_gems:hidden_asset}. Anything else
 * restores it: a piece off, a gem out, death, a non-armor slot, a container, the ground, a crafting output; and a marked
 * piece that isn't hidden by an active perfect set on its wearer restores itself on its next tick (self-heal).
 */
public final class HiddenArmor {
	public static final ResourceKey<EquipmentAsset> EMPTY = ResourceKey.create(EquipmentAssets.ROOT_ID, BubbasInfusedGems.id("empty"));

	private HiddenArmor() {
	}

	/** Whether this entity's armor should be hidden now: alive, wearing the perfect Invisibility set. */
	public static boolean active(LivingEntity entity) {
		if (!entity.isAlive()) {
			return false;
		}
		Balance.EffectData e = Balance.get().effect("invisibility");
		return e != null && GemValues.rawWorn(entity, "invisibility") >= e.wornNumber("full_at", 1.0) - 1e-6;
	}

	/** After the wearer's totals change (server): hide or restore every worn armor piece. */
	public static void update(LivingEntity entity) {
		boolean hide = active(entity);
		for (EquipmentSlot slot : GemTotals.ARMOR) {
			ItemStack stack = entity.getItemBySlot(slot);
			if (hide) {
				hide(stack);
			} else {
				restore(stack);
			}
		}
	}

	public static boolean isHidden(ItemStack stack) {
		return stack.has(ModComponents.HIDDEN_ASSET);
	}

	private static void hide(ItemStack stack) {
		Equippable eq = stack.get(DataComponents.EQUIPPABLE);
		if (stack.isEmpty() || isHidden(stack) || eq == null || eq.assetId().isEmpty()) {
			return;
		}
		stack.set(ModComponents.HIDDEN_ASSET, eq.assetId().get());
		stack.set(DataComponents.EQUIPPABLE, with(eq, EMPTY));
	}

	/** Puts back the real equipment asset and drops the marker; does nothing to an unmarked stack. */
	public static ItemStack restore(ItemStack stack) {
		ResourceKey<EquipmentAsset> real = stack.isEmpty() ? null : stack.get(ModComponents.HIDDEN_ASSET);
		if (real == null) {
			return stack;
		}
		Equippable eq = stack.get(DataComponents.EQUIPPABLE);
		if (eq != null) {
			stack.set(DataComponents.EQUIPPABLE, with(eq, real));
		}
		stack.remove(ModComponents.HIDDEN_ASSET);
		return stack;
	}

	/** Self-heal on a stack's tick: keep it hidden only while worn in armor by an entity with the set active. */
	public static void tick(ItemStack stack, Entity owner, @Nullable EquipmentSlot slot) {
		if (!isHidden(stack)) {
			return;
		}
		if (slot == null || slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR || !(owner instanceof LivingEntity living)
				|| owner.level().isClientSide() || living.getItemBySlot(slot) != stack || !active(living)) {
			if (!owner.level().isClientSide()) {
				restore(stack);
			}
		}
	}

	private static Equippable with(Equippable e, ResourceKey<EquipmentAsset> asset) {
		return new Equippable(e.slot(), e.equipSound(), java.util.Optional.of(asset), e.cameraOverlay(), e.allowedEntities(), e.dispensable(),
				e.swappable(), e.damageOnHurt(), e.equipOnInteract(), e.canBeSheared(), e.shearingSound());
	}
}
