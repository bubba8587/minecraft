// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;
import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.close;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemArrow;
import io.github.bubba8587.bubbas_infused_gems.effect.GemEntity;
import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.ArrowInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Gem effects match the model: armor gems on the wearer (attribute values, damage hooks, healing; the best build),
 * drawbacks on the wearer, weapon gems on what they hit (potency and duration, charged Harming, drawbacks on the
 * target), and bows tipping their arrows (gem effect on hit, plain pickup, the stronger of gem and real tipped arrow).
 */
public class GemEffectsGameTest {
	private static final EquipmentSlot[] ARMOR = GemTotals.ARMOR;
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	/** A wearer that isn't undead (undead swap Healing and Harming gems). */
	private static net.minecraft.world.entity.Mob wearer(GameTestHelper helper, String material, Gem... perPiece) {
		net.minecraft.world.entity.Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.VINDICATOR, new BlockPos(1, 1, 1));
		for (int i = 0; i < 4; i++) {
			zombie.setItemSlot(ARMOR[i], GemKit.socketed(BalanceGameTest.armor(material, PIECES[i]), perPiece));
		}
		return zombie;
	}

	private static double modifier(LivingEntity entity, Holder<Attribute> attribute, String key, int i) {
		AttributeModifier m = entity.getAttribute(attribute).getModifier(BubbasInfusedGems.id("worn/" + key + "/" + i));
		return m == null ? 0.0 : m.amount();
	}

	@GameTest
	public void bestArmorBuildReachesTheCap(GameTestHelper helper) {
		Gem perfect = GemKit.gem("speed", 1.0);
		var zombie = wearer(helper, "netherite", perfect, perfect, perfect);
		helper.succeedWhen(() -> {
			double s = WornEffects.of(zombie).value("speed");
			close(s, Balance.get().effect("speed").wornCap(), 1e-6, helper, "best build speed total (Speed's raised cap)");
			close(modifier(zombie, Attributes.MOVEMENT_SPEED, "speed", 0), 0.4 * Balance.get().effect("speed").wornCap(), 1e-6, helper, "movement speed modifier");
			zombie.discard();
		});
	}

	// Core test: registered by InfusedGemsCoreGameTest (test-lite, specs/quality.md).
	public void wornEffectsMatchTheJson(GameTestHelper helper) {
		Balance b = Balance.get();
		// One mixed gem per piece of full iron: 4 gems of each effect, so every total is the same known number.
		Gem strength = GemKit.gem("strength", 0.9, "jump_boost", 0.5, "weakness");
		var zombie = wearer(helper, "iron", strength, GemKit.gem("resistance", 0.8, "water_breathing", 0.5, null),
				GemKit.gem("instant_health", 1.0, null, 0, null));
		helper.succeedWhen(() -> {
			GemTotals t = WornEffects.of(zombie);
			double pf = b.armorMaterials.get("iron") * b.run.get(4);
			double sStrength = b.stack(List.of(0.9 * pf, 0.9 * pf, 0.9 * pf, 0.9 * pf), b.armorNorm) * b.effect("strength").wornCap() / b.cap;
			close(t.value("strength"), sStrength, 1e-6, helper, "strength total");
			close(modifier(zombie, Attributes.ATTACK_DAMAGE, "strength", 0), 6.0 * sStrength, 1e-6, helper, "attack damage");
			double sJump = b.stack(List.of(0.5 * pf, 0.5 * pf, 0.5 * pf, 0.5 * pf), b.armorNorm) * b.effect("jump_boost").wornCap() / b.cap;
			close(modifier(zombie, Attributes.JUMP_STRENGTH, "jump_boost", 0), 0.2 * sJump, 1e-6, helper, "jump strength");
			close(modifier(zombie, Attributes.SAFE_FALL_DISTANCE, "jump_boost", 1), 2.0 * sJump, 1e-6, helper, "safe fall");
			// Four Weakness drawbacks: the saturating drawback size.
			close(modifier(zombie, Attributes.ATTACK_DAMAGE, "weakness", 0), -4.0 * b.drawbackTotal(4), 1e-6, helper, "weakness drawback");
			double sWater = b.stack(List.of(0.5 * pf, 0.5 * pf, 0.5 * pf, 0.5 * pf), b.armorNorm) * b.effect("water_breathing").wornCap() / b.cap;
			close(modifier(zombie, Attributes.OXYGEN_BONUS, "water_breathing", 0), sWater / (1 - sWater), 1e-6, helper, "oxygen bonus");
			// Resistance through the damage hook.
			double sRes = b.stack(List.of(0.8 * pf, 0.8 * pf, 0.8 * pf, 0.8 * pf), b.armorNorm) * b.effect("resistance").wornCap() / b.cap;
			float hurt = WornEffects.modifyDamage(zombie, zombie.damageSources().generic(), 10.0F);
			close(hurt, 10.0 * (1 - 0.4 * sRes), 1e-4, helper, "resistance damage");
			// Healing gem: a survived attack gives back its share.
			double sHeal = b.stack(List.of(pf, pf, pf, pf), b.armorNorm) * b.effect("instant_health").wornCap() / b.cap;
			zombie.setHealth(10.0F);
			WornEffects.afterDamage(zombie, zombie.damageSources().mobAttack(zombie), 4.0F);
			close(zombie.getHealth(), 10.0 + 4.0 * b.healFraction(sHeal), 1e-4, helper, "healing gem");
			zombie.discard();
		});
	}

	// Core test: registered by InfusedGemsCoreGameTest (test-lite, specs/quality.md).
	public void meleeWeaponGemsHitTheTarget(GameTestHelper helper) {
		Balance b = Balance.get();
		Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Pig target = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 1));
		Gem poison = GemKit.gem("poison", 1.0, null, 0, "speed");
		attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, poison, GemKit.gem("instant_damage", 1.0)));
		target.setHealth(target.getMaxHealth());
		attacker.doHurtTarget(helper.getLevel(), target);
		double s = b.stack(List.of(1.0), b.weaponNorm); // one perfect gem of each in a netherite weapon
		MobEffectInstance effect = target.getEffect(ModEffects.gem("poison"));
		check(helper, effect != null, "no gem poison on the target");
		check(helper, effect.getAmplifier() == GemMobEffect.amplifier(Math.sqrt(s / b.cap)), "poison amplifier " + effect.getAmplifier());
		check(helper, Math.abs(effect.getDuration() - GemHits.duration(new GemHits.Entry("poison", s, false))) <= 1, "poison duration " + effect.getDuration());
		MobEffectInstance drawback = target.getEffect(ModEffects.gem("speed"));
		check(helper, drawback != null && drawback.getAmplifier() == 9 && drawback.getDuration() == 60, "speed drawback on the target: " + drawback);
		// Harming strikes on every hit (owner): a second hit strikes again.
		target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
		target.setHealth(60.0F);
		target.damageCooldownTime = 0;
		attacker.doHurtTarget(helper.getLevel(), target);
		check(helper, 60.0F - target.getHealth() >= GemHits.strike("instant_damage", s), "Harming didn't strike on the second hit");
		attacker.discard();
		target.discard();
		helper.succeed();
	}

	@GameTest
	public void bowsTipArrowsAndPickupStaysPlain(GameTestHelper helper) {
		Zombie shooter = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Pig target = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 1));
		ItemStack bow = GemKit.socketed(Items.BOW, GemKit.gem("poison", 1.0), GemKit.gem("slowness", 1.0));
		Arrow arrow = (Arrow) ((ArrowItem) Items.ARROW).createArrow(helper.getLevel(), new ItemStack(Items.ARROW), shooter, bow);
		check(helper, !((GemArrow) arrow).bubbas$gemHit().isEmpty(), "arrow not tipped");
		check(helper, arrow.getColor() != -1, "gem arrow has no trail color");
		ItemStack pickup = arrow.getPickupItemStackOrigin();
		check(helper, pickup.is(Items.ARROW) && !pickup.has(DataComponents.POTION_CONTENTS), "pickup is not a plain arrow: " + pickup);
		((ArrowInvoker) arrow).bubbas$hit(target);
		check(helper, target.hasEffect(ModEffects.gem("poison")) && target.hasEffect(ModEffects.gem("slowness")), "arrow hit applied no gem effects");

		// A real strong poison arrow beats the gem (vanilla Poison II applies, the gem's poison doesn't)...
		Pig t2 = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		Arrow strong = (Arrow) ((ArrowItem) Items.TIPPED_ARROW).createArrow(helper.getLevel(),
				PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.STRONG_POISON), shooter, bow);
		((ArrowInvoker) strong).bubbas$hit(t2);
		check(helper, t2.hasEffect(MobEffects.POISON) && !t2.hasEffect(ModEffects.gem("poison")), "strong tipped arrow vs gem: both or neither");
		check(helper, t2.hasEffect(ModEffects.gem("slowness")), "the gem's other effect is lost");
		check(helper, strong.getPickupItemStackOrigin().is(Items.TIPPED_ARROW), "tipped arrow came back plain");
		// ...and the gem beats a weak one.
		Pig t3 = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 3));
		Arrow weak = (Arrow) ((ArrowItem) Items.TIPPED_ARROW).createArrow(helper.getLevel(),
				PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON), shooter, bow);
		((ArrowInvoker) weak).bubbas$hit(t3);
		check(helper, !t3.hasEffect(MobEffects.POISON) && t3.hasEffect(ModEffects.gem("poison")), "weak tipped arrow vs gem: both or neither");
		for (LivingEntity e : List.of(shooter, target, t2, t3)) {
			e.discard();
		}
		helper.succeed();
	}

	@GameTest
	public void gemPoisonNeverKillsButWitherCan(GameTestHelper helper) {
		Holder<MobEffect> poison = ModEffects.gem("poison");
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
		pig.setHealth(1.5F);
		poison.value().applyEffectTick(helper.getLevel(), pig, 99);
		check(helper, pig.getHealth() >= 1.0F, "gem poison took the pig below 1 HP: " + pig.getHealth());
		pig.damageCooldownTime = 0;
		ModEffects.gem("wither").value().applyEffectTick(helper.getLevel(), pig, 99);
		check(helper, pig.getHealth() < 1.5F, "gem wither did nothing");
		pig.discard();
		helper.succeed();
	}
}
