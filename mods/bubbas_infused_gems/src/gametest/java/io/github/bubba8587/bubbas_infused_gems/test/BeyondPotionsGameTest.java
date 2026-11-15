// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Gems beyond potions (owner, 2026-11-15): Wind Burst, Weaving, Oozing, Silence, Ender, Breeze do what the thing does. */
public class BeyondPotionsGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	private static void wear(LivingEntity e, String key) {
		Gem g = GemKit.gem(key, 1.0);
		for (int i = 0; i < 4; i++) {
			e.setItemSlot(GemTotals.ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), g, g, g));
		}
		WornEffects.refresh(e);
	}

	@GameTest
	public void windBurstMaceGoesPastTheCap(GameTestHelper helper) {
		double s = Balance.get().cap;
		double[] plain = GemActions.windLaunch(s, false);
		double[] mace = GemActions.windLaunch(s, true);
		check(helper, mace[0] > plain[0] * 1.5, "a Wind Burst mace launches no higher: " + mace[0] + " vs " + plain[0]);
		ItemStack weapon = GemKit.socketed(Items.MACE, GemKit.gem("wind_charged", 1.0), GemKit.gem("wind_charged", 1.0));
		check(helper, !GemActions.windMace(helper.getLevel(), weapon), "a plain mace counted as a Wind Burst mace");
		Holder<Enchantment> windBurst = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST);
		weapon.enchant(windBurst, 1);
		check(helper, GemActions.windMace(helper.getLevel(), weapon), "a Wind Burst mace not recognised");
		Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		attacker.setItemSlot(EquipmentSlot.MAINHAND, weapon);
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 1));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		attacker.doHurtTarget(helper.getLevel(), pig);
		check(helper, pig.getDeltaMovement().y > plain[0], "the Wind Burst mace hit threw the pig only " + pig.getDeltaMovement().y);
		helper.succeed();
	}

	@GameTest
	public void weavingWebsTheTargetOnlyIntoAir(GameTestHelper helper) {
		Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		attacker.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("weaving", 1.0), GemKit.gem("weaving", 1.0)));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
		pig.setHealth(500.0F);
		boolean webbed = false;
		for (int t = 0; t < 60 && !webbed; t++) {
			pig.damageCooldownTime = 0;
			attacker.doHurtTarget(helper.getLevel(), pig);
			webbed = helper.getLevel().getBlockState(pig.blockPosition()).is(Blocks.COBWEB);
		}
		check(helper, webbed, "Weaving never webbed the target");
		// Never replaces a block: a target standing in tall grass keeps it.
		BlockPos grass = new BlockPos(1, 1, 4);
		helper.setBlock(grass, Blocks.SHORT_GRASS);
		Mob other = helper.spawnWithNoFreeWill(EntityTypes.PIG, grass);
		other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
		other.setHealth(500.0F);
		for (int t = 0; t < 60; t++) {
			other.damageCooldownTime = 0;
			GemHits.apply(helper.getLevel(), other, attacker, attacker, List.of(new GemHits.Entry("weaving", Balance.get().cap, false)));
		}
		helper.assertBlockPresent(Blocks.SHORT_GRASS, grass);
		helper.succeed();
	}

	@GameTest
	public void oozingKillSplitsOffOwnedNeutralSlimes(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("oozing", 1.0), GemKit.gem("oozing", 1.0)));
		player.teleportTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)).x, helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)).y,
				helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)).z);
		int cap = (int) Balance.get().effect("oozing").wornNumber("slime_cap", 8);
		for (int kill = 0; kill < cap + 3; kill++) {
			Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
			pig.setHealth(0.05F); // a fresh mock player swings at a fraction of full strength
			player.attack(pig);
		}
		List<Slime> slimes = helper.getLevel().getEntitiesOfClass(Slime.class, new AABB(player.blockPosition()).inflate(16),
				slime -> slime.hasAttached(ModAttachments.SLIME_OWNER));
		check(helper, !slimes.isEmpty(), "an Oozing kill split off no slimes");
		check(helper, slimes.size() == cap, "owned slimes " + slimes.size() + ", cap " + cap);
		Slime slime = slimes.getFirst();
		check(helper, slime.getSize() == 1, "slime size " + slime.getSize());
		check(helper, slime.isPersistenceRequired(), "an Oozing slime can despawn");
		check(helper, slime.isAlliedTo(player) && player.isAlliedTo(slime), "an Oozing slime is not allied to its owner");
		slime.setTarget(player);
		check(helper, slime.getTarget() == null, "an Oozing slime took its owner as a target");
		Zombie stranger = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 5));
		slime.setTarget(stranger);
		check(helper, slime.getTarget() == null, "an Oozing slime took a target (it should be neutral)");
		GemKit.remove(helper, player);
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void silenceKeepsASculkSensorQuiet(GameTestHelper helper) {
		BlockPos sensor = new BlockPos(4, 1, 4);
		helper.setBlock(sensor, Blocks.SCULK_SENSOR);
		Mob quiet = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
		wear(quiet, "silence");
		helper.runAfterDelay(5, () -> {
			for (int i = 0; i < 5; i++) {
				helper.getLevel().gameEvent(GameEvent.STEP, quiet.position(), GameEvent.Context.of(quiet));
			}
		});
		for (int t = 10; t <= 40; t += 5) {
			helper.runAfterDelay(t, () -> check(helper, SculkSensorBlock.getPhase(helper.getBlockState(sensor)) == SculkSensorPhase.INACTIVE,
					"a sculk sensor heard a silenced step"));
		}
		helper.runAfterDelay(45, () -> {
			// Control: the same step without gems is heard.
			WornEffects.of(quiet);
			for (EquipmentSlot slot : GemTotals.ARMOR) {
				quiet.setItemSlot(slot, ItemStack.EMPTY);
			}
			WornEffects.refresh(quiet);
			helper.getLevel().gameEvent(GameEvent.STEP, quiet.position(), GameEvent.Context.of(quiet));
		});
		helper.runAfterDelay(46, () -> helper.succeedWhen(() -> check(helper,
				SculkSensorBlock.getPhase(helper.getBlockState(sensor)) != SculkSensorPhase.INACTIVE, "the control step was not heard")));
	}
}
