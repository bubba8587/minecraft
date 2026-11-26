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

	/**
	 * Vanilla's rocket boost ({@code look*0.1 + (look*1.5 - v)*0.5}) scaled by power, except that along the look it only
	 * ever adds: already faster than the rocket's target speed, it keeps its speed and gets the push. Sideways it is vanilla's.
	 */
	public static Vec3 rocketBoost(Vec3 look, Vec3 movement, double power) {
		double along = movement.dot(look);
		Vec3 sideways = movement.subtract(look.scale(along));
		double alongBoost = 0.1 * power + Math.max(0.0, 1.5 * power - along) * 0.5;
		return look.scale(alongBoost).subtract(sideways.scale(0.5));
	}
}
