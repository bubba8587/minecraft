// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Explorer maps (client part {@code maps}, seed 20261116): a cartographer's graveyard, camp and Fabricator map trades each
 * make a map with a target; the graveyard map is shot in hand (maps-graveyard).
 */
@SuppressWarnings("UnstableApiUsage")
public class MapsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("maps")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> {
					s.setWorldType(new WorldCreationUiState.WorldTypeEntry(
							s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
					s.setSeed("20261116");
					s.setGenerateStructures(true);
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				})
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			for (String trade : new String[] { "graveyard_map", "camp_map", "fabricator_map" }) {
				String result = server.computeOnServer(s -> {
					var level = s.overworld();
					var holder = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
							.get(ResourceKey.create(Registries.VILLAGER_TRADE, BubbasReworkedMobs.id("cartographer/" + trade)));
					if (holder.isEmpty()) {
						return "missing";
					}
					var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
					villager.snapTo(level.getRespawnData().pos().getX(), 80, level.getRespawnData().pos().getZ());
					LootContext loot = new LootContext.Builder(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, villager.position())
							.withParameter(LootContextParams.THIS_ENTITY, villager).withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
							.create(LootContextParamSets.VILLAGER_TRADE)).create(java.util.Optional.empty());
					MerchantOffer offer = holder.get().value().getOffer(loot);
					if (offer == null) {
						return "no offer (no destination found)";
					}
					ItemStack map = offer.getResult();
					var decorations = map.get(DataComponents.MAP_DECORATIONS);
					String target = decorations == null ? "none" : decorations.decorations().values().stream().map(d -> (int) d.x() + "," + (int) d.z())
							.findFirst().orElse("none");
					if (trade.equals("graveyard_map")) {
						s.getPlayerList().getPlayers().getFirst().getInventory().setItem(0, map.copy());
					}
					String fabricator = "";
					if (trade.equals("fabricator_map") && decorations != null && !decorations.decorations().isEmpty()) {
						var d = decorations.decorations().values().iterator().next();
						var site = io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.nearest(level,
								net.minecraft.core.BlockPos.containing(d.x(), 0, d.z()), 400);
						fabricator = site == null ? " (no Fabricator near the X)"
								: " (Fabricator " + Math.round(Math.hypot(site.pad().getX() - d.x(), site.pad().getZ() - d.z())) + " blocks from the X)";
					}
					return map.getHoverName().getString() + " -> " + target + " for " + offer.getCostA().getCount() + " emeralds" + fabricator;
				});
				System.out.println("[maps] " + trade + ": " + result);
				if (result.startsWith("missing") || result.startsWith("no offer") || result.contains("no Fabricator near")) {
					throw new AssertionError("trade " + trade + ": " + result);
				}
			}
			server.runCommand("tp @p ~ ~ ~ ~ 65");
			context.waitTicks(40);
			context.takeScreenshot("maps-graveyard");
		}
	}
}
