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
 * The new top end (owner, 2026-11-15): chance gems are certain at the perfect set (Ender), and the perfect
 * Invisibility set hides armor. The caps themselves (full potion for combat effects, complete for on/off ones, past
 * it for mobility, 88 % for netherite tier 3 Gem Power V) are checked by tools/gem-balance.py and, from real items,
 * by BalanceGameTest and GemsEverywhereGameTest.
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
			if (!hurt && pig.getHealth() == pig.getMaxHealth()) {
				blinked++;
			}
		}
		check(helper, blinked >= 190, "a perfect Ender set blinked away from " + blinked + " of 200 arrows (only a spot-less try lets one hit)");
		pig.teleportTo(home.x, home.y, home.z);
		pig.damageCooldownTime = 0;
		check(helper, pig.hurtServer(helper.getLevel(), pig.damageSources().generic(), 2.0F), "a perfect Ender set dodged non-projectile damage");
		shooter.discard();
		pig.discard();
		helper.succeed();
	}

	private static boolean hidden(ItemStack stack) {
		var eq = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
		return io.github.bubba8587.bubbas_infused_gems.effect.HiddenArmor.isHidden(stack) && eq != null
				&& eq.assetId().orElseThrow().equals(io.github.bubba8587.bubbas_infused_gems.effect.HiddenArmor.EMPTY);
	}

	private static boolean shown(ItemStack stack, String asset) {
		var eq = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
		return !io.github.bubba8587.bubbas_infused_gems.effect.HiddenArmor.isHidden(stack) && eq != null
				&& eq.assetId().orElseThrow().identifier().getPath().equals(asset);
	}

	/**
	 * Owner (2026-11-15): the perfect Invisibility set hides its armor by pointing each piece at an empty equipment asset;
	 * one piece off restores them all, and a marked piece anywhere else (inventory, chest then pickup, ground, a crafting
	 * output) restores itself.
	 */
	@GameTest(maxTicks = 100)
	public void perfectInvisibilityHidesArmorAndEveryExitRestoresIt(GameTestHelper helper) {
		ServerPlayer player = kit(helper, "invisibility");
		for (var slot : GemTotals.ARMOR) {
			check(helper, hidden(player.getItemBySlot(slot)), "perfect Invisibility set: " + slot + " not hidden");
		}
		var eq = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
		check(helper, eq.slot() == net.minecraft.world.entity.EquipmentSlot.CHEST && eq.damageOnHurt(), "the hidden equippable lost its other fields");
		// One piece off: the rest restore; the removed piece (now in the inventory) restores on its tick.
		ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
		player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
		WornEffects.refresh(player);
		check(helper, shown(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), "netherite"), "chestplate still hidden after the helmet came off");
		player.getInventory().setItem(9, helmet);
		player.getInventory().tick();
		check(helper, shown(player.getInventory().getItem(9), "netherite"), "the removed helmet stayed hidden in the inventory");
		// Back on: hidden again.
		player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, player.getInventory().removeItemNoUpdate(9));
		WornEffects.refresh(player);
		check(helper, hidden(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)), "helmet not hidden when the set completed again");
		// A marked piece in a chest restores when picked up (next inventory tick); on the ground, on its first tick.
		ItemStack marked = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).copy();
		helper.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.CHEST);
		var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2), net.minecraft.world.level.block.entity.ChestBlockEntity.class);
		chest.setItem(0, marked.copy());
		ItemStack picked = chest.removeItemNoUpdate(0);
		player.getInventory().setItem(10, picked);
		player.getInventory().tick();
		check(helper, shown(player.getInventory().getItem(10), "netherite"), "a marked piece from a chest stayed hidden after pickup");
		player.getInventory().removeItemNoUpdate(10);
		var item = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), marked.copy());
		helper.getLevel().addFreshEntity(item);
		item.tick();
		check(helper, shown(item.getItem(), "netherite"), "a marked piece on the ground stayed hidden");
		item.discard();
		// Grindstone, smithing, anvil: their output container never holds a marked piece.
		var result = new net.minecraft.world.inventory.ResultContainer();
		result.setItem(0, marked.copy());
		check(helper, shown(result.getItem(0), "netherite"), "a crafting output kept the hidden marker");
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** A mob in the perfect Invisibility set drops its armor visible again. */
	@GameTest(maxTicks = 100)
	public void hiddenArmorDropsVisibleOnDeath(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Gem perfect = GemKit.gem("invisibility", 1.0);
		for (int i = 0; i < 4; i++) {
			zombie.setItemSlot(GemTotals.ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), perfect, perfect, perfect));
			zombie.setGuaranteedDrop(GemTotals.ARMOR[i]);
		}
		WornEffects.refresh(zombie);
		check(helper, hidden(zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)), "the zombie's perfect set is not hidden");
		zombie.hurtServer(helper.getLevel(), zombie.damageSources().genericKill(), Float.MAX_VALUE);
		helper.runAfterDelay(5, () -> {
			var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, zombie.getBoundingBox().inflate(4));
			int armor = 0;
			for (var d : drops) {
				if (d.getItem().has(net.minecraft.core.component.DataComponents.EQUIPPABLE)) {
					armor++;
					check(helper, shown(d.getItem(), "netherite"), "dropped " + d.getItem() + " still hidden");
				}
				d.discard();
			}
			check(helper, armor == 4, "dropped armor pieces: " + armor);
			helper.succeed();
		});
	}
}
