// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

/** Deep-sea fortresses: registration and the block-swap scope. */
public final class DeepseaFortresses {
	public static final StructureType<DeepseaFortressStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			BubbasWorldTweaks.id("deepsea_fortress"), () -> DeepseaFortressStructure.CODEC);
	public static final StructurePieceType TEAR_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, BubbasWorldTweaks.id("deepsea_tear"),
			(StructurePieceType.ContextlessType) TearPiece::new);
	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, BubbasWorldTweaks.id("deepsea_fortress"));
	public static final ResourceKey<LootTable> BARREL_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
			BubbasWorldTweaks.id("chests/deepsea_fortress"));

	public static final ResourceKey<LootTable> FORTRESS_MAP = ResourceKey.create(Registries.LOOT_TABLE,
			BubbasWorldTweaks.id("items/deepsea_fortress_map"));
	/** Share of shipwreck buried-treasure maps that point to a deep-sea fortress instead. */
	public static final float SHIPWRECK_MAP_SHARE = 0.25F;

	private static final ThreadLocal<Boolean> PLACING = ThreadLocal.withInitial(() -> Boolean.FALSE);
	private static final ThreadLocal<List<BlockPos>> PILLAR_FEET = ThreadLocal.withInitial(ArrayList::new);

	private DeepseaFortresses() {
	}

	public static void initialize() {
		LootTableEvents.MODIFY_DROPS.register(DeepseaFortresses::shipwreckMap);
	}

	/** Some shipwreck buried-treasure maps become maps to the nearest deep-sea fortress. */
	private static void shipwreckMap(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		if (!table.is(BuiltInLootTables.SHIPWRECK_MAP) || context.getRandom().nextFloat() >= SHIPWRECK_MAP_SHARE) {
			return;
		}
		for (int i = 0; i < drops.size(); i++) {
			if (drops.get(i).is(Items.BURIED_TREASURE_MAP)) {
				List<ItemStack> map = new ArrayList<>();
				context.getLevel().getServer().reloadableRegistries().getLootTable(FORTRESS_MAP).getRandomItems(context, map::add);
				if (!map.isEmpty()) {
					drops.set(i, map.getFirst());
				}
				return;
			}
		}
	}

	/** Whether this thread is placing a deep-sea fortress's pieces. */
	public static boolean placing() {
		return PLACING.get();
	}

	/** Runs piece placement with the deep-sea block swap on. */
	public static void placing(Runnable placement) {
		boolean outer = PLACING.get();
		List<BlockPos> outerFeet = PILLAR_FEET.get();
		PLACING.set(Boolean.TRUE);
		PILLAR_FEET.set(new ArrayList<>());
		try {
			placement.run();
		} finally {
			PLACING.set(outer);
			PILLAR_FEET.set(outerFeet);
		}
	}

	/** Notes the bottom block of a support pillar placed in this chunk. */
	public static void pillarFoot(BlockPos pos) {
		PILLAR_FEET.get().add(pos.immutable());
	}

	/** Bottom blocks of the support pillars placed so far in this chunk. */
	public static List<BlockPos> pillarFeet() {
		return PILLAR_FEET.get();
	}
}
