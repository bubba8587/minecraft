// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.client.talisman;

import net.minecraft.client.core.registries.BuiltInClientRegistries;
import net.minecraft.core.Registry;
import com.mojang.serialization.MapCodec;
import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.talisman.GraveTalisman;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * The Grave Talisman's glow, worked out on the client for its own player (no packets): every few ticks, while the
 * player carries a talisman anywhere in the inventory, search for the nearest undead and keep the glow level. The item
 * model reads it through the {@code bubbas_totems_artifacts:grave_talisman_glow} numeric property (a range_dispatch in
 * assets/.../items/grave_talisman.json). Talismans held by other players, dropped or in frames stay dark.
 */
public final class GraveTalismanGlow {
	private static int level = GraveTalisman.OFF;
	private static int countdown;

	private GraveTalismanGlow() {
	}

	/** Registers the item model property; also called by datagen, which needs the codec to write the model. */
	public static void registerProperty() {
		if (!BuiltInClientRegistries.RANGE_SELECT_ITEM_MODEL_PROPERTY_TYPE.containsKey(BubbasTotemsArtifacts.id("grave_talisman_glow"))) {
			Registry.register(BuiltInClientRegistries.RANGE_SELECT_ITEM_MODEL_PROPERTY_TYPE, BubbasTotemsArtifacts.id("grave_talisman_glow"), Property.MAP_CODEC);
		}
	}

	public static void initialize() {
		registerProperty();
		ClientTickEvents.END_CLIENT_TICK.register(GraveTalismanGlow::tick);
	}

	/** The current glow level (GraveTalisman.OFF..BRIGHTEST) of the local player's talismans. */
	public static int level() {
		return level;
	}

	private static void tick(Minecraft mc) {
		Player player = mc.player;
		if (player == null || mc.level == null) {
			level = GraveTalisman.OFF;
			countdown = 0;
			return;
		}
		if (--countdown > 0) {
			return;
		}
		countdown = GraveTalisman.SEARCH_INTERVAL_TICKS;
		level = player.getInventory().contains(stack -> stack.is(ModItems.GRAVE_TALISMAN))
				? GraveTalisman.glowAt(mc.level, player.getEyePosition())
				: GraveTalisman.OFF;
	}

	/** Model property: the glow level for the local player's own talismans, 0 for everything else. */
	public record Property() implements RangeSelectItemModelProperty {
		public static final MapCodec<Property> MAP_CODEC = MapCodec.unit(new Property());

		@Override
		public float get(ItemStack stack, @Nullable ClientLevel clientLevel, @Nullable ItemOwner owner, int seed) {
			return owner != null && owner.asLivingEntity() == Minecraft.getInstance().player ? level : GraveTalisman.OFF;
		}

		@Override
		public MapCodec<Property> type() {
			return MAP_CODEC;
		}
	}
}
