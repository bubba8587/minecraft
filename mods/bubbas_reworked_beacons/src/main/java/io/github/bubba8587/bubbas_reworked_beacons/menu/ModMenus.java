// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.menu;

import io.github.bubba8587.bubbas_reworked_beacons.BubbasReworkedBeacons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final MenuType<BeaconItemMenu> BEACON = Registry.register(BuiltInRegistries.MENU, BubbasReworkedBeacons.id("beacon"),
			new MenuType<>(BeaconItemMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}

	public static void initialize() {
	}
}
