// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.loot;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An idle, empty-handed mob in {@code #grabs_loot} walks to the nearest loot display (else loose item) within
 * {@link LootGrabbing#NOTICE_RANGE} and takes it. Never while it has a target: the chase wins (and the goal stops
 * the moment one appears). Add it at a low priority (below attack goals); it only needs {@link Mob}, so a later
 * mixin or event can add it to vanilla mobs too, and the tag check inside keeps it opt-in.
 * <p>
 * Cost: one search a second per idle empty-handed mob (the block entities of the few chunks in range plus one
 * entity query), one path per chosen target; targets it can't reach or gave up on are skipped for a while.
 */
public class GrabLootGoal extends Goal {
	private static final int SEARCH_INTERVAL = 20;
	private static final int TIMEOUT = 300;
	private static final int REPATH_INTERVAL = 20;
	private static final int SKIP_TICKS = 200;
	private static final double REACH_SQR = 1.75 * 1.75;

	private final Mob mob;
	private final double speed;
	private int nextSearch;
	private int startedAt;
	private int nextRepath;
	private @Nullable BlockPos display;
	private @Nullable ItemEntity item;
	private @Nullable Path path;
	/** Display positions and item entities given up on, with the tick they may be tried again. */
	private final Map<Object, Integer> skipped = new HashMap<>();

	public GrabLootGoal(Mob mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		this.nextSearch = mob.getRandom().nextInt(SEARCH_INTERVAL); // spread searches of mobs loaded together
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	private boolean idle() {
		return this.mob.getMainHandItem().isEmpty() && this.mob.getTarget() == null && !this.mob.isPassenger() && LootGrabbing.grabsLoot(this.mob);
	}

	@Override
	public boolean canUse() {
		if (this.mob.tickCount < this.nextSearch) {
			return false;
		}
		this.nextSearch = this.mob.tickCount + SEARCH_INTERVAL;
		if (!this.idle() || !(this.mob.onGround() || this.mob.isInLiquid())) {
			return false; // mid-air (just spawned, falling, jumping) a ground mob can't path yet: try next time, skip nothing
		}
		int now = this.mob.tickCount;
		this.skipped.values().removeIf(until -> until <= now);
		ServerLevel level = getServerLevel(this.mob);
		this.display = LootDisplays.findNearest(level, this.mob.position(), LootGrabbing.NOTICE_RANGE, d -> !this.skipped.containsKey(d.getBlockPos()));
		this.item = null;
		if (this.display == null) {
			this.item = LootGrabbing.nearestItem(level, this.mob, LootGrabbing.NOTICE_RANGE, stack -> true, null);
			if (this.item != null && this.skipped.containsKey(this.item)) {
				this.item = null; // nearest is one we gave up on; try again next search rather than scanning further
			}
		}
		if (this.display == null && this.item == null) {
			return false;
		}
		this.path = this.findPath();
		if (this.path == null) {
			this.skipTarget();
			return false;
		}
		return true;
	}

	private @Nullable Path findPath() {
		Path found = this.display != null ? this.mob.getNavigation().createPath(this.display, 1)
				: this.item != null ? this.mob.getNavigation().createPath(this.item, 1) : null;
		return found != null && found.canReach() ? found : null;
	}

	private void skipTarget() {
		Object target = this.display != null ? this.display : this.item;
		if (target != null) {
			this.skipped.put(target, this.mob.tickCount + SKIP_TICKS);
		}
		this.display = null;
		this.item = null;
	}

	private boolean targetValid() {
		if (this.display != null) {
			return LootDisplays.hasItemAt(this.mob.level(), this.display);
		}
		return this.item != null && this.item.isAlive() && !this.item.getItem().isEmpty();
	}

	@Override
	public boolean canContinueToUse() {
		return this.idle() && this.targetValid() && this.mob.tickCount - this.startedAt < TIMEOUT;
	}

	@Override
	public void start() {
		this.startedAt = this.mob.tickCount;
		this.nextRepath = this.mob.tickCount + REPATH_INTERVAL;
		this.mob.getNavigation().moveTo(this.path, this.speed);
	}

	@Override
	public void tick() {
		Vec3 at = this.display != null ? Vec3.atBottomCenterOf(this.display) : this.item != null ? this.item.position() : null;
		if (at == null) {
			return;
		}
		this.mob.getLookControl().setLookAt(at.x, at.y + 0.25, at.z);
		if (this.mob.distanceToSqr(at) <= REACH_SQR) {
			ServerLevel level = getServerLevel(this.mob);
			if (this.display != null) {
				LootGrabbing.takeDisplay(level, this.mob, this.display);
			} else if (this.item != null) {
				LootGrabbing.takeItem(this.mob, this.item);
			}
			return;
		}
		if (this.mob.getNavigation().isDone() && this.mob.tickCount >= this.nextRepath) {
			this.nextRepath = this.mob.tickCount + REPATH_INTERVAL;
			Path again = this.findPath();
			if (again == null) {
				this.skipTarget();
			} else {
				this.mob.getNavigation().moveTo(again, this.speed);
			}
		}
	}

	@Override
	public void stop() {
		if (this.targetValid() && this.mob.tickCount - this.startedAt >= TIMEOUT) {
			this.skipTarget();
		}
		this.mob.getNavigation().stop();
		this.display = null;
		this.item = null;
		this.path = null;
	}
}
