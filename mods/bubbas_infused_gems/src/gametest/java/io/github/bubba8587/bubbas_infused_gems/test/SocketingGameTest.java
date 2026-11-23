// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import io.github.bubba8587.bubbas_infused_gems.station.Sockets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Socketing at the smithing table. */
public class SocketingGameTest {
	private static final Gem SPEED = GemKit.gem("speed", 0.9);
	private static final Gem POISON = GemKit.gem("poison", 0.8, "slowness", 0.5, "speed");

	private static SmithingMenu smithing(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.SMITHING_TABLE);
		return new SmithingMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(pos)));
	}

	/** Puts base + gem in the table and returns the result slot's item. */
	private static ItemStack socket(SmithingMenu menu, ItemStack base, Gem gem) {
		menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(ItemStack.EMPTY);
		menu.getSlot(SmithingMenu.BASE_SLOT).set(base);
		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(GemKit.item(gem));
		return menu.getSlot(3).getItem().copy();
	}

	// Core test: registered by InfusedGemsCoreGameTest
	public void smithingTableSocketsUpToTheLimit(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		SmithingMenu menu = smithing(helper, player);
		check(helper, menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).mayPlace(GemKit.item(SPEED)), "smithing table refuses a gem");
		check(helper, !menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).mayPlace(GemKit.infused("speed", "strong",
				io.github.bubba8587.bubbas_infused_gems.gem.GemForm.LINGERING, 6)), "an unenchanted Infused Gem fits the gem slot");
		ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
		for (int i = 1; i <= 3; i++) {
			chest = socket(menu, chest, SPEED);
			check(helper, Sockets.list(chest).size() == i, "chestplate holds " + Sockets.list(chest).size() + " after " + i);
		}
		check(helper, socket(menu, chest, SPEED).isEmpty(), "a 4th gem fits armor");
		check(helper, menu.hasRecipeError() || menu.getSlot(3).getItem().isEmpty(), "full armor still offered a result");
		ItemStack sword = socket(menu, socket(menu, new ItemStack(Items.IRON_SWORD), POISON), POISON);
		check(helper, Sockets.list(sword).size() == 2 && socket(menu, sword, POISON).isEmpty(), "a sword holds " + Sockets.list(sword).size() + " gems, or a 3rd fits");
		// Taking the result uses up the gem and the base
		menu.getSlot(SmithingMenu.BASE_SLOT).set(new ItemStack(Items.IRON_HELMET));
		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(GemKit.item(SPEED));
		ItemStack result = menu.getSlot(3).getItem();
		menu.getSlot(3).onTake(player, result);
		check(helper, menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "gem not used up");
		check(helper, result.get(ModComponents.SOCKETED_GEMS).gems().getFirst().equals(SPEED), "socketed the wrong gem");
		menu.removed(player);
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** Every weapon kind takes two gems and no more; tools and elytras take none. */
	@GameTest
	public void everyWeaponKindTakesTwoGems(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		SmithingMenu menu = smithing(helper, player);
		for (Item weapon : List.of(Items.IRON_SWORD, Items.DIAMOND_AXE, Items.NETHERITE_SPEAR, Items.MACE, Items.TRIDENT, Items.BOW, Items.CROSSBOW)) {
			ItemStack w = new ItemStack(weapon);
			w = socket(menu, w, POISON);
			w = socket(menu, w, POISON);
			check(helper, Sockets.list(w).size() == 2, BuiltInRegistries.ITEM.getKey(weapon) + " holds " + Sockets.list(w).size());
			check(helper, socket(menu, w, POISON).isEmpty(), "a 3rd gem fits " + BuiltInRegistries.ITEM.getKey(weapon));
		}
		check(helper, socket(menu, new ItemStack(Items.IRON_PICKAXE), SPEED).isEmpty(), "a pickaxe took a gem");
		check(helper, socket(menu, new ItemStack(Items.ELYTRA), SPEED).isEmpty(), "an elytra took a gem");
		menu.removed(player);
		GemKit.remove(helper, player);
		helper.succeed();
	}

	@GameTest
	public void grindstoneReturnsAllGems(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getInventory().clearContent();
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.GRINDSTONE);
		GrindstoneMenu menu = new GrindstoneMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(pos)));
		ItemStack sword = GemKit.socketed(Items.DIAMOND_SWORD, POISON, GemKit.gem("weakness", 0.6));
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SHARPNESS), 3);
		sword.setDamageValue(100);
		menu.getSlot(0).set(sword);
		ItemStack result = menu.getSlot(2).getItem();
		check(helper, result.is(Items.DIAMOND_SWORD) && !Sockets.hasGems(result), "grindstone result still has gems: " + result);
		check(helper, result.getDamageValue() == 100, "durability changed");
		menu.getSlot(2).onTake(player, result);
		int gems = 0;
		for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
			if (s.is(ModItems.GEM)) {
				gems += s.getCount();
			}
		}
		check(helper, gems == 2, "player got " + gems + " gems back, expected 2");
		// Without enchantments it still strips the gems
		menu.getSlot(0).set(GemKit.socketed(Items.IRON_CHESTPLATE, SPEED));
		ItemStack plain = menu.getSlot(2).getItem();
		check(helper, plain.is(Items.IRON_CHESTPLATE) && !Sockets.hasGems(plain), "unenchanted gear: " + plain);
		menu.getSlot(0).set(ItemStack.EMPTY);
		menu.removed(player);
		GemKit.remove(helper, player);
		helper.succeed();
	}

	@GameTest
	public void brokenArmorDropsItsGems(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 2));
		ItemStack helmet = GemKit.socketed(Items.LEATHER_HELMET, SPEED, POISON, GemKit.gem("strength", 0.7));
		helmet.setDamageValue(helmet.getMaxDamage() - 1);
		zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
		zombie.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(5, zombie, EquipmentSlot.HEAD);
		check(helper, zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "helmet did not break");
		helper.succeedWhen(() -> {
			List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(zombie.blockPosition()).inflate(3),
					e -> e.getItem().is(ModItems.GEM));
			check(helper, drops.size() == 3, drops.size() + " gems dropped, expected 3");
			zombie.discard();
			drops.forEach(ItemEntity::discard);
		});
	}
}
