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
				lines(out, key, total, GemDescribe.worn(key, total), "");
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
				if (e.drawback()) {
					out.add(line(e.key(), GemDescribe.hit(e), e.s() / b.drawbackCap, true));
				} else {
					lines(out, e.key(), e.s(), GemDescribe.hit(e), String.format(java.util.Locale.ROOT, " for %.1f s", GemHits.duration(e) / 20.0));
				}
			}
		}
		return out;
	}

	/** A combined gem (Turtle Master) shows one line per part, each a share of the cap; any other gem one line. */
	private static void lines(List<Component> out, String key, double s, String value, String hitSuffix) {
		Balance b = Balance.get();
		Balance.EffectData e = b.effect(key);
		if (e.parts().isEmpty()) {
			out.add(line(key, value, s / b.cap, false));
			return;
		}
		double p = hitSuffix.isEmpty() ? s : GemHits.potency(new GemHits.Entry(key, s, false));
		e.parts().forEach((part, amount) -> {
			MutableComponent name = Component.translatable(GemText.KEY + "command.part", GemText.effectName(key), GemText.effectName(part));
			MutableComponent l = Component.literal(" ").append(name).append(": " + GemDescribe.part(part, amount, p) + hitSuffix + " ");
			l.append(Component.translatable(GemText.KEY + "command.of_max", GemText.percent(s / b.cap)).withStyle(ChatFormatting.GRAY));
			out.add(l.withColor(GemText.holder(key).value().getColor()));
		});
	}

	private static MutableComponent line(String key, String value, double ofMax, boolean drawback) {
		MutableComponent line = Component.literal(" ").append(GemText.effectName(key)).append(": " + value + " ");
		line.append(Component.translatable(GemText.KEY + (drawback ? "command.of_max_drawback" : "command.of_max"), GemText.percent(ofMax))
				.withStyle(ChatFormatting.GRAY));
		return drawback ? line.withStyle(ChatFormatting.RED) : line.withColor(GemText.holder(key).value().getColor());
	}

	private static int show(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
		// Straight to the asker, so it shows even with the send_command_feedback game rule off.
		for (Component line : lines(player)) {
			source.sendSystemMessage(line);
		}
		return 1;
	}
}
