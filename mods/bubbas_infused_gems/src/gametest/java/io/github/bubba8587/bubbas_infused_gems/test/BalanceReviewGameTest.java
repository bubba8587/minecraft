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

/** The balance review's bugs (2026-11-15): each test would have caught one. */
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

	/** 1. A weak Wind Burst or Oozing gem only softens a fall; a weak Levitation gem doesn't stop one. */
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

	/** 2. A Harming strike leaves the hit's cooldown alone: clicking again during it lands nothing, no gems again. */
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

	/** 3. A hit gem effect and worn gems of the same effect don't add; hit potency stays under the worn cap; Absorption fills once. */
	@GameTest
	public void hitEffectsDoNotStackOnWornGems(GameTestHelper helper) {
		Balance b = Balance.get();
		Mob pig = pig(helper, 1, 1);
		wear(pig, "health_boost");
		double worn = 8 * b.cap;
		var hitEntry = new io.github.bubba8587.bubbas_infused_gems.effect.GemHits.Entry("health_boost", b.cap, false);
		pig.addEffect(new MobEffectInstance(ModEffects.gem("health_boost"), 200, GemMobEffect.amplifier(io.github.bubba8587.bubbas_infused_gems.effect.GemHits.potency(hitEntry))));
		double bonus = pig.getAttributeValue(Attributes.MAX_HEALTH) - pig.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
		check(helper, bonus <= worn + 1e-4, "worn and hit Health Boost added up: +" + bonus + " (cap +" + worn + ")");
		var entry = new io.github.bubba8587.bubbas_infused_gems.effect.GemHits.Entry("health_boost", b.cap, false);
		check(helper, io.github.bubba8587.bubbas_infused_gems.effect.GemHits.potency(entry) <= b.cap + 1e-9, "hit potency past the worn cap");
		Mob other = pig(helper, 3, 3);
		other.addEffect(new MobEffectInstance(ModEffects.gem("absorption"), 200, 49));
		check(helper, other.getAbsorptionAmount() > 0.0F, "a new Absorption hit gave no shield");
		other.setAbsorptionAmount(0.0F);
		other.addEffect(new MobEffectInstance(ModEffects.gem("absorption"), 200, 49));
		close(other.getAbsorptionAmount(), 0.0, 1e-6, helper, "a refreshing Absorption hit refilled the shield");
		helper.succeed();
	}

	/** 4. The refill fills only the gem's share, and the equip-fill throttle survives a save (rejoin). */
	@GameTest
	public void absorptionRefillsOnlyItsShareAndRemembersTheThrottle(GameTestHelper helper) {
		Balance b = Balance.get();
		Mob pig = pig(helper, 1, 1);
		wear(pig, "absorption");
		double share = 16 * b.cap;
		pig.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3)); // an enchanted golden apple: 16
		pig.setAbsorptionAmount(0.0F);
		((GemEntity) pig).bubbas$setLastHurtTick(pig.tickCount - 1000);
		WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
		close(pig.getAbsorptionAmount(), share, 1e-3, helper, "the refill went past the gem's share");
		check(helper, pig.getAttached(ModAttachments.LAST_EQUIP_FILL) != null, "no equip-fill time saved");
		// Rejoin: a saved copy with the same gear refreshes without filling (the throttle came along).
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

	/** 5. A weak Hero of the Village gem can round to nothing off: no 1-emerald floor. */
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

	/** 6. Ender never blinks from a harmless projectile or from an explosion. */
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

	/** 7. The Oozing slime cap is a true per-owner count: a slime gone for good frees a place. */
	@GameTest
	public void oozingSlimeCountFollowsDeaths(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("oozing", 1.0), GemKit.gem("oozing", 1.0)));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		pig.setHealth(0.05F);
		int before = GemActions.ownedSlimes(helper.getLevel(), player);
		player.attack(pig);
		int after = GemActions.ownedSlimes(helper.getLevel(), player);
		check(helper, after == before + 2, "slime count " + before + " -> " + after);
		var slime = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.cubemob.Slime.class, pig.getBoundingBox().inflate(16),
				x -> x.hasAttached(ModAttachments.SLIME_OWNER)).getFirst();
		slime.discard();
		check(helper, GemActions.ownedSlimes(helper.getLevel(), player) == after - 1, "a removed slime still counts");
		GemKit.remove(helper, player);
		helper.succeed();
	}
}
