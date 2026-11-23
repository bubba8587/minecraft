// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.loot.LootGrabbing;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameEvents;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Turning a sleeper into a mob, chain waking, and robbing. */
public final class SleeperWaking {
	private SleeperWaking() {
	}

	/**
	 * Replaces the sleeper with its woken mob wearing the same gear, facing what woke it, then lets neighbours hear
	 * the awaken event. If the spawn is refused (another mod cancelling it), the block and its gear stay.
	 *
	 * @return the mob, or null if nothing spawned
	 */
	public static @Nullable Monster wake(ServerLevel level, BlockPos pos, BlockState state, SleeperBlockEntity sleeper) {
		if (SleeperBlock.isResting(state)) {
			return null; // the resting dead never wake, however they're asked
		}
		if (!(state.getBlock() instanceof SleeperBlock block)) {
			return null;
		}
		Monster mob = ModEntities.woken(block.variant()).create(level, EntitySpawnReason.TRIGGERED);
		if (mob == null) {
			return null;
		}
		float yRot = state.getValue(SleeperBlock.FACING).toYRot();
		Vec3 source = sleeper.getWakeSource();
		if (source != null && source.distanceToSqr(Vec3.atCenterOf(pos)) > 0.01) {
			yRot = (float) (Mth.atan2(source.z - (pos.getZ() + 0.5), source.x - (pos.getX() + 0.5)) * Mth.RAD_TO_DEG) - 90.0F;
		}
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0.0F);
		mob.yHeadRot = yRot;
		mob.yBodyRot = yRot;
		mob.setPersistenceRequired();
		mob.setCustomName(sleeper.components().get(DataComponents.CUSTOM_NAME));
		for (EquipmentSlot slot : SleeperBlockEntity.SLOTS) {
			ItemStack stack = sleeper.getGear(slot);
			if (!stack.isEmpty()) {
				mob.setItemSlot(slot, stack.copy());
				mob.setGuaranteedDrop(slot);
			}
		}
		// Buried and frozen sleepers burst out of the sand or ice they lie in first (SleeperBreakout).
		SleeperBreakout.breakOut(level, pos, block.variant());
		if (!level.addFreshEntity(mob)) {
			return null;
		}
		LootGrabbing.grabOnWake(level, mob); // empty-handed: a weapon within reach, else anything (loot/LootGrabbing)
		NonNullList<ItemStack> unused = sleeper.takeAllGear(); // the mob has it now; keep it from dropping as the block goes
		unused.clear();
		level.setBlock(pos, state.getValue(SleeperBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
				Block.UPDATE_ALL);
		stepOutIfStuck(level, mob, pos);
		// Only a survival or adventure player who woke it becomes its target. A sleeper woken by another mob (the
		// awaken chain, a villager's or creeper's footsteps) wakes without a target: before this, chain-woken undead
		// went for the undead that woke them and the horde fought itself.
		if (sleeper.getWaker() != null && level.getEntity(sleeper.getWaker()) instanceof Player player
				&& !player.isCreative() && !player.isSpectator() && mob.canAttack(player)) {
			mob.setTarget(player);
		}
		mob.playAmbientSound();
		emitAwaken(level, mob);
		return mob;
	}

	/**
	 * A sleeper lying somewhere lower than its mob is tall (a crypt wall niche under a slab, a shallow grave under a
	 * stone) wakes into a box that overlaps blocks: it wouldn't suffocate (its eyes clear a half slab) but it would be
	 * stuck. It steps out to the nearest free spot instead: beside it on the same level first (the open side of a
	 * niche), then one up, then diagonals. If there is none it stays where it woke.
	 */
	static void stepOutIfStuck(ServerLevel level, Monster mob, BlockPos pos) {
		if (level.noCollision(mob, mob.getBoundingBox())) {
			return;
		}
		int[][] offsets = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
				{1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}};
		for (int[] o : offsets) {
			double x = pos.getX() + 0.5 + o[0];
			double y = pos.getY() + o[1];
			double z = pos.getZ() + 0.5 + o[2];
			if (level.noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(x, y, z))) {
				mob.snapTo(x, y, z, mob.getYRot(), mob.getXRot());
				return;
			}
		}
	}

	/** Chain waking: neighbours hear this through the normal listener path; at most {@link UndeadTuning#AWAKEN_CAP} wake per event. */
	public static void emitAwaken(ServerLevel level, Entity source) {
		int saved = SleeperListener.awakenBudget;
		SleeperListener.awakenBudget = UndeadTuning.AWAKEN_CAP;
		try {
			level.gameEvent(ModGameEvents.AWAKEN, source.position(), GameEvent.Context.of(source));
		} finally {
			SleeperListener.awakenBudget = saved;
		}
	}

	/** Empty-hand use: take one item (weapon first, then armor top-down) and roll the wake chance (armor always riskier). */
	public static void rob(ServerLevel level, BlockPos pos, SleeperBlockEntity sleeper, Player player) {
		for (EquipmentSlot slot : SleeperBlockEntity.SLOTS) {
			ItemStack stack = sleeper.getGear(slot);
			if (stack.isEmpty()) {
				continue;
			}
			sleeper.setGear(slot, ItemStack.EMPTY);
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
			level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.6F, 0.8F);
			float chance = (slot.getType() == EquipmentSlot.Type.HAND ? UndeadTuning.ROB_WEAPON_WAKE_CHANCE : UndeadTuning.ROB_ARMOR_WAKE_CHANCE)
					* sleeper.effectiveSensitivity(level);
			if (!sleeper.isWakePending() && level.getRandom().nextFloat() < chance) {
				sleeper.scheduleWake(level, player.position(), player.getUUID(), 2);
			}
			return;
		}
	}
}
