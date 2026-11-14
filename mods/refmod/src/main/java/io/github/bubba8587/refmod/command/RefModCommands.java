// dte:B5,B6
package io.github.bubba8587.refmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.bubba8587.refmod.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * {@code /refmod stats [player]}: prints a player's lamp toggle count. Anyone may query themselves;
 * naming another player needs gamemaster permission. Returns the count as the command result.
 */
public final class RefModCommands {
	private RefModCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> register(dispatcher));
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("refmod")
				.then(Commands.literal("stats")
						.executes(ctx -> stats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(ctx -> stats(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
	}

	private static int stats(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
		int toggles = ModAttachments.getLampToggles(player);
		source.sendSuccess(() -> Component.translatable("commands.refmod.stats", player.getDisplayName(), toggles), false);
		return toggles;
	}
}
