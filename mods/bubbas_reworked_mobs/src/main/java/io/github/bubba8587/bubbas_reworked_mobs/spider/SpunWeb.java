// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * A spun web: a
 * tangle of thick, heavy strands stretched diagonally between many random anchor points on the blocks round a site
 * (ceiling, walls, trunks, branches, ground), criss-crossing through the space at different angles. Drawn on the
 * client by {@code SpunWebRenderer} (thick white strands with a faint sheen and a slight sag; torn ones hang as loose
 * ends). Seeded from the site, so the same site gives the same tangle. No blocks (B15): a nest's or a larder's cobweb
 * blocks are the core, this is the span round it.
 * <ul>
 * <li>Moving through the strands sticks you like a cobweb block does; spiders aren't slowed.</li>
 * <li>Hits tear it ({@link SpiderTuning#WEB_HITS_TO_CUT} bring it down); shears cut it cleanly for string; fire burns it.</li>
 * <li>It tears a little more as it ages.</li>
 * </ul>
 * Cost: one entity box query a tick over its own box; nothing else server-side.
 */
public class SpunWeb extends Entity {
	/** The strands, hub-relative, as "x1,y1,z1,x2,y2,z2;..." (sent once, small). */
	private static final EntityDataAccessor<String> DATA_STRANDS = SynchedEntityData.defineId(SpunWeb.class, EntityDataSerializers.STRING);
	/** How torn: 0 fresh to 1 in tatters. */
	private static final EntityDataAccessor<Float> DATA_TEAR = SynchedEntityData.defineId(SpunWeb.class, EntityDataSerializers.FLOAT);

	/** One strand between two anchor points, offsets from the entity. */
	public record Strand(Vec3 a, Vec3 b) {
		public double length() {
			return a.distanceTo(b);
		}
	}

	private List<Strand> strandCache = List.of();
	private String strandCacheKey = "";
	private AABB span = null;
	private int hits;
	private long born = -1;

	public SpunWeb(EntityType<? extends SpunWeb> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_STRANDS, "");
		builder.define(DATA_TEAR, 0.0F);
	}

	// --- shape --------------------------------------------------------------------------------------------

	public List<Strand> strands() {
		String key = entityData.get(DATA_STRANDS);
		if (!key.equals(strandCacheKey)) {
			List<Strand> list = new ArrayList<>();
			for (String part : key.split(";")) {
				String[] v = part.split(",");
				if (v.length == 6) {
					list.add(new Strand(new Vec3(Float.parseFloat(v[0]), Float.parseFloat(v[1]), Float.parseFloat(v[2])),
							new Vec3(Float.parseFloat(v[3]), Float.parseFloat(v[4]), Float.parseFloat(v[5]))));
				}
			}
			strandCache = List.copyOf(list);
			strandCacheKey = key;
			span = null;
			setBoundingBox(makeBoundingBox(position()));
		}
		return strandCache;
	}

	public void setStrands(List<Strand> strands) {
		StringBuilder sb = new StringBuilder();
		for (Strand s : strands) {
			if (!sb.isEmpty()) {
				sb.append(';');
			}
			sb.append(String.format(Locale.ROOT, "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f", s.a.x, s.a.y, s.a.z, s.b.x, s.b.y, s.b.z));
		}
		entityData.set(DATA_STRANDS, sb.toString());
		strands();
	}

	public float tear() {
		return entityData.get(DATA_TEAR);
	}

	public void setTear(float tear) {
		entityData.set(DATA_TEAR, Math.max(0.0F, Math.min(1.0F, tear)));
	}

	/** A seed for the look (which strands tear, how they hang), the same on both sides. */
	public int lookSeed() {
		return getUUID().hashCode();
	}

	/** The box round all its strands (world space). */
	public AABB span() {
		if (span == null) {
			span = strandBox(position(), strands()).inflate(0.4);
		}
		return span;
	}

	private static AABB strandBox(Vec3 pos, List<Strand> strands) {
		AABB box = new AABB(pos, pos);
		for (Strand s : strands) {
			box = box.minmax(new AABB(pos.add(s.a), pos.add(s.b)));
		}
		return box;
	}

	@Override
	protected AABB makeBoundingBox(Vec3 pos) {
		// Called from Entity's constructor too, before this class's fields exist.
		if (strandCache == null || strandCache.isEmpty()) {
			return super.makeBoundingBox(pos);
		}
		// Pickable over its whole span, so a swing at the strands hits it.
		return strandBox(pos, strandCache).inflate(0.15);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_STRANDS.equals(accessor)) {
			strands();
		}
	}

	// --- building ----------------------------------------------------------------------------------------

	/**
	 * Strings a tangle round {@code centre}, seeded by {@code seed}: rays in {@code rays} random directions (weighted by
	 * {@code upBias}: 0 any way, 1 mostly upward) find anchor points on blocks within {@code maxReach}; {@code extra}
	 * points are anchors too. Then up to {@code strandCount} strands join pairs of anchors that are at least
	 * {@link SpiderTuning#WEB_STRAND_MIN} apart, have a clear line between them, and pass through the space near the
	 * centre (so they cross each other). Returns the web, added to the level, or null if fewer than 3 strands fit.
	 */
	public static SpunWeb spin(ServerLevelAccessor level, Vec3 centre, long seed, int rays, double upBias, double maxReach,
			List<Vec3> extra, int strandCount, float tear) {
		return spin(level, centre, seed, rays, upBias, maxReach, extra, strandCount, tear, 0);
	}

	/**
	 * The same, and ties off up to {@code tieOffs} of its busiest anchors with a vanilla cobweb block (in the air cell
	 * just off the surface), so the strands meet the world in Minecraft's own web.
	 */
	public static SpunWeb spin(ServerLevelAccessor level, Vec3 centre, long seed, int rays, double upBias, double maxReach,
			List<Vec3> extra, int strandCount, float tear, int tieOffs) {
		Random rnd = new Random(seed);
		List<Vec3> anchors = new ArrayList<>(extra);
		for (int i = 0; i < rays; i++) {
			double y = rnd.nextDouble() * 2.0 - 1.0;
			y = y * (1.0 - upBias) + upBias * Math.abs(y);
			double az = rnd.nextDouble() * Math.PI * 2.0;
			double h = Math.sqrt(Math.max(0.0, 1.0 - y * y));
			Vec3 dir = new Vec3(Math.cos(az) * h, y, Math.sin(az) * h);
			Vec3 end = centre.add(dir.scale(maxReach));
			BlockHitResult hit = level.clip(new ClipContext(centre, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
			if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(centre) > 1.0) {
				anchors.add(hit.getLocation());
			}
		}
		// Candidate strands: long, clear, passing near the centre.
		record Candidate(Vec3 a, Vec3 b, double score) {
		}
		List<Candidate> candidates = new ArrayList<>();
		for (int i = 0; i < anchors.size(); i++) {
			for (int j = i + 1; j < anchors.size(); j++) {
				Vec3 a = anchors.get(i);
				Vec3 b = anchors.get(j);
				double len = a.distanceTo(b);
				if (len < SpiderTuning.WEB_STRAND_MIN) {
					continue;
				}
				double pass = distanceToSegment(centre, a, b);
				if (pass > maxReach * 0.55) {
					continue;
				}
				if (!clear(level, a, b, centre)) {
					continue;
				}
				candidates.add(new Candidate(a, b, len * (1.0 + rnd.nextDouble()) / (1.0 + pass)));
			}
		}
		candidates.sort((p, q) -> Double.compare(q.score, p.score));
		List<Strand> strands = new ArrayList<>();
		int[] uses = new int[anchors.size()];
		for (Candidate c : candidates) {
			if (strands.size() >= strandCount) {
				break;
			}
			int ia = anchors.indexOf(c.a);
			int ib = anchors.indexOf(c.b);
			// Spread strands over many anchors rather than fanning from one.
			if (uses[ia] >= 3 || uses[ib] >= 3) {
				continue;
			}
			uses[ia]++;
			uses[ib]++;
			strands.add(new Strand(c.a.subtract(centre), c.b.subtract(centre)));
		}
		if (strands.size() < 3) {
			return null;
		}
		SpunWeb web = Spiders.SPUN_WEB.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (web == null) {
			return null;
		}
		web.setPos(centre.x, centre.y, centre.z);
		web.setStrands(strands);
		web.setTear(tear);
		level.addFreshEntity(web);
		if (tieOffs > 0) {
			List<Integer> order = new ArrayList<>();
			for (int i = 0; i < anchors.size(); i++) {
				if (uses[i] > 0) {
					order.add(i);
				}
			}
			order.sort((p, q) -> Integer.compare(uses[q], uses[p]));
			int tied = 0;
			for (int i : order) {
				if (tied >= tieOffs) {
					break;
				}
				Vec3 a = anchors.get(i);
				BlockPos cell = BlockPos.containing(a.add(centre.subtract(a).normalize().scale(0.3)));
				if (level.getBlockState(cell).isAir()) {
					level.setBlock(cell, Blocks.COBWEB.defaultBlockState(), 3);
					tied++;
				}
			}
		}
		return web;
	}

	/** Nothing solid on the straight line between two anchors (each pulled a little off its surface). */
	private static boolean clear(ServerLevelAccessor level, Vec3 a, Vec3 b, Vec3 centre) {
		Vec3 a1 = a.add(centre.subtract(a).normalize().scale(0.15));
		Vec3 b1 = b.add(centre.subtract(b).normalize().scale(0.15));
		return level.clip(new ClipContext(a1, b1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))
				.getType() == HitResult.Type.MISS;
	}

	private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double t = Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / ab.lengthSqr()));
		return p.distanceTo(a.add(ab.scale(t)));
	}

	// --- tick: slowing, fire, ageing --------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (born < 0) {
			born = level.getGameTime();
		}
		List<Strand> strands = strands();
		if (strands.isEmpty()) {
			discard();
			return;
		}
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, span(), EntitySelector.NO_SPECTATORS.and(e -> !(e instanceof Spider)))) {
			if (touching(e)) {
				// Exactly vanilla's cobweb (WebBlock.entityInside), Weaving included.
				e.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), e.hasEffect(net.minecraft.world.effect.MobEffects.WEAVING)
						? SpiderTuning.WEB_STUCK_WEAVING : SpiderTuning.WEB_STUCK);
			}
		}
		if ((tickCount + getId()) % 20 == 0) {
			if (fireNear(level, strands)) {
				burn(level);
				return;
			}
			float age = (level.getGameTime() - born) / (float) SpiderTuning.WEB_AGE_TICKS_TO_TATTERS;
			float target = Math.min(SpiderTuning.WEB_AGE_TEAR_MAX, age * SpiderTuning.WEB_AGE_TEAR_MAX) + hits * SpiderTuning.WEB_TEAR_PER_HIT;
			if (target > tear() + 0.01F) {
				setTear(target);
			}
		}
	}

	/** Its box touches a strand (within half a block of one). */
	public boolean touching(Entity e) {
		AABB box = e.getBoundingBox().inflate(0.25);
		Vec3 c = box.getCenter().subtract(position());
		double reach = Math.max(box.getXsize(), box.getYsize()) * 0.5 + 0.1;
		for (Strand s : strands()) {
			if (distanceToSegment(c, s.a, s.b) < reach) {
				return true;
			}
		}
		return false;
	}

	private boolean fireNear(ServerLevel level, List<Strand> strands) {
		if (fireAt(level, blockPosition())) {
			return true;
		}
		for (int i = 0; i < Math.min(4, strands.size()); i++) {
			Strand s = strands.get(i);
			if (fireAt(level, BlockPos.containing(position().add(s.a))) || fireAt(level, BlockPos.containing(position().add(s.b)))) {
				return true;
			}
		}
		return false;
	}

	private static boolean fireAt(ServerLevel level, BlockPos pos) {
		return level.getBlockStatesIfLoaded(new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))
				.anyMatch(state -> state.is(BlockTags.FIRE) || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA));
	}

	private void burn(ServerLevel level) {
		level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 20, 1.0, 1.0, 1.0, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.6F);
		discard();
	}

	// --- cutting ---------------------------------------------------------------------------------------------

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isAttackable() {
		return true;
	}

	@Override
	public boolean canBePickedFromInside() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (isRemoved()) {
			return false;
		}
		if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)
				|| source.getDirectEntity() != null && source.getDirectEntity().isOnFire()) {
			burn(level);
			return true;
		}
		if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
			discard();
			return true;
		}
		if (source.getEntity() instanceof Spider) {
			return false;
		}
		hits++;
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.5F, 1.8F);
		if (hits >= SpiderTuning.WEB_HITS_TO_CUT) {
			discard();
		} else {
			setTear(tear() + SpiderTuning.WEB_TEAR_PER_HIT);
		}
		return true;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.SHEARS)) {
			return InteractionResult.PASS;
		}
		if (level() instanceof ServerLevel level) {
			held.hurtAndBreak(1, player, hand);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 1.4F);
			// string drops at the cut (location is relative to the web)
			Vec3 cut = location == null ? position() : position().add(location);
			level.addFreshEntity(new ItemEntity(level, cut.x, cut.y, cut.z,
					new ItemStack(Items.STRING, SpiderTuning.WEB_SHEAR_STRING)));
			discard();
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 96.0 * 96.0;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putString("Strands", entityData.get(DATA_STRANDS));
		output.putFloat("Tear", tear());
		output.putInt("Hits", hits);
		output.putLong("Born", born);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(DATA_STRANDS, input.getStringOr("Strands", ""));
		setTear(input.getFloatOr("Tear", 0.0F));
		hits = input.getIntOr("Hits", 0);
		born = input.getLongOr("Born", -1L);
		strands();
	}
}
