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
import io.github.bubba8587.bubbas_infused_gems.effect.GemValues;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModCommands;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import io.github.bubba8587.bubbas_infused_gems.station.Sockets;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.FoodDataAccessor;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.ThrownTridentInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Every gem effect does something, in armor (on the wearer) and on a weapon (on what it hits), through the real game
 * paths: a player's own sword swing, a mob's hit, a thrown trident. Written after v0, where player swings applied
 * nothing and hit Poison was too short to tick.
 */
public class GemsEverywhereGameTest {
	private static final EquipmentSlot[] ARMOR = GemTotals.ARMOR;
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	/** 12 perfect gems in full netherite: exactly the cap. */
	private static void wear(LivingEntity e, String key) {
		Gem g = GemKit.gem(key, 1.0);
		for (int i = 0; i < 4; i++) {
			e.setItemSlot(ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), g, g, g));
		}
		WornEffects.refresh(e);
	}

	private static void strip(LivingEntity e) {
		for (EquipmentSlot slot : ARMOR) {
			e.setItemSlot(slot, ItemStack.EMPTY);
		}
		WornEffects.refresh(e);
		e.removeAllEffects();
	}

	private static double modifier(LivingEntity e, String attribute, String id) {
		AttributeInstance inst = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(attribute)).map(e::getAttribute).orElse(null);
		AttributeModifier m = inst == null ? null : inst.getModifier(BubbasInfusedGems.id(id));
		return m == null ? 0.0 : m.amount();
	}

	private static Mob pig(GameTestHelper helper, int x, int z) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(x, 1, z));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
		pig.setHealth(50.0F);
		return pig;
	}

	private static int count(GameTestHelper helper, Class<? extends LivingEntity> type, LivingEntity near) {
		return helper.getLevel().getEntitiesOfClass(type, new AABB(near.blockPosition()).inflate(4)).size();
	}

	@GameTest(maxTicks = 100)
	public void everyGemWorksInArmor(GameTestHelper helper) {
		Balance b = Balance.get();
		double s = b.cap;
		Mob pig = pig(helper, 1, 1);
		for (String key : b.gemEffects) {
			wear(pig, key);
			Balance.EffectData e = b.effect(key);
			if (!e.wornAttributes().isEmpty() && !e.wornFlag("wet_only")) {
				for (int i = 0; i < e.wornAttributes().size(); i++) {
					if (BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(e.wornAttributes().get(i).attribute())).map(pig::getAttribute).orElse(null) == null) {
						continue; // pigs have no attack damage or speed: checked on a player below
					}
					close(modifier(pig, e.wornAttributes().get(i).attribute(), "worn/" + key + "/" + i), e.wornAttributes().get(i).at(s), 1e-6,
							helper, key + " worn attribute " + i);
				}
			}
			pig.setHealth(50.0F);
			pig.damageCooldownTime = 0;
			switch (key) {
				case "regeneration" -> {
					for (int t = 0; t < 20; t++) {
						WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
					}
					close(pig.getHealth(), 50.0 + 0.8 * s, 1e-3, helper, "regeneration heal per second");
				}
				case "resistance" -> close(WornEffects.modifyDamage(pig, pig.damageSources().generic(), 10F), 10 * (1 - 0.4 * s), 1e-4, helper, key);
				case "turtle_master" -> {
					close(WornEffects.modifyDamage(pig, pig.damageSources().generic(), 10F), 10 * (1 - 0.8 * s), 1e-4, helper, "Turtle Master resistance");
					close(modifier(pig, "minecraft:movement_speed", "worn/turtle_master/0"), -0.9 * s, 1e-6, helper, "Turtle Master slowness");
				}
				case "fire_resistance" -> close(WornEffects.modifyDamage(pig, pig.damageSources().inFire(), 10F), 10 * (1 - s), 1e-4, helper, key);
				case "night_vision", "hero_of_the_village" -> close(GemValues.value(pig, key), s, 1e-6, helper, key + " value for its hook");
				case "invisibility" -> close(pig.getVisibilityPercent(helper.getLevel(), null), 1 - 0.93 * s, 1e-6, helper, "stealth");
				case "slow_falling" -> {
					pig.setDeltaMovement(0, -0.5, 0);
					close(WornEffects.gravity(pig, 0.08), 0.08 * (1 - 0.875 * s), 1e-6, helper, "slow falling gravity");
				}
				case "dolphins_grace" -> close(WornEffects.waterDrag(pig, 0.8), 0.8 + 0.16 * s, 1e-6, helper, "water drag");
				case "conduit_power" -> close(GemValues.value(pig, key), s, 1e-6, helper, "conduit power (wet test below)");
				case "instant_health" -> {
					pig.setHealth(20F);
					WornEffects.afterDamage(pig, pig.damageSources().mobAttack(pig), 4F);
					close(pig.getHealth(), 20 + 4 * b.healFraction(s), 1e-4, helper, "healing gem");
				}
				case "instant_damage" -> close(WornEffects.modifyDamage(pig, pig.damageSources().mobAttack(pig), 10F), 10 * (1 + b.healFraction(s)), 1e-4,
						helper, "harming gem");
				case "poison", "wither" -> {
					pig.tickCount = 1000;
					WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
					check(helper, pig.getHealth() < 50F, key + " worn did no damage");
				}
				case "glowing" -> {
					pig.tickCount = 2000;
					WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
					check(helper, pig.hasEffect(ModEffects.vanilla(key)), key + " is not on while worn");
				}
				case "blindness", "nausea", "darkness" -> {
					// Drawn on the wearer's own screen, partial and steady: no vanilla instance (it would pulse and restart).
					pig.tickCount = 2000;
					WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
					check(helper, !pig.hasEffect(ModEffects.vanilla(key)), key + " worn put the full vanilla effect on");
					close(GemValues.rawWorn(pig, key), s, 1e-6, helper, key + " screen strength");
				}
				case "levitation" -> {
					pig.setDeltaMovement(0, 0, 0);
					close(-WornEffects.gravity(pig, 0.08), WornEffects.levitationRise(s), 1e-9, helper, "levitation: straight to the rise speed");
				}
				case "wind_charged", "oozing" -> {
					// A landing bounces instead of hurting (Wind Burst: a small burst; Oozing: a slime block's bounce).
					pig.setDeltaMovement(0, -0.8, 0);
					check(helper, io.github.bubba8587.bubbas_infused_gems.effect.GemActions.landing(pig, 6.0), key + " did not cancel a 6-block fall");
					check(helper, pig.getDeltaMovement().y > 0.2, key + " landing bounce " + pig.getDeltaMovement().y);
				}
				case "weaving" -> close(io.github.bubba8587.bubbas_infused_gems.effect.GemActions.webSlowdown(pig, new Vec3(0.25, 0.05, 0.25)).x, 1.0,
						1e-6, helper, "Weaving at the cap: full speed in cobwebs");
				case "silence" -> check(helper, io.github.bubba8587.bubbas_infused_gems.effect.GemActions.silenced(pig,
						net.minecraft.world.level.gameevent.GameEvent.STEP), "Silence at the cap left a step vibration");
				case "ender" -> {
					Zombie shooter = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1));
					Arrow arrow = (Arrow) ((ArrowItem) Items.ARROW).createArrow(helper.getLevel(), new ItemStack(Items.ARROW), shooter, null);
					boolean blinked = false;
					for (int t = 0; t < 40 && !blinked; t++) {
						Vec3 at = pig.position();
						blinked = !pig.hurtServer(helper.getLevel(), pig.damageSources().arrow(arrow, shooter), 2.0F) && !pig.position().equals(at);
						pig.damageCooldownTime = 0;
					}
					check(helper, blinked, "Ender never blinked away from an arrow");
					pig.teleportTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)).x, helper.absoluteVec(new Vec3(1.5, 1, 1.5)).y, helper.absoluteVec(new Vec3(1.5, 1, 1.5)).z);
					shooter.discard();
				}
				case "breeze" -> {
					Zombie shooter = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1));
					Arrow arrow = (Arrow) ((ArrowItem) Items.ARROW).createArrow(helper.getLevel(), new ItemStack(Items.ARROW), shooter, null);
					boolean deflected = false;
					for (int t = 0; t < 40 && !deflected; t++) {
						deflected = pig.deflection(arrow) != net.minecraft.world.entity.projectile.ProjectileDeflection.NONE;
					}
					check(helper, deflected, "Breeze never deflected an arrow");
					shooter.discard();
				}
				case "infested" -> {
					boolean hatched = false;
					for (int t = 0; t < 400 && !hatched; t++) {
						WornEffects.afterDamage(pig, pig.damageSources().generic(), 1F);
						hatched = count(helper, Silverfish.class, pig) > 0;
					}
					check(helper, hatched, "infested gem never hatched silverfish");
					helper.getLevel().getEntitiesOfClass(Silverfish.class, new AABB(pig.blockPosition()).inflate(4)).forEach(Silverfish::discard);
				}
				default -> {
				}
			}
			strip(pig);
		}
		pig.discard();
		helper.succeed();
	}

	@GameTest
	public void playerOnlyGemsWork(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		double s = Balance.get().cap;
		wear(player, "hunger");
		float before = ((FoodDataAccessor) player.getFoodData()).bubbas$exhaustion();
		WornEffects.tick(helper.getLevel(), player, WornEffects.of(player));
		close(((FoodDataAccessor) player.getFoodData()).bubbas$exhaustion() - before, 0.015 * s, 1e-5, helper, "hunger per tick");
		strip(player);
		for (String key : List.of("strength", "haste", "weakness", "mining_fatigue")) {
			wear(player, key);
			Balance.EffectData e = Balance.get().effect(key);
			for (int i = 0; i < e.wornAttributes().size(); i++) {
				close(modifier(player, e.wornAttributes().get(i).attribute(), "worn/" + key + "/" + i), e.wornAttributes().get(i).at(s), 1e-6, helper,
						key + " worn attribute " + i);
			}
			strip(player);
		}
		wear(player, "hero_of_the_village");
		close(WornEffects.heroModifier(player, 0.0), 0.20, 1e-6, helper, "hero price cut: 20% at the best build (owner)");
		close(WornEffects.heroModifier(player, 0.3), 0.3, 1e-6, helper, "a stronger vanilla hero wins");
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void conduitPowerOnlyInWater(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		helper.setBlock(at, Blocks.WATER);
		helper.setBlock(at.above(), Blocks.WATER);
		Mob pig = pig(helper, 2, 2);
		wear(pig, "conduit_power");
		Mob dry = pig(helper, 0, 0);
		wear(dry, "conduit_power");
		double s = Balance.get().cap;
		helper.succeedWhen(() -> {
			check(helper, modifier(pig, "minecraft:oxygen_bonus", "wet/conduit_power/0") > 1.0, "conduit air in water");
			check(helper, modifier(dry, "minecraft:oxygen_bonus", "wet/conduit_power/0") == 0.0, "conduit power on dry land");
		});
	}

	@GameTest
	public void everyGemWorksOnHit(GameTestHelper helper) {
		Balance b = Balance.get();
		// Two perfect gems in a netherite sword: the weapon cap.
		double s = b.cap;
		int n = 0;
		for (String key : b.gemEffects) {
			Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
			attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem(key, 1.0), GemKit.gem(key, 1.0)));
			Mob target = pig(helper, 2, 3);
			float before = target.getHealth();
			attacker.doHurtTarget(helper.getLevel(), target);
			Balance.EffectData e = b.effect(key);
			GemHits.Entry entry = new GemHits.Entry(key, s, false);
			switch (e.hitKind()) {
				case "action" -> {
					// Wind Burst throws the target; Weaving (a chance) and Oozing (on a kill) have their own tests.
					if (key.equals("wind_charged")) {
						check(helper, target.getDeltaMovement().y > 0.5, "Wind Burst hit threw the target only " + target.getDeltaMovement().y);
					}
				}
				case "instant" -> {
					Mob control = pig(helper, 3, 3);
					Zombie plain = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
					plain.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
					plain.doHurtTarget(helper.getLevel(), control);
					float plainLoss = 50F - control.getHealth();
					float loss = before - target.getHealth();
					double expected = key.equals("instant_damage") ? plainLoss + GemHits.strike(key, s) : plainLoss - GemHits.strike(key, s);
					close(loss, expected, 0.05, helper, key + " strike");
					control.discard();
					plain.discard();
				}
				case "onoff" -> {
					MobEffectInstance inst = target.getEffect(ModEffects.vanilla(key));
					check(helper, inst != null && Math.abs(inst.getDuration() - GemHits.duration(entry)) <= 1, key + " on hit: " + inst);
				}
				default -> {
					MobEffectInstance inst = target.getEffect(ModEffects.gem(key));
					check(helper, inst != null, key + " on hit: no gem effect");
					check(helper, inst.getAmplifier() == GemMobEffect.amplifier(Math.sqrt(s)), key + " hit potency " + inst.getAmplifier());
					check(helper, Math.abs(inst.getDuration() - GemHits.duration(entry)) <= 1 && GemHits.duration(entry) <= 160,
							key + " hit duration " + inst.getDuration());
				}
			}
			attacker.discard();
			target.discard();
			n++;
		}
		check(helper, n == b.gemEffects.size(), "not every gem tried");
		helper.succeed();
	}

	/** The bug the owner hit in v0: a player's own sword swing applied nothing, and hit Poison never ticked. */
	@GameTest(maxTicks = 200)
	public void playerSwordSwingPoisonsAndItTicks(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("poison", 1.0), GemKit.gem("glowing", 1.0)));
		Mob pig = pig(helper, 2, 2);
		player.attack(pig);
		MobEffectInstance poison = pig.getEffect(ModEffects.gem("poison"));
		check(helper, poison != null, "a player's sword swing applied no gem poison");
		check(helper, poison.getDuration() >= Balance.get().effect("poison").hitFloorTicks(), "hit poison is under its floor: " + poison.getDuration() + " ticks");
		check(helper, pig.hasEffect(MobEffects.GLOWING), "a player's sword swing applied no glowing");
		float afterHit = pig.getHealth();
		helper.succeedWhen(() -> {
			check(helper, pig.getHealth() < afterHit - 3.0F, "poison hasn't ticked enough yet: " + pig.getHealth() + " after the hit left " + afterHit);
			GemKit.remove(helper, player);
		});
	}

	@GameTest
	public void thrownTridentAppliesItsGems(GameTestHelper helper) {
		Zombie thrower = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Mob pig = pig(helper, 3, 1);
		ItemStack trident = GemKit.socketed(Items.TRIDENT, GemKit.gem("slowness", 1.0));
		ThrownTrident thrown = new ThrownTrident(helper.getLevel(), thrower, trident);
		((ThrownTridentInvoker) thrown).bubbas$hit(new EntityHitResult(pig));
		check(helper, pig.hasEffect(ModEffects.gem("slowness")), "thrown trident applied no gem slowness");
		thrown.discard();
		helper.succeed();
	}

	@GameTest
	public void arrowKeepsItsGemsAcrossASave(GameTestHelper helper) {
		Zombie shooter = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		ItemStack bow = GemKit.socketed(Items.BOW, GemKit.gem("poison", 1.0), GemKit.gem("instant_damage", 1.0));
		Arrow arrow = (Arrow) ((ArrowItem) Items.ARROW).createArrow(helper.getLevel(), new ItemStack(Items.ARROW), shooter, bow);
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		arrow.saveWithoutId(out);
		Arrow loaded = new Arrow(EntityTypes.ARROW, helper.getLevel());
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), out.buildResult()));
		check(helper, ((GemArrow) loaded).bubbas$gemHit().equals(((GemArrow) arrow).bubbas$gemHit()) && !((GemArrow) loaded).bubbas$gemHit().isEmpty(),
				"arrow lost its gems in a save: " + ((GemArrow) loaded).bubbas$gemHit());
		helper.succeed();
	}

	@GameTest
	public void gemAndVanillaEffectStrongerApplies(GameTestHelper helper) {
		Mob pig = pig(helper, 1, 1);
		double s = Balance.get().cap;
		double gem = 0.4 * s;
		wear(pig, "speed");
		close(modifier(pig, "minecraft:movement_speed", "worn/speed/0"), gem, 1e-6, helper, "speed gems alone");
		pig.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
		close(modifier(pig, "minecraft:movement_speed", "worn/speed/0"), (1 + gem) / 1.2 - 1, 1e-6, helper, "gems on top of Speed I: only the rest");
		pig.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
		close(modifier(pig, "minecraft:movement_speed", "worn/speed/0"), 0.0, 1e-9, helper, "Speed II is stronger: gems off");
		pig.removeEffect(MobEffects.SPEED);
		close(modifier(pig, "minecraft:movement_speed", "worn/speed/0"), gem, 1e-6, helper, "potion gone: gems back");
		strip(pig);
		// On a target: a stronger vanilla effect keeps the gem's off; a weaker one gives way to it.
		Holder<MobEffect> gemPoison = ModEffects.gem("poison");
		pig.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1));
		pig.addEffect(new MobEffectInstance(gemPoison, 400, GemMobEffect.amplifier(0.9)));
		check(helper, pig.hasEffect(MobEffects.POISON) && !pig.hasEffect(gemPoison), "Poison II vs a 90% gem: both or the gem");
		pig.removeAllEffects();
		pig.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
		pig.addEffect(new MobEffectInstance(gemPoison, 400, GemMobEffect.amplifier(0.9)));
		check(helper, !pig.hasEffect(MobEffects.POISON) && pig.hasEffect(gemPoison), "Poison I vs a 90% gem: both or vanilla");
		pig.discard();
		helper.succeed();
	}

	@GameTest
	public void undeadInvertHealingAndIgnorePoison(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		double s = Balance.get().cap;
		wear(zombie, "instant_health");
		close(WornEffects.modifyDamage(zombie, zombie.damageSources().mobAttack(zombie), 10F), 10 * (1 + Balance.get().healFraction(s)), 1e-4, helper,
				"a healing gem hurts an undead wearer");
		strip(zombie);
		wear(zombie, "instant_damage");
		zombie.setHealth(10F);
		WornEffects.afterDamage(zombie, zombie.damageSources().mobAttack(zombie), 4F);
		check(helper, zombie.getHealth() > 10F, "a harming gem doesn't heal an undead wearer");
		strip(zombie);
		wear(zombie, "poison");
		zombie.setHealth(10F);
		zombie.tickCount = 1000;
		WornEffects.tick(helper.getLevel(), zombie, WornEffects.of(zombie));
		close(zombie.getHealth(), 10F, 1e-6, helper, "undead ignore poison gems");
		strip(zombie);
		// Weapon hits on an undead target (owner, Q32): Harming heals it, Healing hurts it, Poison and Regeneration do nothing.
		for (String key : List.of("instant_damage", "instant_health", "poison", "regeneration")) {
			Zombie target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 2));
			target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
			target.setHealth(50F);
			Zombie hitter = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 2));
			hitter.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.WOODEN_SWORD, GemKit.gem(key, 1.0), GemKit.gem(key, 1.0)));
			Zombie control = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 2));
			control.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
			control.setHealth(50F);
			Zombie plain = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 3));
			plain.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
			hitter.doHurtTarget(helper.getLevel(), target);
			plain.doHurtTarget(helper.getLevel(), control);
			float extra = control.getHealth() - target.getHealth(); // > 0: the gem hurt more than the plain hit
			switch (key) {
				case "instant_damage" -> check(helper, extra <= -0.99F, "Harming should heal an undead target: " + extra);
				case "instant_health" -> check(helper, extra >= 0.99F, "Healing should hurt an undead target: " + extra);
				default -> check(helper, target.getEffect(ModEffects.gem(key)) == null, key + " landed on an undead target");
			}
			for (Zombie z : List.of(target, hitter, control, plain)) {
				z.discard();
			}
		}
		zombie.discard();
		helper.succeed();
	}

	@GameTest
	public void absorptionDoesNotRefillOnReequip(GameTestHelper helper) {
		Mob pig = pig(helper, 1, 1);
		double full = 16 * Balance.get().cap;
		pig.tickCount = 1000;
		wear(pig, "absorption");
		close(pig.getAbsorptionAmount(), full, 1e-3, helper, "equipping fills the shield at once (owner)");
		// Hurt, then swap gear right away: no second equip-fill within 30 s.
		pig.setAbsorptionAmount(2.0F);
		strip(pig);
		wear(pig, "absorption");
		WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
		check(helper, pig.getAbsorptionAmount() < full - 0.5, "a quick re-equip refilled the shield: " + pig.getAbsorptionAmount());
		check(helper, ((GemEntity) pig).bubbas$lastHurtTick() == pig.tickCount, "a quick re-equip did not start the usual wait");
		// 30 s without damage: the normal refill.
		pig.tickCount += 600;
		WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
		close(pig.getAbsorptionAmount(), full, 1e-3, helper, "refilled 30 s after the last damage");
		// A re-equip 30 s after the last equip-fill fills at once again.
		pig.setAbsorptionAmount(0.0F);
		strip(pig);
		wear(pig, "absorption");
		close(pig.getAbsorptionAmount(), full, 1e-3, helper, "equip-fill again once 30 s have passed");
		pig.discard();
		helper.succeed();
	}

	/** Owner (playtest): a Levitation set really lifts its wearer, continuously, like vanilla Levitation but slower. */
	@GameTest(maxTicks = 120)
	public void levitationSetLiftsTheWearer(GameTestHelper helper) {
		Mob best = helper.spawn(EntityTypes.PIG, new BlockPos(1, 1, 1));
		Mob weak = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 1));
		wear(best, "levitation");
		weak.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem("levitation", 0.5)));
		WornEffects.refresh(weak);
		double y0 = best.getY();
		double w0 = weak.getY();
		helper.runAfterDelay(80, () -> {
			double rose = best.getY() - y0;
			double weakRose = weak.getY() - w0;
			// 4 s: the best build drifts up near half of vanilla Levitation I's speed, a single weak gem slowly.
			check(helper, rose > 1.6, "best Levitation set rose only " + rose + " blocks in 4 s");
			check(helper, weakRose > 0.25 && weakRose < rose, "a weak Levitation gem rose " + weakRose + " blocks in 4 s");
			helper.succeed();
		});
	}

	/** Owner (Q33): stealth scales, the locator bar hides from a threshold, only the perfect full set is really invisible. */
	@GameTest
	public void invisibilityStealthThresholdAndFullSet(GameTestHelper helper) {
		Mob pig = pig(helper, 1, 1);
		Balance.EffectData e = Balance.get().effect("invisibility");
		double hideAt = Double.parseDouble(e.wornAttributes().getFirst().curve().substring(5));
		for (double q : new double[] {hideAt / Balance.get().cap - 0.05, hideAt / Balance.get().cap + 0.05}) {
			Gem g = GemKit.gem("invisibility", q);
			for (int i = 0; i < 4; i++) {
				pig.setItemSlot(ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), g, g, g));
			}
			WornEffects.refresh(pig);
			double s = WornEffects.of(pig).value("invisibility");
			boolean hidden = modifier(pig, "minecraft:waypoint_transmit_range", "worn/invisibility/0") == -1.0;
			check(helper, hidden == (s >= hideAt), "locator bar at " + s + ": hidden " + hidden);
			close(pig.getVisibilityPercent(helper.getLevel(), null), 1 - 0.93 * s, 1e-6, helper, "stealth scales at " + s);
			WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
			check(helper, !pig.hasEffect(MobEffects.INVISIBILITY), "real invisibility below the full set at " + s);
		}
		wear(pig, "invisibility");
		pig.tickCount = 400;
		WornEffects.tick(helper.getLevel(), pig, WornEffects.of(pig));
		check(helper, pig.hasEffect(MobEffects.INVISIBILITY), "the perfect full set is not really invisible");
		pig.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
		WornEffects.refresh(pig);
		check(helper, !pig.hasEffect(MobEffects.INVISIBILITY), "a piece off: still really invisible");
		check(helper, pig.getVisibilityPercent(helper.getLevel(), null) < 0.5, "a piece off: stealth gone too");
		pig.removeEffect(MobEffects.INVISIBILITY);
		pig.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0));
		WornEffects.refresh(pig);
		check(helper, pig.hasEffect(MobEffects.INVISIBILITY), "a real potion's invisibility was taken away");
		pig.discard();
		helper.succeed();
	}

	@GameTest
	public void turtleMasterGivesBothPartsAndTurtleHelmetsTakeNoGems(GameTestHelper helper) {
		check(helper, Sockets.slots(new ItemStack(Items.TURTLE_HELMET)) == 0, "the turtle helmet has gem slots");
		check(helper, !Sockets.canSocket(new ItemStack(Items.TURTLE_HELMET), GemKit.item(GemKit.gem("speed", 1.0))), "a gem fits a turtle helmet");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		wear(player, "turtle_master");
		player.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("turtle_master", 1.0)));
		List<String> lines = ModCommands.lines(player).stream().map(Component::getString).toList();
		String all = String.join("\n", lines);
		check(helper, lines.stream().filter(l -> l.contains("Turtle Master (Resistance)")).count() == 2, "/gems Resistance part lines:\n" + all);
		check(helper, lines.stream().filter(l -> l.contains("Turtle Master (Slowness)")).count() == 2, "/gems Slowness part lines:\n" + all);
		Mob pig = pig(helper, 2, 2);
		GemHits.onWeaponHit(helper.getLevel(), player, player, pig, player.getMainHandItem());
		MobEffectInstance tm = pig.getEffect(ModEffects.gem("turtle_master"));
		check(helper, tm != null, "Turtle Master hit applied nothing");
		double p = GemMobEffect.potency(tm.getAmplifier());
		close(WornEffects.modifyDamage(pig, pig.damageSources().generic(), 10F), 10 * (1 - 0.8 * p), 1e-3, helper, "Turtle Master hit resistance");
		check(helper, pig.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BubbasInfusedGems.id("effect/turtle_master/0")) != null,
				"Turtle Master hit slowness");
		GemKit.remove(helper, player);
		pig.discard();
		helper.succeed();
	}

	/** The weakest gem a player can make of this effect: its weakest potion, Infusion I, tier 1 weak. */
	private static Gem weakest(String key) {
		Balance b = Balance.get();
		double variant = b.effect(key).variants().values().stream().mapToDouble(Balance.Variant::factor).min().orElse(1.0);
		return GemKit.gem(key, variant * b.infusion(1).factor() * b.grades.get("weak"));
	}

	/** Owner: every hit must do something a player notices. One hit of the weakest gem in a wooden sword, per effect. */
	@GameTest
	public void weakestGemHitStillDoesSomething(GameTestHelper helper) {
		Balance b = Balance.get();
		for (String key : b.gemEffects) {
			Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
			attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.WOODEN_SWORD, weakest(key)));
			Mob target = pig(helper, 2, 3);
			Balance.EffectData e = b.effect(key);
			float before = target.getHealth();
			attacker.doHurtTarget(helper.getLevel(), target);
			switch (e.hitKind()) {
				case "action" -> {
					if (key.equals("wind_charged")) {
						check(helper, target.getDeltaMovement().y > 0.05, "the weakest Wind Burst hit threw nothing: " + target.getDeltaMovement().y);
					}
				}
				case "instant" -> {
					// Against a hit with no gem: the strike adds or heals at least 1 HP.
					Mob control = pig(helper, 3, 3);
					Zombie plain = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
					plain.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
					plain.doHurtTarget(helper.getLevel(), control);
					float diff = (before - target.getHealth()) - (50F - control.getHealth());
					check(helper, Math.abs(diff) >= 0.99F, key + ": the weakest strike changed health by " + diff);
					control.discard();
					plain.discard();
				}
				case "onoff" -> {
					MobEffectInstance inst = target.getEffect(ModEffects.vanilla(key));
					check(helper, inst != null && inst.getDuration() >= e.hitFloorTicks(), key + ": weakest hit " + inst + ", floor " + e.hitFloorTicks());
				}
				default -> {
					MobEffectInstance inst = target.getEffect(ModEffects.gem(key));
					check(helper, inst != null && inst.getDuration() >= Math.max(e.hitFloorTicks(), 40), key + ": weakest hit " + inst);
					check(helper, GemValues.hit(target, key) > 0.0, key + ": weakest hit has no potency");
				}
			}
			attacker.discard();
			target.discard();
		}
		helper.succeed();
	}

	/** Owner: worn Saturation restores food like the vanilla effect; one weak gem slowly, the best build about a point a second. */
	@GameTest
	public void saturationGemsRestoreFood(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		// One weak gem in a netherite chestplate: a food point every 20-30 s, so +1 within 30 s.
		player.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem("saturation", 0.5)));
		WornEffects.refresh(player);
		double weak = WornEffects.of(player).value("saturation");
		double every = 1.0 / WornEffects.saturationRate(weak);
		check(helper, every >= 20.0 && every <= 30.0, "one weak Saturation gem restores a point every " + every + " s");
		player.getFoodData().setFoodLevel(6);
		player.getFoodData().setSaturation(0.0F);
		for (int t = 0; t < 600; t++) {
			WornEffects.tick(helper.getLevel(), player, WornEffects.of(player));
		}
		check(helper, player.getFoodData().getFoodLevel() >= 7, "weak Saturation: food " + player.getFoodData().getFoodLevel() + " after 30 s");
		check(helper, player.getFoodData().getSaturationLevel() > 0.0F, "weak Saturation gave no saturation");
		strip(player);
		wear(player, "saturation");
		player.getFoodData().setFoodLevel(6);
		player.getFoodData().setSaturation(0.0F);
		for (int t = 0; t < 100; t++) {
			WornEffects.tick(helper.getLevel(), player, WornEffects.of(player));
		}
		check(helper, player.getFoodData().getFoodLevel() >= 10, "best Saturation: food " + player.getFoodData().getFoodLevel() + " after 5 s");
		strip(player);
		// On a hit: the target gets the same restore for a few seconds.
		player.getFoodData().setFoodLevel(6);
		GemHits.apply(helper.getLevel(), player, null, null, List.of(new GemHits.Entry("saturation", Balance.get().cap, false)));
		check(helper, player.hasEffect(ModEffects.gem("saturation")), "Saturation hit applied nothing");
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** Owner: every Hunger hit costs a player target at least a food point's worth (4.0 exhaustion), even the weakest. */
	@GameTest
	public void weakestHungerHitCostsAFoodPoint(GameTestHelper helper) {
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		target.setGameMode(GameType.SURVIVAL);
		Balance.EffectData e = Balance.get().effect("hunger");
		double s = Balance.get().stack(List.of((double) weakest("hunger").main() * 0.75), Balance.get().weaponNorm); // wooden sword
		GemHits.Entry entry = new GemHits.Entry("hunger", s, false);
		GemHits.apply(helper.getLevel(), target, null, null, List.of(entry));
		MobEffectInstance inst = target.getEffect(ModEffects.gem("hunger"));
		check(helper, inst != null && inst.getDuration() >= 80, "weakest hunger hit lasts under 4 s: " + inst);
		double overTime = e.hitUnit() * GemMobEffect.potency(inst.getAmplifier()) * inst.getDuration();
		float now = ((FoodDataAccessor) target.getFoodData()).bubbas$exhaustion();
		close(now + overTime, 4.0, 1e-3, helper, "weakest hunger hit: exhaustion now " + now + " + over time " + overTime);
		close(e.hitUnit(), 0.03, 1e-9, helper, "hunger hit rate: twice Hunger III");
		GemKit.remove(helper, target);
		helper.succeed();
	}

	/** The weakest Poison and Wither hits really tick after the hit's damage cooldown; the weakest Regeneration heals a heart. */
	@GameTest(maxTicks = 400)
	public void weakestDamageAndHealOverTimeHitsTakeEffect(GameTestHelper helper) {
		Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Mob poisoned = pig(helper, 2, 1);
		Mob withered = pig(helper, 2, 3);
		Mob healed = pig(helper, 4, 1);
		attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.WOODEN_SWORD, weakest("poison")));
		attacker.doHurtTarget(helper.getLevel(), poisoned);
		float poisonedAfterHit = poisoned.getHealth();
		attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.WOODEN_SWORD, weakest("wither")));
		attacker.doHurtTarget(helper.getLevel(), withered);
		float witheredAfterHit = withered.getHealth();
		attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.WOODEN_SWORD, weakest("regeneration")));
		attacker.doHurtTarget(helper.getLevel(), healed);
		float healedAfterHit = healed.getHealth();
		helper.runAfterDelay(160, () -> {
			// Three damage ticks of the weakest poison or wither at least.
			double p = GemMobEffect.potency(GemMobEffect.amplifier(Math.sqrt(Balance.get().stack(List.of(weakest("poison").main() * 0.75), Balance.get().weaponNorm))));
			check(helper, poisonedAfterHit - poisoned.getHealth() >= 3 * 0.5 * 25 / 12.0 * 0.8 * p * 0.99, "the weakest poison hit barely ticked: "
					+ (poisonedAfterHit - poisoned.getHealth()));
			check(helper, withered.getHealth() < witheredAfterHit - 0.1F, "the weakest wither hit did nothing");
			check(helper, healed.getHealth() >= healedAfterHit + 1.99F, "the weakest regeneration hit healed less than a heart: " + (healed.getHealth() - healedAfterHit));
			helper.succeed();
		});
	}
}
