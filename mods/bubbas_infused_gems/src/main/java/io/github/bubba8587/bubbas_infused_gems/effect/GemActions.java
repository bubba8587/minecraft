// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.UUID;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Gems beyond potions (owner, 2026-11-15): Wind Burst, Weaving and Oozing do what the thing does, and Silence, Ender and
 * Breeze give the wearer what sculk, chorus fruit and breezes do. Numbers from the balance model (WORN table).
 */
public final class GemActions {
	private GemActions() {
	}

	private static double num(String key, String name, double fallback) {
		return Balance.get().effect(key).wornNumber(name, fallback);
	}

	private static double rel(double s) {
		return Math.max(s, 0.0) / Balance.get().cap;
	}

	// ------------------------------------------------------------------ hits

	/** A Wind Burst, Weaving or Oozing gem's hit (an action, not a timed effect). */
	public static void hit(ServerLevel level, String key, double s, LivingEntity target, @Nullable Entity attacker, @Nullable Entity direct, ItemStack weapon) {
		switch (key) {
			case "wind_charged" -> windHit(level, s, target, direct != null ? direct : attacker, weapon);
			case "weaving" -> webHit(level, s, target);
			case "oozing" -> {
				if (target.isDeadOrDying() && attacker instanceof LivingEntity owner) {
					splitSlimes(level, s, target, owner);
				}
			}
			default -> {
			}
		}
	}

