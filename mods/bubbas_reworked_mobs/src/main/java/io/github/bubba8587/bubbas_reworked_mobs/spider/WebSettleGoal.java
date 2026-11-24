// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A small spider in a giant's territory settles at one of its subsidiary webs (specs/spiders.md "Territories"):
 * checked every SETTLE_CHECK_TICKS; it walks under the web, takes the web's hub as its home (vanilla home
 * restriction), and now and then climbs up and rests in the web. Not for giants or nest sleepers.
 */
public class WebSettleGoal extends Goal {
	/** A spider whose home is a territory web. */
	public static final String SETTLER_TAG = "bubbas_reworked_mobs.web_settler";
	/** A spider resting up in its web (no gravity; Stormie's movement steps aside). */
	public static final String RESTING_TAG = "bubbas_reworked_mobs.web_resting";

	private enum Phase {
		WALK, CLIMB, REST, DOWN
	}

	private final Spider spider;
	private @Nullable Phase phase;
	private @Nullable UUID web;
	private @Nullable Vec3 ground;
	private @Nullable Vec3 perch;
	private int nextCheck;
	private int ticks;
	private int restTicks;
	private boolean wantRest;

	public WebSettleGoal(Spider spider) {
		this.spider = spider;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
	}

	/** Whether {@code spider} is resting up in a web (held still there). */
	public static boolean resting(Spider spider) {
		return spider.entityTags().contains(RESTING_TAG);
	}

	/** The web it has settled at, if any (tests). */
	public @Nullable UUID web() {
		return web;
	}

	@Override
	public boolean canUse() {
		if (spider instanceof GiantSpider || spider instanceof NestSpider || !(spider.level() instanceof ServerLevel level)) {
			return false;
		}
		if (phase == null && resting(spider)) {
			unrest(); // saved while resting
		}
		if (spider.tickCount < nextCheck) {
			return false;
		}
		nextCheck = spider.tickCount + SpiderTuning.SETTLE_CHECK_TICKS;
		if (spider.getTarget() != null || spider.isPassenger() || spider.isVehicle()) {
			return false;
		}
		SpunWeb at = currentWeb(level);
		if (at == null) {
			unsettle();
			at = pick(level);
			if (at == null) {
				return false;
			}
			web = at.getUUID();
			spider.addTag(SETTLER_TAG);
			spider.setHomeTo(BlockPos.containing(at.position()), SpiderTuning.SETTLE_HOME_RADIUS);
		}
		ground = GiantSpider.standingSpotIn(level, spider.getRandom(), column(at, 0.5), true);
		if (ground == null) {
			ground = GiantSpider.standingSpotIn(level, spider.getRandom(), column(at, 2.5), true);
		}
		if (ground == null) {
			return false;
		}
		wantRest = spider.getRandom().nextFloat() < SpiderTuning.REST_CHANCE;
		if (!wantRest && horizontal(spider.position(), ground) < SpiderTuning.SETTLE_HOME_RADIUS * 0.5) {
			return false;
		}
		perch = new Vec3(ground.x, at.getY() - spider.getBbHeight() * 0.5, ground.z);
		phase = Phase.WALK;
		return true;
	}

	/** A box round the web's hub column, from the hub down past the ground. */
	private static AABB column(SpunWeb at, double half) {
		Vec3 hub = at.position();
		return new AABB(hub.x - half, hub.y - SpiderTuning.WANDER_DEPTH, hub.z - half, hub.x + half, hub.y - 0.5, hub.z + half);
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		return Math.hypot(a.x - b.x, a.z - b.z);
	}

	/** Its web if it still stands, loaded, inside a territory that's still claimed; null otherwise. */
	private @Nullable SpunWeb currentWeb(ServerLevel level) {
		if (web == null) {
			return null;
		}
		if (level.getEntity(web) instanceof SpunWeb w && !w.isRemoved()) {
			SpiderTerritories.Territory t = SpiderTerritories.at(level, BlockPos.containing(w.position()));
			if (t != null && t.webs().stream().anyMatch(s -> s.id().equals(web))) {
				return w;
			}
		}
		return null;
	}

