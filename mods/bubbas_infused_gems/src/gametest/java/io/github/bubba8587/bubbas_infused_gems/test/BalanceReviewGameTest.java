// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;
import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.close;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import io.github.bubba8587.bubbas_infused_gems.effect.GemEntity;
import io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.effect.GemValues;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModAttachments;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.VillagerInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** The balance review's bugs: each test would have caught one. */
public class BalanceReviewGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	private static void wear(LivingEntity e, String key) {
		Gem g = GemKit.gem(key, 1.0);
		for (int i = 0; i < 4; i++) {
			e.setItemSlot(GemTotals.ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), g, g, g));
		}
		WornEffects.refresh(e);
	}

	private static void weak(LivingEntity e, String key) {
		e.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem(key, 0.5)));
		WornEffects.refresh(e);
	}

	private static Mob pig(GameTestHelper helper, int x, int z) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(x, 1, z));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
		pig.setHealth(100.0F);
		return pig;
	}

	/** 1. */
	@GameTest
	public void weakGemsDoNotNegateFalls(GameTestHelper helper) {
		for (String key : List.of("wind_charged", "oozing")) {
			Mob pig = pig(helper, 1, 1);
			weak(pig, key);
			float factor = GemActions.landing(pig, 10.0);
			check(helper, factor > 0.5F && factor < 1.0F, key + ": a weak gem takes fall damage x" + factor);
			pig.discard();
		}
		Mob pig = pig(helper, 1, 1);
		weak(pig, "levitation");
		pig.setDeltaMovement(0, -0.8, 0);
		check(helper, WornEffects.gravity(pig, 0.08) > 0.05, "a weak Levitation gem stopped a fall: gravity " + WornEffects.gravity(pig, 0.08));
		helper.succeed();
	}

	/** 2. */
	@GameTest
	public void harmingStrikeKeepsTheHitCooldown(GameTestHelper helper) {
		Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("instant_damage", 1.0), GemKit.gem("poison", 1.0)));
		Mob pig = pig(helper, 2, 1);
		attacker.doHurtTarget(helper.getLevel(), pig);
		float afterFirst = pig.getHealth();
		check(helper, afterFirst < 100.0F - 2.0F, "the first hit and strike did " + (100.0F - afterFirst));
		check(helper, pig.damageCooldownTime > 10, "the strike reset the hit cooldown: " + pig.damageCooldownTime);
		pig.removeEffect(ModEffects.gem("poison"));
		for (int i = 0; i < 5; i++) {
			attacker.doHurtTarget(helper.getLevel(), pig);
		}
		close(pig.getHealth(), afterFirst, 1e-4, helper, "spam clicks during the cooldown landed damage");
		check(helper, !pig.hasEffect(ModEffects.gem("poison")), "spam clicks during the cooldown re-applied hit gems");
		helper.succeed();
	}

	/** 3. */
	@GameTest
	public void hitEffectsDoNotStackOnWornGems(GameTestHelper helper) {
		Balance b = Balance.get();
		Mob pig = pig(helper, 1, 1);
		wear(pig, "health_boost");
		double worn = 8 * b.effect("health_boost").wornCap();
		var hitEntry = new io.github.bubba8587.bubbas_infused_gems.effect.GemHits.Entry("health_boost", b.cap, false);
		pig.addEffect(new MobEffectInstance(ModEffects.gem("health_boost"), 200, GemMobEffect.amplifier(io.github.bubba8587.bubbas_infused_gems.effect.GemHits.potency(hitEntry))));
		double bonus = pig.getAttributeValue(Attributes.MAX_HEALTH) - pig.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
		check(helper, bonus <= worn + 1e-4, "worn and hit Health Boost added up: +" + bonus + " (cap +" + worn + ")");
		var entry = new io.github.bubba8587.bubbas_infused_gems.effect.GemHits.Entry("health_boost", b.cap, false);
		check(helper, io.github.bubba8587.bubbas_infused_gems.effect.GemHits.potency(entry) <= b.effect("health_boost").wornCap() + 1e-9, "hit potency past the worn cap");
		Mob other = pig(helper, 3, 3);
		other.addEffect(new MobEffectInstance(ModEffects.gem("absorption"), 200, 49));
		check(helper, other.getAbsorptionAmount() > 0.0F, "a new Absorption hit gave no shield");
		other.setAbsorptionAmount(0.0F);
		other.addEffect(new MobEffectInstance(ModEffects.gem("absorption"), 200, 49));
		close(other.getAbsorptionAmount(), 0.0, 1e-6, helper, "a refreshing Absorption hit refilled the shield");
		helper.succeed();
	}

	private static void strip(LivingEntity e) {
		for (EquipmentSlot slot : GemTotals.ARMOR) {
			e.setItemSlot(slot, ItemStack.EMPTY);
		}
		WornEffects.refresh(e);
	}

	/** 4. */
	@GameTest
	public void absorptionRefillsOnlyItsShareAndRemembersTheThrottle(GameTestHelper helper) {
		Balance b = Balance.get();
		double share = 16 * b.effect("absorption").wornCap();
		Mob fresh = pig(helper, 3, 1);
		fresh.tickCount = 1000;
		wear(fresh, "absorption");
		close(fresh.getAbsorptionAmount(), share, 1e-3, helper, "equipping fills the shield at once (owner)");
		fresh.setAbsorptionAmount(2.0F);
		strip(fresh);
		wear(fresh, "absorption");
		WornEffects.tick(helper.getLevel(), fresh, WornEffects.of(fresh));
		check(helper, fresh.getAbsorptionAmount() < share - 0.5, "a quick re-equip refilled the shield: " + fresh.getAbsorptionAmount());
		check(helper, ((GemEntity) fresh).bubbas$lastHurtTick() == fresh.tickCount, "a quick re-equip did not start the usual wait");
		fresh.tickCount += 600;
		WornEffects.tick(helper.getLevel(), fresh, WornEffects.of(fresh));
		close(fresh.getAbsorptionAmount(), share, 1e-3, helper, "refilled 30 s after the last damage");
		fresh.setAttached(ModAttachments.LAST_EQUIP_FILL, helper.getLevel().getGameTime() - 600);
		fresh.setAbsorptionAmount(0.0F);
		strip(fresh);
		wear(fresh, "absorption");
		close(fresh.getAbsorptionAmount(), share, 1e-3, helper, "equip-fill again once 30 s have passed");
		fresh.discard();
		Mob pig = pig(helper, 1, 1);
		wear(pig, "absorption");
		pig.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
		pig.setAbsorptionAmount(0.0F);
		((GemEntity) pig).bubbas$setLastHurtTick(pig.tickCount - 1000);
		WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
		close(pig.getAbsorptionAmount(), share, 1e-3, helper, "the refill went past the gem's share");
		check(helper, pig.getAttached(ModAttachments.LAST_EQUIP_FILL) != null, "no equip-fill time saved");
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		pig.saveWithoutId(out);
		Mob copy = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), out.buildResult()));
		copy.removeAllEffects();
		copy.setAbsorptionAmount(0.0F);
		WornEffects.refresh(copy);
		close(copy.getAbsorptionAmount(), 0.0, 1e-6, helper, "rejoining refilled the shield inside the throttle");
		helper.succeed();
	}

	/** 5. */
	@GameTest
	public void weakHeroGemHasNoEmeraldFloor(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		weak(player, "hero_of_the_village");
		Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(2, 1, 2));
		villager.setVillagerData(villager.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.FARMER))
				.withLevel(5));
		check(helper, !villager.getOffers().isEmpty(), "a farmer with no trades");
		((VillagerInvoker) villager).bubbas$updatePrices(player);
		for (MerchantOffer offer : villager.getOffers()) {
			int expected = (int) Math.floor(WornEffects.heroModifier(player, 0.0) * offer.getBaseCostA().getCount());
			check(helper, offer.getSpecialPriceDiff() == -expected, "price cut " + offer.getSpecialPriceDiff() + " on " + offer.getBaseCostA() + ", expected " + -expected);
		}
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** 6. */
	@GameTest
	public void enderIgnoresHarmlessProjectilesAndExplosions(GameTestHelper helper) {
		Mob pig = pig(helper, 2, 2);
		wear(pig, "ender");
		Zombie shooter = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 5));
		Arrow arrow = (Arrow) ((ArrowItem) Items.ARROW).createArrow(helper.getLevel(), new ItemStack(Items.ARROW), shooter, null);
		Vec3 at = pig.position();
		for (int i = 0; i < 40; i++) {
			pig.hurtServer(helper.getLevel(), pig.damageSources().arrow(arrow, shooter), 0.0F);
			pig.damageCooldownTime = 0;
			pig.hurtServer(helper.getLevel(), pig.damageSources().explosion(arrow, shooter), 1.0F);
			pig.damageCooldownTime = 0;
		}
		check(helper, pig.position().equals(at), "Ender blinked from a 0-damage projectile or an explosion");
		helper.succeed();
	}

	/** A weak Saturation gem fills food only to about 7 with a small share of saturation. */
	@GameTest(maxTicks = 100)
	public void weakAndMidSaturationStopShort(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		weak(player, "saturation");
		double s = GemValues.worn(player, "saturation");
		int target = WornEffects.saturationFoodTarget(s);
		check(helper, target >= 6 && target <= 8, "weak gem (" + s + ") fills to " + target);
		int top = run(helper, player, 2, 6000);
		check(helper, top == target, "weak gem reached food " + top + ", target " + target);
		check(helper, player.getFoodData().getSaturationLevel() < top * 0.5F,
				"weak gem saturation " + player.getFoodData().getSaturationLevel() + " at food " + top);
		Gem g = GemKit.gem("saturation", 1.0);
		player.setItemSlot(GemTotals.ARMOR[0], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[0]), g, g, g));
		player.setItemSlot(GemTotals.ARMOR[1], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[1]), g, g, g));
		WornEffects.refresh(player);
		double mid = GemValues.worn(player, "saturation");
		int midTarget = WornEffects.saturationFoodTarget(mid);
		int midTop = run(helper, player, 2, 3000);
		check(helper, midTarget > 8 && midTarget < 20 && midTop == midTarget, "mid build (" + mid + ") reached food " + midTop + ", target " + midTarget);
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** Ticks a Saturation build from the given food and returns the highest food level seen. */
	private static int run(GameTestHelper helper, ServerPlayer player, int food, int ticks) {
		player.getFoodData().setFoodLevel(food);
		player.getFoodData().setSaturation(0.0F);
		int top = food;
		for (int t = 0; t < ticks; t++) {
			WornEffects.tick(helper.getLevel(), player, WornEffects.of(player));
			top = Math.max(top, player.getFoodData().getFoodLevel());
		}
		return top;
	}

	/** Only the perfect Saturation set fills food to 20. */
	@GameTest
	public void perfectSaturationFeedsFastRegeneration(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		wear(player, "saturation");
		int target = WornEffects.saturationFoodTarget(GemValues.worn(player, "saturation"));
		check(helper, target == 20, "perfect set target " + target);
		player.setHealth(4.0F);
		player.getFoodData().setFoodLevel(10);
		player.getFoodData().setSaturation(0.0F);
		int top = 0;
		for (int t = 0; t < 400; t++) {
			WornEffects.tick(helper.getLevel(), player, WornEffects.of(player));
			player.getFoodData().tick(player);
			top = Math.max(top, player.getFoodData().getFoodLevel());
		}
		check(helper, top == 20, "a perfect Saturation set reached food " + top);
		check(helper, player.getHealth() - 4.0F > 6.0F, "a perfect Saturation set healed only " + (player.getHealth() - 4.0F) + " HP in 20 s");
		GemKit.remove(helper, player);
		helper.succeed();
	}
}
