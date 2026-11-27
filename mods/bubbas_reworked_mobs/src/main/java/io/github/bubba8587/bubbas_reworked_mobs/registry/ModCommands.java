// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayCommand;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnCommand;
import io.github.bubba8587.bubbas_reworked_mobs.factory.FactoryCommand;
import net.minecraft.commands.CommandPermissionLevels;
import net.minecraft.commands.Commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** {@code /bubbas_reworked_mobs ...}: building and testing helpers for ops (permission level 2, like /setblock). */
public final class ModCommands {
	private ModCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
				Commands.literal(BubbasReworkedMobs.MOD_ID)
						.requires(Commands.hasPermission(CommandPermissionLevels.LEVEL_GAMEMASTERS))
						.then(LootDisplayCommand.build(context))
						.then(CairnCommand.build())
						.then(FactoryCommand.build())
						.then(io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand.build())
						.then(io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand.build())
						.then(io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPostCommand.build())
						.then(io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderCommand.build())));
	}
}
