// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.registry.ModCommands;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * {@code /gems}: lists the wearer's combined armor gem effects (in-game value and share of the cap), marks drawbacks,
 * and says what each hit of the held weapon applies; the command is registered for everyone.
 */
public class GemsCommandGameTest {
	@GameTest
	public void gemsCommandListsCombinedEffects(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, GemKit.gem("speed", 1.0), GemKit.gem("speed", 1.0, null, 0, "hunger")));
		player.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem("speed", 1.0)));
		player.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.IRON_SWORD, GemKit.gem("poison", 0.8)));
		List<String> lines = ModCommands.lines(player).stream().map(Component::getString).toList();
		String all = String.join("\n", lines);
		check(helper, lines.stream().anyMatch(l -> l.contains("Speed: +") && l.contains("% speed") && l.contains("of max")), "no speed line:\n" + all);
		check(helper, lines.stream().anyMatch(l -> l.contains("Hunger") && l.contains("drawback")), "drawback not marked:\n" + all);
		check(helper, lines.stream().anyMatch(l -> l.contains("Poison") && l.contains("HP/s poison for")), "no weapon hit line:\n" + all);
		var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
		check(helper, dispatcher.getRoot().getChild("gems") != null, "/gems not registered");
		check(helper, dispatcher.getRoot().getChild("gems").getRequirement().test(player.createCommandSourceStack()), "/gems needs permission");
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** /gems kit (owner, for testing): a full netherite set of perfect gems reaches the cap, and /gems shows it. */
	@GameTest
	public void kitGivesAPerfectSet(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
		check(helper, dispatcher.getRoot().getChild("gems").getChild("kit") != null, "/gems kit not registered");
		try {
			ModCommands.kit(player.createCommandSourceStack(), "invisibility", "netherite");
		} catch (Exception e) {
			helper.fail("kit failed: " + e);
		}
		double s = io.github.bubba8587.bubbas_infused_gems.effect.WornEffects.of(player).value("invisibility");
		check(helper, Math.abs(s - io.github.bubba8587.bubbas_infused_gems.balance.Balance.get().cap) < 1e-6, "kit total " + s);
		check(helper, player.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS), "no netherite boots");
		check(helper, player.getInventory().contains(new ItemStack(Items.IRON_HELMET)), "the old helmet was lost");
		check(helper, player.getInventory().contains(s2 -> s2.is(Items.NETHERITE_SWORD)), "no sword");
		List<String> lines = ModCommands.lines(player).stream().map(Component::getString).toList();
		check(helper, lines.stream().anyMatch(l -> l.contains("Invisibility") && l.contains("100% of max")), "/gems after the kit:\n" + String.join("\n", lines));
		try {
			ModCommands.kit(player.createCommandSourceStack(), "speed", "gold");
		} catch (Exception e) {
			helper.fail("gold kit failed: " + e);
		}
		check(helper, player.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE), "gold kit");
		GemKit.remove(helper, player);
		helper.succeed();
	}
}
