// dte:C6,B17,B18
package io.github.bubba8587.bubbas_totems_artifacts.herding;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;

/** The Totem of Herding. */
public final class Herding {
	private Herding() {
	}

	/** Whether holder holds a Totem of Herding in either hand. */
	public static boolean holds(LivingEntity holder) {
		return holder.getMainHandItem().is(ModItems.TOTEM_OF_HERDING) || holder.getOffhandItem().is(ModItems.TOTEM_OF_HERDING);
	}

	/** Whether mob is livestock the totem herds. */
	public static boolean isLivestock(Mob mob) {
		return mob instanceof Animal && !(mob instanceof TamableAnimal pet && pet.isTame());
	}

	/** Pasture edge for pathfinding: blocks livestock from leaving. */
	public static boolean keepsIn(Mob mob, int x, int y, int z) {
		if (!(mob.level() instanceof ServerLevel level) || !isLivestock(mob) || mob.isLeashed()) {
			return false;
		}
		for (TotemAreas.Area area : TotemAreas.all(level, TotemAreas.Kind.HERDING)) {
			if (area.box().contains(mob.position()) && !area.box().contains(x + 0.5, y + 0.5, z + 0.5)) {
				return level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), TotemsTuning.HERD_LEAD_OUT_RANGE,
						EntitySelector.NO_SPECTATORS.and(p -> p instanceof LivingEntity living && holds(living))) == null;
			}
		}
		return false;
	}
}
