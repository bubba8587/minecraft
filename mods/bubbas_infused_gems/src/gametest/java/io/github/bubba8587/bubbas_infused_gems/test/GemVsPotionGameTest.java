// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;
import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.close;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.ArrowInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Owner (open question 31): a gem and a vanilla effect of the same kind don't add, the stronger applies, with care for the
 * edge cases: a stronger potion running out (the gem takes over at once), a weaker potion (its timer runs, the gem tops
 * up to its own value), milk, beacons and conduits, golden apples, splash and tipped arrows, death or reload, and gear
 * taken off mid-potion. No gem modifier is ever left behind.
 */
public class GemVsPotionGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	private static void wear(LivingEntity e, String key) {
		Gem g = GemKit.gem(key, 1.0);
		for (int i = 0; i < 4; i++) {
			e.setItemSlot(GemTotals.ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), g, g, g));
		}
		WornEffects.refresh(e);
	}

	private static void strip(LivingEntity e) {
		for (EquipmentSlot slot : GemTotals.ARMOR) {
			e.setItemSlot(slot, ItemStack.EMPTY);
		}
		WornEffects.refresh(e);
	}

	private static double gemSpeed(LivingEntity e) {
		AttributeModifier m = e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BubbasInfusedGems.id("worn/speed/0"));
		return m == null ? 0.0 : m.amount();
	}

	/** Health Boost gems (a combat effect at the shared cap) carry the stronger-applies tests: +8 x s max health. */
	private static double gemBoost(LivingEntity e) {
		AttributeModifier m = e.getAttribute(Attributes.MAX_HEALTH).getModifier(BubbasInfusedGems.id("worn/health_boost/0"));
		return m == null ? 0.0 : m.amount();
	}

	/** Max health above the base: what actually applies. */
	private static double boost(LivingEntity e) {
		AttributeInstance a = e.getAttribute(Attributes.MAX_HEALTH);
		return a.getValue() - a.getBaseValue();
	}

	private static double unusedSpeed(LivingEntity e) {
		AttributeModifier m = e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BubbasInfusedGems.id("worn/speed/0"));
		return m == null ? 0.0 : m.amount();
	}

	/** Movement speed as a multiple of the base: what actually applies. */
	private static double speedFactor(LivingEntity e) {
		AttributeInstance a = e.getAttribute(Attributes.MOVEMENT_SPEED);
		return a.getValue() / a.getBaseValue();
	}

	private static void noGemModifiersLeft(GameTestHelper helper, LivingEntity e, String when) {
		for (var attribute : new net.minecraft.core.Holder[] {Attributes.MOVEMENT_SPEED, Attributes.MAX_HEALTH, Attributes.MAX_ABSORPTION, Attributes.BURNING_TIME}) {
			@SuppressWarnings("unchecked")
			AttributeInstance a = e.getAttribute(attribute);
			if (a != null) {
				for (AttributeModifier m : a.getModifiers()) {
					check(helper, !m.id().getNamespace().equals(BubbasInfusedGems.MOD_ID), when + ": gem modifier left behind " + m.id());
				}
			}
		}
	}

	private static Mob pig(GameTestHelper helper) {
		return helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
	}

	private static final double S = 0.90; // twelve perfect gems in netherite: the cap

	@GameTest(maxTicks = 100)
	public void strongerPotionRunsOutAndTheGemTakesOverAtOnce(GameTestHelper helper) {
		Mob pig = pig(helper);
		wear(pig, "health_boost");
		pig.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 10, 1));
		close(gemBoost(pig), 0.0, 1e-9, helper, "Health Boost II on: gems off");
		close(boost(pig), 8.0, 1e-6, helper, "Health Boost II alone applies");
		helper.succeedWhen(() -> {
			check(helper, !pig.hasEffect(MobEffects.HEALTH_BOOST), "potion still running");
			close(boost(pig), 8 * S, 1e-6, helper, "the gem took over the tick the potion ended");
		});
	}

	@GameTest
	public void weakerPotionKeepsItsTimerAndTheGemTopsUp(GameTestHelper helper) {
		Mob pig = pig(helper);
		wear(pig, "health_boost");
		pig.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 600, 0));
		check(helper, pig.getEffect(MobEffects.HEALTH_BOOST).getDuration() == 600, "the potion's timer is gone");
		close(boost(pig), 8 * S, 1e-6, helper, "Health Boost I + gems: the gems' value, not the sum");
		// Gear off mid-potion: the potion stays, the gem part goes, nothing left behind.
		strip(pig);
		close(boost(pig), 4.0, 1e-6, helper, "gear off: Health Boost I alone");
		pig.removeAllEffects();
		noGemModifiersLeft(helper, pig, "gear off and potion gone");
		close(boost(pig), 0.0, 1e-9, helper, "nothing left");
		helper.succeed();
	}

	@GameTest
	public void milkClearsThePotionAndTheGemStays(GameTestHelper helper) {
		Mob pig = pig(helper);
		wear(pig, "health_boost");
		pig.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 600, 1));
		close(gemBoost(pig), 0.0, 1e-9, helper, "Health Boost II on: gems off");
		pig.removeAllEffects(); // what milk does
		close(boost(pig), 8 * S, 1e-6, helper, "after milk: the gems");
		helper.succeed();
	}

	@GameTest
	public void beaconsAndConduitsFollowTheSameRule(GameTestHelper helper) {
		Mob pig = pig(helper);
		wear(pig, "health_boost");
		// Beacons and conduits apply ambient effects, refreshed every few seconds.
		pig.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 260, 1, true, true));
		close(boost(pig), 8.0, 1e-6, helper, "Health Boost II alone");
		pig.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 260, 1, true, true));
		close(boost(pig), 8.0, 1e-6, helper, "refreshed: still Health Boost II alone");
		strip(pig);
		pig.removeAllEffects();
		wear(pig, "water_breathing");
		pig.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
		pig.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 260, 0, true, true));
		AttributeModifier air = pig.getAttribute(Attributes.OXYGEN_BONUS).getModifier(BubbasInfusedGems.id("worn/water_breathing/0"));
		check(helper, air == null, "real Water Breathing on: the gem's air bonus should be off");
		helper.succeed();
	}

	@GameTest
	public void goldenAppleAbsorptionAndTotemEffectsCombine(GameTestHelper helper) {
		Mob pig = pig(helper);
		wear(pig, "absorption");
		double gemMax = 16 * S;
		close(pig.getMaxAbsorption(), gemMax, 1e-4, helper, "gem shield size");
		pig.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0)); // a golden apple: Absorption I, 4
		close(pig.getMaxAbsorption(), gemMax, 1e-4, helper, "golden apple + gems: the gems' size, not the sum");
		pig.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3)); // an enchanted apple: Absorption IV, 16
		close(pig.getMaxAbsorption(), 16.0, 1e-4, helper, "enchanted apple is stronger: its 16 alone");
		pig.removeAllEffects();
		close(pig.getMaxAbsorption(), gemMax, 1e-4, helper, "apple over: the gems again");
		strip(pig);
		noGemModifiersLeft(helper, pig, "absorption gear off");
		// Totem: Fire Resistance on top of Fire Resistance gems: the real effect wins (it is full immunity).
		wear(pig, "fire_resistance");
		pig.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
		AttributeModifier burn = pig.getAttribute(Attributes.BURNING_TIME).getModifier(BubbasInfusedGems.id("worn/fire_resistance/0"));
		check(helper, burn == null, "totem Fire Resistance on: the gem's shorter burning should step aside");
		helper.succeed();
	}

	@GameTest
	public void splashCloudsAndTippedArrowsUseTheSamePath(GameTestHelper helper) {
		Mob pig = pig(helper);
		wear(pig, "speed");
		// A splash potion, a lingering cloud and a tipped arrow all end in addEffect; the arrow is the real thing here.
		Mob shooter = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(3, 1, 1));
		Arrow arrow = (Arrow) ((ArrowItem) Items.TIPPED_ARROW).createArrow(helper.getLevel(),
				PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.STRONG_SWIFTNESS), shooter, null);
		((ArrowInvoker) arrow).bubbas$hit(pig);
		check(helper, pig.hasEffect(MobEffects.SPEED), "tipped arrow gave no Speed");
		double sp = Balance.get().effect("speed").wornCap();
		close(gemSpeed(pig), (1 + 0.4 * sp) / 1.4 - 1, 1e-6, helper, "Speed II arrow: the gems only add their part above it (raised Speed cap)");
		pig.removeAllEffects();
		close(gemSpeed(pig), 0.4 * sp, 1e-6, helper, "arrow's effect gone: gems back");
		helper.succeed();
	}

	@GameTest
	public void deathOrReloadLeavesNoModifierBehind(GameTestHelper helper) {
		Mob pig = pig(helper);
		wear(pig, "health_boost");
		pig.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 600, 0));
		// Saved and loaded again (a reload, or a respawned copy): gem modifiers are transient, so none come back by
		// themselves, and the next refresh (on joining the level) restores exactly the right value.
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		pig.saveWithoutId(out);
		Mob copy = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), out.buildResult()));
		close(gemBoost(copy), 0.0, 1e-9, helper, "a gem modifier survived a save");
		WornEffects.refresh(copy);
		close(boost(copy), 8 * S, 1e-6, helper, "after reload: gems over Health Boost I, not added");
		// Death clears effects and drops the armor: nothing of ours stays.
		strip(copy);
		copy.removeAllEffects();
		noGemModifiersLeft(helper, copy, "after death");
		close(Balance.get().cap, S, 1e-9, helper, "cap");
		helper.succeed();
	}
}
