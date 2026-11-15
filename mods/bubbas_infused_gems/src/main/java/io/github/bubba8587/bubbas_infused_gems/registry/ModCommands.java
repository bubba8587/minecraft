// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.SocketedGems;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
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
								.executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player"))))
						// /gems kit <effect> [material] (owner, for testing): a full perfect set and sword of one effect.
						.then(Commands.literal("kit")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("effect", StringArgumentType.word())
										.suggests((c, b) -> SharedSuggestionProvider.suggest(Balance.get().gemEffects, b))
										.executes(c -> kit(c.getSource(), StringArgumentType.getString(c, "effect"), "netherite"))
										.then(Commands.argument("material", StringArgumentType.word())
												.suggests((c, b) -> SharedSuggestionProvider.suggest(KIT_MATERIALS, b))
												.executes(c -> kit(c.getSource(), StringArgumentType.getString(c, "effect"),
														StringArgumentType.getString(c, "material"))))))));
	}

	/** Armor materials the kit can use (vanilla's item name prefixes follow). */
	public static final List<String> KIT_MATERIALS = List.of("leather", "chainmail", "copper", "iron", "gold", "diamond", "netherite");
	private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(Component.translatable(GemText.KEY + "command.kit.unknown"));
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}

	/**
	 * Puts a full armor set of this material on the player, 3 perfect gems of the effect in every piece (Infusion VI, tier
	 * 3, full strength, nothing else), and gives a matching sword with 2. Worn armor goes back to the inventory (or drops).
	 */
	public static int kit(CommandSourceStack source, String effect, String material) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (!Balance.get().gemEffects.contains(effect) || !KIT_MATERIALS.contains(material)) {
			throw UNKNOWN.create();
		}
		Gem perfect = new Gem(GemText.holder(effect), 6, 3, 1.0F, Optional.empty(), Optional.empty());
		String prefix = material.equals("gold") ? "golden" : material;
		for (int i = 0; i < 4; i++) {
			EquipmentSlot slot = io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR[i];
			ItemStack old = player.getItemBySlot(slot);
			ItemStack piece = new ItemStack(item(prefix + "_" + PIECES[i]));
			piece.set(ModComponents.SOCKETED_GEMS, SocketedGems.EMPTY.with(perfect).with(perfect).with(perfect));
			player.setItemSlot(slot, piece);
			if (!old.isEmpty()) {
				player.getInventory().placeItemBackInInventory(old, net.minecraft.util.Prediction.SERVER_ONLY);
			}
		}
		String sword = switch (material) {
			case "leather" -> "wooden_sword";
			case "chainmail" -> "stone_sword";
			case "gold" -> "golden_sword";
			default -> material + "_sword";
		};
		ItemStack weapon = new ItemStack(item(sword));
		weapon.set(ModComponents.SOCKETED_GEMS, SocketedGems.EMPTY.with(perfect).with(perfect));
		player.getInventory().placeItemBackInInventory(weapon, net.minecraft.util.Prediction.SERVER_ONLY);
		WornEffects.refresh(player);
		source.sendSystemMessage(Component.translatable(GemText.KEY + "command.kit", GemText.effectName(effect), material).withStyle(ChatFormatting.GOLD));
		return 1;
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
			out.add(l.withColor(io.github.bubba8587.bubbas_infused_gems.gem.GemColors.text(GemText.holder(key).value().getColor())));
		});
	}

	private static MutableComponent line(String key, String value, double ofMax, boolean drawback) {
		MutableComponent line = Component.literal(" ").append(GemText.effectName(key)).append(": " + value + " ");
		line.append(Component.translatable(GemText.KEY + (drawback ? "command.of_max_drawback" : "command.of_max"), GemText.percent(ofMax))
				.withStyle(ChatFormatting.GRAY));
		return drawback ? line.withStyle(ChatFormatting.RED) : line.withColor(io.github.bubba8587.bubbas_infused_gems.gem.GemColors.text(GemText.holder(key).value().getColor()));
	}

	private static int show(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
		// Straight to the asker, so it shows even with the send_command_feedback game rule off.
		for (Component line : lines(player)) {
			source.sendSystemMessage(line);
		}
		return 1;
	}
}
