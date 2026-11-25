// dte:C1,B17,B15
package io.github.bubba8587.bubbas_reworked_mobs.soulfire;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.BaseFireBlock;

/** Soulfire burning and Soulguard (specs/soulfire.md): the per-entity state and its rules. */
public final class SoulBurning {
	/** Set while a soul source lights an entity, so the ignition doesn't count as normal fire. */
	private static final ThreadLocal<Boolean> SOUL_IGNITING = ThreadLocal.withInitial(() -> false);

	private SoulBurning() {
	}

	static void initialize() {
		// Soulguard: no Soulfire damage of any kind
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> !(source.is(Soulfire.BYPASSES_TOTEMS) && hasSoulguard(entity)));
	}

	// --- state ---

	public static boolean isSoulBurning(Entity entity) {
		return Boolean.TRUE.equals(entity.getAttached(Soulfire.SOUL_BURNING));
	}

	private static void mark(Entity entity) {
		if (!isSoulBurning(entity)) {
			entity.setAttached(Soulfire.SOUL_BURNING, true);
		}
	}

	/** Back to normal burning (or none). */
	public static void clear(Entity entity) {
		if (entity.hasAttached(Soulfire.SOUL_BURNING)) {
			entity.removeAttached(Soulfire.SOUL_BURNING);
		}
	}

	// --- Soulguard ---

	/** The Soulguard effect, or the Soulguard enchantment on something worn. */
	public static boolean hasSoulguard(Entity entity) {
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		if (living.hasEffect(Soulfire.SOULGUARD)) {
			return true;
		}
		return living.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.flatMap(r -> r.get(Soulfire.SOULGUARD_ENCHANTMENT))
				.map(h -> EnchantmentHelper.getEnchantmentLevel(h, living) > 0).orElse(false);
	}

	public static boolean isUndead(Entity entity) {
		return entity.is(EntityTypeTags.UNDEAD);
	}

	// --- sources ---

	/** Soul fire / a lit soul campfire touching: vanilla's fire ignite (BaseFireBlock.fireIgnite), as Soulfire. */
	public static void contactIgnite(Entity entity) {
		if (hasSoulguard(entity)) {
			entity.clearFire();
			clear(entity);
			return;
		}
		soulIgnite(entity, () -> BaseFireBlock.fireIgnite(entity));
	}

	/** Sets an entity alight with Soulfire for this many seconds (Soulfire charges). */
	public static void igniteForSeconds(Entity entity, float seconds) {
		if (hasSoulguard(entity)) {
			return;
		}
		soulIgnite(entity, () -> entity.igniteForSeconds(seconds));
	}

	/** Sets an entity alight with Soulfire for at least this many ticks (Phasing). */
	public static void igniteForTicks(Entity entity, int ticks) {
		if (hasSoulguard(entity)) {
			return;
		}
		soulIgnite(entity, () -> entity.igniteForTicks(ticks));
	}

	private static void soulIgnite(Entity entity, Runnable ignite) {
		SOUL_IGNITING.set(true);
		try {
			ignite.run();
		} finally {
			SOUL_IGNITING.set(false);
		}
		if (!entity.level().isClientSide() && entity.getRemainingFireTicks() > 0) {
			mark(entity);
		}
	}

	/** Any other ignition (fire, lava, fire charges, Fire Aspect, flaming arrows, lightning) is normal burning. */
	public static void onIgnite(Entity entity) {
		if (!SOUL_IGNITING.get() && !entity.level().isClientSide()) {
			clear(entity);
		}
	}

	// --- damage ---

	/** Contact damage from soul fire or a lit soul campfire; none with Soulguard. */
	public static void hurtByContact(Entity entity, boolean campfire, float vanillaAmount) {
		if (hasSoulguard(entity) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		float fire = campfire ? SoulfireTuning.UNDEAD_CAMPFIRE_CONTACT_DAMAGE : SoulfireTuning.UNDEAD_FIRE_CONTACT_DAMAGE;
		if (isUndead(entity)) {
			entity.hurtServer(level, entity.damageSources().source(campfire ? Soulfire.SOUL_CAMPFIRE : Soulfire.SOUL_FIRE), fire);
		} else if (fireResistant(entity)) {
			entity.hurtServer(level, entity.damageSources().source(Soulfire.SOUL_SEAR), vanillaAmount - fire);
		} else {
			entity.hurtServer(level, entity.damageSources().source(campfire ? Soulfire.SOUL_CAMPFIRE : Soulfire.SOUL_FIRE), vanillaAmount);
		}
	}

	/** The once-a-second burn while soul-burning: normal burn damage, plus the soul bonus against the living. */
	public static boolean hurtByBurn(ServerLevel level, Entity entity) {
		if (!isUndead(entity) && fireResistant(entity)) {
			return entity.hurtServer(level, entity.damageSources().source(Soulfire.SOUL_SEAR), SoulfireTuning.BURN_DAMAGE * (SoulfireTuning.SOUL_BONUS - 1.0F));
		}
		float amount = SoulfireTuning.BURN_DAMAGE * (isUndead(entity) ? 1.0F : SoulfireTuning.SOUL_BONUS);
		return entity.hurtServer(level, entity.damageSources().source(Soulfire.SOUL_BURN), amount);
	}

	/** Fire Resistance blocks the fire part of Soulfire; the soul part gets through. */
	private static boolean fireResistant(Entity entity) {
		return entity instanceof net.minecraft.world.entity.LivingEntity living && living.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
	}

	/** Each server tick, after the fire timer: ends the state when the fire is out, puts it out under Soulguard, sounds. */
	public static void tick(ServerLevel level, Entity entity) {
		if (!isSoulBurning(entity)) {
			return;
		}
		if (entity.getRemainingFireTicks() <= 0 || entity.fireImmune()) {
			clear(entity);
			return;
		}
		if (hasSoulguard(entity)) {
			entity.clearFire();
			clear(entity);
			return;
		}
		if (entity.tickCount % SoulfireTuning.SOUND_INTERVAL == 0 && entity.getRandom().nextInt(SoulfireTuning.SOUND_ONE_IN) == 0 && !entity.isSilent()) {
			SoundEvent sound = switch (entity.getRandom().nextInt(3)) {
				case 0 -> SoundEvents.SOUL_ESCAPE.value();
				case 1 -> SoundEvents.SOUL_SAND_STEP;
				default -> SoundEvents.SOUL_SOIL_HIT;
			};
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(), 0.6F,
					0.8F + entity.getRandom().nextFloat() * 0.4F);
		}
	}
}
