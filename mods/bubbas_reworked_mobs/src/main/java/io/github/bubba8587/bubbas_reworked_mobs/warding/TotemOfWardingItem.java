// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.warding;

import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperFinder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Totem of Warding (specs/undead.md "Warding"), held in either hand: every {@link WardingTuning#PULSE_INTERVAL_TICKS}
 * it pulses. Monsters within {@link WardingTuning#PULSE_RADIUS} flee from the holder for a few seconds and nearby
 * sleepers settle a little. No spawn-proofing. A player sees the rhythm as vanilla's item cooldown sweep on the slot
 * (the same as after an ender pearl), so there is no hidden timer; mobs holding one pulse on the same rhythm.
 */
public class TotemOfWardingItem extends Item {
	/** Next pulse for non-player holders (mobs have no item cooldowns). */
	private static final Map<Entity, Long> MOB_NEXT_PULSE = new WeakHashMap<>();

	public TotemOfWardingItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND || !(owner instanceof LivingEntity holder) || !holder.isAlive()
				|| holder.isSpectator()) {
			return;
		}
		if (holder instanceof Player player) {
			if (player.getCooldowns().isOnCooldown(stack)) {
				return;
			}
			player.getCooldowns().addCooldown(stack, WardingTuning.PULSE_INTERVAL_TICKS);
		} else {
			long now = level.getGameTime();
			Long next = MOB_NEXT_PULSE.get(holder);
			if (next != null && now < next) {
				return;
			}
			MOB_NEXT_PULSE.put(holder, now + WardingTuning.PULSE_INTERVAL_TICKS);
		}
		pulse(level, holder);
	}

	/** One pulse around {@code holder}: monsters flee, sleepers settle, soul-fire ring and a chime. */
	public static void pulse(ServerLevel level, LivingEntity holder) {
		long now = level.getGameTime();
		Vec3 centre = holder.position();
		double r = WardingTuning.PULSE_RADIUS;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, holder.getBoundingBox().inflate(r),
				m -> m != holder && m instanceof Enemy && m.isAlive() && m.distanceToSqr(holder) <= r * r)) {
			scare(mob, holder, now);
		}
		for (SleeperBlockEntity sleeper : SleeperFinder.within(level, centre, r)) {
			sleeper.calm(now, WardingTuning.PULSE_CALM_POINTS);
		}
		// A ring of soul fire flying outward at waist height, and a low chime.
		int count = 32;
		double y = centre.y + holder.getBbHeight() * 0.5;
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count;
			double dx = Math.cos(angle);
			double dz = Math.sin(angle);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, centre.x + dx * 0.6, y, centre.z + dz * 0.6, 0, dx, 0.0, dz, 0.18);
		}
		level.sendParticles(ParticleTypes.SOUL, centre.x, y, centre.z, 6, 0.4, 0.3, 0.4, 0.02);
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 0.6F);
	}

	private static void scare(Mob mob, LivingEntity holder, long now) {
		// Brain-driven monsters (piglins, hoglins) flee through their own vanilla avoid memory; goal-driven ones
		// (zombies, skeletons, creepers, spiders...) get the flee goal.
		if (mob.getBrain().checkMemory(MemoryModuleType.AVOID_TARGET, MemoryStatus.REGISTERED)) {
			mob.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, holder, WardingTuning.FLEE_TICKS);
		} else if (mob instanceof PathfinderMob pathfinder && WardFleeGoal.isGoalDriven(pathfinder)) {
			WardFleeGoal.of(pathfinder).scare(holder, now + WardingTuning.FLEE_TICKS);
		}
	}
}
