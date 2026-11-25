// dte:C1,B15,B17,B21
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * One registered camp or pillager outpost (specs/monster-camps.md), saved with the level: where it is, its kind, and
 * its state (armed: a banner of its kind stands; spawned: its roster has appeared since the last reset; the roster
 * still alive). Fixed from the structure at worldgen; registered the first time a player comes near.
 */
public final class CampSite {
	public static final Codec<CampSite> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("id").forGetter(s -> s.id),
			Codec.STRING.optionalFieldOf("kind").forGetter(s -> Optional.ofNullable(s.kind).map(CampKind::id)),
			BoundingBox.CODEC.fieldOf("box").forGetter(s -> s.box),
			BlockPos.CODEC.fieldOf("centre").forGetter(s -> s.centre),
			Codec.INT.optionalFieldOf("size", 0).forGetter(s -> s.size == null ? 0 : s.size.ordinal()),
			BlockPos.CODEC.listOf().optionalFieldOf("towers", List.of()).forGetter(s -> s.towers),
			BlockPos.CODEC.listOf().optionalFieldOf("ground", List.of()).forGetter(s -> s.ground),
			BlockPos.CODEC.listOf().optionalFieldOf("shade", List.of()).forGetter(s -> s.shade),
			BlockPos.CODEC.listOf().optionalFieldOf("pens", List.of()).forGetter(s -> s.pens),
			Codec.BOOL.optionalFieldOf("stable", false).forGetter(s -> s.stable),
			Codec.BOOL.optionalFieldOf("spawned", false).forGetter(s -> s.spawned),
			Codec.BOOL.optionalFieldOf("armed", true).forGetter(s -> s.armed),
			Codec.BOOL.optionalFieldOf("rearmed", false).forGetter(s -> s.rearmed),
			Codec.BOOL.optionalFieldOf("quiet", false).forGetter(s -> s.quiet),
			UUIDUtil.CODEC.listOf().optionalFieldOf("members", List.of()).forGetter(s -> s.members)).apply(i, CampSite::new));

	final long id;
	final @Nullable CampKind kind;
	final BoundingBox box;
	final BlockPos centre;
	final CampTuning.@Nullable Size size;
	/** Archer spots on the towers (with height). */
	final List<BlockPos> towers;
	/** Spots on the ground (columns; height from the world when the roster appears). */
	final List<BlockPos> ground;
	/** Shade spots (heights approximate except the room's; camp/CampShade resolves them). */
	final List<BlockPos> shade;
	/** Tether posts: hoglin pens' (Nether outposts) and the stable's. */
	final List<BlockPos> pens;
	/** Whether it has a stable of tethered mounts. */
	final boolean stable;
	boolean spawned;
	boolean armed;
	/** A banner was put back after the camp was cleared of them: the next reset check is sure to reset. */
	boolean rearmed;
	/** A lit campfire burns in the camp (as last seen while a player was near): no roster, no reset. */
	boolean quiet;
	final List<UUID> members;

	private CampSite(long id, Optional<String> kind, BoundingBox box, BlockPos centre, int size, List<BlockPos> towers, List<BlockPos> ground,
			List<BlockPos> shade, List<BlockPos> pens, boolean stable, boolean spawned, boolean armed, boolean rearmed, boolean quiet, List<UUID> members) {
		this.id = id;
		this.kind = kind.map(k -> CampKind.valueOf(k.toUpperCase(java.util.Locale.ROOT))).orElse(null);
		this.box = box;
		this.centre = centre;
		this.size = this.kind == null ? null : CampTuning.Size.values()[Math.floorMod(size, CampTuning.Size.values().length)];
		this.towers = List.copyOf(towers);
		this.ground = List.copyOf(ground);
		this.shade = List.copyOf(shade);
		this.pens = List.copyOf(pens);
		this.stable = stable;
		this.spawned = spawned;
		this.armed = armed;
		this.rearmed = rearmed;
		this.quiet = quiet;
		this.members = new ArrayList<>(members);
	}

	/** A camp, from its piece. */
	static CampSite camp(long id, CampPiece piece, List<BlockPos> towers, List<BlockPos> ground) {
		return new CampSite(id, Optional.of(piece.kind().id()), piece.getBoundingBox(), piece.centre(), piece.size().ordinal(), towers, ground,
				piece.shadeSpots(), piece.tetherPosts(), piece.hasStable(), false, true,
				false, false, List.of());
	}

	/** A pillager outpost. */
	static CampSite outpost(long id, BoundingBox box) {
		return new CampSite(id, Optional.empty(), box, box.getCenter(), 0, List.of(), List.of(), List.of(), List.of(), false, false, true, false, false, List.of());
	}

	public long id() {
		return id;
	}

	/** The camp's kind, or null for a pillager outpost. */
	public @Nullable CampKind kind() {
		return kind;
	}

	public boolean isOutpost() {
		return kind == null;
	}

	public BoundingBox box() {
		return box;
	}

	public BlockPos centre() {
		return centre;
	}

	public CampTuning.@Nullable Size size() {
		return size;
	}

	/** Whether one of its banners stands (as last seen while a player was near). */
	public boolean armed() {
		return armed;
	}

	/** Whether a lit campfire burns in it (it's quiet: no roster, no reset). */
	public boolean quiet() {
		return quiet;
	}

	/** Whether its roster has appeared since it last reset. */
	public boolean spawned() {
		return spawned;
	}

	/** Whether it has a stable (mounted patrols can ride out of it). */
	public boolean hasStable() {
		return stable;
	}

	/** Its tether posts: hoglin pens' and the stable's. */
	public List<BlockPos> pens() {
		return pens;
	}

	/** Its shade spots. */
	public List<BlockPos> shade() {
		return shade;
	}

	/** Its roster still alive (UUIDs). */
	public List<UUID> members() {
		return List.copyOf(members);
	}
}
