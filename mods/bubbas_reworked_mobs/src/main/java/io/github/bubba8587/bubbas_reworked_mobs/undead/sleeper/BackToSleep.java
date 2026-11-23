// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Back to sleep at dawn (owner, 2026-11-22, specs/undead.md). A woken undead that was awake in the dark and is still
 * about when the sun comes up, with nothing to fight, walks to the nearest place out of the sun and becomes a sleeper
 * again there, with everything it carries. Crypts refill themselves; a cleared graveyard quietly repopulates if you
 * leave survivors.
 *
 * <p>Owner: an undead can always sleep by sitting down in a pocket where it's safe to stand. So a place is any spot on
 * a solid floor out of the sun (a light-blocking roof somewhere over it) and dim (block light at most
 * {@link UndeadTuning#SLEEP_MAX_LIGHT}): standing room means it sits down there; a one-high niche means it lies down.
 * An open grave (a pit walled on three sides) does too, sun or not: it lies down in it.
 *
 * <p>The walking and the nearest-first search are vanilla's {@link MoveToBlockGoal} (its target is the floor block).
 * "Was awake in the dark" is an entity tag, so it survives a reload; mobs woken by day don't go straight back to sleep.
 */
public final class BackToSleep extends MoveToBlockGoal {
	/** Entity tag: this woken undead has been awake while it was dark. */
	public static final String SAW_NIGHT_TAG = "bubbas_reworked_mobs.saw_night";

	private final Monster undead;

	public BackToSleep(PathfinderMob mob) {
		super(mob, 1.2, UndeadTuning.SLEEP_SEARCH_RANGE, UndeadTuning.SLEEP_VERTICAL_RANGE);
		this.undead = (Monster) mob;
	}

	/** Every server tick of a woken undead: remembers that it saw the night. */
	public static void noteNight(ServerLevel level, Monster mob) {
		if (level.dimensionType().hasSkyLight() && level.isDarkOutside() && !mob.entityTags().contains(SAW_NIGHT_TAG)) {
			mob.addTag(SAW_NIGHT_TAG);
		}
	}

	private boolean sleepy() {
		return undead.level() instanceof ServerLevel level && level.dimensionType().hasSkyLight() && level.isBrightOutside()
				&& undead.entityTags().contains(SAW_NIGHT_TAG) && undead.getTarget() == null && !undead.isPassenger() && !undead.isLeashed();
	}

	@Override
	public boolean canUse() {
		return sleepy() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return sleepy() && super.canContinueToUse();
	}

	@Override
	protected int nextStartTick(PathfinderMob mob) {
		return reducedTickDelay(UndeadTuning.SLEEP_RESEARCH_TICKS + mob.getRandom().nextInt(20));
	}

	/** Close enough to lie down: beside it will do (a one-high niche has no room to step into first). */
	@Override
	public double acceptedDistance() {
		return 1.6;
	}

	@Override
	public void tick() {
		super.tick();
		if (isReachedTarget() && undead.level() instanceof ServerLevel level && isValidTarget(level, blockPos)) {
			lieDown(level, undead, blockPos.above());
		}
	}

	/** {@code floor} is the block it would rest on; the place itself is the one above. */
	@Override
	protected boolean isValidTarget(LevelReader level, BlockPos floor) {
		return pose(level, floor.above()) != null;
	}

	/**
	 * How it would rest at {@code at}, or null if it can't: sitting where there's room to stand, lying in a one-high
	 * niche, both out of the sun and dim; lying in an open grave.
	 */
	public static @Nullable SleeperPose pose(LevelReader level, BlockPos at) {
		BlockState here = level.getBlockState(at);
		boolean water = here.is(Blocks.WATER) && here.getFluidState().isSource();
		if (!(here.isAir() || water) || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) {
			return null;
		}
		boolean standing = free(level, at.above());
		if (roofed(level, at) && level.getBrightness(LightLayer.BLOCK, at) <= UndeadTuning.SLEEP_MAX_LIGHT) {
			return standing ? SleeperPose.SITTING : SleeperPose.LYING;
		}
		int walls = 0;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(at.relative(side)).isFaceSturdy(level, at.relative(side), side.getOpposite())) {
				walls++;
			}
		}
		return walls >= 3 ? SleeperPose.LYING : null; // an open grave
	}

	private static boolean free(LevelReader level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/** Whether a light-blocking block stands over it within 16 (a barrier or glass roof doesn't keep the sun off). */
	private static boolean roofed(LevelReader level, BlockPos at) {
		for (int dy = 1; dy <= 16; dy++) {
			BlockState state = level.getBlockState(at.above(dy));
			if (state.getLightDampening() > 0 || state.canOcclude()) {
				return true;
			}
		}
		return false;
	}

	/** The undead variant whose woken mob this is, or null. */
	public static @Nullable UndeadVariant variantOf(Monster mob) {
		for (UndeadVariant variant : UndeadVariant.values()) {
			if (ModEntities.woken(variant) == mob.getType()) {
				return variant;
			}
		}
		return null;
	}

	/** The mob rests at {@code at}: a sleeper block takes its place, with its gear and name. */
	public static void lieDown(ServerLevel level, Monster mob, BlockPos at) {
		UndeadVariant variant = variantOf(mob);
		SleeperPose pose = pose(level, at);
		if (variant == null || pose == null) {
			return;
		}
		boolean water = level.getBlockState(at).is(Blocks.WATER);
		Direction facing = Direction.fromYRot(mob.getYRot());
		if (pose == SleeperPose.LYING) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (free(level, at.relative(side))) {
					facing = side; // it lies looking out of its niche
					break;
				}
			}
		}
		level.setBlock(at, ModBlocks.sleeper(variant).defaultBlockState().setValue(SleeperBlock.POSE, pose).setValue(SleeperBlock.FACING, facing)
				.setValue(SleeperBlock.WATERLOGGED, water), Block.UPDATE_ALL);
		if (level.getBlockEntity(at) instanceof SleeperBlockEntity sleeper) {
			for (EquipmentSlot slot : SleeperBlockEntity.SLOTS) {
				ItemStack stack = mob.getItemBySlot(slot);
				if (!stack.isEmpty()) {
					sleeper.setGear(slot, stack.copy());
				}
			}
			if (mob.hasCustomName()) {
				sleeper.setComponents(net.minecraft.core.component.DataComponentMap.builder().addAll(sleeper.components())
						.set(DataComponents.CUSTOM_NAME, mob.getCustomName()).build());
			}
			sleeper.setChanged();
		}
		level.playSound(null, at, variant.sound().getPlaceSound(), SoundSource.HOSTILE, 0.6F, 0.7F);
		mob.discard();
	}
}
