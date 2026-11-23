// dte:C4,B16
package io.github.bubba8587.bubbas_infused_gems.client;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.effect.GemEntity;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class BubbasInfusedGemsClient implements ClientModInitializer {
	/** The local player's armor last time its gem totals were computed. */
	private static final ItemStack[] LAST_ARMOR = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
	private static LocalPlayer lastPlayer;
	private static java.util.List<io.github.bubba8587.bubbas_infused_gems.effect.GemBeacons.Received> lastBeacon;

	@Override
	public void onInitializeClient() {
		FabricLoader.getInstance().getModContainer(BubbasInfusedGems.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasInfusedGems.id(BubbasInfusedGems.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Infused Gems 32x (Faithful style)"), PackActivationType.NORMAL));
		ItemTintSources.ID_MAPPER.put(BubbasInfusedGems.id("gem_color"), GemColorSource.MAP_CODEC);
		// The local player moves on the client
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			LocalPlayer player = mc.player;
			if (player == null) {
				return;
			}
			boolean changed = player != lastPlayer;
			lastPlayer = player;
			for (int i = 0; i < 4; i++) {
				ItemStack now = player.getItemBySlot(GemTotals.ARMOR[i]);
				if (!ItemStack.matches(now, LAST_ARMOR[i])) {
					LAST_ARMOR[i] = now.copy();
					changed = true;
				}
			}
			// Gems from Reworked Beacons arrive as a synced attachment
			var beacon = player.getAttached(io.github.bubba8587.bubbas_infused_gems.registry.ModAttachments.BEACON_GEMS);
			if (beacon != lastBeacon || (beacon != null && beacon.stream().anyMatch(r -> r.until() <= player.level().getGameTime()))) {
				lastBeacon = beacon;
				changed = true;
			}
			if (changed) {
				((GemEntity) player).bubbas$setWornGems(GemTotals.ofArmor(player));
			}
		});
	}
}
