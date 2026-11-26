// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.storage.loot.LootTable;

/** Deep-sea fortresses: registration and the block-swap scope. */
public final class DeepseaFortresses {
	public static final StructureType<DeepseaFortressStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			BubbasWorldTweaks.id("deepsea_fortress"), () -> DeepseaFortressStructure.CODEC);
	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, BubbasWorldTweaks.id("deepsea_fortress"));
	public static final ResourceKey<LootTable> BARREL_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
			BubbasWorldTweaks.id("chests/deepsea_fortress"));

	private static final ThreadLocal<Boolean> PLACING = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private DeepseaFortresses() {
	}

	public static void initialize() {
	}

	/** Whether this thread is placing a deep-sea fortress's pieces. */
	public static boolean placing() {
		return PLACING.get();
	}

	/** Runs piece placement with the deep-sea block swap on. */
	public static void placing(Runnable placement) {
		boolean outer = PLACING.get();
		PLACING.set(Boolean.TRUE);
		try {
			placement.run();
		} finally {
			PLACING.set(outer);
		}
	}
}
