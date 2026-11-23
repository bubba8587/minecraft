// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** A payload from "another mod", registered through the same entrypoint key other mods use. */
public class TestPayload implements BeaconPayload {
	public static final int POWER = 5;
	public static volatile @Nullable BeaconContext lastApplied;

	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(Items.ECHO_SHARD);
	}

	@Override
	public int power(ItemStack stack) {
		return POWER;
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.literal("Test payload, Gem Power " + context.effectivePower()));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		lastApplied = context;
	}
}
