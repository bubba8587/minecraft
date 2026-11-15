// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.registry.ModCommands;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
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
}
