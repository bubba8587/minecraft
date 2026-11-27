// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.core.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AllOf;
import net.minecraft.world.item.enchantment.effects.DamageEntity;
import net.minecraft.world.item.enchantment.effects.PlaySoundEffect;
import net.minecraft.world.item.enchantment.effects.SpawnParticlesEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;

/**
 * Combat rules for our woken undead (specs/undead.md "Tuning"): armor-shredding fist streaks, Smite's chance to kill
 * outright, and arrow damage (skeletons shrug arrows off, zombies take headshots). Vanilla mobs are untouched: every
 * rule checks {@link ModTags#WOKEN_UNDEAD} or runs in our own entity classes.
 */
public final class UndeadCombat {
	/** Smite's outright kill. Bypasses armor, the hit cooldown (the sword hit just set it), enchantments and resistance. */
	public static final ResourceKey<DamageType> PURGED = ResourceKey.create(Registries.DAMAGE_TYPE, BubbasReworkedMobs.id("purged"));

	/** Each undead's current fist streak (one victim at a time). Weak keys: a dead or unloaded mob's entry goes with it. */
	private static final Map<Entity, Streak> STREAKS = new WeakHashMap<>();

	private UndeadCombat() {
	}

	public static final ResourceKey<PlacedFeature> FROZEN_FROSTBITE = ResourceKey.create(Registries.PLACED_FEATURE, BubbasReworkedMobs.id("frozen_frostbite"));

	public static void initialize() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, BubbasReworkedMobs.id("frozen_frostbite"), FrozenFrostbiteFeature.CODEC);
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.ICE_CAVES), GenerationStep.Decoration.UNDERGROUND_DECORATION, FROZEN_FROSTBITE);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamage, damage, blocked) -> {
			if (!blocked && baseDamage > 0.0F) {
				shredArmor(victim, source);
			}
		});
		EnchantmentEvents.MODIFY_WITH_LOOKUP.register((key, builder, source, registries) -> {
			if (key != Enchantments.SMITE) {
				return;
			}
			var damageTypes = registries.lookup(Registries.DAMAGE_TYPE).orElseThrow();
			var entityTypes = registries.lookup(Registries.ENTITY_TYPE).orElseThrow();
			builder.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
					AllOf.entityEffects(
							new SpawnParticlesEffect(ParticleTypes.SOUL_FIRE_FLAME, SpawnParticlesEffect.inBoundingBox(), SpawnParticlesEffect.inBoundingBox(),
									SpawnParticlesEffect.movementScaled(0.0F), SpawnParticlesEffect.fixedVelocity(ConstantFloat.of(0.15F)), ConstantFloat.of(0.1F)),
							new SpawnParticlesEffect(ParticleTypes.SOUL, SpawnParticlesEffect.inBoundingBox(), SpawnParticlesEffect.inBoundingBox(),
									SpawnParticlesEffect.movementScaled(0.0F), SpawnParticlesEffect.fixedVelocity(ConstantFloat.of(0.05F)), ConstantFloat.of(0.0F)),
							new PlaySoundEffect(List.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ZOMBIE_VILLAGER_CURE)), ConstantFloat.of(1.0F), ConstantFloat.of(1.4F)),
							new DamageEntity(LevelBasedValue.constant(10000.0F), LevelBasedValue.constant(10000.0F), damageTypes.getOrThrow(PURGED))),
					LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
									EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(entityTypes, ModTags.WOKEN_UNDEAD)))
							.and(LootItemRandomChanceCondition.randomChance(
									ContextFloatProviders.forEnchantmentLevel(LevelBasedValue.perLevel(UndeadTuning.SMITE_KILL_CHANCE_PER_LEVEL)))));
		});
	}

	// --- fists ------------------------------------------------------------------------------------

	/** A bare-fisted melee hit from one of ours: extra armor wear that grows while the hits keep coming. */
	private static void shredArmor(LivingEntity victim, DamageSource source) {
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity undead) || source.getDirectEntity() != attacker || !attacker.is(ModTags.WOKEN_UNDEAD)
				|| !undead.getMainHandItem().isEmpty()) {
			return;
		}
		long now = victim.level().getGameTime();
		Streak streak = STREAKS.get(attacker);
		if (streak == null || streak.victimId != victim.getId() || now - streak.lastHit > UndeadTuning.FIST_STREAK_WINDOW_TICKS) {
			streak = new Streak(victim.getId());
			STREAKS.put(attacker, streak);
		}
		streak.hits++;
		streak.lastHit = now;
		int[] wear = UndeadTuning.FIST_STREAK_ARMOR_WEAR;
		int extra = wear[Math.min(streak.hits, wear.length) - 1];
		if (extra <= 0) {
			return;
		}
		List<EquipmentSlot> worn = new ArrayList<>(4);
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = victim.getItemBySlot(slot);
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			if (equippable != null && equippable.damageOnHurt() && stack.isDamageableItem() && stack.canBeHurtBy(source)) {
				worn.add(slot);
			}
		}
		// Spread evenly; the remainder goes to the top pieces first.
		for (int i = 0; i < worn.size(); i++) {
			int share = extra / worn.size() + (i < extra % worn.size() ? 1 : 0);
			if (share > 0) {
				victim.getItemBySlot(worn.get(i)).hurtAndBreak(share, victim, worn.get(i));
			}
		}
	}

	/** Length of an undead's current fist streak against {@code victim} (0 if none or it lapsed); for tests and debugging. */
	public static int fistStreak(Entity undead, LivingEntity victim) {
		Streak streak = STREAKS.get(undead);
		if (streak == null || streak.victimId != victim.getId() || victim.level().getGameTime() - streak.lastHit > UndeadTuning.FIST_STREAK_WINDOW_TICKS) {
			return 0;
		}
		return streak.hits;
	}

	private static final class Streak {
		final int victimId;
		int hits;
		long lastHit;

		Streak(int victimId) {
			this.victimId = victimId;
		}
	}

	// --- arrows -----------------------------------------------------------------------------------

	/** Damage an arrow deals to one of our skeletons (called from its hurtServer, before armor). */
	public static float skeletonDamage(DamageSource source, float damage) {
		return source.getDirectEntity() instanceof AbstractArrow ? damage * UndeadTuning.SKELETON_ARROW_DAMAGE_FACTOR : damage;
	}

	/**
	 * Damage an arrow deals to one of our zombies. When the arrow hurts, vanilla has just moved it to the hit point on
	 * the zombie's (slightly inflated) hitbox, so its height says where it struck.
	 */
	public static float zombieDamage(LivingEntity zombie, DamageSource source, float damage) {
		if (source.getDirectEntity() instanceof AbstractArrow arrow && isHeadHit(zombie, arrow.getY())) {
			return damage * UndeadTuning.ZOMBIE_HEADSHOT_FACTOR;
		}
		return damage;
	}

	public static boolean isHeadHit(LivingEntity mob, double hitY) {
		return hitY >= mob.getY() + mob.getBbHeight() * (1.0F - UndeadTuning.HEAD_FRACTION_OF_HEIGHT);
	}

	/** True when this damage source is our Smite kill. */
	public static boolean isPurge(DamageSource source) {
		return source.is(PURGED);
	}
}