	/** The nearest subsidiary web of the territory it stands in with room for another settler. */
	private @Nullable SpunWeb pick(ServerLevel level) {
		SpiderTerritories.Territory t = SpiderTerritories.at(level, spider.blockPosition());
		if (t == null) {
			return null;
		}
		List<SpunWeb> subs = SpiderTerritories.subsidiaryWebs(level, t);
		return subs.stream().filter(w -> settlers(level, w) < SpiderTuning.SETTLERS_PER_WEB)
				.min(Comparator.comparingDouble(w -> w.distanceToSqr(spider))).orElse(null);
	}

	private int settlers(ServerLevel level, SpunWeb w) {
		BlockPos hub = BlockPos.containing(w.position());
		return level.getEntitiesOfClass(Spider.class, w.span().inflate(SpiderTuning.SETTLE_HOME_RADIUS),
				s -> s != spider && s.entityTags().contains(SETTLER_TAG) && s.getHomePosition().equals(hub)).size();
	}

	private void unsettle() {
		if (web != null || spider.entityTags().contains(SETTLER_TAG)) {
			spider.removeTag(SETTLER_TAG);
			spider.clearHome();
		}
		web = null;
	}

	private void unrest() {
		spider.removeTag(RESTING_TAG);
		spider.setNoGravity(false);
		spider.resetFallDistance();
	}

	@Override
	public void start() {
		ticks = 0;
		spider.getNavigation().moveTo(ground.x, ground.y, ground.z, 0.8);
	}

	@Override
	public boolean canContinueToUse() {
		return phase != null && spider.getTarget() == null && spider.isAlive() && !spider.isPassenger();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ticks++;
		if (phase == null) {
			return;
		}
		switch (phase) {
			case WALK -> {
				if (horizontal(spider.position(), ground) < 0.8 || spider.getNavigation().isDone() && horizontal(spider.position(), ground) < 2.0) {
					spider.getNavigation().stop();
					if (wantRest && roomAt(perch)) {
						spider.addTag(RESTING_TAG);
						spider.setNoGravity(true);
						phase = Phase.CLIMB;
					} else {
						phase = null;
					}
				} else if (ticks > SpiderTuning.SETTLE_WALK_TICKS) {
					phase = null;
				} else if (spider.getNavigation().isDone() && ticks % 20 == 0) {
					spider.getNavigation().moveTo(ground.x, ground.y, ground.z, 0.8);
				}
			}
			case CLIMB -> {
				if (step(perch)) {
					phase = Phase.REST;
					restTicks = SpiderTuning.REST_TICKS_MIN + spider.getRandom().nextInt(SpiderTuning.REST_TICKS_MAX - SpiderTuning.REST_TICKS_MIN + 1);
				}
			}
			case REST -> {
				spider.setDeltaMovement(Vec3.ZERO);
				spider.setPos(perch);
				if (--restTicks <= 0 || currentWeb((ServerLevel) spider.level()) == null) {
					phase = Phase.DOWN;
				}
			}
			case DOWN -> {
				if (step(ground)) {
					phase = null;
				}
			}
		}
	}

	/** A straight move toward {@code to} at REST_CLIMB_SPEED; true once there. */
	private boolean step(Vec3 to) {
		spider.setDeltaMovement(Vec3.ZERO);
		spider.resetFallDistance();
		Vec3 d = to.subtract(spider.position());
		double len = d.length();
		if (len <= SpiderTuning.REST_CLIMB_SPEED) {
			spider.setPos(to);
			return true;
		}
		spider.setPos(spider.position().add(d.scale(SpiderTuning.REST_CLIMB_SPEED / len)));
		return false;
	}

	private boolean roomAt(Vec3 at) {
		AABB box = spider.getBoundingBox().move(at.subtract(spider.position()));
		return spider.level().noCollision(spider, box) && spider.level().getEntitiesOfClass(Spider.class, box, s -> s != spider).isEmpty();
	}

	@Override
	public void stop() {
		if (resting(spider)) {
			unrest();
		}
		phase = null;
		spider.getNavigation().stop();
	}
}
