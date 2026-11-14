// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import java.util.Comparator;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameEvents;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
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
		if (mob instanceof Zombie && mob.getMainHandItem().isEmpty()) {
			pickUpNearbyWeapon(level, pos, mob);
		}
		if (!level.addFreshEntity(mob)) {
			return null;
		}
		NonNullList<ItemStack> unused = sleeper.takeAllGear(); // the mob has it now; keep it from dropping as the block goes
		unused.clear();
		level.setBlock(pos, state.getValue(SleeperBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
				Block.UPDATE_ALL);
		if (sleeper.getWaker() != null && level.getEntity(sleeper.getWaker()) instanceof LivingEntity target
				&& !(target instanceof Player player && (player.isCreative() || player.isSpectator())) && mob.canAttack(target)) {
			mob.setTarget(target);
		}
		mob.playAmbientSound();
		emitAwaken(level, mob);
		return mob;
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

	/** A zombie that wakes empty-handed grabs the nearest weapon lying close by (e.g. the one stolen from it and dropped). */
	private static void pickUpNearbyWeapon(ServerLevel level, BlockPos pos, Monster mob) {
		List<ItemEntity> weapons = level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(UndeadTuning.WEAPON_PICKUP_RANGE),
				item -> item.isAlive() && item.getItem().is(ModTags.SLEEPER_WEAPONS));
		weapons.stream().min(Comparator.comparingDouble(item -> item.distanceToSqr(Vec3.atCenterOf(pos)))).ifPresent(item -> {
			mob.setItemSlot(EquipmentSlot.MAINHAND, item.getItem().split(1));
			mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
			if (item.getItem().isEmpty()) {
				item.discard();
			}
		});
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
					* sleeper.getSensitivity();
			if (!sleeper.isWakePending() && level.getRandom().nextFloat() < chance) {
				sleeper.scheduleWake(level, player.position(), player.getUUID(), 2);
			}
			return;
		}
	}
}
