// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulfireTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;

/** Eye of the Soul and Phasing (specs/soulfire.md "Eye of the Soul: phasing"): registrations and the block rules. */
public final class Phasing {
	public static final Holder<MobEffect> PHASING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BubbasReworkedMobs.id("phasing"),
			new PhasingEffect());

	public static final Item EYE_OF_THE_SOUL = register("eye_of_the_soul", new Item.Properties()
			.stacksTo(SoulfireTuning.EYE_STACK_SIZE).useCooldown(SoulfireTuning.EYE_COOLDOWN_SECONDS));

	/** Set while the server checks a player's move against the blocks it would newly touch. */
	private static final ThreadLocal<Boolean> PRE_MOVE_CHECK = ThreadLocal.withInitial(() -> false);

	private Phasing() {
	}

	private static Item register(String name, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new EyeOfTheSoulItem(properties.setId(key)));
	}

	public static void initialize() {
		// Phasing: no early removal (milk, totems, other mods' clears) except by command
		ServerMobEffectEvents.ALLOW_EARLY_REMOVE.register((effect, entity, context) -> !effect.is(PHASING) || context.isFromCommand());
		// No suffocation while Phasing
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(source.is(DamageTypes.IN_WALL) && isPhasing(entity)));
	}

	/** Starts (or tops up) Phasing. */
	public static void start(LivingEntity entity) {
		entity.addEffect(new MobEffectInstance(PHASING, SoulfireTuning.PHASING_TICKS, 0, false, false, true));
	}

	public static boolean isPhasing(@Nullable Entity entity) {
		return entity instanceof Player player && player.hasEffect(PHASING);
	}

	/** Blocks Phasing passes through: every breakable block (bedrock, barriers, portal frames and the like stay solid). */
	public static boolean passable(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getDestroySpeed(level, pos) != -1.0F;
	}

	// --- collision ---

	/**
	 * A block's collision shape for a Phasing player: vanilla's scaffolding rule (ScaffoldingBlock.getCollisionShape) on
	 * the block's own shape: solid while the player is above it and not descending, else nothing.
	 */
	public static VoxelShape collisionShape(VoxelShape shape, BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (!(context instanceof EntityCollisionContext entityContext) || !isPhasing(entityContext.getEntity()) || shape.isEmpty()
				|| !passable(state, level, pos)) {
			return shape;
		}
		if (context.isPlacement()) {
			return PRE_MOVE_CHECK.get() ? Shapes.empty() : shape;
		}
		return context.isAbove(shape, pos, true) && !context.isDescending() ? shape : Shapes.empty();
	}

	/** Runs the server's "colliding with anything new" move check, under which Phasing blocks count as scaffolding. */
	public static <T> T preMoveCheck(java.util.function.Supplier<T> check) {
		PRE_MOVE_CHECK.set(true);
		try {
			return check.get();
		} finally {
			PRE_MOVE_CHECK.set(false);
		}
	}

	// --- climbing ---

	/** A Phasing player inside a block it passes through: counts as on a climbable, as in scaffolding. */
	public static boolean climbing(LivingEntity entity) {
		return isPhasing(entity) && !entity.isSpectator() && insideBlock(entity);
	}

	private static boolean insideBlock(LivingEntity entity) {
		AABB box = entity.getBoundingBox().deflate(1.0E-3);
		VoxelShape boxShape = null;
		BlockGetter level = entity.level();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !passable(state, level, pos)) {
				continue;
			}
			VoxelShape shape = state.getCollisionShape(level, pos);
			if (shape == Shapes.block()) {
				return true;
			}
			if (!shape.isEmpty()) {
				if (boxShape == null) {
					boxShape = Shapes.create(box);
				}
				if (Shapes.joinIsNotEmpty(shape.move(pos), boxShape, BooleanOp.AND)) {
					return true;
				}
			}
		}
		return false;
	}
}
