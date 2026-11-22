// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.end;

import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Totem of the End's teleport (owner's landing rules, 2026-11-22). Aim at a block within
 * {@link TotemsTuning#END_RANGE}; nothing hit, nothing happens.
 * <ul>
 * <li>Top face: you stand on that block (if you fit there and it's safe; otherwise nothing happens).</li>
 * <li>Side face: on top of that block when you fit above it; otherwise downward, from the space in front of the face,
 * to the nearest safe footing below.</li>
 * <li>Bottom face: downward from the space under the block, the same way.</li>
 * </ul>
 * "Fit" is vanilla's own collision test for the entity's box (as {@code randomTeleport} uses), so slabs, stairs and
 * paths are footing and a low ceiling is not room. "Safe": the box and the block under it touch no lava, fire,
 * campfire, powder snow, magma, cactus or berry bush; the void is never footing (scanning stops at the world's floor).
 * Water is allowed (vanilla's pearl and chorus fruit allow it).
 */
public final class EndTeleport {
	private EndTeleport() {
	}

	/** What the user aims at within range (collision shapes, so grass and flowers are aimed through; fluids too). */
	public static BlockHitResult aim(Level level, LivingEntity user) {
		Vec3 eye = user.getEyePosition();
		Vec3 end = eye.add(user.getViewVector(1.0F).scale(TotemsTuning.END_RANGE));
		return level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
	}

	/** Where {@code user} lands for this hit, by the rules above, or null when there's nowhere. */
	public static @Nullable Vec3 landing(Level level, Entity user, BlockHitResult hit) {
		if (hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		BlockPos pos = hit.getBlockPos();
		Direction face = hit.getDirection();
		if (face == Direction.UP) {
			return standOn(level, user, pos);
		}
		if (face != Direction.DOWN) {
			Vec3 top = standOn(level, user, pos);
			if (top != null) {
				return top;
			}
		}
		return downFrom(level, user, pos.relative(face));
	}

	/** Standing on top of {@code footing}, if the user fits there and it's safe; else null. */
	public static @Nullable Vec3 standOn(Level level, Entity user, BlockPos footing) {
		BlockState state = level.getBlockState(footing);
		VoxelShape shape = state.getCollisionShape(level, footing);
		if (shape.isEmpty() || hazard(state)) {
			return null;
		}
		Vec3 feet = new Vec3(footing.getX() + 0.5, footing.getY() + shape.max(Direction.Axis.Y), footing.getZ() + 0.5);
		AABB box = user.getDimensions(user.getPose()).makeBoundingBox(feet);
		if (!level.noCollision(user, box) || touchesHazard(level, box)) {
			return null;
		}
		return feet;
	}

	/** The nearest safe footing at or below the open space {@code start}, scanning down to the world's floor; else null. */
	public static @Nullable Vec3 downFrom(Level level, Entity user, BlockPos start) {
		BlockPos.MutableBlockPos footing = new BlockPos.MutableBlockPos();
		for (int y = start.getY() - 1; y >= level.getMinY(); y--) {
			footing.set(start.getX(), y, start.getZ());
			if (!level.getBlockState(footing).getCollisionShape(level, footing).isEmpty()) {
				Vec3 spot = standOn(level, user, footing.immutable());
				if (spot != null) {
					return spot;
				}
			}
		}
		return null;
	}

	private static boolean touchesHazard(Level level, AABB box) {
		// The box's own cells plus the block under the feet.
		BlockPos min = BlockPos.containing(box.minX, box.minY - 0.01, box.minZ);
		BlockPos max = BlockPos.containing(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			BlockState state = level.getBlockState(p);
			if (hazard(state) || state.getFluidState().is(FluidTags.LAVA)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hazard(BlockState state) {
		return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.MAGMA_BLOCK)
				|| state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.LAVA) || state.is(Blocks.WITHER_ROSE)
				|| state.getFluidState().is(FluidTags.LAVA);
	}

	/**
	 * Teleports {@code user} to {@code to} like an ender pearl does (dismounted first, motion stopped, fall distance
	 * reset), with an enderman's teleport sound and portal particles at both ends and a teleport game event.
	 */
	public static void teleport(ServerLevel level, LivingEntity user, Vec3 to) {
		if (user.isPassenger()) {
			user.stopRiding();
		}
		Vec3 from = user.position();
		effects(level, from);
		if (user instanceof ServerPlayer player) {
			player.teleport(new TeleportTransition(level, to, Vec3.ZERO, 0.0F, 0.0F, Relative.ROTATION, TeleportTransition.DO_NOTHING));
		} else {
			user.teleportTo(to.x, to.y, to.z);
			user.setDeltaMovement(Vec3.ZERO);
		}
		user.resetFallDistance();
		user.resetCurrentImpulseContext();
		level.gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(user));
		effects(level, to);
	}

	private static void effects(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1.0, at.z, 32, 0.3, 0.9, 0.3, 0.3);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
