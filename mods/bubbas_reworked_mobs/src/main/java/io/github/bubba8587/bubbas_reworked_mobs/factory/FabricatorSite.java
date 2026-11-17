// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * One Ancient Fabricator (specs/mob-factory.md): where it is (origin = first air above the middle of the deck), how
 * it's turned, its seed and mob, and (beside a city) where the city's centre is, relative to it. Everything else
 * (the design, the world positions of its working parts) follows from these. Value-equal by origin.
 */
public final class FabricatorSite {
	private final BlockPos origin;
	private final Rotation rotation;
	private final long seed;
	private final FactoryDesign.Mob mob;
	private final @Nullable BlockPos cityLocal;
	private @Nullable FactoryDesign design;

	public FabricatorSite(BlockPos origin, Rotation rotation, long seed, FactoryDesign.Mob mob, @Nullable BlockPos cityLocal) {
		this.origin = origin.immutable();
		this.rotation = rotation;
		this.seed = seed;
		this.mob = mob;
		this.cityLocal = cityLocal;
	}

	public BlockPos origin() {
		return this.origin;
	}

	public Rotation rotation() {
		return this.rotation;
	}

	public long seed() {
		return this.seed;
	}

	public FactoryDesign.Mob mob() {
		return this.mob;
	}

	public @Nullable BlockPos cityLocal() {
		return this.cityLocal;
	}

	/** The design (worked out once per site and kept in a small shared cache). */
	public FactoryDesign design() {
		if (this.design == null) {
			this.design = DESIGNS.get(this);
		}
		return this.design;
	}

	public BlockPos world(BlockPos local) {
		return FactoryDesign.toWorld(this.origin, local, this.rotation);
	}

	public BlockPos pad() {
		return world(design().pad());
	}

	public BlockPos control() {
		return world(design().control());
	}

	public BlockPos dropPoint() {
		return world(design().dropPoint());
	}

	public BlockPos dripperTip() {
		return world(design().dripperTip());
	}

	public BlockPos spawner() {
		return world(design().spawner());
	}

	public BlockPos floorBeacon() {
		return world(design().floorBeacon());
	}

	public BlockPos ceilingBeacon() {
		return world(design().ceilingBeacon());
	}

	public BlockPos wardenSpot() {
		return world(design().wardenSpot());
	}

	public BlockPos jukebox() {
		return world(design().jukebox());
	}

	public List<BlockPos> timerLamps() {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : design().timerLamps()) {
			out.add(world(p));
		}
		return out;
	}

	/** The pad's 5 x 5 top, 3 high: what stands here rides the teleporter. */
	public AABB padArea() {
		BlockPos p = pad();
		return new AABB(p.getX() - 2, p.getY(), p.getZ() - 2, p.getX() + 3, p.getY() + 3, p.getZ() + 3);
	}

	/** The cavern round the deck, for counting mobs there. */
	public AABB deckArea() {
		BlockPos p = pad();
		return new AABB(p.getX() - 12, p.getY() - 3, p.getZ() - 14, p.getX() + 13, p.getY() + 8, p.getZ() + 12);
	}

	/** The world box the whole build covers (cavern, office, tunnel). */
	public BoundingBox box() {
		return design().box(this.origin, this.rotation);
	}

	/** World yaw (degrees) for local direction -x: the Warden's window looks that way. */
	public float wardenYaw() {
		return 90.0F + 90.0F * this.rotation.ordinal();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof FabricatorSite s && s.origin.equals(this.origin);
	}

	@Override
	public int hashCode() {
		return this.origin.hashCode();
	}

	@Override
	public String toString() {
		return "Ancient Fabricator (%s) at %d %d %d".formatted(this.mob.id(), this.origin.getX(), this.origin.getY(), this.origin.getZ());
	}

	/** Designs by site: about 54,000 blocks each, so only the last few are kept. Thread-safe (worldgen workers). */
	private static final class DesignCache {
		private final Map<String, FactoryDesign> map = new LinkedHashMap<>(16, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, FactoryDesign> eldest) {
				return size() > 6;
			}
		};

		synchronized FactoryDesign get(FabricatorSite site) {
			String key = site.seed + "/" + site.mob + "/" + Objects.toString(site.cityLocal);
			FactoryDesign d = this.map.get(key);
			if (d == null) {
				d = FactoryDesign.design(site.seed, site.mob, site.cityLocal);
				this.map.put(key, d);
			}
			return d;
		}
	}

	private static final DesignCache DESIGNS = new DesignCache();
}
