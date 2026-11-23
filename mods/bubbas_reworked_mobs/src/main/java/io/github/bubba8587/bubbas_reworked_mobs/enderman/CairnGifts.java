// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Cairn offerings: one item, once ever per picked site (specs/endermen.md "Cairn gifts"). */
public final class CairnGifts {
	/** Sites whose offering was laid, by origin. */
	public record Given(List<Long> offered) {
		static final Codec<Given> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.listOf().optionalFieldOf("offered", List.of()).forGetter(Given::offered)).apply(i, Given::new));
		static final Given NONE = new Given(List.of());
	}

	public static final AttachmentType<Given> GIFTS = AttachmentRegistry.createPersistent(BubbasReworkedMobs.id("cairn_gifts"), Given.CODEC);

	/** Salt for picking which sites leave an offering (from the site's own seed). */
	private static final long OFFERING_SALT = 0x0FFE_2161_1BE6_1A11L;

	private static @Nullable Supplier<ItemStack> offering;

	private CairnGifts() {
	}

	/** Sets the items (null for none). */
	public static void set(@Nullable Supplier<ItemStack> offeringItem) {
		offering = offeringItem;
	}

	/** Whether this site is one whose endermen leave an offering: fixed by its seed. */
	public static boolean offers(CairnSite site) {
		return new SplittableRandom(site.seed() ^ OFFERING_SALT).nextDouble() < EndermanTuning.OFFERING_SHARE;
	}

	public static boolean offered(ServerLevel level, CairnSite site) {
		return level.getAttachedOrElse(GIFTS, Given.NONE).offered().contains(site.origin().asLong());
	}

	private static void mark(ServerLevel level, CairnSite site) {
		Given given = level.getAttachedOrElse(GIFTS, Given.NONE);
		List<Long> offered = new ArrayList<>(given.offered());
		offered.add(site.origin().asLong());
		level.setAttached(GIFTS, new Given(List.copyOf(offered)));
	}

	static void tick(ServerLevel level) {
		if (offering == null || level.dimension() != Level.OVERWORLD || level.getServer() instanceof GameTestServer
				|| level.getGameTime() % EndermanTuning.OFFERING_CHECK_TICKS != 0) {
			return;
		}
		for (CairnSite site : CairnSites.nearPlayers(level, EndermanTuning.SITE_ACTIVE_RANGE, false)) {
			tryOffering(level, site, false);
		}
	}

	/**
	 * One check for a site's offering: a chosen, complete site not yet marked, with one of its own endermen at it, lays
	 * it (with 1 in OFFERING_ONE_IN, unless {@code force}). Returns where it was laid, or null.
	 */
	public static @Nullable BlockPos tryOffering(ServerLevel level, CairnSite site, boolean force) {
		Supplier<ItemStack> item = offering;
		if (item == null || !offers(site) || offered(level, site) || !level.isLoaded(site.origin()) || EndermanWards.isWarded(level, site.origin())) {
			return null;
		}
		if (!force && level.getRandom().nextInt(EndermanTuning.OFFERING_ONE_IN) != 0) {
			return null;
		}
		if (!site.complete(level)) {
			return null;
		}
		double range = EndermanTuning.OFFERING_WITNESS_RANGE;
		Enderman witness = null;
		for (Enderman e : EndermanSpawning.ownEndermen(level, site)) {
			if (e.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(site.origin())) <= range * range) {
				witness = e;
				break;
			}
		}
		if (witness == null) {
			return null;
		}
		BlockPos spot = offeringSpot(level, site);
		if (spot == null) {
			return null;
		}
		ItemStack stack = item.get();
		if (stack.isEmpty()) {
			return null;
		}
		// Marked first: whatever happens next, this site never lays another.
		mark(level, site);
		Direction facing = Direction.getApproximateNearest(site.origin().getX() - spot.getX(), 0, site.origin().getZ() - spot.getZ());
		LootDisplays.place(level, spot, DisplayPlacement.GROUND, facing, stack);
		level.sendParticles(ParticleTypes.PORTAL, spot.getX() + 0.5, spot.getY() + 0.4, spot.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.3);
		level.playSound(null, spot, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.8F);
		return spot;
	}

	/**
	 * Where a site's offering goes: on the ground a little outside the cairn's footprint, on the side picked by its
	 * seed (then round the cairn until a spot is free), on a floor that holds a display up and not on any design block.
	 */
	public static @Nullable BlockPos offeringSpot(ServerLevel level, CairnSite site) {
		SplittableRandom random = new SplittableRandom(site.seed() ^ OFFERING_SALT ^ 0x51DEL);
		double start = random.nextDouble() * Math.PI * 2.0;
		double r = site.radius() + EndermanTuning.OFFERING_GAP;
		BlockPos origin = site.origin();
		for (int i = 0; i < 16; i++) {
			double a = start + Math.PI * 2.0 * i / 16.0;
			int x = origin.getX() + (int) Math.round(Math.cos(a) * r);
			int z = origin.getZ() + (int) Math.round(Math.sin(a) * r);
			for (int dy = 3; dy >= -3; dy--) {
				BlockPos pos = new BlockPos(x, origin.getY() + dy, z);
				if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !Block.canSupportCenter(level, pos.below(), Direction.UP)) {
					continue;
				}
				CairnSite cover = CairnSites.covering(level, pos);
				if (cover == null || cover.at(pos) == null && cover.at(pos.below()) == null) {
					return pos;
				}
			}
		}
		return null;
	}
}
