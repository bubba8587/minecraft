// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import java.util.EnumSet;
import java.util.Optional;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Fire ward: fire-fearing hostile mobs with no target keep away from lit fire. */
public final class FireWard extends Goal {
	/** Repelling blocks. */
	public static final TagKey<Block> REPELLENTS = TagKey.create(Registries.BLOCK, BubbasReworkedMobs.id("undead_repellents"));
	/** Mobs exempt from the ward. */
	public static final TagKey<EntityType<?>> UNAFRAID = TagKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id("unafraid_of_fire"));

	private final PathfinderMob mob;
	private Optional<BlockPos> fire = Optional.empty();
	private Vec3 away = Vec3.ZERO;

	public FireWard(PathfinderMob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	/** Whether the mob gets the ward goal. */
	public static boolean fearsFire(Mob mob) {
		return mob instanceof Enemy && mob instanceof PathfinderMob && !mob.fireImmune() && !mob.is(UNAFRAID);
	}

	/** Whether the block repels. */
	public static boolean repels(BlockState state) {
		return state.is(REPELLENTS) && (!(state.getBlock() instanceof CampfireBlock) || CampfireBlock.isLitCampfire(state));
	}

	/** Nearest repelling block. */
	public static Optional<BlockPos> nearest(ServerLevel level, Mob mob) {
		return level.findBlocksInBoxByManhattanDistance(mob.blockPosition(), UndeadTuning.FIRE_WARD_RADIUS, UndeadTuning.FIRE_WARD_VERTICAL)
				.filterState(FireWard::repels).findFirst();
	}

	@Override
	public boolean canUse() {
		if (mob.getTarget() != null || (mob.tickCount + mob.getId()) % 10 != 0 || !(mob.level() instanceof ServerLevel level)) {
			return false;
		}
		fire = nearest(level, mob);
		if (fire.isEmpty()) {
			return false;
		}
		Vec3 pos = DefaultRandomPos.getPosAway(mob, UndeadTuning.FIRE_WARD_RADIUS + 3, 4, Vec3.atCenterOf(fire.get()));
		if (pos == null) {
			return false;
		}
		away = pos;
		return true;
	}

	@Override
	public void start() {
		mob.getNavigation().moveTo(away.x, away.y, away.z, 1.1);
	}

	@Override
	public boolean canContinueToUse() {
		return mob.getTarget() == null && !mob.getNavigation().isDone();
	}
}
