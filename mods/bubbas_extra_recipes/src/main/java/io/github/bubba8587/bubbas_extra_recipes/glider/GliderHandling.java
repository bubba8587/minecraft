// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.glider;

import io.github.bubba8587.bubbas_extra_recipes.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The Glider's handling (specs/extra-recipes-mod.md). Starting values, tune by playtest. Lift and sink rate stay
 * vanilla. Everything here applies only while the chest slot holds our Glider; an elytra flies exactly as in vanilla.
 */
public final class GliderHandling {
	/** Vanilla blends the flight direction 10 % per tick toward where the player looks; the Glider 5 %. */
	public static final double VANILLA_TURN_BLEND = 0.1;
	public static final double GLIDER_TURN_BLEND = 0.03;

	/**
	 * A firework rocket pulls a gliding player toward 1.5 blocks/tick along the look direction (plus a 0.1 push);
	 * on the Glider both are scaled by this, so a rocket accelerates less and tops out lower.
	 */
	public static final double GLIDER_ROCKET_POWER = 0.6;

	private GliderHandling() {
	}

	public static boolean isWearingGlider(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GLIDER);
	}

	/**
	 * The velocity change vanilla's FireworkRocketEntity#tick adds each tick to the entity it is attached to, with the
	 * target speed and push scaled by {@code power} (1.0 = vanilla).
	 */
	public static Vec3 rocketBoost(Vec3 look, Vec3 movement, double power) {
		return new Vec3(
				look.x * 0.1 * power + (look.x * 1.5 * power - movement.x) * 0.5,
				look.y * 0.1 * power + (look.y * 1.5 * power - movement.y) * 0.5,
				look.z * 0.1 * power + (look.z * 1.5 * power - movement.z) * 0.5);
	}
}
