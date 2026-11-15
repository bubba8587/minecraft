// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.Optional;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.registry.ModCommands;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.gem.SocketedGems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The new top end (owner, 2026-11-15): the perfect set reaches the full potion for combat effects, is complete for
 * on/off ones, goes past it for mobility; chance gems are certain; a netherite set of tier 3 Infusion V is 93 % of it.
 */
public class TopEndGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	private static ServerPlayer kit(GameTestHelper helper, String effect) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		try {
			ModCommands.kit(player.createCommandSourceStack(), effect, "netherite");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		WornEffects.tick(helper.getLevel(), player, WornEffects.of(player));
		return player;
	}

	private static double bonus(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a) {
		return p.getAttributeValue(a) - p.getAttribute(a).getBaseValue();
	}

	@GameTest
	public void perfectSetsReachTheNewCaps(GameTestHelper helper) {
		ServerPlayer p = kit(helper, "strength");
		check(helper, Math.abs(bonus(p, Attributes.ATTACK_DAMAGE) - 6.0) < 1e-4, "perfect Strength: +" + bonus(p, Attributes.ATTACK_DAMAGE) + " (Strength II is +6)");
		GemKit.remove(helper, p);
		p = kit(helper, "health_boost");
		check(helper, Math.abs(bonus(p, Attributes.MAX_HEALTH) - 8.0) < 1e-4, "perfect Health Boost: +" + bonus(p, Attributes.MAX_HEALTH));
		GemKit.remove(helper, p);
		p = kit(helper, "luck");
		check(helper, Math.abs(bonus(p, Attributes.LUCK) - 1.2) < 1e-4, "perfect Luck: +" + bonus(p, Attributes.LUCK) + " (owner: 1.20 of Luck I)");
		GemKit.remove(helper, p);
		p = kit(helper, "fire_resistance");
		check(helper, p.hasEffect(MobEffects.FIRE_RESISTANCE), "a perfect Fire Resistance set gives no real fire immunity");
		GemKit.remove(helper, p);
		p = kit(helper, "water_breathing");
		check(helper, p.hasEffect(MobEffects.WATER_BREATHING), "a perfect Water Breathing set gives no real water breathing");
		GemKit.remove(helper, p);
		helper.succeed();
	}

	/** Owner: a full netherite set of tier 3 Infusion V gems lands at 93 % of the perfect set (was 87 %). */
	@GameTest
	public void netheriteInfusionVReferenceBuild(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// A brewed gem's strength: potion factor (strong Swiftness, 1.0) x Infusion factor, x tier 3's full grade.
		float strength = (float) Balance.get().infusion(5).factor();
		Gem five = new Gem(GemText.holder("speed"), 5, 3, strength, Optional.empty(), Optional.empty());
		for (int i = 0; i < 4; i++) {
			ItemStack piece = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
					net.minecraft.resources.Identifier.withDefaultNamespace("netherite_" + PIECES[i])));
			piece.set(ModComponents.SOCKETED_GEMS, SocketedGems.EMPTY.with(five).with(five).with(five));
			player.setItemSlot(GemTotals.ARMOR[i], piece);
		}
		WornEffects.refresh(player);
		double share = WornEffects.of(player).value("speed") / Balance.get().effect("speed").wornCap();
		check(helper, Math.abs(share - 0.93) < 0.005, "netherite + tier 3 Infusion V: " + share + " of the perfect set, model 0.93");
		helper.succeed();
	}

	/**
	 * Owner: a perfect Ender set always blinks away from a projectile that would hurt (not from other damage). On a mob
	 * wearing the same perfect set: a mock player teleported back each round is briefly invulnerable (awaiting its client).
	 */
	@GameTest
	public void perfectEnderAlwaysBlinks(GameTestHelper helper) {
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
		Gem perfect = GemKit.gem("ender", 1.0);
		for (int i = 0; i < 4; i++) {
			pig.setItemSlot(GemTotals.ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), perfect, perfect, perfect));
		}
		WornEffects.refresh(pig);
		check(helper, Math.abs(WornEffects.of(pig).value("ender") - Balance.get().cap) < 1e-9, "not the perfect Ender set: " + WornEffects.of(pig).value("ender"));
		Zombie shooter = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1));
		Arrow arrow = (Arrow) ((ArrowItem) Items.ARROW).createArrow(helper.getLevel(), new ItemStack(Items.ARROW), shooter, null);
		Vec3 home = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		int blinked = 0;
		for (int i = 0; i < 200; i++) {
			pig.teleportTo(home.x, home.y, home.z);
			pig.setHealth(pig.getMaxHealth());
			pig.damageCooldownTime = 0;
			boolean hurt = pig.hurtServer(helper.getLevel(), pig.damageSources().arrow(arrow, shooter), 4.0F);
			if (!hurt && pig.position().distanceTo(home) > 0.5 && pig.getHealth() == pig.getMaxHealth()) {
				blinked++;
			}
		}
		check(helper, blinked == 200, "a perfect Ender set blinked away from " + blinked + " of 200 arrows");
		pig.teleportTo(home.x, home.y, home.z);
		pig.damageCooldownTime = 0;
		check(helper, pig.hurtServer(helper.getLevel(), pig.damageSources().generic(), 2.0F), "a perfect Ender set dodged non-projectile damage");
		shooter.discard();
		pig.discard();
		helper.succeed();
	}
}
