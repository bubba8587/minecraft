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

/** Back to sleep at dawn: a woken undead walks to a shaded spot and becomes a sleeper again. */
public final class BackToSleep extends MoveToBlockGoal {
	/** Entity tag: this woken undead has been awake while it was dark. */
	public static final String SAW_NIGHT_TAG = "bubbas_reworked_mobs.saw_night";

	private final Monster undead;

	public BackToSleep(PathfinderMob mob) {
		super(mob, 1.2, UndeadTuning.SLEEP_SEARCH_RANGE, UndeadTuning.SLEEP_VERTICAL_RANGE);
		this.undead = (Monster) mob;
	}

	/** Tags a woken undead that is awake at night. */
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

	/** Arrival distance. */
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

	/** Valid floor block under a resting place. */
	@Override
	protected boolean isValidTarget(LevelReader level, BlockPos floor) {
		return pose(level, floor.above()) != null;
	}

	/** Pose for resting at {@code at}, or null. */
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
		return walls >= 3 ? SleeperPose.LYING : null;
	}

	private static boolean free(LevelReader level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/** Light-blocking roof within 16 above. */
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

	/** Replaces the mob with a sleeper block holding its gear and name. */
	public static void lieDown(ServerLevel level, Monster mob, BlockPos at) {
		UndeadVariant variant = variantOf(mob);
		SleeperPose pose = pose(level, at);
		if (variant == null || pose == null) {
			return;
		}
		boolean water = level.getBlockState(at).is(Blocks.WATER);
		Direction facing = Direction.fromYRot(mob.getYRot());
		if (pose.isFlat()) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (free(level, at.relative(side))) {
					facing = side;
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
