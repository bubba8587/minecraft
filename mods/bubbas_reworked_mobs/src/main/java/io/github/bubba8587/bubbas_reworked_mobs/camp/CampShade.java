// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Camp mobs and the sun (owner, 2026-11-22: camps have guaranteed shade spots and an underground room their mobs can go
 * in, but they burn otherwise). Camp mobs burn in daylight like any other. By day, a camp mob that burns in daylight:
 * <ul>
 * <li>paths around sunlit ground (vanilla's avoid-sun pathing, which skeletons already use), so its wandering keeps to
 * shade;</li>
 * <li>when it stands in the sun with no target (or is already on fire), walks to the nearest free shade spot of its
 * camp: under the lean-to, under or on a roofed tower, or down in the room.</li>
 * </ul>
 * By night it's free again. Run from the mob's AI step (mixin/CampMemberMixin), every {@link #EVERY} ticks.
 */
public final class CampShade {
	static final int EVERY = 10;
	private static final Map<Mob, BlockPos> HEADING = new WeakHashMap<>();

	private CampShade() {
	}

	public static void tick(ServerLevel level, Mob mob) {
		if ((level.getGameTime() + mob.getId()) % EVERY != 0 || !mob.is(EntityTypeTags.BURN_IN_DAYLIGHT)) {
			return;
		}
		boolean day = sunBurns(level, mob.blockPosition());
		if (mob.getNavigation() instanceof GroundPathNavigation ground) {
			ground.setAvoidSun(day);
		}
		if (!day) {
			HEADING.remove(mob);
			return;
		}
		BlockPos eye = BlockPos.containing(mob.getX(), mob.getEyeY(), mob.getZ());
		boolean exposed = level.canSeeSky(eye);
		if (!exposed) {
			HEADING.remove(mob);
			return;
		}
		if (mob.getTarget() != null && !mob.isOnFire()) {
			return; // fighting in the sun: its choice
		}
		CampSite site = siteOf(level, mob);
		if (site == null) {
			return;
		}
		BlockPos heading = HEADING.get(mob);
		if (heading == null || taken(level, mob, heading) || mob.getNavigation().isDone()) {
			heading = nearestFree(level, mob, site);
			if (heading == null) {
				return;
			}
			HEADING.put(mob, heading);
		}
		mob.getNavigation().moveTo(heading.getX() + 0.5, heading.getY(), heading.getZ() + 0.5, 1.2);
	}

	/** Whether the sun burns monsters here now (vanilla's rule, without the light-level dice). */
	public static boolean sunBurns(ServerLevel level, BlockPos pos) {
		return level.isBrightOutside() && level.environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, pos);
	}

	static @Nullable CampSite siteOf(ServerLevel level, Mob mob) {
		for (String tag : mob.entityTags()) {
			if (tag.startsWith(CampTuning.CAMP_TAG + ":")) {
				try {
					return CampSites.get(level, Long.parseLong(tag.substring(CampTuning.CAMP_TAG.length() + 1)));
				} catch (NumberFormatException ignored) {
					return null;
				}
			}
		}
		return null;
	}

	/** The nearest resolved shade spot of the camp nobody else stands in, or null. */
	static @Nullable BlockPos nearestFree(ServerLevel level, @Nullable Mob mob, CampSite site) {
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (BlockPos spot : site.shade) {
			BlockPos at = resolve(level, spot);
			if (at == null || (mob != null && taken(level, mob, at))) {
				continue;
			}
			double d = mob == null ? 0.0 : at.distToCenterSqr(mob.position());
			if (d < bestDist) {
				bestDist = d;
				best = at;
			}
		}
		return best;
	}

	private static boolean taken(ServerLevel level, Mob mob, BlockPos at) {
		return !level.getEntitiesOfClass(Mob.class, new AABB(at), other -> other != mob && other.entityTags().contains(CampTuning.CAMP_TAG)).isEmpty();
	}

	/**
	 * Where a mob can stand at this shade spot: the nearest height (the given one first, then up and down to 4 away)
	 * with something to stand on, two free blocks and no sky overhead; null if none (the world has changed there).
	 */
	public static @Nullable BlockPos resolve(ServerLevel level, BlockPos spot) {
		if (!level.isLoaded(spot)) {
			return null;
		}
		for (int i = 0; i <= 8; i++) {
			int dy = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2);
			BlockPos at = spot.above(dy);
			BlockState below = level.getBlockState(at.below());
			if (below.isFaceSturdy(level, at.below(), Direction.UP) || below.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
					|| below.getBlock() instanceof net.minecraft.world.level.block.StairBlock) {
				if (level.getBlockState(at).getCollisionShape(level, at).isEmpty() && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
						&& level.getFluidState(at).isEmpty() && !level.canSeeSky(at.above())) {
					return at;
				}
			}
		}
		return null;
	}
}
