// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import java.util.EnumMap;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** Registered blocks. Sleepers are the owner-approved B15 exception (specs/undead.md). */
public final class ModBlocks {
	public static final Map<UndeadVariant, SleeperBlock> SLEEPERS = new EnumMap<>(UndeadVariant.class);

	static {
		for (UndeadVariant variant : UndeadVariant.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, BubbasReworkedMobs.id(variant.sleeperId()));
			BlockBehaviour.Properties props = BlockBehaviour.Properties.of().setId(key)
					.mapColor(variant.mapColor()).sound(variant.sound()).strength(1.5F)
					.noOcclusion().pushReaction(PushReaction.POPPED)
					.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
			SLEEPERS.put(variant, Registry.register(BuiltInRegistries.BLOCK, key, new SleeperBlock(variant, props)));
		}
	}

	private ModBlocks() {
	}

	public static SleeperBlock sleeper(UndeadVariant variant) {
		return SLEEPERS.get(variant);
	}

	public static void initialize() {
	}
}
