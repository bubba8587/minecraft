// dte:C4
package io.github.bubba8587.bubbas_infused_gems.station;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.SocketedGems;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Gem slots on gear (owner: 3 per armor piece, 2 per weapon), and taking gems back out. */
public final class Sockets {
	/** Armor that takes gems: every humanoid armor piece (data tag). */
	public static final TagKey<Item> ARMOR = TagKey.create(Registries.ITEM, BubbasInfusedGems.id("socketable_armor"));
	/** Weapons that take gems: swords, axes, spears, maces, tridents, bows, crossbows (data tag). */
	public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, BubbasInfusedGems.id("socketable_weapons"));

	private Sockets() {
	}

	public static int slots(ItemStack stack) {
		if (stack.is(ARMOR)) {
			return Balance.get().armorSlots;
		}
		return stack.is(WEAPONS) ? Balance.get().weaponSlots : 0;
	}

	public static SocketedGems gems(ItemStack stack) {
		return stack.getOrDefault(ModComponents.SOCKETED_GEMS, SocketedGems.EMPTY);
	}

	public static List<Gem> list(ItemStack stack) {
		return gems(stack).gems();
	}

	public static boolean hasGems(ItemStack stack) {
		return !list(stack).isEmpty();
	}

	public static boolean canSocket(ItemStack base, ItemStack gem) {
		return gem.is(ModItems.GEM) && gem.has(ModComponents.GEM) && base.getCount() == 1 && gems(base).size() < slots(base);
	}

	/** The base item with the gem added, or empty when it has no free slot. */
	public static ItemStack socket(ItemStack base, ItemStack gemStack) {
		if (!canSocket(base, gemStack)) {
			return ItemStack.EMPTY;
		}
		ItemStack out = base.copyWithCount(1);
		out.set(ModComponents.SOCKETED_GEMS, gems(base).with(gemStack.get(ModComponents.GEM)));
		return out;
	}

	/** The gem items in this item (empty list if none). */
	public static List<ItemStack> gemItems(ItemStack stack) {
		return list(stack).stream().map(GemEnchanting::finished).toList();
	}

	public static ItemStack withoutGems(ItemStack stack) {
		ItemStack out = stack.copy();
		out.remove(ModComponents.SOCKETED_GEMS);
		return out;
	}

	/** Gives the gems to the player (inventory, else dropped at their feet). */
	public static void give(Player player, List<ItemStack> gems) {
		for (ItemStack gem : gems) {
			player.getInventory().placeItemBackInInventory(gem, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}

	/** Drops a broken item's gems where its owner stands (owner: dropped, not lost). */
	public static void popOut(Entity owner, ItemStack broken) {
		if (owner.level() instanceof ServerLevel level) {
			for (ItemStack gem : gemItems(broken)) {
				owner.spawnAtLocation(level, gem);
			}
		}
	}
}
