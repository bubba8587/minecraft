// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * The optional link to Bubba's Reworked Beacons (like Totems & Artifacts': no dependency, compiled against it only).
 * With it, a Fabricator's floor beacon holds a <b>Fabricator Heart</b> (an echo shard marked as one), and the
 * beacon's payload ({@code compat/FabricatorBeaconPayload}, loaded only through that mod's entrypoint) gives the
 * debuffs over the pair's area; take the heart out or break the beacon and they end. Only {@code compat/} classes
 * touch the beacons API, and only when the mod is loaded.
 */
public final class FabricatorLink {
	public static final String BEACONS = "bubbas_reworked_beacons";
	private static final String HEART_KEY = "bubbas_reworked_mobs:fabricator_heart";

	private FabricatorLink() {
	}

	public static boolean beaconsLoaded() {
		return FabricLoader.getInstance().isModLoaded(BEACONS);
	}

	/** A Fabricator Heart: an echo shard named and marked as one. */
	public static ItemStack heart() {
		ItemStack stack = new ItemStack(Items.ECHO_SHARD);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(HEART_KEY, true);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.ITEM_NAME, Component.literal("Fabricator Heart"));
		return stack;
	}

	public static boolean isHeart(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return stack.is(Items.ECHO_SHARD) && data != null && data.copyTag().getBooleanOr(HEART_KEY, false);
	}

	static void placeHeart(ServerLevel level, BlockPos floorBeacon) {
		io.github.bubba8587.bubbas_reworked_mobs.compat.FabricatorBeaconPayload.placeHeart(level, floorBeacon);
	}
}
