// dte:C6,B17,B18
package io.github.bubba8587.bubbas_totems_artifacts.gathering;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.mixin.ItemEntityAccessor;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The Totem of Gathering. */
public final class Gathering {
	private Gathering() {
	}

	/** Whether item may be pulled toward holder. */
	static boolean pullable(ItemEntity item, @Nullable LivingEntity holder) {
		if (!item.isAlive() || item.getItem().isEmpty()) {
			return false;
		}
		int delay = ((ItemEntityAccessor) item).bta$pickupDelay();
		if (delay >= 32767) {
			return false;
		}
		return holder == null || delay <= 0 || item.getOwner() != holder;
	}

	/** One tick of a held totem. */
	public static int pull(ServerLevel level, LivingEntity holder) {
		double r = TotemsTuning.GATHER_RADIUS;
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, holder.getBoundingBox().inflate(r),
				EntitySelector.withinDistance(holder.getX(), holder.getY(), holder.getZ(), r).and(e -> pullable((ItemEntity) e, holder)));
		int pulled = 0;
		for (ItemEntity item : items) {
			if (pulled++ >= TotemsTuning.GATHER_MAX_ITEMS_PER_TICK) {
				break;
			}
			Vec3 delta = new Vec3(holder.getX() - item.getX(), holder.getY() + holder.getEyeHeight() / 2.0 - item.getY(), holder.getZ() - item.getZ());
			double distance = delta.length();
			if (distance > 1.0E-3) {
				double speed = TotemsTuning.GATHER_SPEED * Math.min(1.0, distance);
				item.setDeltaMovement(delta.scale(speed / distance).add(0.0, 0.04, 0.0));
				item.needsSync = true;
			}
		}
		return Math.min(pulled, TotemsTuning.GATHER_MAX_ITEMS_PER_TICK);
	}
}
