// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Owner and test commands: {@code /bubbas_totems_artifacts relic_map <name> <4|9>} gives that relic's finished map;
 * {@code /bubbas_totems_artifacts relic_shred <name> <4|9> <count>} gives a shred; {@code /bubbas_totems_artifacts
 * relic_vault <small|grand|crypt>} builds a vault with its door at your feet, the room to the south.
 */
public final class RelicCommand {
	private RelicCommand() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
				Commands.literal(BubbasTotemsArtifacts.MOD_ID).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("relic_map").then(name().then(Commands.argument("size", IntegerArgumentType.integer(4, 9))
								.executes(c -> give(c, IntegerArgumentType.getInteger(c, "size"))))))
						.then(Commands.literal("relic_shred").then(name().then(Commands.argument("size", IntegerArgumentType.integer(4, 9))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 9)).executes(c -> giveShred(c))))))
						.then(Commands.literal("relic_vault").then(Commands.argument("kind", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] { "small", "grand", "crypt" }, b))
								.executes(RelicCommand::vault)))));
	}

	private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> name() {
		return Commands.argument("name", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(RelicNames.ALL, b));
	}

	private static int give(CommandContext<CommandSourceStack> c, int size) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayerOrException();
		ItemStack map = RelicMaps.map(c.getSource().getLevel(), new RelicShred(StringArgumentType.getString(c, "name"), size, size));
		if (map.isEmpty()) {
			c.getSource().sendFailure(Component.literal("No such relic (names: adjective_noun, sizes 4 or 9)."));
			return 0;
		}
		player.getInventory().add(map);
		return 1;
	}

	private static int giveShred(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayerOrException();
		player.getInventory().add(RelicMaps.shred(StringArgumentType.getString(c, "name"), IntegerArgumentType.getInteger(c, "size"),
				IntegerArgumentType.getInteger(c, "count")));
		return 1;
	}

	private static int vault(CommandContext<CommandSourceStack> c) {
		String kind = StringArgumentType.getString(c, "kind");
		RelicVaultPiece.Kind k = switch (kind) {
			case "grand" -> RelicVaultPiece.Kind.GRAND;
			case "crypt" -> RelicVaultPiece.Kind.CRYPT;
			default -> RelicVaultPiece.Kind.SMALL;
		};
		BlockPos door = BlockPos.containing(c.getSource().getPosition());
		RelicVaultPiece piece = new RelicVaultPiece(k, k == RelicVaultPiece.Kind.CRYPT ? "" : RelicNames.ALL.get(0), door, c.getSource().getLevel().getRandom().nextLong());
		piece.buildNow(c.getSource().getLevel());
		c.getSource().sendSuccess(() -> Component.literal("Built a " + kind + " relic vault, door at " + door.toShortString() + "."), true);
		return 1;
	}
}
