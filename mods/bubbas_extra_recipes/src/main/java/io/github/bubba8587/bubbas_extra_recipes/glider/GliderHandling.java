// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.glider;

import io.github.bubba8587.bubbas_extra_recipes.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** The Glider's handling. */
public final class GliderHandling {
	/** Glider turn blend per tick. */
	public static final double VANILLA_TURN_BLEND = 0.1;
	public static final double GLIDER_TURN_BLEND = 0.03;

	/** Glider rocket power. */
	public static final double GLIDER_ROCKET_POWER = 0.6;

	private GliderHandling() {
	}

	public static boolean isWearingGlider(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GLIDER);
	}

	/** Vanilla's rocket boost, scaled by power. */
	public static Vec3 rocketBoost(Vec3 look, Vec3 movement, double power) {
		return new Vec3(
				look.x * 0.1 * power + (look.x * 1.5 * power - movement.x) * 0.5,
				look.y * 0.1 * power + (look.y * 1.5 * power - movement.y) * 0.5,
				look.z * 0.1 * power + (look.z * 1.5 * power - movement.z) * 0.5);
	}
}
