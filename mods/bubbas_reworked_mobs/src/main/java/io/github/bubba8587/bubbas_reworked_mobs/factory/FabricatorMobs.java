// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Fabricator's mobs (specs/mob-factory.md). A dropped mob is <b>deactivated</b>: no AI at all (its whole server
 * AI step is skipped, {@code mixin/MobDeactivatedMixin}), silent, head hanging (looking down), never hurt by its
 * fall. Sent up, it lies limp on the surface in particles for {@link FabricatorTuning#WAKE_TICKS}, then <b>wakes</b>
 * as the world's reworked mob: it was made with the {@code TRIGGERED} spawn reason, which keeps it reworked
 * ({@code MobFinalizeSpawnMixin} only turns spawner and unmarked natural spawns vanilla). Saved with the mob.
 */
public final class FabricatorMobs {
	/** On the deck, deactivated. */
	public static final int ON_DECK = 0;
	/** Arrived on the surface, about to wake. */
	public static final int ARRIVED = 1;

	public record State(int stage, long wakeAt) {
		static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("stage").forGetter(State::stage),
				Codec.LONG.fieldOf("wake_at").forGetter(State::wakeAt)).apply(i, State::new));
	}

	public static final AttachmentType<State> DEACTIVATED = AttachmentRegistry.createPersistent(
			BubbasReworkedMobs.id("fabricator_mob"), State.CODEC);

	/** Head pitch of a deactivated mob: looking at its feet. */
	private static final float LIMP_PITCH = 60.0F;

	private FabricatorMobs() {
	}

	public static EntityType<? extends Mob> type(FactoryDesign.Mob mob) {
		return switch (mob) {
			case ZOMBIE -> EntityTypes.ZOMBIE;
			case SKELETON -> EntityTypes.SKELETON;
			case CREEPER -> EntityTypes.CREEPER;
			case SPIDER -> EntityTypes.SPIDER;
			case ENDERMAN -> EntityTypes.ENDERMAN;
		};
	}

	public static boolean isDeactivated(LivingEntity entity) {
		return entity.hasAttached(DEACTIVATED);
	}

	/** The dripper lets one go: a deactivated mob under its tip, falling onto the pad. */
	public static @Nullable Mob drop(ServerLevel level, FabricatorSite site) {
		Mob mob = type(site.mob()).create(level, EntitySpawnReason.TRIGGERED);
		if (mob == null) {
			return null;
		}
		BlockPos at = site.dropPoint();
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, LIMP_PITCH);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.TRIGGERED, null);
		deactivate(mob);
		level.addFreshEntity(mob);
		BlockPos tip = site.dripperTip();
		level.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, tip.getX() + 0.5, tip.getY(), tip.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.0);
		level.sendParticles(ParticleTypes.SCULK_SOUL, tip.getX() + 0.5, tip.getY() - 0.5, tip.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.02);
		level.playSound(null, tip, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 1.5F, 0.6F);
		return mob;
	}

	public static void deactivate(Mob mob) {
		mob.setAttached(DEACTIVATED, new State(ON_DECK, 0L));
		mob.setSilent(true);
		mob.setXRot(LIMP_PITCH);
		mob.setTarget(null);
	}

	/** Called instead of the mob's AI while it is deactivated (mixin/MobDeactivatedMixin). */
	public static void tick(Mob mob, State state) {
		mob.resetFallDistance();
		mob.setXRot(LIMP_PITCH);
		mob.setYHeadRot(mob.yBodyRot);
		mob.setTarget(null);
		if (state.stage() == ARRIVED && mob.level() instanceof ServerLevel level) {
			if (level.getGameTime() >= state.wakeAt()) {
				wake(mob);
			} else if (mob.tickCount % 4 == 0) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 6, 0.4, 0.6, 0.4, 0.02);
			}
		}
	}

	/** Wakes it: the AI comes back, a reworked mob of the world. */
	public static void wake(Mob mob) {
		mob.removeAttached(DEACTIVATED);
		mob.setSilent(false);
		mob.setXRot(0.0F);
		if (mob.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 8, 0.3, 0.4, 0.3, 0.03);
		}
	}

	/** Sends it up: to its destination, limp in particles, waking soon. */
	public static void arrive(ServerLevel level, Mob mob, BlockPos to) {
		departParticles(level, mob.position());
		mob.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
		mob.resetFallDistance();
		mob.setAttached(DEACTIVATED, new State(ARRIVED, level.getGameTime() + FabricatorTuning.WAKE_TICKS));
		arriveParticles(level, Vec3.atBottomCenterOf(to));
	}

	public static void departParticles(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 40, 0.5, 1.0, 0.5, 0.05);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	public static void arriveParticles(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 80, 0.6, 1.2, 0.6, 0.08);
		level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 0.5, at.z, 60, 0.8, 0.4, 0.8, 0.6);
		level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 0.5, at.z, 10, 0.5, 0.5, 0.5, 0.03);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4F, 0.5F);
	}

	/**
	 * A surface spot for a mob (F9): within {@link FabricatorTuning#SURFACE_RANGE} sideways of the pad, well above
	 * it, on the top block of a loaded, ticking column, where this mob could spawn naturally right now (vanilla's
	 * placement and light rules: dark ground, so in practice at night) and no player is within 24. Null when none.
	 */
	public static @Nullable BlockPos surfaceForMob(ServerLevel level, FabricatorSite site, EntityType<?> type) {
		BlockPos pad = site.pad();
		var random = level.getRandom();
		for (int i = 0; i < FabricatorTuning.SURFACE_TRIES; i++) {
			BlockPos pos = column(level, pad, random);
			if (pos == null) {
				continue;
			}
			if (SpawnPlacements.isSpawnPositionOk(type, level, pos) && SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)
					&& level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
					&& level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, FabricatorTuning.NO_PLAYER_RANGE, false) == null) {
				return pos;
			}
		}
		return null;
	}

	/** A surface spot for anything else on the pad (a player who hopped on): any time, standing room on solid ground. */
	public static @Nullable BlockPos surfaceForRider(ServerLevel level, FabricatorSite site, LivingEntity rider) {
		BlockPos pad = site.pad();
		var random = level.getRandom();
		for (int i = 0; i < FabricatorTuning.SURFACE_TRIES * 2; i++) {
			BlockPos pos = column(level, pad, random);
			if (pos != null && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty()
					&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
					&& level.noCollision(rider, rider.getDimensions(rider.getPose()).makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
				return pos;
			}
		}
		return null;
	}

	private static @Nullable BlockPos column(ServerLevel level, BlockPos pad, net.minecraft.util.RandomSource random) {
		double a = random.nextDouble() * Math.PI * 2.0;
		double r = 8.0 + random.nextDouble() * (FabricatorTuning.SURFACE_RANGE - 8.0);
		int x = pad.getX() + (int) Math.round(Math.cos(a) * r);
		int z = pad.getZ() + (int) Math.round(Math.sin(a) * r);
		BlockPos probe = new BlockPos(x, pad.getY(), z);
		if (!level.isPositionEntityTicking(probe)) {
			return null;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (y < pad.getY() + FabricatorTuning.MIN_RISE) {
			return null;
		}
		return new BlockPos(x, y, z);
	}
}
