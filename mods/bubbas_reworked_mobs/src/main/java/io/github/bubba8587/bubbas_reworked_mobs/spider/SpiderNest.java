// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The brain of one cave nest (specs/spiders.md "Cave nests"): an invisible, untouchable entity at the nest's centre,
 * just under the ceiling. It does what a sleeping block can't: every {@link SpiderTuning#NEST_SCAN_TICKS} ticks one
 * box query for players below it, and when one is there it drops the sleeper hanging closest above them. Now and then
 * (only with nobody near) it hangs a new sleeper, up to the cap. It goes away when its cobwebs are mined out.
 * Not a mob: no AI, no mob cap, nothing rendered. Cost per nest: one entity box query every 4 ticks.
 */
public class SpiderNest extends Entity {
	/** How deep below the nest it looks (the room's height, measured at worldgen; generous for old nests). */
	private int depth = 32;
	private int cooldown;

	public SpiderNest(EntityType<? extends SpiderNest> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
		setInvisible(true);
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (cooldown > 0) {
			cooldown--;
		}
		int phase = (int) ((level.getGameTime() + getId()) % SpiderTuning.NEST_SCAN_TICKS);
		if (phase == 0 && cooldown == 0) {
			scan(level);
		}
		if ((level.getGameTime() + getId()) % SpiderTuning.REFILL_CHECK_TICKS == 0) {
			upkeep(level);
		}
	}

	/** The column under the nest where a player sets off a drop. */
	public AABB triggerBox() {
		double r = SpiderTuning.TRIGGER_RADIUS;
		return new AABB(getX() - r, getY() - depth, getZ() - r, getX() + r, getY(), getZ() + r);
	}

	private AABB nestBox() {
		double r = SpiderTuning.NEST_RADIUS + 1.0;
		return new AABB(getX() - r, getY() - 2.5, getZ() - r, getX() + r, getY() + 1.0, getZ() + r);
	}

	public List<NestSpider> sleepers(ServerLevel level) {
		return level.getEntitiesOfClass(NestSpider.class, nestBox(), NestSpider::sleeping);
	}

	private void scan(ServerLevel level) {
		List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, triggerBox(), EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.ENTITY_STILL_ALIVE));
		if (players.isEmpty()) {
			return;
		}
		ServerPlayer player = players.getFirst();
		NestSpider best = null;
		double bestDist = Double.MAX_VALUE;
		for (NestSpider spider : sleepers(level)) {
			double dx = spider.getX() - player.getX();
			double dz = spider.getZ() - player.getZ();
			double d = dx * dx + dz * dz;
			if (d < bestDist) {
				bestDist = d;
				best = spider;
			}
		}
		if (best != null) {
			best.drop(player);
			cooldown = SpiderTuning.DROP_COOLDOWN_TICKS;
		}
	}

	private void upkeep(ServerLevel level) {
		if (!enoughWebs(level)) {
			discard();
			return;
		}
		if (sleepers(level).size() >= SpiderTuning.NEST_SLEEPER_CAP || random.nextFloat() >= SpiderTuning.REFILL_CHANCE) {
			return;
		}
		if (level.getNearestPlayer(getX(), getY(), getZ(), SpiderTuning.REFILL_QUIET_RADIUS, false) != null) {
			return;
		}
		hang(level, this, random.nextInt());
	}

	private boolean enoughWebs(ServerLevel level) {
		BlockPos centre = blockPosition();
		int r = SpiderTuning.NEST_RADIUS;
		return level.findBlocksIn(centre.offset(-r, -2, -r), centre.offset(r, 1, r)).filterState(state -> state.is(Blocks.COBWEB))
				.atLeastMatched(SpiderTuning.NEST_MIN_WEBS_ALIVE);
	}

	/**
	 * Hangs a new sleeper in this nest: under a ceiling block within the nest's radius (the first free spot from a
	 * seeded pick). Returns it, or null if there was no room.
	 */
	public static @Nullable NestSpider hang(Level level, SpiderNest nest, int seed) {
		BlockPos centre = nest.blockPosition();
		java.util.Random rnd = new java.util.Random(seed);
		for (int tries = 0; tries < 8; tries++) {
			int dx = rnd.nextInt(3) - 1;
			int dz = rnd.nextInt(3) - 1;
			BlockPos at = centre.offset(dx, 0, dz);
			// Walk up to the ceiling.
			int up = 0;
			while (up < 3 && !level.getBlockState(at.above()).isCollisionShapeFullBlock(level, at.above())) {
				at = at.above();
				up++;
			}
			if (!level.getBlockState(at.above()).isCollisionShapeFullBlock(level, at.above())) {
				continue;
			}
			Vec3 anchor = new Vec3(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
			AABB room = new AABB(anchor.x - 0.6, anchor.y - 1.0, anchor.z - 0.6, anchor.x + 0.6, anchor.y, anchor.z + 0.6);
			if (!level.getEntitiesOfClass(NestSpider.class, room).isEmpty()) {
				continue;
			}
			NestSpider spider = Spiders.NEST_SPIDER.create(level, EntitySpawnReason.STRUCTURE);
			if (spider == null) {
				return null;
			}
			spider.setYRot(rnd.nextFloat() * 360.0F);
			spider.setYBodyRot(spider.getYRot());
			spider.setYHeadRot(spider.getYRot());
			spider.hangFrom(anchor);
			level.addFreshEntity(spider);
			return spider;
		}
		return null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("Depth", depth);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		depth = input.getIntOr("Depth", 32);
	}
}
