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

/** /gems: lists the wearer's combined armor gem effects. */
public class GemsCommandGameTest {
	// Core test: registered by InfusedGemsCoreGameTest
	public void gemsCommandListsCombinedEffects(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, GemKit.gem("speed", 1.0), GemKit.gem("speed", 1.0, null, 0, "hunger")));
		player.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem("speed", 1.0)));
		player.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.IRON_SWORD, GemKit.gem("poison", 0.8)));
		List<String> lines = ModCommands.lines(player).stream().map(Component::getString).toList();
		String all = String.join("\n", lines);
		check(helper, lines.stream().anyMatch(l -> l.contains("Speed +") && l.contains("%)") && !l.contains("of max")), "no speed line:\n" + all);
		// Drawbacks: just the effect and its values, in red
		check(helper, ModCommands.lines(player).stream().anyMatch(c -> c.getString().contains("Hunger") && !c.getString().contains("rawback")
				&& net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.RED).equals(c.getStyle().getColor())), "drawback not red:\n" + all);
		check(helper, lines.stream().anyMatch(l -> l.contains("Poison") && l.contains("HP/s for")), "no weapon hit line:\n" + all);
		check(helper, lines.stream().anyMatch(l -> l.contains("Target, on hit:")), "no weapon header:\n" + all);
		// TODO 26.4: re-check that /gems is registered and needs no permission (the dispatcher is no longer exposed)
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** /gems kit: a full netherite set of perfect gems reaches the cap, and /gems shows it. */
	@GameTest
	public void kitGivesAPerfectSet(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		// TODO 26.4: re-check that /gems kit is registered (the dispatcher is no longer exposed)
		try {
			ModCommands.kit(player.createCommandSourceStack(), "invisibility", "netherite");
		} catch (Exception e) {
			helper.fail("kit failed: " + e);
		}
		double s = io.github.bubba8587.bubbas_infused_gems.effect.WornEffects.of(player).value("invisibility");
		check(helper, Math.abs(s - io.github.bubba8587.bubbas_infused_gems.balance.Balance.get().effect("invisibility").wornCap()) < 1e-6, "kit total " + s);
		check(helper, player.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS), "no netherite boots");
		check(helper, player.getInventory().contains(new ItemStack(Items.IRON_HELMET)), "the old helmet was lost");
		check(helper, player.getInventory().contains(s2 -> s2.is(Items.NETHERITE_SWORD)), "no sword");
		List<String> lines = ModCommands.lines(player).stream().map(Component::getString).toList();
		check(helper, lines.stream().anyMatch(l -> l.contains("Invisibility") && l.contains("(100%)")), "/gems after the kit:\n" + String.join("\n", lines));
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
