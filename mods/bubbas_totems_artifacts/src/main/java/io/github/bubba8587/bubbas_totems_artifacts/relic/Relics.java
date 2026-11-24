// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.compat.SleeperHook;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModCreativeTabs;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

/** Wiring for relic maps and vaults (specs/totems-artifacts.md, "Relic maps"). */
public final class Relics {
	public static final DataComponentType<RelicShred> SHRED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BubbasTotemsArtifacts.id("relic_shred"),
			DataComponentType.<RelicShred>builder().persistent(RelicShred.CODEC).networkSynchronized(RelicShred.STREAM_CODEC).build());

	public static final Item RELIC_MAP_SHRED = item();

	public static final StructureType<RelicVaultStructure> VAULT_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			BubbasTotemsArtifacts.id("relic_vault"), () -> RelicVaultStructure.CODEC);
	public static final StructurePieceType VAULT_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			BubbasTotemsArtifacts.id("relic_vault"), (StructurePieceType.ContextlessType) RelicVaultPiece::new);

	private Relics() {
	}

	private static Item item() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasTotemsArtifacts.id("relic_map_shred"));
		return Registry.register(BuiltInRegistries.ITEM, key, new RelicMapShredItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).setId(key)));
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, BubbasTotemsArtifacts.id("relic_sites"), RelicVaultPlacement.CODEC);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, BubbasTotemsArtifacts.id("relic_shreds"), RelicShredRecipe.SERIALIZER);
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, BubbasTotemsArtifacts.id("random_relic_shred"), RelicShredFunction.MAP_CODEC);
		RelicLoot.initialize();
		RelicCommand.initialize();
		CreativeModeTabEvents.modifyOutputEvent(ModCreativeTabs.MAIN_KEY).register(output -> {
			for (int size : RelicNames.SIZES) {
				output.accept(RelicMaps.shred("", size, 1));
			}
		});
		SleeperHook.get().attachToNobleCrypts((cryptSeed, farWall) -> {
			RandomSource random = RandomSource.create(RandomSupport.mixStafford13(cryptSeed) ^ 0x7A017L);
			return random.nextFloat() < RelicTuning.NOBLE_CRYPT_VAULT_CHANCE ? RelicVaultPiece.behindCrypt(farWall, random.nextLong()) : null;
		});
	}
}
