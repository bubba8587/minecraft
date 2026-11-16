// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.compat;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconSlots;
import io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorLink;
import io.github.bubba8587.bubbas_reworked_mobs.factory.Fabricators;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Fabricator Heart in a Bubba's Reworked Beacons beacon (specs/mob-factory.md, F7): every player in the beacon's
 * area (a Fabricator's paired box) gets Mining Fatigue III, Weakness and Slowness, so tunnelling in is slow. The only
 * class that touches the beacons API, loaded only through its entrypoint (so only with that mod installed).
 */
public final class FabricatorBeaconPayload implements BeaconPayload {
	@Override
	public boolean accepts(ItemStack stack) {
		return FabricatorLink.isHeart(stack);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.literal("Mining Fatigue III").withStyle(ChatFormatting.RED),
				Component.literal("Weakness").withStyle(ChatFormatting.RED),
				Component.literal("Slowness").withStyle(ChatFormatting.RED));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		for (LivingEntity e : context.livingEntities()) {
			if (e instanceof Player player && !player.isCreative()) {
				Fabricators.applyDebuffs(player, context.effectDurationTicks());
			}
		}
	}

	/** Puts a Fabricator Heart in the floor beacon (first time a Fabricator runs). */
	public static void placeHeart(ServerLevel level, BlockPos floorBeacon) {
		if (BeaconSlots.item(level, floorBeacon).isEmpty()) {
			BeaconSlots.setItem(level, floorBeacon, FabricatorLink.heart());
		}
	}
}
