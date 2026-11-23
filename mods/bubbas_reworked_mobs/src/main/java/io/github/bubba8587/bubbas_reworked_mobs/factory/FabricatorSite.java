// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** One Ancient Fabricator site: origin, turn, seed, mob and city offset. */
public final class FabricatorSite {
	private final BlockPos origin;
	private final Rotation rotation;
	private final long seed;
	private final FactoryDesign.Mob mob;
	private final @Nullable BlockPos cityLocal;
	private volatile @Nullable Parts parts;

	/** World positions of the working parts. */
	private record Parts(BlockPos pad, BlockPos control, BlockPos dropPoint, BlockPos dripperTip, BlockPos spawner, BlockPos floorBeacon,
			BlockPos ceilingBeacon, BlockPos wardenSpot, BlockPos jukebox, List<BlockPos> timerLamps, BoundingBox box) {
	}

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

	/** The design, from the shared cache. */
	public FactoryDesign design() {
		return DESIGNS.get(this);
	}

	private Parts parts() {
		Parts p = this.parts;
		if (p == null) {
			FactoryDesign d = design();
			List<BlockPos> lamps = new ArrayList<>();
			for (BlockPos lamp : d.timerLamps()) {
				lamps.add(world(lamp));
			}
			p = new Parts(world(d.pad()), world(d.control()), world(d.dropPoint()), world(d.dripperTip()), world(d.spawner()),
					world(d.floorBeacon()), world(d.ceilingBeacon()), world(d.wardenSpot()), world(d.jukebox()), List.copyOf(lamps),
					d.box(this.origin, this.rotation));
			this.parts = p;
		}
		return p;
	}

	public BlockPos world(BlockPos local) {
		return FactoryDesign.toWorld(this.origin, local, this.rotation);
	}

	public BlockPos pad() {
		return parts().pad();
	}

	public BlockPos control() {
		return parts().control();
	}

	public BlockPos dropPoint() {
		return parts().dropPoint();
	}

	public BlockPos dripperTip() {
		return parts().dripperTip();
	}

	public BlockPos spawner() {
		return parts().spawner();
	}

	public BlockPos floorBeacon() {
		return parts().floorBeacon();
	}

	public BlockPos ceilingBeacon() {
		return parts().ceilingBeacon();
	}

	public BlockPos wardenSpot() {
		return parts().wardenSpot();
	}

	public BlockPos jukebox() {
		return parts().jukebox();
	}

	public List<BlockPos> timerLamps() {
		return parts().timerLamps();
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

	/** The world box the whole build covers. */
	public BoundingBox box() {
		return parts().box();
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

	/** Recent designs by seed, mob and city; built outside the lock, one build per key at a time. */
	private static final class DesignCache {
		private final Map<String, FactoryDesign> map = new LinkedHashMap<>(16, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, FactoryDesign> eldest) {
				return size() > 6;
			}
		};
		private final ConcurrentHashMap<String, CompletableFuture<FactoryDesign>> building = new ConcurrentHashMap<>();

		FactoryDesign get(FabricatorSite site) {
			String key = site.seed + "/" + site.mob + "/" + Objects.toString(site.cityLocal);
			synchronized (this) {
				FactoryDesign d = this.map.get(key);
				if (d != null) {
					return d;
				}
			}
			CompletableFuture<FactoryDesign> mine = new CompletableFuture<>();
			CompletableFuture<FactoryDesign> other = this.building.putIfAbsent(key, mine);
			if (other != null) {
				return other.join();
			}
			try {
				FactoryDesign d = FactoryDesign.design(site.seed, site.mob, site.cityLocal);
				synchronized (this) {
					this.map.put(key, d);
				}
				mine.complete(d);
				return d;
			} catch (RuntimeException | Error e) {
				mine.completeExceptionally(e);
				throw e;
			} finally {
				this.building.remove(key, mine);
			}
		}
	}

	private static final DesignCache DESIGNS = new DesignCache();
}
