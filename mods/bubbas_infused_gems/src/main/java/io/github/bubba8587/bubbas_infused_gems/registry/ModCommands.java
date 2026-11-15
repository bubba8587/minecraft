// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemDescribe;
import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * {@code /gems [player]} (owner): your combined gem effects. Every player can run it for themselves; looking at another
 * player needs operator rights. For each effect from all socketed armor gems: the total after stacking and the
 * material and matching-set factors, in game terms and as a share of the cap, drawbacks marked; then what each hit of
 * the held weapon applies.
 */
public final class ModCommands {
	private ModCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
				Commands.literal("gems")
						.executes(c -> show(c.getSource(), c.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player"))))));
	}

	/** The command's lines for this player (also used by the game test). */
	public static List<Component> lines(ServerPlayer player) {
		Balance b = Balance.get();
		List<Component> out = new ArrayList<>();
		GemTotals worn = GemTotals.ofArmor(player);
		out.add(Component.translatable(GemText.KEY + "command.armor", player.getDisplayName()).withStyle(ChatFormatting.GOLD));
		if (worn.isEmpty()) {
			out.add(Component.translatable(GemText.KEY + "command.none").withStyle(ChatFormatting.GRAY));
		}
		for (String key : worn.keys()) {
			double total = worn.totals().getOrDefault(key, 0.0);
			if (total > 0.0) {
				out.add(line(key, GemDescribe.worn(key, total), total / b.cap, false));
			}
			double drawback = worn.drawbacks().getOrDefault(key, 0.0);
			if (drawback > 0.0) {
				out.add(line(key, GemDescribe.worn(key, drawback), drawback / b.drawbackCap, true));
			}
		}
		ItemStack weapon = player.getMainHandItem();
		List<GemHits.Entry> hit = GemHits.plan(weapon);
		if (!hit.isEmpty()) {
			out.add(Component.translatable(GemText.KEY + "command.weapon", weapon.getHoverName()).withStyle(ChatFormatting.GOLD));
			for (GemHits.Entry e : hit) {
				out.add(line(e.key(), GemDescribe.hit(e), e.s() / (e.drawback() ? b.drawbackCap : b.cap), e.drawback()));
			}
		}
		return out;
	}

	private static MutableComponent line(String key, String value, double ofMax, boolean drawback) {
		MutableComponent line = Component.literal(" ").append(GemText.effectName(key)).append(": " + value + " ");
		line.append(Component.translatable(GemText.KEY + (drawback ? "command.of_max_drawback" : "command.of_max"), GemText.percent(ofMax))
				.withStyle(ChatFormatting.GRAY));
		return drawback ? line.withStyle(ChatFormatting.RED) : line.withColor(GemText.holder(key).value().getColor());
	}

	private static int show(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
		for (Component line : lines(player)) {
			source.sendSuccess(() -> line, false);
		}
		return 1;
	}
}