	/** Whether this weapon is a mace with the Wind Burst enchantment (owner: the gems go past the cap there). */
	public static boolean windMace(ServerLevel level, ItemStack weapon) {
		if (weapon == null || !weapon.is(Items.MACE)) {
			return false;
		}
		Holder<Enchantment> windBurst = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST);
		return EnchantmentHelper.getItemEnchantmentLevel(windBurst, weapon) > 0;
	}

	/** Wind Burst's launch at strength s: (up, away) in blocks/tick; a Wind Burst mace multiplies it past the cap. */
	public static double[] windLaunch(double s, boolean mace) {
		double r = rel(s) * (mace ? num("wind_charged", "mace_bonus", 1.6) : 1.0);
		return new double[] {num("wind_charged", "hit_up", 0.9) * r, num("wind_charged", "hit_push", 0.6) * r};
	}

	private static void windHit(ServerLevel level, double s, LivingEntity target, @Nullable Entity from, ItemStack weapon) {
		double[] launch = windLaunch(s, windMace(level, weapon));
		Vec3 away = from == null ? Vec3.ZERO : new Vec3(target.getX() - from.getX(), 0.0, target.getZ() - from.getZ());
		away = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize().scale(launch[1]);
		Vec3 v = target.getDeltaMovement();
		target.setDeltaMovement(v.x + away.x, Math.max(v.y, 0.0) + launch[0], v.z + away.z);
		target.syncVelocity = true;
		burst(level, target);
	}

	private static void burst(ServerLevel level, Entity at) {
		level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.getX(), at.getY(), at.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.playSound(null, at.getX(), at.getY(), at.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.6F, 1.0F);
	}

	private static void webHit(ServerLevel level, double s, LivingEntity target) {
		if (target.getRandom().nextDouble() >= num("weaving", "web_chance", 0.35) * rel(s)) {
			return;
		}
		BlockPos pos = target.blockPosition();
		// Only into air: never replaces a block (owner). The web is real and stays.
		if (level.getBlockState(pos).isAir()) {
			level.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
		}
	}

	/** Owned Oozing slimes alive near the owner (the cap counts these). */
	public static int ownedSlimes(ServerLevel level, LivingEntity owner) {
		UUID id = owner.getUUID();
		return level.getEntitiesOfClass(Slime.class, new AABB(owner.blockPosition()).inflate(128.0),
				slime -> id.equals(slime.getAttached(ModAttachments.SLIME_OWNER))).size();
	}

	private static void splitSlimes(ServerLevel level, double s, LivingEntity target, LivingEntity owner) {
		int count = rel(s) >= 0.5 ? (int) num("oozing", "slimes_max", 2) : (int) num("oozing", "slimes_min", 1);
		int room = (int) num("oozing", "slime_cap", 8) - ownedSlimes(level, owner);
		for (int i = 0; i < Math.min(count, room); i++) {
			// Neutral, so allowed in peaceful too (vanilla refuses slimes there).
			Slime slime = EntityTypes.SLIME.create(level, new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.TRIGGERED, true));
			if (slime == null) {
				return;
			}
			slime.setSize(1, true);
			slime.setAttached(ModAttachments.SLIME_OWNER, owner.getUUID());
			slime.setPersistenceRequired(); // owner: they stay (no despawn timer, no distance despawn)
			slime.snapTo(target.getX() + (target.getRandom().nextDouble() - 0.5), target.getY() + 0.5,
					target.getZ() + (target.getRandom().nextDouble() - 0.5), target.getRandom().nextFloat() * 360.0F, 0.0F);
			level.addFreshEntity(slime);
		}
	}

	/** Whether this mob is an Oozing slime (neutral: it never takes a target, drops nothing). */
	public static boolean ownedSlime(Entity entity) {
		return entity.hasAttached(ModAttachments.SLIME_OWNER);
	}

	public static boolean ownedBy(Entity entity, Entity other) {
		UUID owner = entity.getAttached(ModAttachments.SLIME_OWNER);
		return owner != null && owner.equals(other.getUUID());
	}

	// ------------------------------------------------------------------ worn

	/**
	 * A wearer would take fall damage: Wind Burst and Oozing cancel it and bounce the wearer (a small burst; a slime
	 * block's bounce). Returns true when the fall's damage is cancelled.
	 */
	public static boolean landing(LivingEntity entity, double fallDistance) {
		if (!(entity.level() instanceof ServerLevel level) || WornEffects.of(entity).isEmpty() || fallDistance <= 1.0) {
			return false;
		}
		double wind = GemValues.value(entity, "wind_charged");
		double ooze = GemValues.value(entity, "oozing");
		boolean hurts = fallDistance > entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE);
		double bounce = 0.0;
		if (wind > 0.0 && hurts) {
			bounce = Math.max(bounce, num("wind_charged", "land_bounce", 0.6) * rel(wind));
			burst(level, entity);
		}
		if (ooze > 0.0 && !entity.isSuppressingBounce()) {
			double impact = Math.sqrt(2.0 * 0.08 * fallDistance);
			bounce = Math.max(bounce, impact * num("oozing", "bounce", 0.9) * rel(ooze));
		}
		if (bounce <= 0.0 && !(ooze > 0.0)) {
			return false;
		}
		Vec3 v = entity.getDeltaMovement();
		entity.setDeltaMovement(v.x, bounce, v.z);
		entity.syncVelocity = true;
		entity.resetFallDistance();
		return true;
	}

	/** Weaving worn: the cobweb slowdown moves s/cap of the way to none. */
	public static Vec3 webSlowdown(LivingEntity entity, Vec3 multiplier) {
		double r = Math.min(1.0, rel(GemValues.value(entity, "weaving")));
		if (r <= 0.0) {
			return multiplier;
		}
		return new Vec3(multiplier.x + (1.0 - multiplier.x) * r, multiplier.y + (1.0 - multiplier.y) * r, multiplier.z + (1.0 - multiplier.z) * r);
	}

	/** Silence worn: whether this movement vibration from the wearer is dropped (chance s/cap; all at the cap). */
	public static boolean silenced(@Nullable Entity source, Holder<GameEvent> event) {
		if (!(source instanceof LivingEntity entity) || WornEffects.of(entity).isEmpty() && GemValues.hit(entity, "silence") <= 0.0) {
			return false;
		}
		if (!(event.is(GameEvent.STEP) || event.is(GameEvent.SWIM) || event.is(GameEvent.SPLASH) || event.is(GameEvent.HIT_GROUND)
				|| event.is(GameEvent.FLAP) || event.is(GameEvent.ELYTRA_GLIDE))) {
			return false;
		}
		double r = rel(GemValues.value(entity, "silence"));
		return r > 0.0 && (r >= 1.0 - 1e-9 || entity.getRandom().nextDouble() < r);
	}

	/** Ender worn: a projectile would hurt the wearer; with chance, chorus fruit's random teleport instead (true: no damage). */
	public static boolean blink(ServerLevel level, LivingEntity entity, Entity direct) {
		if (!(direct instanceof Projectile projectile) || projectile.getOwner() == entity) {
			return false;
		}
		double r = rel(GemValues.value(entity, "ender"));
		if (r <= 0.0 || entity.getRandom().nextDouble() >= num("ender", "chance", 0.5) * r) {
			return false;
		}
		double lo = num("ender", "diameter_min", 4.0);
		double diameter = lo + (num("ender", "diameter_max", 16.0) - lo) * Math.min(r, 1.0);
		for (int attempt = 0; attempt < 16; attempt++) {
			double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * diameter;
			double y = Math.clamp(entity.getY() + (entity.getRandom().nextDouble() - 0.5) * diameter, level.getMinY(), level.getMinY() + level.getLogicalHeight() - 1);
			double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * diameter;
			Vec3 old = entity.position();
			if (entity.randomTeleport(x, y, z, true, BlockTags.CONSUMABLE_DOES_NOT_TELEPORT_TO)) {
				level.gameEvent(GameEvent.TELEPORT, old, GameEvent.Context.of(entity));
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
				entity.resetFallDistance();
				return true;
			}
		}
		return false;
	}

	/** Breeze worn: with chance, an incoming projectile is deflected the way a breeze deflects it. */
	public static ProjectileDeflection deflection(LivingEntity entity, Projectile projectile, ProjectileDeflection vanilla) {
		if (vanilla != ProjectileDeflection.NONE || projectile.getOwner() == entity || entity.level().isClientSide()) {
			return vanilla;
		}
		double r = rel(GemValues.value(entity, "breeze"));
		if (r <= 0.0 || entity.getRandom().nextDouble() >= num("breeze", "chance", 0.5) * r) {
			return vanilla;
		}
		return (p, e, random, power) -> {
			e.level().playSound(null, e, SoundEvents.BREEZE_DEFLECT, e.getSoundSource(), 1.0F, 1.0F);
			ProjectileDeflection.REVERSE.deflect(p, e, random, power);
		};
	}
}
